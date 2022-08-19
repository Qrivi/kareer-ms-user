package be.kommaboard.kareer.user

import be.kommaboard.kareer.user.repository.entity.Invite
import be.kommaboard.kareer.user.service.exception.InvalidInviteStatusException

fun String.toInviteStatus(): Invite.Status = try {
    Invite.Status.valueOf(this.trim().uppercase())
} catch (e: IllegalArgumentException) {
    throw InvalidInviteStatusException(this.trim())
}
