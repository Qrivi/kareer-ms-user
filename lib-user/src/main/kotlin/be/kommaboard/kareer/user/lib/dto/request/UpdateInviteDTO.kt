package be.kommaboard.kareer.user.lib.dto.request

import javax.validation.constraints.NotBlank

data class UpdateInviteDTO(

    @get:NotBlank(message = "{UpdateInviteDTO.status.NotBlank}")
    val status: String?,
)
