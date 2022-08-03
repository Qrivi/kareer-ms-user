package be.kommaboard.kareer.user.service

import be.kommaboard.kareer.common.hashedWithSalt
import be.kommaboard.kareer.common.security.Role
import be.kommaboard.kareer.user.UserConfig
import be.kommaboard.kareer.user.repository.TicketRepository
import be.kommaboard.kareer.user.repository.UserRepository
import be.kommaboard.kareer.user.repository.entity.Ticket
import be.kommaboard.kareer.user.repository.entity.User
import be.kommaboard.kareer.user.service.exception.IncorrectCredentialsException
import be.kommaboard.kareer.user.service.exception.TicketAlreadyUsedException
import be.kommaboard.kareer.user.service.exception.TicketDoesNotExistException
import be.kommaboard.kareer.user.service.exception.TicketExpiredException
import be.kommaboard.kareer.user.service.exception.TicketInvalidException
import be.kommaboard.kareer.user.service.exception.UserAlreadyExistsException
import be.kommaboard.kareer.user.service.exception.UserDoesNotExistException
import org.bouncycastle.asn1.x500.style.RFC4519Style.name
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.event.ContextRefreshedEvent
import org.springframework.context.event.EventListener
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.security.crypto.bcrypt.BCrypt
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.ZonedDateTime
import java.util.UUID

@Service
@Transactional
class UserService(
    private val userConfig: UserConfig,
    private val userRepository: UserRepository,
    private val ticketRepository: TicketRepository,
    private val clock: Clock,
) {
    private val logger: Logger = LoggerFactory.getLogger(this.javaClass)

    @EventListener
    fun onApplicationEvent(event: ContextRefreshedEvent) {
        if (userRepository.count() == 0L) {
            logger.warn("No admins found in database. Creating admin with default credentials...")
            createUser(
                email = userConfig.adminEmail!!,
                password = userConfig.adminPassword!!,
                fullName = "Admin",
                role = Role.ADMIN,
                activate = true,
            )
        }
    }

    fun getAllUsers(): List<User> = userRepository.findAll()

    fun getPagedUsers(
        pageRequest: PageRequest,
        email: String?,
        companyUuid: UUID?,
        role: Role?,
    ): Page<User> {
        if (email != null && companyUuid != null && role != null)
            return userRepository.findAllByCompanyUuidAndRoleAndEmailContainsIgnoreCase(companyUuid, role, email, pageRequest)
        else if (email != null && companyUuid != null)
            return userRepository.findAllByCompanyUuidAndEmailContainsIgnoreCase(companyUuid, email, pageRequest)
        else if (email != null && role != null)
            return userRepository.findAllByRoleAndEmailContainsIgnoreCase(role, email, pageRequest)
        else if (companyUuid != null && role != null)
            return userRepository.findAllByCompanyUuidAndRole(companyUuid, role, pageRequest)
        else if (email != null)
            return userRepository.findAllByEmailContainsIgnoreCase(email, pageRequest)
        else if (companyUuid != null)
            return userRepository.findAllByCompanyUuid(companyUuid, pageRequest)
        else if (role != null)
            return userRepository.findAllByRole(role, pageRequest)
        else
            return userRepository.findAll(pageRequest)
    }

    fun getUserByUuid(uuid: UUID) = userRepository.findByUuid(uuid) ?: throw UserDoesNotExistException()

    fun getTicketByUuid(uuid: UUID) = ticketRepository.findByUuid(uuid) ?: throw TicketDoesNotExistException()

    fun getUserByEmail(email: String) = userRepository.findByEmail(email)

    fun getUserByEmailAndPassword(
        email: String,
        password: String,
    ): User {
        val user = getUserByEmail(email) ?: throw IncorrectCredentialsException()
        if (!BCrypt.checkpw(password, user.password)) throw IncorrectCredentialsException()
        return user
    }

    fun createUser(
        email: String,
        password: String,
        fullName: String,
        shortName: String? = null,
        companyUuid: UUID? = null,
        role: Role,
        activate: Boolean = false,
    ): User {
        val formattedEmail = email.trim().lowercase()

        if (userRepository.existsByEmailIgnoreCase(formattedEmail))
            throw UserAlreadyExistsException(formattedEmail)

        val now = ZonedDateTime.now(clock)

        // Create the new user
        val user = userRepository.saveAndFlush(
            User(
                creationDate = now,
                email = formattedEmail,
                password = password.hashedWithSalt(userConfig.salt!!),
                fullName = fullName.trim(),
                shortName = if (!shortName.isNullOrBlank()) shortName.trim() else fullName.trim().substringBefore(" "),
                companyUuid = companyUuid,
                role = role,
                status = if (activate) User.Status.ACTIVATED else User.Status.REGISTERED,
            )
        )

        // Create a ticket to activate the user later
        if (!activate) {
            ticketRepository.save(
                Ticket(
                    user = user,
                    creationDate = now,
                    token = UUID.randomUUID().toString().hashedWithSalt(userConfig.salt!!),
                    kind = Ticket.Kind.CONFIRM_EMAIL,
                )
            )
        }

        // TODO Add message to the queue to send out activation e-mail

        return user
    }

    fun confirmEmail(
        userUuid: UUID,
        ticketUuid: UUID,
        token: String,
    ): User {
        val user = getUserByUuid(userUuid)
        val ticket = getTicketByUuid(ticketUuid)

        // Verify that passed token matches with ticket in the database
        if (token != ticket.token) throw TicketInvalidException(ticketUuid)
        // Verify that passed user UUID matches with user linked to ticket in the database
        if (userUuid != ticket.user.uuid) throw TicketInvalidException(ticketUuid)
        // Verify that ticket was meant to confirm e-mail
        if (Ticket.Kind.CONFIRM_EMAIL != ticket.kind) throw TicketInvalidException(ticketUuid)
        // Verify that the ticket was not expired
        if (ZonedDateTime.now(clock).isBefore(ticket.creationDate.plusHours(userConfig.confirmEmailTtl!!))) throw TicketExpiredException(ticketUuid)
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
