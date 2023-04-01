package be.kommaboard.kareer.user.lib.dto.request

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern

data class UpdateInvitationDTO(

    @NotBlank(message = "{UpdateInvitationDTO.status.NotBlank}")
    @Pattern(message = "{UpdateInvitationDTO.status.Pattern}", regexp = "^(pending|accepted|declined|retracted)$", flags = [Pattern.Flag.CASE_INSENSITIVE])
    val status: String?,
)
