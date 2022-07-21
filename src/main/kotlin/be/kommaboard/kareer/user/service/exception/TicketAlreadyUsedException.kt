package be.kommaboard.kareer.user.service.exception

import java.util.UUID

class TicketAlreadyUsedException(uuid: UUID) : IllegalArgumentException("Ticket with UUID $uuid was already used")
