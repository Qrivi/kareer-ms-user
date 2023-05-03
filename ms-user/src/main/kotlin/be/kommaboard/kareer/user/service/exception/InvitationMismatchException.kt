package be.kommaboard.kareer.user.service.exception

class InvitationMismatchException : IllegalArgumentException("Provided invitation and e-mail do not match")
