package be.kommaboard.kareer.user.lib.dto.response

import java.time.ZonedDateTime
import java.util.UUID

data class UserDTO(
    val uuid: UUID,
    val creationDate: ZonedDateTime,
    val email: String,
    val phone: String?,
    val lastName: String,
    val firstName: String,
    val nickname: String,
    val organizationUuid: UUID?,
    val role: String,
    val status: String,
    val avatarUrl: String?,
    val bannerUrl: String?,
)
