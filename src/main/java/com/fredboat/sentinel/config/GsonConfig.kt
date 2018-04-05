package com.fredboat.sentinel.config

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.LongSerializationPolicy
import org.springframework.context.annotation.Configuration

@Configuration
open class GsonConfig {

    fun buildGson(): Gson = GsonBuilder()
            .setLongSerializationPolicy(LongSerializationPolicy.STRING)
            .create()

}