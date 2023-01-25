package be.kommaboard.kareer.user.service.exception

class UserAlreadyExistsException(val email: String) : IllegalArgumentException("User for $email already exists")
