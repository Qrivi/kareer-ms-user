package be.kommaboard.kareer.user.controller

import be.kommaboard.kareer.authorization.InternalHttpHeaders
import be.kommaboard.kareer.authorization.Role
import be.kommaboard.kareer.authorization.authorizationCheck
import be.kommaboard.kareer.authorization.exception.InvalidCredentialsException
import be.kommaboard.kareer.authorization.toUuid
import be.kommaboard.kareer.common.dto.ListDTO
import be.kommaboard.kareer.common.exception.InvalidPageOrSizeException
import be.kommaboard.kareer.common.exception.RequestValidationException
import be.kommaboard.kareer.common.trimOrNullIfBlank
import be.kommaboard.kareer.common.util.HttpHeadersBuilder
import be.kommaboard.kareer.user.UserConfig
import be.kommaboard.kareer.user.lib.dto.request.CreateInviteDTO
import be.kommaboard.kareer.user.lib.dto.request.UpdateInviteDTO
import be.kommaboard.kareer.user.lib.dto.response.InviteDTO
import be.kommaboard.kareer.user.proxy.OrganizationProxy
import be.kommaboard.kareer.user.repository.entity.Invite
import be.kommaboard.kareer.user.service.UserService
import be.kommaboard.kareer.user.service.exception.InvalidInviteStatusException
import be.kommaboard.kareer.user.toInviteStatus
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.validation.BindingResult
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import javax.servlet.http.HttpServletRequest
import javax.validation.Valid

