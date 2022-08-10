package be.kommaboard.kareer.user.lib.dto.response

import java.time.ZonedDateTime
import java.util.UUID

data class InviteDTO(
    val uuid: UUID,
    val inviterUuid: UUID,
    val creationDate: ZonedDateTime,
    val inviteeEmail: String,
    val inviteeName: String,
    val used: Boolean,
)