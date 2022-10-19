package be.kommaboard.kareer.user

import be.kommaboard.kareer.user.repository.entity.Invite
import be.kommaboard.kareer.user.service.exception.InvalidInviteStatusException
import java.util.Optional

fun String.toInviteStatus(): Invite.Status = try {
    Invite.Status.valueOf(this.trim().uppercase())
} catch (e: IllegalArgumentException) {
    throw InvalidInviteStatusException(this.trim())
}

fun Optional<String>.toInviteStatus(): Optional<Invite.Status> = Optional.of(this.get().toInviteStatus())