@RestController
@RequestMapping("/users/v1/invites")
class InviteController(
    private val userConfig: UserConfig,
    private val userService: UserService,
    private val organizationProxy: OrganizationProxy,
) {
    private val logger = LoggerFactory.getLogger(this.javaClass)

    @Operation(hidden = true)
    @GetMapping("/all")
    fun getAllInvites(
        @RequestHeader(InternalHttpHeaders.CONSUMER_ROLE) consumerRole: String,
        @RequestHeader(InternalHttpHeaders.CONSUMER_ID) consumerId: String,
    ): ResponseEntity<ListDTO<InviteDTO>> {
        logger.info("Handling GET /users/v1/invites/all [getAllInvites] for {}", consumerId)
        authorizationCheck(consumerId, userConfig.consumerId, consumerRole)

        val invites = userService.getAllInvites()

        return ResponseEntity
            .status(HttpStatus.OK)
            .headers(HttpHeadersBuilder().contentLanguage().build())
            .body(ListDTO(invites.map(Invite::toDTO)))
    }

    @Operation(
        summary = "Get invites for my organization",
        description = "Returns the invites for the manager's organization, so requires the `MANAGER`. `page` and `size` function as offset and limit.",
        responses = [ApiResponse(responseCode = "200")],
    )
    @GetMapping
    fun getInvites(
        @RequestHeader(InternalHttpHeaders.CONSUMER_ROLE) consumerRole: String,
        @RequestHeader(InternalHttpHeaders.CONSUMER_ID) consumerId: String,
        @RequestParam status: String?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "10") size: Int,
        @RequestParam sort: String?,
        request: HttpServletRequest,
    ): ResponseEntity<ListDTO<InviteDTO>> {
        logger.info("Handling GET /users/v1/invites [getUsers] for {}", consumerId)
        authorizationCheck(consumerId, userConfig.consumerId, consumerRole, Role.MANAGER)

        if (page < 0 || size < 1)
            throw InvalidPageOrSizeException()

        val invitesPage = userService.getPagedInvites(
            pageRequest = if (sort.isNullOrBlank()) PageRequest.of(page, size, Sort.unsorted()) else PageRequest.of(page, size, Sort.by(*sort.split(',').toTypedArray())),
            organizationUuid = userService.getUserByUuid(consumerId.toUuid()).organizationUuid!!,
            status = status.trimOrNullIfBlank()?.toInviteStatus(),
        )

        return ResponseEntity
            .status(HttpStatus.OK)
            .headers(
                HttpHeadersBuilder()
                    .contentLanguage()
                    .link(request, invitesPage)
                    .build()
            )
            .body(ListDTO(invitesPage.content.map(Invite::toDTO), invitesPage))
    }

    @Operation(
        summary = "Get an invite by its UUID",
        description = "Gets the invite which UUID matches the path variable. Requires `ADMIN` or `MANAGER` role. Managers can only get invites belonging to their organization.",
        responses = [ApiResponse(responseCode = "200")],
    )
    @GetMapping("/{uuid}")
    fun getInvite(
        @RequestHeader(InternalHttpHeaders.CONSUMER_ROLE) consumerRole: String,
        @RequestHeader(InternalHttpHeaders.CONSUMER_ID) consumerId: String,
        @PathVariable uuid: String,
    ): ResponseEntity<InviteDTO> {
        logger.info("Handling GET /users/v1/invites/{uuid} [getInvite] for {}", consumerId)
        authorizationCheck(consumerId, userConfig.consumerId, consumerRole, Role.ADMIN, Role.MANAGER)

        val invite = userService.getInviteByUuid(uuid.toUuid())

        if (Role.MANAGER.matches(consumerRole)) {
            val manager = userService.getUserByUuid(consumerId.toUuid())

            if (manager.organizationUuid != invite.inviter.organizationUuid)
                throw InvalidCredentialsException()
        }

        return ResponseEntity
            .status(HttpStatus.OK)
            .headers(HttpHeadersBuilder().contentLanguage().build())
            .body(invite.toDTO())
    }

    @Operation(
        summary = "Create a new invite",
        description = "Requires the `MANAGER` role. Creates an invite that will be sent by e-mail that recipient can use to register with.",
        responses = [ApiResponse(responseCode = "201")],
    )
    @PostMapping
    fun createInvite(
        @RequestHeader(InternalHttpHeaders.CONSUMER_ROLE) consumerRole: String,
        @RequestHeader(InternalHttpHeaders.CONSUMER_ID) consumerId: String,
        @Valid @RequestBody dto: CreateInviteDTO,
        validation: BindingResult,
    ): ResponseEntity<InviteDTO> {
        logger.info("Handling POST /users/v1/invites [createInvite] for {}", consumerId)
        authorizationCheck(consumerId, userConfig.consumerId, consumerRole, Role.MANAGER)

        val manager = userService.getUserByUuid(consumerId.toUuid())

        val organization = organizationProxy.getOrganization(
            consumerRole = Role.SYSTEM.name,
            consumerId = userConfig.consumerId!!,
            uuid = manager.organizationUuid.toString(),
        )

        if (validation.hasErrors())
            throw RequestValidationException(validation)

        val invite = userService.createInvite(
            manager = manager,
            organization = organization,
            inviteeEmail = dto.email!!,
            inviteeLastName = dto.lastName!!,
            inviteeFirstName = dto.firstName!!,
        )

        return ResponseEntity
            .status(HttpStatus.CREATED)
            .headers(HttpHeadersBuilder().contentLanguage().build())
            .body(invite.toDTO())
    }

    @Operation(
        summary = "Update an invite",
        description = "Updates the invite which UUID matches the path variable with the values passed in the request body. When using the `MANAGER` role, only changes to invites belonging to the manager's organization will go through.",
        responses = [ApiResponse(responseCode = "200")],
    )
    @PatchMapping("/{uuid}")
    fun updateInvite(
        @RequestHeader(InternalHttpHeaders.CONSUMER_ROLE) consumerRole: String,
        @RequestHeader(InternalHttpHeaders.CONSUMER_ID) consumerId: String,
        @PathVariable uuid: String,
        @Valid @RequestBody dto: UpdateInviteDTO,
        validation: BindingResult,
    ): ResponseEntity<InviteDTO> {
        logger.info("Handling PATCH /users/v1/invites/{uuid} [updateInvite] for {}", consumerId)
        authorizationCheck(consumerId, userConfig.consumerId, consumerRole, Role.MANAGER)

        val invite = userService.getInviteByUuid(uuid.toUuid())
        val status = dto.status!!.toInviteStatus()

        // Extra requirements if performed as a manager
        if (Role.MANAGER.matches(consumerRole)) {
            val manager = userService.getUserByUuid(consumerId.toUuid())

            // Managers can only update invites sent by themselves or other managers in their organization
            if (manager.organizationUuid != invite.inviter.organizationUuid)
                throw InvalidCredentialsException()

            // Managers can not accept or decline invites (only the invitee can), but can retract invites or undo retraction
            if (status != Invite.Status.PENDING && status != Invite.Status.RETRACTED)
                throw InvalidInviteStatusException(dto.status!!.trim())
        }

        val updatedInvite = userService.updateInvite(
            invite = invite,
            status = status,
        )

        return ResponseEntity
            .status(HttpStatus.OK)
            .headers(HttpHeadersBuilder().contentLanguage().build())
            .body(updatedInvite.toDTO())
    }
}
