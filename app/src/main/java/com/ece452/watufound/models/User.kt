package com.ece452.watufound.models

enum class UserRole {
    USER,
    MODERATOR,
    ADMIN
}

data class User(
    val uid: String = "",
    val displayName: String = "",
    val email: String = "",
    val role: UserRole = UserRole.USER,
    val isActive: Boolean = true,
    val isBanned: Boolean = false,
    val banReason: String = "",
    val bannedAt: Long = 0,
    val bannedBy: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val lastLoginAt: Long = System.currentTimeMillis()
) 