package com.fredboat.sentinel.entities

data class User(
        val id: String,
        val name: String,
        val discrim: Short
)

data class Member(
        val id: String,
        val name: String,
        val discrim: Short,
        val voiceChannel: String?
)