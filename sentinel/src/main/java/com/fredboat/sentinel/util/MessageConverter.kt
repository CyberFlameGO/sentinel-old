package com.fredboat.sentinel.util

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule

class MessageConverter {
    private val mapper = ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .registerKotlinModule()

    fun fromJson(a: Any): Pair<ByteArray, HashMap<String, Any>> {
        val body = mapper.writeValueAsBytes(a)
        val builder = HashMap<String, Any>()
        builder["__TypeId__"] = a.javaClass.name
        return body to builder
    }
}