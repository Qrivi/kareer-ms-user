package be.kommaboard.kareer.user.repository

import be.kommaboard.kareer.user.repository.entity.Ticket
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface TicketRepository : JpaRepository<Ticket, UUID> {

    fun findByUuid(uuid: UUID): Ticket?
}
