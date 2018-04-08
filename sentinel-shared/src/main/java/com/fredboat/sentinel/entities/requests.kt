package com.fredboat.sentinel.entities

data class UsersRequest(
        val shard: Int
)

data class UsersResponse(
        val users: List<User>) {

    override fun toString() = "UsersResponse(users.size=${users.size})"
}