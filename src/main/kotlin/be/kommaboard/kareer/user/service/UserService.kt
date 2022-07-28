package be.kommaboard.kareer.user.service

import be.kommaboard.kareer.common.hashedWithSalt
import be.kommaboard.kareer.common.security.Role
import be.kommaboard.kareer.user.UserConfig
import be.kommaboard.kareer.user.repository.TicketRepository
import be.kommaboard.kareer.user.repository.UserRepository
import be.kommaboard.kareer.user.repository.entity.Ticket
import be.kommaboard.kareer.user.repository.entity.User
import be.kommaboard.kareer.user.service.exception.TicketAlreadyUsedException
import be.kommaboard.kareer.user.service.exception.TicketExpiredException
import be.kommaboard.kareer.user.service.exception.TicketInvalidException
import be.kommaboard.kareer.user.service.exception.TicketNotFoundException
import be.kommaboard.kareer.user.service.exception.UserNotFoundException
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.event.ContextRefreshedEvent
import org.springframework.context.event.EventListener
import org.springframework.security.crypto.bcrypt.BCrypt
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.ZonedDateTime
import java.util.UUID

@Service
@Transactional
class UserService(
    private val userConfig: UserConfig,
    private val userRepository: UserRepository,
    private val ticketRepository: TicketRepository,
) {
    private val logger: Logger = LoggerFactory.getLogger(this.javaClass)

    @EventListener
    fun onApplicationEvent(event: ContextRefreshedEvent) {
        if (userRepository.count() == 0L) {
            logger.warn("No admins found in database. Creating admin with default credentials...")
            createUser(
                email = userConfig.adminEmail,
                password = userConfig.adminPassword,
                name = "Admin",
                role = Role.ADMIN,
                activate = true,
            )
        }
    }

    fun getUserByUuid(uuid: UUID) = userRepository.findByUuid(uuid) ?: throw UserNotFoundException(uuid)

    fun getTicketByUuid(uuid: UUID) = ticketRepository.findByUuid(uuid) ?: throw TicketNotFoundException(uuid)

    fun getUserByEmail(email: String) = userRepository.findByEmail(email)

    fun createUser(email: String, password: String, name: String, alias: String? = null, companyUuid: UUID? = null, role: Role, activate: Boolean = false): User {
        val now = ZonedDateTime.now()

        // Create the new user
        val user = User(
            creationDate = now,
            email = email.trim(),
            password = password.hashedWithSalt(userConfig.salt),
            name = name.trim(),
            alias = if (!alias.isNullOrBlank()) alias.trim() else name.trim().substringBefore(" "),
            companyUuid = companyUuid,
            role = role,
            status = if (activate) User.Status.ACTIVATED else User.Status.REGISTERED,
        )
        userRepository.saveAndFlush(user)

        // Create a ticket to activate the user later
        if (!activate) {
            val ticket = Ticket(
                user = user,
                creationDate = now,
                token = UUID.randomUUID().toString().hashedWithSalt(userConfig.salt),
                kind = Ticket.Kind.CONFIRM_EMAIL,
            )
            ticketRepository.save(ticket)
        }

        // TODO Add message to the queue to send out activation e-mail

        return user
    }

    fun activateUser(userUuid: UUID, ticketUuid: UUID, token: String): User {
        val user = getUserByUuid(userUuid)
        val ticket = getTicketByUuid(ticketUuid)

        // Verify that passed token matches with ticket in the database
        if (token != ticket.token) throw TicketInvalidException(ticketUuid)
        // Verify that passed user UUID matches with user linked to ticket in the database
        if (userUuid != ticket.user.uuid) throw TicketInvalidException(ticketUuid)
        // Verify that the ticket was not expired
        if (ZonedDateTime.now().isBefore(ticket.creationDate.plusDays(userConfig.confirmEmailTTL))) throw TicketExpiredException(ticketUuid)
        // Verify that the ticket was not used before
        if (ticket.used) throw TicketAlreadyUsedException(ticketUuid)

        // Mark user as activated
        user.status = User.Status.ACTIVATED
        userRepository.save(user)

        // Mark ticket as used
        ticket.used = true
        ticketRepository.save(ticket)

        return user
    }
}
