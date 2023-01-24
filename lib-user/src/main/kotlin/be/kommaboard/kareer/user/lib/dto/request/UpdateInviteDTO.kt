package be.kommaboard.kareer.user.lib.dto.request

import javax.validation.constraints.NotBlank
import javax.validation.constraints.Pattern

data class UpdateInviteDTO(

    @NotBlank(message = "{UpdateInviteDTO.status.NotBlank}")
    @Pattern(message = "{UpdateInviteDTO.status.Pattern}", regexp = "^(pending|accepted|declined|retracted)$", flags = [Pattern.Flag.CASE_INSENSITIVE])
    val status: String?,
)
