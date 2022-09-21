package be.kommaboard.kareer.user.repository

import be.kommaboard.kareer.authorization.Role
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

    fun findAllByKeywordsContainsIgnoreCase(keywords: String, pageable: Pageable): Page<User>

    fun findAllByOrganizationUuidAndRole(organizationUUID: UUID, role: Role, pageable: Pageable): Page<User>

    fun findAllByRoleAndKeywordsContainsIgnoreCase(role: Role, keywords: String, pageable: Pageable): Page<User>

    fun findAllByOrganizationUuidAndKeywordsContainsIgnoreCase(organizationUUID: UUID, keywords: String, pageable: Pageable): Page<User>

    fun findAllByOrganizationUuidAndRoleAndKeywordsContainsIgnoreCase(organizationUUID: UUID, role: Role, keywords: String, pageable: Pageable): Page<User>

    fun findByUuid(uuid: UUID): User?

    fun findByEmail(keywords: String): User?

    fun existsByEmailIgnoreCase(keywords: String): Boolean
}
