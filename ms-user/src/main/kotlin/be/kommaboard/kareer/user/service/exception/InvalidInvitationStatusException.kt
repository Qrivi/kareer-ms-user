package be.kommaboard.kareer.user.service.exception

class InvalidInvitationStatusException(val value: String) : IllegalArgumentException("\"$value\" cannot be converted to an invitation status")
