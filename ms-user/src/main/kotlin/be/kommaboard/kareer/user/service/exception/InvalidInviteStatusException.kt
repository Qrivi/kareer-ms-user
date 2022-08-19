package be.kommaboard.kareer.user.service.exception

class InvalidInviteStatusException(val value: String) : IllegalArgumentException("\"$value\" cannot be converted to an invite status")
