package com.fredboat.sentinel.extension

import com.fredboat.sentinel.metrics.Counters
import net.dv8tion.jda.core.exceptions.ErrorResponseException
import net.dv8tion.jda.core.requests.RestAction

/*
fun <T> RestAction<T>.toMono(name: String) = Mono.create<T> { sink ->
    this.queue(
            { if (it != null) sink.success(it) else sink.success() },
            { sink.error(it) }
    )
}
        .doOnError { Counters.failedRestActions.labels(name).inc() }
        .doOnSuccess { Counters.successfulRestActions.labels(name).inc() }*/

fun <T> RestAction<T>.queue(name: String) = toFuture(name)

fun <T> RestAction<T>.toFuture(name: String) = submit().whenComplete { _, t ->
    if (t == null) {
        Counters.successfulRestActions.labels(name).inc()
        return@whenComplete
    }
    val errCode = (t as? ErrorResponseException)?.errorCode?.toString() ?: "none"
    Counters.failedRestActions.labels(name, errCode)
}.toCompletableFuture()!!