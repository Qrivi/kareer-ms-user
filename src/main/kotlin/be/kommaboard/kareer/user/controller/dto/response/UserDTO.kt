package be.kommaboard.kareer.user.controller.dto.response

import be.kommaboard.kareer.common.response.dto.ResponseDTO
import be.kommaboard.kareer.user.repository.entity.User
import java.time.ZonedDateTime

data class UserDTO(
    val uuid: String,
    val creationDate: ZonedDateTime,
    val email: String,
    val name: String,
    val alias: String,
    val role: String,
    val status: String,
) : ResponseDTO() {

    constructor(user: User) : this(
        uuid = user.uuid.toString(),
        creationDate = user.creationDate,
        email = user.email,
        name = user.name,
        alias = user.alias,
        role = user.role.name,
        status = user.status.name,
    )
}
