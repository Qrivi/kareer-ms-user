package be.kommaboard.kareer.user.repository

import be.kommaboard.kareer.user.repository.entity.Invite
import be.kommaboard.kareer.user.repository.entity.Ticket
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface InviteRepository : JpaRepository<Invite, UUID> {

    fun findByUuid(uuid: UUID): Invite?
}
