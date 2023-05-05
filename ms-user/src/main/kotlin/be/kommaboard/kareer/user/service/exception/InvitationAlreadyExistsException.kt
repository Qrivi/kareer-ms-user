package be.kommaboard.kareer.user.service.exception

class InvitationAlreadyExistsException(val email: String) : IllegalArgumentException("Invitation for $email already exists")
