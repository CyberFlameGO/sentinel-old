/*
 * Copyright © 2018 Frederik Mikkelsen <fred at frederikam.com>
 * FredBoat microservice for handling JDA and Lavalink over RabbitMQ.
 *
 * This program is licensed under GNU AGPLv3 under no warranty.
 */

package com.fredboat.sentinel.test

import com.fredboat.sentinel.ApplicationState
import com.fredboat.sentinel.main
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.ParameterContext
import org.junit.jupiter.api.extension.ParameterResolver
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationContext
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class SharedSpringContext : ParameterResolver, BeforeAllCallback {

    companion object {
        private val log: Logger = LoggerFactory.getLogger(SharedSpringContext::class.java)
        private var application: ApplicationContext? = null
    }

    override fun beforeAll(context: ExtensionContext) {
        if (application != null) return // Don't start the application again

        log.info("Initializing test context")
        val latch = CountDownLatch(1)
        ApplicationState.isTesting = true
        ApplicationState.integrationCallback = {
            application = it
            latch.countDown()
        }
        main(emptyArray())
        val success = latch.await(10, TimeUnit.SECONDS)
        if (!success) throw IllegalStateException("Application startup timed out")
        log.info("Successfully initialized test context ${application!!.javaClass.simpleName}")
    }

    override fun supportsParameter(parameterContext: ParameterContext, extensionContext: ExtensionContext): Boolean {
        return application!!.getBean(parameterContext.parameter.type) != null
    }

    override fun resolveParameter(parameterContext: ParameterContext, extensionContext: ExtensionContext): Any {
        return application!!.getBean(parameterContext.parameter.type)
    }

}