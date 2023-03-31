package be.kommaboard.kareer.user.repository

import be.kommaboard.kareer.user.repository.entity.Invitation
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface InviteRepository : JpaRepository<Invitation, UUID> {

    @Query("SELECT i FROM Invitation i JOIN i.inviter u WHERE u.organizationUuid = :organizationUuid")
    fun findAllByOrganizationUuid(organizationUuid: UUID, pageable: Pageable): Page<Invitation>

    @Query("SELECT i FROM Invitation i JOIN i.inviter u WHERE u.organizationUuid = :organizationUuid and i.status = :status")
    fun findAllByOrganizationUuidAndStatus(organizationUuid: UUID, status: Invitation.Status, pageable: Pageable): Page<Invitation>

    fun findByUuid(uuid: UUID): Invitation?
}
