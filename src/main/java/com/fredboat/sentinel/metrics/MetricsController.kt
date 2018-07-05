package com.fredboat.sentinel.metrics

import ch.qos.logback.classic.LoggerContext
import com.fredboat.sentinel.metrics.collectors.ShardStatusCollector
import io.prometheus.client.hotspot.DefaultExports
import io.prometheus.client.logback.InstrumentedAppender
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Controller

@Controller
class MetricsController(prometheusAppender: InstrumentedAppender, shardStatusCollector: ShardStatusCollector) {
    companion object {
        private val log: Logger = LoggerFactory.getLogger(MetricsController::class.java)
    }

    init {
        log.info("Setting up metrics")

        //log metrics
        val factory = LoggerFactory.getILoggerFactory() as LoggerContext
        val root = factory.getLogger(Logger.ROOT_LOGGER_NAME)
        prometheusAppender.context = root.loggerContext
        prometheusAppender.start()
        root.addAppender(prometheusAppender)

        //jvm (hotspot) metrics
        DefaultExports.initialize()

        shardStatusCollector.register<ShardStatusCollector>()

        log.info("Metrics set up")
    }

}