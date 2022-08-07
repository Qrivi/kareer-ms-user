package be.kommaboard.kareer.user.service.exception

import java.util.UUID

class TicketExpiredException(uuid: UUID) : IllegalArgumentException("Ticket with UUID $uuid is expired")
