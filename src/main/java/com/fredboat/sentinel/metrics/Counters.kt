package com.fredboat.sentinel.metrics

import io.prometheus.client.Counter

object Counters {

    // ################################################################################
    // ##                              JDA Stats
    // ################################################################################

    val failedRequests = Counter.build()
            .name("requests_failed_total")
            .help("All failed requests consumed by Sentinel")
            .labelNames("class")
            .register()!!

    val jdaEvents = Counter.build()
            .name("jda_events_received_total")
            .help("All events that JDA provides us with by class")
            .labelNames("class") //GuildJoinedEvent, MessageReceivedEvent, ReconnectEvent etc
            .register()!!

    val successfulRestActions = Counter.build()
            .name("jda_restactions_successful_total")
            .help("Total successful JDA restactions sent by FredBoat")
            .labelNames("restaction") // sendMessage, deleteMessage, sendTyping etc
            .register()!!

    val failedRestActions = Counter.build()
            .name("jda_restactions_failed_total")
            .help("Total failed JDA restactions sent by FredBoat")
            .labelNames("restaction", "error_response_code") //Use the error response codes like: 50013, 10008 etc
            .register()!!

}