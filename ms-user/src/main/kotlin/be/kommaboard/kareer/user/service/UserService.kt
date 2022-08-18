package be.kommaboard.kareer.user.service

import be.kommaboard.kareer.authorization.Role
import be.kommaboard.kareer.authorization.Status
import be.kommaboard.kareer.authorization.hashedWithSalt
import be.kommaboard.kareer.common.trimOrNullIfBlank
import be.kommaboard.kareer.mailing.lib.dto.MailMeta
import be.kommaboard.kareer.mailing.lib.dto.UserInvitationMailDTO
import be.kommaboard.kareer.mailing.lib.service.MailingQueueService
import be.kommaboard.kareer.organization.lib.dto.response.OrganizationDTO
import be.kommaboard.kareer.user.UserConfig
import be.kommaboard.kareer.user.repository.InviteRepository
import be.kommaboard.kareer.user.repository.TicketRepository
import be.kommaboard.kareer.user.repository.UserRepository
import be.kommaboard.kareer.user.repository.entity.Invite
import be.kommaboard.kareer.user.repository.entity.Ticket
import be.kommaboard.kareer.user.repository.entity.User
import be.kommaboard.kareer.user.service.exception.IncorrectCredentialsException
import be.kommaboard.kareer.user.service.exception.TicketAlreadyUsedException
import be.kommaboard.kareer.user.service.exception.TicketDoesNotExistException
import be.kommaboard.kareer.user.service.exception.TicketExpiredException
import be.kommaboard.kareer.user.service.exception.TicketInvalidException
import be.kommaboard.kareer.user.service.exception.UserAlreadyExistsException
import be.kommaboard.kareer.user.service.exception.UserDoesNotExistException
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.event.ContextRefreshedEvent
import org.springframework.context.event.EventListener
import org.springframework.context.i18n.LocaleContextHolder
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
    private val clock: Clock,
    private val userConfig: UserConfig,
    private val inviteRepository: InviteRepository,
    private val ticketRepository: TicketRepository,
    private val userRepository: UserRepository,
    private val mailingQueueService: MailingQueueService,
) {
    private val logger: Logger = LoggerFactory.getLogger(this.javaClass)

    @EventListener
    fun onApplicationEvent(event: ContextRefreshedEvent) {
        if (userRepository.count() == 0L) {
            logger.warn("No admins found in database. Creating admin with default credentials...")
            createUser(
                email = userConfig.adminEmail!!,
                password = userConfig.adminPassword!!,
                lastName = "Admin",
                firstName = "Admin",
                role = Role.ADMIN,
                activate = true,
            )
        }
    }

    fun getAllUsers(): List<User> = userRepository.findAll()

    fun getPagedUsers(
        pageRequest: PageRequest,
        emailPart: String?,
        organizationUuid: UUID?,
        role: Role?,
    ): Page<User> {
        if (emailPart != null && organizationUuid != null && role != null)
            return userRepository.findAllByOrganizationUuidAndRoleAndEmailContainsIgnoreCase(organizationUuid, role, emailPart, pageRequest)
        else if (emailPart != null && organizationUuid != null)
            return userRepository.findAllByOrganizationUuidAndEmailContainsIgnoreCase(organizationUuid, emailPart, pageRequest)
        else if (emailPart != null && role != null)
            return userRepository.findAllByRoleAndEmailContainsIgnoreCase(role, emailPart, pageRequest)
        else if (organizationUuid != null && role != null)
            return userRepository.findAllByOrganizationUuidAndRole(organizationUuid, role, pageRequest)
        else if (emailPart != null)
            return userRepository.findAllByEmailContainsIgnoreCase(emailPart, pageRequest)
        else if (organizationUuid != null)
            return userRepository.findAllByOrganizationUuid(organizationUuid, pageRequest)
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
        lastName: String,
        firstName: String,
        nickname: String? = null,
        organizationUuid: UUID? = null,
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
                organizationUuid = organizationUuid,
                creationDate = now,
                email = formattedEmail,
                password = password.hashedWithSalt(userConfig.salt!!),
                lastName = lastName.trim(),
                firstName = firstName.trim(),
                nickname = nickname.trimOrNullIfBlank() ?: firstName,
                role = role,
                status = if (activate) Status.ACTIVATED else Status.REGISTERED,
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

    fun createInvite(
        manager: User,
        organization: OrganizationDTO,
        inviteeEmail: String,
        inviteeLastName: String,
        inviteeFirstName: String,
    ): Invite {
        val formattedInviteeEmail = inviteeEmail.trim().lowercase()

        if (userRepository.existsByEmailIgnoreCase(formattedInviteeEmail))
            throw UserAlreadyExistsException(formattedInviteeEmail)

        val invite = inviteRepository.saveAndFlush(
            Invite(
                inviter = manager,
                creationDate = ZonedDateTime.now(clock),
                inviteeEmail = formattedInviteeEmail,
                inviteeLastName = inviteeLastName.trim(),
                inviteeFirstName = inviteeFirstName.trim(),
            )
        )

        val mailDTO = UserInvitationMailDTO(
            meta = MailMeta(
                language = LocaleContextHolder.getLocale().language,
                recipientEmail = invite.inviteeEmail,
                recipientName = invite.inviteeFirstName,
            ),
            inviterName = manager.fullName(),
            organizationName = organization.name,
            inviteLink = "https://kommaboard.be/kareerfrontent/?uuid=${invite.uuid}"
        )
        mailingQueueService.sendToQueue(mailDTO)

        return invite
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
        user.status = Status.ACTIVATED
        userRepository.save(user)

        // Mark ticket as used
        ticket.used = true
        ticketRepository.save(ticket)

        return user
    }
}
