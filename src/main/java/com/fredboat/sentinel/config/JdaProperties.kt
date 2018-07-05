package com.fredboat.sentinel.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component
@ConfigurationProperties(prefix = "sentinel")
data class JdaProperties(
        var discordToken: String = "",
        var shardStart: Int = 0,
        var shardEnd: Int = 0,
        var shardCount: Int = 1
)