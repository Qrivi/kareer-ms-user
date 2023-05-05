package be.kommaboard.kareer.user.controller

import be.kommaboard.kareer.authorization.InternalHttpHeaders
import be.kommaboard.kareer.authorization.Role
import be.kommaboard.kareer.authorization.exception.InvalidCredentialsException
import be.kommaboard.kareer.authorization.util.authorizationCheck
import be.kommaboard.kareer.authorization.util.isRole
import be.kommaboard.kareer.authorization.util.toUuid
import be.kommaboard.kareer.common.dto.ListDTO
import be.kommaboard.kareer.common.exception.InvalidPageOrSizeException
import be.kommaboard.kareer.common.exception.RequestValidationException
import be.kommaboard.kareer.common.util.HttpHeadersBuilder
import be.kommaboard.kareer.common.util.toSort
import be.kommaboard.kareer.common.util.trimOrNullIfBlank
import be.kommaboard.kareer.user.KareerConfig
import be.kommaboard.kareer.user.lib.dto.request.CreateInvitationDTO
import be.kommaboard.kareer.user.lib.dto.request.UpdateInvitationDTO
import be.kommaboard.kareer.user.lib.dto.response.InvitationDTO
import be.kommaboard.kareer.user.proxy.OrganizationProxy
import be.kommaboard.kareer.user.repository.entity.Invitation
import be.kommaboard.kareer.user.repository.entity.toInvitationStatus
import be.kommaboard.kareer.user.service.UserService
import be.kommaboard.kareer.user.service.exception.InvitationStatusInvalidException
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
@RequestMapping("/invitations/v1")
class InvitationController(
    private val kareerConfig: KareerConfig,
    private val userService: UserService,
    private val organizationProxy: OrganizationProxy,
) {
    private val logger = LoggerFactory.getLogger(this.javaClass)

    @Operation(hidden = true)
    @GetMapping("/all")
    fun getAllInvitations(
        @RequestHeader(InternalHttpHeaders.CONSUMER_ROLE) consumerRole: String,
        @RequestHeader(InternalHttpHeaders.CONSUMER_ID) consumerId: String,
    ): ResponseEntity<ListDTO<InvitationDTO>> {
        logger.info("Handling GET /users/v1/invitations/all [getAllInvitations] for {}", consumerId)
        authorizationCheck(consumerId, kareerConfig.consumerId, consumerRole)

        val invitations = userService.getAllInvitations()

        return ResponseEntity
            .status(HttpStatus.OK)
            .headers(HttpHeadersBuilder().contentLanguage().build())
            .body(ListDTO(invitations.map(Invitation::toDTO)))
    }

    @Operation(
        summary = "Get invitations for my organization",
        description = "Returns the invitations for the manager's organization, so requires the `MANAGER`. `page` and `size` function as offset and limit.",
        responses = [ApiResponse(responseCode = "200")],
    )
    @GetMapping
    fun getInvitations(
        @RequestHeader(InternalHttpHeaders.CONSUMER_ROLE) consumerRole: String,
        @RequestHeader(InternalHttpHeaders.CONSUMER_ID) consumerId: String,
        @RequestParam status: String?,
        @RequestParam(defaultValue = "1") page: Int,
        @RequestParam(defaultValue = "10") size: Int,
        @RequestParam(defaultValue = "creationDate") sort: String,
        request: HttpServletRequest,
    ): ResponseEntity<ListDTO<InvitationDTO>> {
        logger.info("Handling GET /users/v1/invitations [getUsers] for {}", consumerId)
        authorizationCheck(consumerId, kareerConfig.consumerId, consumerRole, Role.MANAGER)

        if (page < 1 || size < 1) {
            throw InvalidPageOrSizeException()
        }

        val invitationsPage = userService.getPagedInvitations(
            pageRequest = PageRequest.of(page - 1, size, sort.toSort()),
            organizationUuid = userService.getUserByUuid(consumerId.toUuid()).details!!.organizationUuid,
            status = status.trimOrNullIfBlank()?.toInvitationStatus(),
        )

        return ResponseEntity
            .status(HttpStatus.OK)
            .headers(
                HttpHeadersBuilder()
                    .contentLanguage()
                    .link(request, invitationsPage)
                    .build(),
            )
            .body(ListDTO(invitationsPage.content.map(Invitation::toDTO), invitationsPage))
    }

    @Operation(
        summary = "Get an invitation by its UUID",
        description = "Gets the invitation which UUID matches the path variable. Requires `ADMIN` or `MANAGER` role. Managers can only get invitations belonging to their organization.",
        responses = [ApiResponse(responseCode = "200")],
    )
    @GetMapping("/{uuid}")
    fun getInvitation(
        @RequestHeader(InternalHttpHeaders.CONSUMER_ROLE) consumerRole: String,
        @RequestHeader(InternalHttpHeaders.CONSUMER_ID) consumerId: String,
        @PathVariable uuid: String,
    ): ResponseEntity<InvitationDTO> {
        logger.info("Handling GET /users/v1/invitation/{uuid} [getInvitation] for {}", consumerId)
        authorizationCheck(consumerId, kareerConfig.consumerId, consumerRole, Role.ADMIN, Role.MANAGER)

        val invitation = userService.getInvitationByUuid(uuid.toUuid())

        if (consumerRole.isRole(Role.MANAGER)) {
            val manager = userService.getUserByUuid(consumerId.toUuid())

            if (manager.details!!.organizationUuid != invitation.inviter.details!!.organizationUuid) {
                throw InvalidCredentialsException()
            }
        }

        return ResponseEntity
            .status(HttpStatus.OK)
            .headers(HttpHeadersBuilder().contentLanguage().build())
            .body(invitation.toDTO())
    }

    @Operation(
        summary = "Create a new invitation",
        description = "Requires the `MANAGER` role. Creates an invitation that will be sent by e-mail that recipient can use to register with.",
        responses = [ApiResponse(responseCode = "201")],
    )
    @PostMapping
    fun createInvitation(
        @RequestHeader(InternalHttpHeaders.CONSUMER_ROLE) consumerRole: String,
        @RequestHeader(InternalHttpHeaders.CONSUMER_ID) consumerId: String,
        @Valid @RequestBody
        dto: CreateInvitationDTO,
        validation: BindingResult,
    ): ResponseEntity<InvitationDTO> {
        logger.info("Handling POST /users/v1/invitations [createInvitation] for {}", consumerId)
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

        val invitation = userService.createInvitation(
            dto = dto,
            manager = manager,
            organization = organization,
        )

        return ResponseEntity
            .status(HttpStatus.CREATED)
            .headers(HttpHeadersBuilder().contentLanguage().build())
            .body(invitation.toDTO())
    }

    @Operation(
        summary = "Update an invitation",
        description = "Updates the invitation which UUID matches the path variable with the values passed in the request body. When using the `MANAGER` role, only changes to invitations belonging to the manager's organization will go through.",
        responses = [ApiResponse(responseCode = "200")],
    )
    @PatchMapping("/{uuid}")
    fun updateInvitation(
        @RequestHeader(InternalHttpHeaders.CONSUMER_ROLE) consumerRole: String,
        @RequestHeader(InternalHttpHeaders.CONSUMER_ID) consumerId: String,
        @PathVariable uuid: String,
        @Valid @RequestBody
        dto: UpdateInvitationDTO,
        validation: BindingResult,
    ): ResponseEntity<InvitationDTO> {
        logger.info("Handling PATCH /users/v1/invitations/{uuid} [updateInvitation] for {}", consumerId)
        authorizationCheck(consumerId, kareerConfig.consumerId, consumerRole, Role.MANAGER)

        val invitation = userService.getInvitationByUuid(uuid.toUuid())
        val status = dto.status?.toInvitationStatus() ?: throw InvitationStatusInvalidException(dto.status ?: "")

        // Extra requirements if performed as a manager
        if (consumerRole.isRole(Role.MANAGER)) {
            val manager = userService.getUserByUuid(consumerId.toUuid())

            // Managers can only update invitations sent by themselves or other managers in their organization
            if (manager.details!!.organizationUuid != invitation.inviter.details!!.organizationUuid) {
                throw InvalidCredentialsException()
            }

            // Managers can not accept or decline invitations (only the invitee can), but can retract invitations or undo retraction
            if (status != Invitation.Status.PENDING && status != Invitation.Status.RETRACTED) {
                throw InvitationStatusInvalidException(status.name.lowercase())
            }
        }

        val updatedInvitation = userService.updateInvitationStatus(
            invitation = invitation,
            status = status,
        )

        return ResponseEntity
            .status(HttpStatus.OK)
            .headers(HttpHeadersBuilder().contentLanguage().build())
            .body(updatedInvitation.toDTO())
    }
}
