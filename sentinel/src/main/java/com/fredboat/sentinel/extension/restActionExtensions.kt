package com.fredboat.sentinel.extension

import com.fredboat.sentinel.metrics.Counters
import net.dv8tion.jda.core.exceptions.ErrorResponseException
import net.dv8tion.jda.core.requests.RestAction
import java.util.concurrent.TimeUnit

fun <T> RestAction<T>.queue(name: String) { toFuture(name) }
fun <T> RestAction<T>.complete(name: String): T = toFuture(name).get(30, TimeUnit.SECONDS)

private fun <T> RestAction<T>.toFuture(name: String) = submit().whenComplete { _, t ->
    if (t == null) {
        Counters.successfulRestActions.labels(name).inc()
        return@whenComplete
    }
    val errCode = (t as? ErrorResponseException)?.errorCode?.toString() ?: "none"
    Counters.failedRestActions.labels(name, errCode)
}.toCompletableFuture()!!