package be.kommaboard.kareer.user.repository

import be.kommaboard.kareer.common.security.Role
import be.kommaboard.kareer.user.repository.entity.User
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface UserRepository : JpaRepository<User, UUID> {

    fun findAllByRole(role: Role, pageable: Pageable): Page<User>

    fun findAllByOrganizationUuid(organizationUUID: UUID, pageable: Pageable): Page<User>

    fun findAllByEmailContainsIgnoreCase(email: String, pageable: Pageable): Page<User>

    fun findAllByOrganizationUuidAndRole(organizationUUID: UUID, role: Role, pageable: Pageable): Page<User>

    fun findAllByRoleAndEmailContainsIgnoreCase(role: Role, email: String, pageable: Pageable): Page<User>

    fun findAllByOrganizationUuidAndEmailContainsIgnoreCase(organizationUUID: UUID, email: String, pageable: Pageable): Page<User>

    fun findAllByOrganizationUuidAndRoleAndEmailContainsIgnoreCase(organizationUUID: UUID, role: Role, email: String, pageable: Pageable): Page<User>

    fun findByUuid(uuid: UUID): User?

    fun findByEmail(email: String): User?

    fun existsByEmailIgnoreCase(email: String): Boolean
}
