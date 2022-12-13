package be.kommaboard.kareer.user.service

import be.kommaboard.kareer.authorization.Role
import be.kommaboard.kareer.authorization.Status
import be.kommaboard.kareer.authorization.hashedWithSalt
import be.kommaboard.kareer.common.getOrNull
import be.kommaboard.kareer.common.makeKeywords
import be.kommaboard.kareer.common.trimOrNullIfBlank
import be.kommaboard.kareer.mailing.lib.dto.MailMeta
import be.kommaboard.kareer.mailing.lib.dto.UserInvitationMailDTO
import be.kommaboard.kareer.mailing.lib.service.MailingQueueService
import be.kommaboard.kareer.organization.lib.dto.response.OrganizationDTO
import be.kommaboard.kareer.user.KareerConfig
import be.kommaboard.kareer.user.proxy.OrganizationProxy
import be.kommaboard.kareer.user.repository.InviteRepository
import be.kommaboard.kareer.user.repository.TicketRepository
import be.kommaboard.kareer.user.repository.UserRepository
import be.kommaboard.kareer.user.repository.entity.Invite
import be.kommaboard.kareer.user.repository.entity.Ticket
import be.kommaboard.kareer.user.repository.entity.User
import be.kommaboard.kareer.user.service.exception.IncorrectCredentialsException
import be.kommaboard.kareer.user.service.exception.InviteDoesNotExistException
import be.kommaboard.kareer.user.service.exception.TicketAlreadyUsedException
import be.kommaboard.kareer.user.service.exception.TicketDoesNotExistException
import be.kommaboard.kareer.user.service.exception.TicketExpiredException
import be.kommaboard.kareer.user.service.exception.TicketInvalidException
import be.kommaboard.kareer.user.service.exception.UserAlreadyExistsException
import be.kommaboard.kareer.user.service.exception.UserDoesNotExistException
import org.slf4j.LoggerFactory
import org.springframework.context.event.ContextRefreshedEvent
import org.springframework.context.event.EventListener
import org.springframework.context.i18n.LocaleContextHolder
import org.springframework.data.domain.PageRequest
import org.springframework.security.crypto.bcrypt.BCrypt
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.ZonedDateTime
import java.util.Optional
import java.util.UUID

