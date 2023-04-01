package be.kommaboard.kareer.user.lib.dto.request

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class CreateInvitationDTO(

    @get:NotBlank(message = "{CreateInvitationDTO.email.NotBlank}")
    @get:Size(message = "{CreateInvitationDTO.email.Size}", max = 100)
    @get:Email(message = "{CreateInvitationDTO.email.Email}")
    val email: String?,

    @get:NotBlank(message = "{CreateInvitationDTO.lastName.NotBlank}")
    @get:Size(message = "{CreateInvitationDTO.lastName.Size}", max = 100)
    val lastName: String?,

    @get:NotBlank(message = "{CreateInvitationDTO.firstName.NotBlank}")
    @get:Size(message = "{CreateInvitationDTO.firstName.Size}", max = 100)
    val firstName: String?,
)
