package be.kommaboard.kareer.user.lib.dto.request

import be.kommaboard.kareer.user.lib.constraint.AssignableInviteStatus
import be.kommaboard.kareer.user.lib.constraint.NotCommon
import be.kommaboard.kareer.user.lib.constraint.NotSimple
import javax.validation.constraints.NotBlank
import javax.validation.constraints.Size

data class UpdateInviteDTO(

    @get:NotBlank(message = "{UpdateInviteDTO.status.NotBlank}")
    @get:AssignableInviteStatus(message = "{UpdateInviteDTO.status.AssignableInviteStatus}")
    val status: String?,
)
