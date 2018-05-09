@file:Suppress("unused")

package com.fredboat.sentinel.entities

interface IMessage {
    var content: String
}

data class Message(override var content: String) : IMessage

// Embed builder originally by Frostbyte, but is heavily modified
data class Embed(
        override var content: String = "",
        var title: String? = null,
        var url: String? = null,
        var description: String? = null,
        var color: Int? = null,
        var timestamp: Long? = null,
        var footer: Footer? = null,
        var thumbnail: String? = null,
        var image: String? = null,
        var author: Author? = null,
        var fields: MutableList<Field> = mutableListOf()
) : IMessage

data class Field(
        var title: String = "",
        var body: String = "",
        var inline: Boolean = false
)

data class Footer(
        var iconUrl: String? = null,
        var text: String? = null
)

data class Author(
        var name: String = "",
        var url: String? = null,
        var iconUrl: String? = null
)

inline fun embed(block: Embed.() -> Unit): Embed = Embed().apply(block)

inline fun Embed.footer(block: Footer.()->Unit) {
    footer = Footer().apply(block)
}

inline fun Embed.author(block: Author.() -> Unit) {
    author = Author().apply(block)
}

inline fun Embed.field(block: Field.()->Unit) {
    fields.add(Field().apply(block))
}
