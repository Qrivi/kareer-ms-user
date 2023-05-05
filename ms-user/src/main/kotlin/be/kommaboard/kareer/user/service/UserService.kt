package be.kommaboard.kareer.user.service

import be.kommaboard.kareer.authorization.Role
import be.kommaboard.kareer.authorization.Status
import be.kommaboard.kareer.authorization.exception.InvalidCredentialsException
import be.kommaboard.kareer.authorization.util.hashedWithSalt
import be.kommaboard.kareer.authorization.util.toRole
import be.kommaboard.kareer.common.util.makeKeywords
import be.kommaboard.kareer.common.util.trimOrNullIfBlank
import be.kommaboard.kareer.mailing.lib.dto.MailMeta
import be.kommaboard.kareer.mailing.lib.dto.UserInvitationMailDTO
import be.kommaboard.kareer.mailing.lib.service.MailingQueueService
import be.kommaboard.kareer.organization.lib.dto.response.OrganizationDTO
import be.kommaboard.kareer.user.KareerConfig
import be.kommaboard.kareer.user.lib.dto.request.CreateAdminDTO
import be.kommaboard.kareer.user.lib.dto.request.CreateInvitationDTO
import be.kommaboard.kareer.user.lib.dto.request.CreateUserDTO
import be.kommaboard.kareer.user.lib.dto.request.EditUserDetailsSkillsDTO
import be.kommaboard.kareer.user.lib.dto.request.UpdateUserDTO
import be.kommaboard.kareer.user.lib.dto.request.UpdateUserDetailsDTO
import be.kommaboard.kareer.user.repository.InvitationRepository
import be.kommaboard.kareer.user.repository.TicketRepository
import be.kommaboard.kareer.user.repository.UserRepository
import be.kommaboard.kareer.user.repository.entity.Invitation
import be.kommaboard.kareer.user.repository.entity.Ticket
import be.kommaboard.kareer.user.repository.entity.User
import be.kommaboard.kareer.user.repository.entity.UserDetails
import be.kommaboard.kareer.user.service.exception.IncorrectCredentialsException
import be.kommaboard.kareer.user.service.exception.InvitationAlreadyExistsException
import be.kommaboard.kareer.user.service.exception.InvitationDoesNotExistException
import be.kommaboard.kareer.user.service.exception.SkillLimitException
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
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.time.Clock
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.UUID

