package be.kommaboard.kareer.user.lib.dto.response

import java.time.ZonedDateTime

data class UserDTO(
    val uuid: String,
    val creationDate: ZonedDateTime,
    val email: String,
    val fullName: String,
    val shortName: String,
    val organizationUuid: String?,
    val role: String,
    val status: String,
)
