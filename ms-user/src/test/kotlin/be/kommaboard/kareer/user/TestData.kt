package be.kommaboard.kareer.user

import be.kommaboard.kareer.authorization.Role
import be.kommaboard.kareer.authorization.Status
import be.kommaboard.kareer.authorization.hashedWithSalt
import be.kommaboard.kareer.authorization.toUuid
import be.kommaboard.kareer.user.repository.entity.User
import org.springframework.security.crypto.bcrypt.BCrypt
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.util.UUID

class TestData {

    val organizationUuid: UUID = "88888888-8888-8888-8888-888888888888".toUuid()

    val salt: String = BCrypt.gensalt(12)

    val clock: Clock = Clock.fixed(
        Instant.parse("2010-10-10T10:10:10.00Z"),
        ZoneOffset.UTC,
    )

    val users = listOf(
        User(
            organizationUuid = null,
            creationDate = ZonedDateTime.now(clock),
            email = "ann.chovey@kommaboard.be",
            password = "AnnChovey1234".hashedWithSalt(salt),
            lastName = "Chovey",
            firstName = "Ann",
            nickname = "Ann",
            role = Role.ADMIN,
            status = Status.ACTIVATED,
            keywords = "",
        ),
        User(
            organizationUuid = organizationUuid,
            creationDate = ZonedDateTime.now(clock),
            email = "barry.cuda@kommaboard.be",
            password = "BarryCuda1234".hashedWithSalt(salt),
            lastName = "Cuda",
            firstName = "Barry",
            nickname = "Barry",
            role = Role.MANAGER,
            status = Status.ACTIVATED,
            keywords = "",
        ),
        User(
            organizationUuid = organizationUuid,
            creationDate = ZonedDateTime.now(clock),
            email = "claude.strophobia@kommaboard.be",
            password = "ClaudeStrophobia1234".hashedWithSalt(salt),
            lastName = "Strophobia",
            firstName = "Claude",
            nickname = "Claude",
            role = Role.USER,
            status = Status.ACTIVATED,
            keywords = "",
        ),
    )
}