@Service
@Transactional
class UserService(
    private val clock: Clock,
    private val kareerConfig: KareerConfig,
    private val inviteRepository: InviteRepository,
    private val ticketRepository: TicketRepository,
    private val userRepository: UserRepository,
    private val organizationProxy: OrganizationProxy,
    private val mailingQueueService: MailingQueueService,
) {
    private val logger = LoggerFactory.getLogger(this.javaClass)

    @EventListener
    fun onApplicationEvent(event: ContextRefreshedEvent) {
        if (userRepository.count() == 0L) {
            logger.warn("No admins found in database. Creating admin with default credentials...")
            createUser(
                email = kareerConfig.adminEmail!!,
                password = kareerConfig.adminPassword!!,
                lastName = "Admin",
                firstName = "Admin",
                role = Role.ADMIN,
                title = "Super Administrator",
                activate = true,
            )
        }
    }

    fun getAllUsers(): List<User> = userRepository.findAll()

    fun getAllInvites(): List<Invite> = inviteRepository.findAll()

    fun getPagedUsers(
        pageRequest: PageRequest,
        keywords: String?,
        organizationUuid: UUID?,
        role: Role?,
    ) = when {
        keywords != null && organizationUuid != null && role != null -> userRepository.findAllByOrganizationUuidAndRoleAndKeywordsContainsIgnoreCase(organizationUuid, role, keywords, pageRequest)
        keywords != null && organizationUuid != null -> userRepository.findAllByOrganizationUuidAndKeywordsContainsIgnoreCase(organizationUuid, keywords, pageRequest)
        keywords != null && role != null -> userRepository.findAllByRoleAndKeywordsContainsIgnoreCase(role, keywords, pageRequest)
        organizationUuid != null && role != null -> userRepository.findAllByOrganizationUuidAndRole(organizationUuid, role, pageRequest)
        organizationUuid != null -> userRepository.findAllByOrganizationUuid(organizationUuid, pageRequest)
        keywords != null -> userRepository.findAllByKeywordsContainsIgnoreCase(keywords, pageRequest)
        role != null -> userRepository.findAllByRole(role, pageRequest)
        else -> userRepository.findAll(pageRequest)
    }

    fun getPagedInvites(
        pageRequest: PageRequest,
        organizationUuid: UUID,
        status: Invite.Status?,
    ) = when {
        status != null -> inviteRepository.findAllByOrganizationUuidAndStatus(organizationUuid, status, pageRequest)
        else -> inviteRepository.findAllByOrganizationUuid(organizationUuid, pageRequest)
    }

    fun getUserByUuid(
        uuid: UUID,
    ) = userRepository.findByUuid(uuid)
        ?: throw UserDoesNotExistException()

    fun getUserBySlug(
        slug: String,
    ) = userRepository.findBySlug(slug)
        ?: throw UserDoesNotExistException()

    fun getInviteByUuid(
        uuid: UUID,
    ) = inviteRepository.findByUuid(uuid)
        ?: throw InviteDoesNotExistException()

    fun getTicketByUuid(
        uuid: UUID,
    ) = ticketRepository.findByUuid(uuid)
        ?: throw TicketDoesNotExistException()

    fun getUserByEmailAndPassword(
        email: String,
        password: String,
    ): User {
        val user = userRepository.findByEmail(email) ?: throw IncorrectCredentialsException()
        if (!BCrypt.checkpw(password, user.password)) throw IncorrectCredentialsException()
        return user
    }

    fun createUser(
        slug: String? = null,
        email: String,
        phone: String? = null,
        password: String,
        lastName: String,
        firstName: String,
        nickname: String? = null,
        title: String,
        organizationUuid: UUID? = null,
        organizationName: String? = null,
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
                slug = slug,
                email = formattedEmail,
                phone = phone,
                password = password.hashedWithSalt(kareerConfig.salt!!),
                lastName = lastName.trim(),
                firstName = firstName.trim(),
                nickname = nickname.trimOrNullIfBlank() ?: firstName,
                title = title.trim(),
                role = role,
                status = if (activate) Status.ACTIVATED else Status.REGISTERED,
                avatarReference = null,
                bannerReference = null,
                keywords = makeKeywords(firstName, lastName, firstName, formattedEmail, phone, organizationName)
            )
        )

        // Create a ticket to activate the user later
        if (!activate) {
            ticketRepository.save(
                Ticket(
                    user = user,
                    creationDate = now,
                    token = UUID.randomUUID().toString().hashedWithSalt(kareerConfig.salt!!),
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
                status = Invite.Status.PENDING,
            )
        )

        val mailDTO = UserInvitationMailDTO(
            meta = MailMeta(
                language = LocaleContextHolder.getLocale().language,
                recipientEmail = invite.inviteeEmail,
                recipientName = invite.inviteeFirstName,
            ),
            inviterName = manager.fullName(), organizationName = organization.name, inviteLink = "https://kommaboard.be/kareerfrontent/?uuid=${invite.uuid}"
        )
        mailingQueueService.sendToQueue(mailDTO)

        return invite
    }

    fun updateUser(
        uuid: UUID,
        organizationName: String?,
        slug: Optional<String?>? = null,
        email: Optional<String>? = null,
        phone: Optional<String?>? = null,
        password: Optional<String>? = null,
        lastName: Optional<String>? = null,
        firstName: Optional<String>? = null,
        nickname: Optional<String?>? = null,
        title: Optional<String>? = null,
        role: Optional<Role>? = null,
    ): User {
        val formattedEmail = email?.get().trimOrNullIfBlank()?.lowercase()
        if (formattedEmail != null && userRepository.existsByEmailIgnoreCase(formattedEmail))
            throw UserAlreadyExistsException(formattedEmail)

        return userRepository.save(
            getUserByUuid(uuid).apply {
                slug?.let {
                    this.slug = it.getOrNull()
                }
                formattedEmail?.let {
                    this.email = formattedEmail
                }
                phone?.let {
                    this.phone = it.getOrNull()
                }
                password?.let {
                    this.password = password.get().hashedWithSalt(kareerConfig.salt!!)
                }
                lastName?.let {
                    this.lastName = it.get().trim()
                }
                firstName?.let {
                    this.firstName = it.get().trim()
                    this.nickname = it.get().trim()
                }
                nickname?.let {
                    this.nickname = it.orElse(this.firstName)!!
                }
                title?.let {
                    this.title = it.get().trim()
                }
                role?.let {
                    this.role = it.get()
                }
                keywords = makeKeywords(this.firstName, this.lastName, this.firstName, this.email, this.phone, organizationName)
            }
        )
    }

    fun updateUserAvatar(
        uuid: UUID,
        reference: String?
    ) = userRepository.save(
        getUserByUuid(uuid).apply {
            this.avatarReference = reference
        }
    )

    fun updateUserBanner(
        uuid: UUID,
        reference: String?
    ) = userRepository.save(
        getUserByUuid(uuid).apply {
            this.bannerReference = reference
        }
    )

    fun updateInviteStatus(
        invite: Invite,
        status: Invite.Status,
    ): Invite {
        invite.status = status

        return inviteRepository.save(invite)
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
        if (ZonedDateTime.now(clock).isBefore(ticket.creationDate.plusHours(kareerConfig.confirmEmailTtl!!))) throw TicketExpiredException(ticketUuid)
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