@Service
@Transactional
class UserService(
    private val clock: Clock,
    private val kareerConfig: KareerConfig,
    private val invitationRepository: InvitationRepository,
    private val ticketRepository: TicketRepository,
    private val userRepository: UserRepository,
    private val mailingQueueService: MailingQueueService,
) {
    private val logger = LoggerFactory.getLogger(this.javaClass)

    @EventListener
    fun onApplicationEvent(event: ContextRefreshedEvent) {
        if (userRepository.count() == 0L) {
            logger.warn("No admins found in database. Creating admin with default credentials...")
            createAdmin(
                CreateAdminDTO(
                    email = kareerConfig.adminEmail!!,
                    password = kareerConfig.adminPassword!!,
                    lastName = "istrator",
                    firstName = "Admin",
                ),
            )
        }
    }

    fun userEmailInUse(
        email: String,
    ) = userRepository.existsByEmailIgnoreCase(email)

    fun getAllUsers(): List<User> = userRepository.findAll()

    fun getAllInvitations(): List<Invitation> = invitationRepository.findAll()

    fun getPagedUsers(
        pageRequest: PageRequest,
        keywords: String?,
        organizationUuid: UUID?,
        roles: List<Role>?,
    ) = when {
        keywords != null && organizationUuid != null && roles != null -> userRepository.findAllByDetailsOrganizationUuidAndRoleInAndKeywordsContainsIgnoreCase(organizationUuid, roles, keywords, pageRequest)
        keywords != null && organizationUuid != null -> userRepository.findAllByDetailsOrganizationUuidAndKeywordsContainsIgnoreCase(organizationUuid, keywords, pageRequest)
        keywords != null && roles != null -> userRepository.findAllByRoleInAndKeywordsContainsIgnoreCase(roles, keywords, pageRequest)
        organizationUuid != null && roles != null -> userRepository.findAllByDetailsOrganizationUuidAndRoleIn(organizationUuid, roles, pageRequest)
        organizationUuid != null -> userRepository.findAllByDetailsOrganizationUuid(organizationUuid, pageRequest)
        keywords != null -> userRepository.findAllByKeywordsContainsIgnoreCase(keywords, pageRequest)
        roles != null -> userRepository.findAllByRoleIn(roles, pageRequest)
        else -> userRepository.findAll(pageRequest)
    }

    fun getPagedInvitations(
        pageRequest: PageRequest,
        organizationUuid: UUID,
        status: Invitation.Status?,
    ) = when {
        status != null -> invitationRepository.findAllByOrganizationUuidAndStatus(organizationUuid, status, pageRequest)
        else -> invitationRepository.findAllByOrganizationUuid(organizationUuid, pageRequest)
    }

    fun getUserByUuid(
        uuid: UUID,
    ) = userRepository.findByUuid(uuid)
        ?: throw UserDoesNotExistException()

    fun getUserBySlug(
        slug: String,
    ) = userRepository.findBySlug(slug)
        ?: throw UserDoesNotExistException()

    fun getInvitationByUuid(
        uuid: UUID,
    ) = invitationRepository.findByUuid(uuid)
        ?: throw InvitationDoesNotExistException()

    fun getTicketByUuid(
        uuid: UUID,
    ) = ticketRepository.findByUuid(uuid)
        ?: throw TicketDoesNotExistException()

    fun getUserByUuidAndPassword(
        uuid: UUID,
        password: String,
    ): User {
        val user = userRepository.findByUuid(uuid) ?: throw IncorrectCredentialsException()
        if (!BCrypt.checkpw(password, user.password)) throw IncorrectCredentialsException()
        return user
    }

    fun getUserByEmailAndPassword(
        email: String,
        password: String,
    ): User {
        val user = userRepository.findByEmailIgnoreCase(email) ?: throw IncorrectCredentialsException()
        if (!BCrypt.checkpw(password, user.password)) throw IncorrectCredentialsException()
        return user
    }

    fun createAdmin(
        dto: CreateAdminDTO,
    ): User {
        val formattedEmail = dto.email!!.trim().lowercase()
        if (userRepository.existsByEmailIgnoreCase(formattedEmail)) {
            throw UserAlreadyExistsException(formattedEmail)
        }

        return userRepository.save(
            User(
                creationDate = ZonedDateTime.now(clock),
                role = Role.ADMIN,
                status = Status.ACTIVATED,
                password = dto.password!!.hashedWithSalt(kareerConfig.salt!!),
                email = formattedEmail,
                lastName = dto.lastName!!.trim(),
                firstName = dto.firstName!!.trim(),
                nickname = null,
                slug = null,
                details = null,
                avatarReference = null,
                bannerReference = null,
                keywords = "admin",
                preferences = null,
            ),
        )
    }

    fun createUser(
        dto: CreateUserDTO,
        invitation: Invitation,
        organization: OrganizationDTO,
    ): User {
        val formattedEmail = dto.email!!.trim().lowercase()
        if (userRepository.existsByEmailIgnoreCase(formattedEmail)) {
            throw UserAlreadyExistsException(formattedEmail)
        }

        val now = ZonedDateTime.now(clock)
        val localNow = LocalDate.now(clock)
        val activate = dto.email.equals(invitation.inviteeEmail, true)

        // Create the new user
        val user = userRepository.saveAndFlush(
            User(
                creationDate = now,
                role = Role.USER,
                status = if (activate) Status.ACTIVATED else Status.REGISTERED,
                password = dto.password!!.hashedWithSalt(kareerConfig.salt!!),
                email = formattedEmail,
                lastName = dto.lastName!!.trim(),
                firstName = dto.firstName!!.trim(),
                nickname = dto.nickname.trimOrNullIfBlank() ?: dto.firstName,
                slug = dto.slug,
                details = UserDetails(
                    organizationUuid = organization.uuid,
                    phone = dto.details?.phone,
                    locationAddress = dto.details?.locationAddress.trimOrNullIfBlank(),
                    locationAddress2 = dto.details?.locationAddress2.trimOrNullIfBlank(),
                    locationCode = dto.details?.locationCode.trimOrNullIfBlank(),
                    locationCity = dto.details?.locationCity.trimOrNullIfBlank(),
                    locationCountry = dto.details?.locationCountry.trimOrNullIfBlank(),
                    title = dto.details?.title.trimOrNullIfBlank() ?: "${organization.name} Employee",
                    skills = skillsMap(dto.details?.skills),
                    experienceDate = dto.details?.experienceDate ?: localNow,
                    birthday = dto.details?.birthday,
                    startDate = localNow,
                    about = dto.details?.about,
                ),
                avatarReference = null,
                bannerReference = null,
                keywords = makeKeywords(dto.firstName, dto.lastName, dto.firstName, formattedEmail, organization.name),
                preferences = null,
            ),
        )

        // Create a ticket to activate the user later
        if (!activate) {
            ticketRepository.save(
                Ticket(
                    user = user,
                    creationDate = now,
                    token = UUID.randomUUID().toString().hashedWithSalt(kareerConfig.salt!!),
                    kind = Ticket.Kind.CONFIRM_EMAIL,
                ),
            )

            // TODO Send "confirm email address" mail
        }

        return user
    }

    fun createInvitation(
        dto: CreateInvitationDTO,
        manager: User,
        organization: OrganizationDTO,
    ): Invitation {
        val formattedEmail = dto.email!!.trim().lowercase()
        if (invitationRepository.existsByInviteeEmailIgnoreCase(formattedEmail)) {
            throw InvitationAlreadyExistsException(formattedEmail)
        }

        val invitation = invitationRepository.saveAndFlush(
            Invitation(
                inviter = manager,
                creationDate = ZonedDateTime.now(clock),
                inviteeEmail = formattedEmail,
                inviteeLastName = dto.lastName!!.trim(),
                inviteeFirstName = dto.firstName!!.trim(),
                status = Invitation.Status.PENDING,
            ),
        )

        val mailDTO = UserInvitationMailDTO(
            meta = MailMeta(
                language = LocaleContextHolder.getLocale().language,
                headerImage = organization.logoUrl ?: "https://kareer-dev.kommaboard.be/static/favicon.svg",
                headerText = organization.name,
                recipientEmail = invitation.inviteeEmail,
                recipientName = invitation.inviteeFirstName,
            ),
            inviterName = manager.fullName(),
            organizationName = organization.name,
            registrationLink = kareerConfig.registerUrl!!
                .replace("{invitationUuid}", invitation.uuid.toString())
                .replace("{inviteeEmail}", URLEncoder.encode(invitation.inviteeEmail, StandardCharsets.UTF_8)),
        )
        mailingQueueService.sendToQueue(mailDTO)

        return invitation
    }

    fun updateUser(
        user: User,
        dto: UpdateUserDTO,
    ): User {
        val formattedEmail = dto.email?.trimOrNullIfBlank()?.lowercase()
        val formattedSlug = dto.slug?.trimOrNullIfBlank()?.lowercase()

        if (formattedEmail != null && formattedEmail != user.email && userRepository.existsByEmailIgnoreCase(formattedEmail)) {
            throw UserAlreadyExistsException(formattedEmail)
        }
        if (user.details != null && formattedSlug != null && formattedSlug != user.slug && userRepository.existsByDetailsOrganizationUuidAndSlugIgnoreCase(user.details.organizationUuid, formattedSlug)) {
            throw UserAlreadyExistsException(formattedSlug)
        }

        val organizationName = user.keywords.substringBeforeLast(" ")

        return userRepository.save(
            user.apply {
                dto.role?.let { this.role = it.toRole() }
                dto.slug?.let { this.slug = formattedSlug }
                formattedEmail?.let { this.email = it }
                dto.password?.let { this.password = password.hashedWithSalt(kareerConfig.salt!!) }
                dto.lastName?.let { this.lastName = it.trim() }
                dto.firstName?.let { this.firstName = it.trim() }
                dto.nickname?.let { this.nickname = it.trimOrNullIfBlank() }
                keywords = makeKeywords(this.fullName(), this.firstName, this.email, organizationName)
            },
        )
    }

    fun updateUserDetails(
        user: User,
        dto: UpdateUserDetailsDTO,
    ): User {
        if (user.role != Role.USER && user.role != Role.MANAGER) {
            throw InvalidCredentialsException()
        }

        return userRepository.save(
            user.apply {
                this.details?.apply {
                    dto.phone?.let { this.phone = it }
                    dto.locationAddress?.let { this.locationAddress = it.trimOrNullIfBlank() }
                    dto.locationAddress2?.let { this.locationAddress2 = it.trimOrNullIfBlank() }
                    dto.locationCode?.let { this.locationCode = it.trimOrNullIfBlank() }
                    dto.locationCity?.let { this.locationCity = it.trimOrNullIfBlank() }
                    dto.locationCountry?.let { this.locationCountry = it.trimOrNullIfBlank() }
                    dto.title?.let { this.title = it }
                    dto.skills?.let { this.skills = skillsMap(it) }
                    dto.experienceDate?.let { this.experienceDate = it }
                    dto.birthday?.let { this.birthday = it }
                    dto.about?.let { this.about = it }
                }
            },
        )
    }

    fun updateUserAvatar(
        user: User,
        reference: String?,
    ) = userRepository.save(
        user.apply {
            this.avatarReference = reference
        },
    )

    fun updateUserBanner(
        user: User,
        reference: String?,
    ) = userRepository.save(
        user.apply {
            this.bannerReference = reference
        },
    )

    fun updateUserPreferences(
        user: User,
        preferences: String?,
    ) = userRepository.save(
        user.apply {
            this.preferences = preferences
        },
    )

    fun updateInvitationStatus(
        invitation: Invitation,
        status: Invitation.Status,
    ): Invitation {
        invitation.status = status

        return invitationRepository.save(invitation)
    }

    fun appendUserDetailsSkills(
        user: User,
        dto: EditUserDetailsSkillsDTO,
    ): User {
        if (user.details == null) {
            throw InvalidCredentialsException()
        }

        val updatedUser = user.apply {
            this.details.apply {
                dto.skills?.forEach {
                    if (it.isNotBlank()) {
                        this!!.skills[it.trim().lowercase().replace("\\W".toRegex(), "")] = it.trim()
                    }
                }
            }
        }

        // Magic number, but unfortunately no efficient way to sync this with @ItemSize constraint in lib-user
        if (updatedUser.details!!.skills.size > 15) {
            throw SkillLimitException(15)
        }

        return userRepository.save(updatedUser)
    }

    fun removeUserDetailsSkills(
        user: User,
        dto: EditUserDetailsSkillsDTO,
    ): User {
        if (user.details == null) {
            throw InvalidCredentialsException()
        }

        val updatedUser = user.apply {
            this.details.apply {
                dto.skills?.forEach {
                    if (it.isNotBlank()) {
                        this!!.skills.remove(it.trim().lowercase().replace("\\W".toRegex(), ""))
                    }
                }
            }
        }

        return userRepository.save(updatedUser)
    }

    fun confirmEmail(
        user: User,
        ticket: Ticket,
        ticketUuid: UUID,
        token: String,
    ): User {
        // Verify that passed token matches with ticket in the database
        if (token != ticket.token) throw TicketInvalidException(ticketUuid)
        // Verify that passed user UUID matches with user linked to ticket in the database
        if (user.uuid != ticket.user.uuid) throw TicketInvalidException(ticketUuid)
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

    private fun skillsMap(skills: List<String>?) = skills?.filterNot { it.isBlank() }?.associate { it.trim().lowercase().replace("\\W".toRegex(), "") to it.trim() }?.toMutableMap() ?: mutableMapOf()
}
