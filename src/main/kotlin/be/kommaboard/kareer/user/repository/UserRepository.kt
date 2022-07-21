package be.kommaboard.kareer.user.repository

import be.kommaboard.kareer.user.repository.entity.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface UserRepository : JpaRepository<User, UUID> {

    fun findByUuid(uuid: UUID): User?

    fun findByEmail(email: String): User?
}
