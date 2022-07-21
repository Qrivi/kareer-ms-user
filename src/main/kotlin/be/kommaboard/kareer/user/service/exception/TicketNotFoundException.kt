package be.kommaboard.kareer.user.service.exception

import java.util.UUID

class TicketNotFoundException(uuid: UUID) : IllegalArgumentException("Ticket with UUID $uuid does not exist")
