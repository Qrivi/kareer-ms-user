package be.kommaboard.kareer.user.lib.dto.response

import java.time.ZonedDateTime
import java.util.UUID

data class UserDTO(
    val uuid: UUID,
    val creationDate: ZonedDateTime,
    val role: String,
    val status: String,
    val email: String,
    val lastName: String,
    val firstName: String,
    val nickname: String?,
    val slug: String?,
    val details: UserDetailsDTO?,
    val avatarUrl: String?,
    val bannerUrl: String?,
    val preferences: String?,
)
