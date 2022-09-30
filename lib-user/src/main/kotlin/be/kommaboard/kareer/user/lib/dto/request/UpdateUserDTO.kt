package be.kommaboard.kareer.user.lib.dto.request

import be.kommaboard.kareer.user.lib.constraint.AssignableRole
import be.kommaboard.kareer.user.lib.constraint.NotCommon
import be.kommaboard.kareer.user.lib.constraint.NotSimple
import javax.validation.constraints.Email
import javax.validation.constraints.Size

data class UpdateUserDTO(

    @get:Size(message = "{UpdateUserDTO.email.Size}", max = 100)
    @get:Email(message = "{UpdateUserDTO.email.Email}")
    val email: String?,

    @get:Size(message = "{UpdateUserDTO.password.size}", min = 8)
    @get:NotCommon(message = "{UpdateUserDTO.password.NotCommon}")
    @get:NotSimple(message = "{UpdateUserDTO.password.NotSimple}")
    val password: String?,

    @get:Size(message = "{UpdateUserDTO.lastName.Size}", max = 100)
    val lastName: String?,

    @get:Size(message = "{UpdateUserDTO.firstName.Size}", max = 100)
    val firstName: String?,

    @get:Size(message = "{UpdateUserDTO.nickname.Size}", max = 100)
    val nickname: String?,

    @get:AssignableRole(message = "{UpdateUserDTO.role.AssignableRole}")
    val role: String?,
)
