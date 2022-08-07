package be.kommaboard.kareer.user.service.exception

class UserAlreadyExistsException(val email: String) : IllegalArgumentException("User with e-mail $email already exists")
