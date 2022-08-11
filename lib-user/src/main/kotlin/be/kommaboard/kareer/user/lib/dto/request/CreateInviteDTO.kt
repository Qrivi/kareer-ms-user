package be.kommaboard.kareer.user.lib.dto.request

import javax.validation.constraints.Email
import javax.validation.constraints.NotBlank
import javax.validation.constraints.Size

data class CreateInviteDTO(

    @get:NotBlank(message = "{CreateInviteDTO.email.NotBlank}")
    @get:Size(message = "{CreateInviteDTO.email.Size}", max = 100)
    @get:Email(message = "{CreateInviteDTO.email.Email}")
    val email: String?,

    @get:NotBlank(message = "{CreateInviteDTO.lastName.NotBlank}")
    @get:Size(message = "{CreateInviteDTO.lastName.Size}", max = 100)
    val lastName: String?,

    @get:NotBlank(message = "{CreateInviteDTO.firstName.NotBlank}")
    @get:Size(message = "{CreateInviteDTO.firstName.Size}", max = 100)
    val firstName: String?,
)
