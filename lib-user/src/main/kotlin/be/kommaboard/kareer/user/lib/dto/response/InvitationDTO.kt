package be.kommaboard.kareer.user.lib.dto.response

import java.time.ZonedDateTime
import java.util.UUID

data class InvitationDTO(
    val uuid: UUID,
    val inviterUuid: UUID,
    val creationDate: ZonedDateTime,
    val inviteeEmail: String,
    val inviteeLastName: String,
    val inviteeFirstName: String,
    val status: String,
)
