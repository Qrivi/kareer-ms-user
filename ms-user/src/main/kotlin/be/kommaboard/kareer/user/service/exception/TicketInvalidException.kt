package be.kommaboard.kareer.user.service.exception

import java.util.UUID

class TicketInvalidException(uuid: UUID) : IllegalArgumentException("Ticket with UUID $uuid has mismatching data")
