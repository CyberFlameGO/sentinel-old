package com.fredboat.sentinel.extension

import com.fredboat.sentinel.metrics.Counters
import net.dv8tion.jda.core.requests.RestAction
import java.util.concurrent.CompletableFuture

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

fun <T> RestAction<T>.toFuture(name: String): CompletableFuture<T> {
    return submit().whenComplete { _, t ->
        val counter = if (t == null) Counters.successfulRestActions else Counters.failedRestActions
        counter.labels(name).inc()
    }.toCompletableFuture()
}