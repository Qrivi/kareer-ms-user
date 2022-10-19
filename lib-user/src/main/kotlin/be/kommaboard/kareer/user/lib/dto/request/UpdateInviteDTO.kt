package be.kommaboard.kareer.user.lib.dto.request

import java.util.Optional
import javax.validation.constraints.NotBlank

data class UpdateInviteDTO(

    val status: Optional<
        @NotBlank(message = "{UpdateInviteDTO.status.NotBlank}")
        String>?,
)
