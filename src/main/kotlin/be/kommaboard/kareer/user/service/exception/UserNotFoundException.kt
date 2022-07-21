package be.kommaboard.kareer.user.service.exception

import java.util.UUID

class UserNotFoundException(uuid: UUID) : IllegalArgumentException("User with UUID $uuid does not exist")
