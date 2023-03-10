package be.kommaboard.kareer.user.lib.dto.response

import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.UUID

data class UserDTO(
    val uuid: UUID,
    val slug: String?,
    val organizationUuid: UUID?,
    val creationDate: ZonedDateTime,
    val role: String,
    val status: String,
    val email: String,
    val phone: String?,
    val lastName: String,
    val firstName: String,
    val nickname: String?,
    val title: String,
    val birthday: LocalDate?,
    val avatarUrl: String?,
    val bannerUrl: String?,
    val preferences: String?,
)
