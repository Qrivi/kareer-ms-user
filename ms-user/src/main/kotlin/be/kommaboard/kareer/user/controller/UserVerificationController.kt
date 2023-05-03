package be.kommaboard.kareer.user.controller

import be.kommaboard.kareer.authorization.InternalHttpHeaders
import be.kommaboard.kareer.authorization.util.authorizationCheck
import be.kommaboard.kareer.common.exception.RequestValidationException
import be.kommaboard.kareer.common.util.HttpHeadersBuilder
import be.kommaboard.kareer.user.KareerConfig
import be.kommaboard.kareer.user.lib.dto.request.VerifyCredentialsDTO
import be.kommaboard.kareer.user.lib.dto.request.VerifyInvitationDTO
import be.kommaboard.kareer.user.lib.dto.response.InvitationDTO
import be.kommaboard.kareer.user.lib.dto.response.UserDTO
import be.kommaboard.kareer.user.service.UserService
import be.kommaboard.kareer.user.service.exception.InvitationInvalidException
import be.kommaboard.kareer.user.service.exception.InvitationMismatchException
import be.kommaboard.kareer.user.service.exception.UserAlreadyExistsException
import io.swagger.v3.oas.annotations.Operation
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.validation.BindingResult
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.Clock

@RestController
@RequestMapping("/userverification/v1")
class UserVerificationController(
    private val kareerConfig: KareerConfig,
    private val userService: UserService,
) {
    private val logger = LoggerFactory.getLogger(this.javaClass)

    @Operation(hidden = true)
    @PostMapping
    fun verifyCredentials(
        @RequestHeader(InternalHttpHeaders.CONSUMER_ROLE) consumerRole: String,
        @RequestHeader(InternalHttpHeaders.CONSUMER_ID) consumerId: String,
        @Valid @RequestBody
        dto: VerifyCredentialsDTO,
        validation: BindingResult,
    ): ResponseEntity<UserDTO> {
        logger.info("Handling POST /users/v1/verification [verifyCredentials] for {}", consumerId)
        authorizationCheck(consumerId, kareerConfig.consumerId, consumerRole)

        if (validation.hasErrors()) {
            throw RequestValidationException(validation)
        }

        val user = userService.getUserByEmailAndPassword(
            email = dto.email!!,
            password = dto.password!!,
        )

        return ResponseEntity
            .status(HttpStatus.OK)
            .headers(HttpHeadersBuilder().contentLanguage().build())
            .body(user.toDTO())
    }

    @Operation(hidden = true)
    @PostMapping("/invitation")
    fun verifyInvitation( // Simple check to call when loading registration page, to make sure the invite is valid
        @RequestHeader(InternalHttpHeaders.CONSUMER_ROLE) consumerRole: String,
        @RequestHeader(InternalHttpHeaders.CONSUMER_ID) consumerId: String,
        @RequestBody dto: VerifyInvitationDTO,
    ): ResponseEntity<InvitationDTO> {
        logger.info("Handling POST /users/v1/verification/invitation [verifyInvitation] for {}", consumerId)
        authorizationCheck(consumerId, kareerConfig.consumerId, consumerRole)

        if (dto.invitationUuid == null || dto.email == null) {
            throw InvitationMismatchException()
        }

        val invitation = userService.getInvitationByUuid(dto.invitationUuid!!)

        if (invitation.inviteeEmail != dto.email) {
            throw InvitationMismatchException()
        }

        return ResponseEntity
            .status(HttpStatus.OK)
            .headers(HttpHeadersBuilder().contentLanguage().build())
            .body(invitation.toDTO())
    }

    @Operation(hidden = true)
    @PostMapping("/registration")
    fun verifyRegistration( // Simple check to make sure registering with a different e-mailadres than the invite will work
        @RequestHeader(InternalHttpHeaders.CONSUMER_ROLE) consumerRole: String,
        @RequestHeader(InternalHttpHeaders.CONSUMER_ID) consumerId: String,
        @RequestBody dto: VerifyInvitationDTO,
    ): ResponseEntity<Unit> {
        logger.info("Handling POST /users/v1/verification/registration [verifyRegistration] for {}", consumerId)
        authorizationCheck(consumerId, kareerConfig.consumerId, consumerRole)

        if (dto.invitationUuid == null || dto.email == null) {
            throw InvitationInvalidException()
        }

        // Throws InvitationDoesNotExistException
        userService.getInvitationByUuid(dto.invitationUuid!!)

        if (userService.userEmailInUse(dto.email!!)) {
            throw UserAlreadyExistsException(dto.email!!)
        }

        return ResponseEntity
            .noContent()
            .build()
    }
}
