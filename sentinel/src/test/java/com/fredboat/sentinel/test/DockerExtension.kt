package com.fredboat.sentinel.test

import com.palantir.docker.compose.DockerComposeRule
import com.palantir.docker.compose.configuration.ProjectName
import com.palantir.docker.compose.configuration.ShutdownStrategy
import com.palantir.docker.compose.connection.Container
import com.palantir.docker.compose.connection.waiting.HealthChecks
import com.palantir.docker.compose.connection.waiting.SuccessOrFailure
import com.rabbitmq.client.Connection
import com.rabbitmq.client.ConnectionFactory
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.ExtensionContext
import org.slf4j.LoggerFactory


/**
 * Created by napster on 30.03.18.
 *
 * This extension will create necessary dependencies before the first test that is using this extension is run,
 * and will clean up the docker container during shutdown. The database will be reused between all tests using this
 * extension, with setup run only once.
 * Do not kill the test execution via SIGKILL (this happens when running with IntelliJ's debug mode and clicking the
 * stop button), or else you might end up with orphaned docker containers on your machine.
 */
class DockerExtension : BeforeAllCallback {
    init {
        //cant use AfterAllCallback#afterAll because that's too early (spring context is still alive) and leads to exception spam
        Runtime.getRuntime().addShutdownHook(Thread({ docker.after() }, "Docker container shutdown hook"))
    }

    companion object {
        private val log = LoggerFactory.getLogger(DockerExtension::class.java)

        internal var docker: DockerComposeRule = DockerComposeRule.builder().run {
            pullOnStartup(true)
            file("src/test/resources/docker-compose.yaml")
            projectName(ProjectName.fromString("integration"))
            shutdownStrategy(identifyShutdownStrategy())
            if (System.getProperty("skipRabbit", "<none>") != "<none>" ) {
                log.info("Skipping rabbit service")
            } else {
                waitingForService("rabbitmq", HealthChecks.toHaveAllPortsOpen())
                waitingForService("rabbitmq") { checkRabbitmq(it) }
            }
            build()
        }

        private var hasSetup = false

        private fun identifyShutdownStrategy(): ShutdownStrategy {
            val keepPostgresContainer = System.getProperty("keepDocker", "false")
            if ("true".equals(keepPostgresContainer, ignoreCase = true)) {
                log.warn("Keeping the containers after the tests. Do NOT use this option in a CI environment, this " + "is meant to speed up repeatedly running tests in development only.")
                return ShutdownStrategy.SKIP
            }

            return ShutdownStrategy.GRACEFUL
        }

        private fun checkRabbitmq(container: Container): SuccessOrFailure {
            try {
                val id = docker.dockerCompose().id(container)
                if (!id.isPresent) {
                    return SuccessOrFailure.failure("no id on container")
                }
                val factory = ConnectionFactory()
                var conn: Connection? = null
                try {
                    // This will fail if rabbit is not ready yet
                    conn = factory.newConnection()
                } finally {
                    conn?.close()
                }
                return SuccessOrFailure.success()
            } catch (e: Exception) {
                log.error("Failed health check for ${container.containerName}: ${e.message}")
                return SuccessOrFailure.fromException(e)
            }
        }
    }

    override fun beforeAll(context: ExtensionContext) {
        if (hasSetup) return
        val timeStart = System.currentTimeMillis()
        try {
            docker.before()
        } catch (e: Exception) {
            log.error("Docker failed, exiting...", e)
            System.exit(-1)
        }

        hasSetup = true
        log.info("Started docker in " + (System.currentTimeMillis() - timeStart) + "ms")
    }
}