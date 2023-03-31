package be.kommaboard.kareer.user.controller

import be.kommaboard.kareer.authorization.InternalHttpHeaders
import be.kommaboard.kareer.authorization.Role
import be.kommaboard.kareer.authorization.authorizationCheck
import be.kommaboard.kareer.authorization.exception.InvalidCredentialsException
import be.kommaboard.kareer.authorization.isRole
import be.kommaboard.kareer.authorization.toUuid
import be.kommaboard.kareer.common.dto.ListDTO
import be.kommaboard.kareer.common.exception.InvalidPageOrSizeException
import be.kommaboard.kareer.common.exception.RequestValidationException
import be.kommaboard.kareer.common.toSort
import be.kommaboard.kareer.common.trimOrNullIfBlank
import be.kommaboard.kareer.common.util.HttpHeadersBuilder
import be.kommaboard.kareer.user.KareerConfig
import be.kommaboard.kareer.user.lib.dto.request.CreateInvitationDTO
import be.kommaboard.kareer.user.lib.dto.request.UpdateInvitationDTO
import be.kommaboard.kareer.user.lib.dto.response.InvitationDTO
import be.kommaboard.kareer.user.proxy.OrganizationProxy
import be.kommaboard.kareer.user.repository.entity.Invitation
import be.kommaboard.kareer.user.repository.entity.toInvitationStatus
import be.kommaboard.kareer.user.service.UserService
import be.kommaboard.kareer.user.service.exception.InvalidInviteStatusException
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageRequest
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

@RestController
@RequestMapping("/users/v1/invites")
class InvitationController(
    private val kareerConfig: KareerConfig,
    private val userService: UserService,
    private val organizationProxy: OrganizationProxy,
) {
    private val logger = LoggerFactory.getLogger(this.javaClass)

    @Operation(hidden = true)
    @GetMapping("/all")
    fun getAllInvites(
        @RequestHeader(InternalHttpHeaders.CONSUMER_ROLE) consumerRole: String,
        @RequestHeader(InternalHttpHeaders.CONSUMER_ID) consumerId: String,
    ): ResponseEntity<ListDTO<InvitationDTO>> {
        logger.info("Handling GET /users/v1/invites/all [getAllInvites] for {}", consumerId)
        authorizationCheck(consumerId, kareerConfig.consumerId, consumerRole)

        val invites = userService.getAllInvites()

        return ResponseEntity
            .status(HttpStatus.OK)
            .headers(HttpHeadersBuilder().contentLanguage().build())
            .body(ListDTO(invites.map(Invitation::toDTO)))
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
        @RequestParam(defaultValue = "1") page: Int,
        @RequestParam(defaultValue = "10") size: Int,
        @RequestParam(defaultValue = "creationDate") sort: String,
        request: HttpServletRequest,
    ): ResponseEntity<ListDTO<InvitationDTO>> {
        logger.info("Handling GET /users/v1/invites [getUsers] for {}", consumerId)
        authorizationCheck(consumerId, kareerConfig.consumerId, consumerRole, Role.MANAGER)

        if (page < 1 || size < 1) {
            throw InvalidPageOrSizeException()
        }

        val invitesPage = userService.getPagedInvites(
            pageRequest = PageRequest.of(page - 1, size, sort.toSort()),
            organizationUuid = userService.getUserByUuid(consumerId.toUuid()).details!!.organizationUuid,
            status = status.trimOrNullIfBlank()?.toInvitationStatus(),
        )

        return ResponseEntity
            .status(HttpStatus.OK)
            .headers(
                HttpHeadersBuilder()
                    .contentLanguage()
                    .link(request, invitesPage)
                    .build(),
            )
            .body(ListDTO(invitesPage.content.map(Invitation::toDTO), invitesPage))
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
    ): ResponseEntity<InvitationDTO> {
        logger.info("Handling GET /users/v1/invites/{uuid} [getInvite] for {}", consumerId)
        authorizationCheck(consumerId, kareerConfig.consumerId, consumerRole, Role.ADMIN, Role.MANAGER)

        val invite = userService.getInviteByUuid(uuid.toUuid())

        if (consumerRole.isRole(Role.MANAGER)) {
            val manager = userService.getUserByUuid(consumerId.toUuid())

            if (manager.details!!.organizationUuid != invite.inviter.details!!.organizationUuid) {
                throw InvalidCredentialsException()
            }
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
        @Valid @RequestBody
        dto: CreateInvitationDTO,
        validation: BindingResult,
    ): ResponseEntity<InvitationDTO> {
        logger.info("Handling POST /users/v1/invites [createInvite] for {}", consumerId)
        authorizationCheck(consumerId, kareerConfig.consumerId, consumerRole, Role.MANAGER)

        val manager = userService.getUserByUuid(consumerId.toUuid())

        val organization = organizationProxy.getOrganization(
            consumerRole = Role.SYSTEM.name,
            consumerId = kareerConfig.consumerId!!,
            uuid = manager.details!!.organizationUuid.toString(),
        )

        if (validation.hasErrors()) {
            throw RequestValidationException(validation)
        }

        // TODO Add a check to avoid creation of multiple invites to the same e-mail address

        val invite = userService.createInvite(
            dto = dto,
            manager = manager,
            organization = organization,
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
        @Valid @RequestBody
        dto: UpdateInvitationDTO,
        validation: BindingResult,
    ): ResponseEntity<InvitationDTO> {
        logger.info("Handling PATCH /users/v1/invites/{uuid} [updateInvite] for {}", consumerId)
        authorizationCheck(consumerId, kareerConfig.consumerId, consumerRole, Role.MANAGER)

        val invite = userService.getInviteByUuid(uuid.toUuid())
        val status = dto.status?.toInvitationStatus() ?: throw InvalidInviteStatusException(dto.status ?: "")

        // Extra requirements if performed as a manager
        if (consumerRole.isRole(Role.MANAGER)) {
            val manager = userService.getUserByUuid(consumerId.toUuid())

            // Managers can only update invites sent by themselves or other managers in their organization
            if (manager.details!!.organizationUuid != invite.inviter.details!!.organizationUuid) {
                throw InvalidCredentialsException()
            }

            // Managers can not accept or decline invites (only the invitee can), but can retract invites or undo retraction
            if (status != Invitation.Status.PENDING && status != Invitation.Status.RETRACTED) {
                throw InvalidInviteStatusException(status.name.lowercase())
            }
        }

        val updatedInvite = userService.updateInviteStatus(
            invitation = invite,
            status = status,
        )

        return ResponseEntity
            .status(HttpStatus.OK)
            .headers(HttpHeadersBuilder().contentLanguage().build())
            .body(updatedInvite.toDTO())
    }
}
