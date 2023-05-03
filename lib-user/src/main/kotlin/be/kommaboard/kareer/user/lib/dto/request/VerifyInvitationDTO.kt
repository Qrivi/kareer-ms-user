package be.kommaboard.kareer.user.lib.dto.request

import java.util.UUID

data class VerifyInvitationDTO(
    val invitationUuid: UUID?,
    val email: String?,
)
