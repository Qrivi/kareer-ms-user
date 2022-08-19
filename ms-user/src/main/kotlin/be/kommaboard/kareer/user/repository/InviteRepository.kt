package be.kommaboard.kareer.user.repository

import be.kommaboard.kareer.user.repository.entity.Invite
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface InviteRepository : JpaRepository<Invite, UUID> {

    @Query("SELECT i FROM Invite i JOIN i.inviter u WHERE u.organizationUuid = :organizationUuid")
    fun findAllByOrganizationUuid(organizationUuid: UUID, pageable: Pageable): Page<Invite>

    @Query("SELECT i FROM Invite i JOIN i.inviter u WHERE u.organizationUuid = :organizationUuid and i.status = :status")
    fun findAllByOrganizationUuidAndStatus(organizationUuid: UUID, status: Invite.Status, pageable: Pageable): Page<Invite>

    fun findByUuid(uuid: UUID): Invite?
}
