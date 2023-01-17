package be.kommaboard.kareer.user.controller

import be.kommaboard.kareer.common.dto.ErrorsDTO
import be.kommaboard.kareer.common.service.CommonMessageService
import be.kommaboard.kareer.common.util.HttpHeadersBuilder
import be.kommaboard.kareer.user.service.exception.IncorrectCredentialsException
import be.kommaboard.kareer.user.service.exception.InvalidInviteException
import be.kommaboard.kareer.user.service.exception.InvalidInviteStatusException
import be.kommaboard.kareer.user.service.exception.InvalidOrganizationUuidException
import be.kommaboard.kareer.user.service.exception.InviteDoesNotExistException
import be.kommaboard.kareer.user.service.exception.UserAlreadyExistsException
import be.kommaboard.kareer.user.service.exception.UserDoesNotExistException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler

@ControllerAdvice
class ExceptionResolver(
    private val commonMessageService: CommonMessageService,
) {

    @ExceptionHandler
    fun resolve(e: IncorrectCredentialsException) = ResponseEntity
        .status(HttpStatus.BAD_REQUEST)
        .headers(HttpHeadersBuilder().contentLanguage().build())
        .body(ErrorsDTO(commonMessageService["exception.IncorrectCredentials"]))

    @ExceptionHandler
    fun resolve(e: InvalidInviteException) = ResponseEntity
        .status(HttpStatus.BAD_REQUEST)
        .headers(HttpHeadersBuilder().contentLanguage().build())
        .body(ErrorsDTO(commonMessageService["exception.InvalidInvite"]))

    @ExceptionHandler
    fun resolve(e: InvalidInviteStatusException) = ResponseEntity
        .status(HttpStatus.BAD_REQUEST)
        .headers(HttpHeadersBuilder().contentLanguage().build())
        .body(ErrorsDTO(commonMessageService["exception.InvalidInviteStatus", e.value]))

    @ExceptionHandler
    fun resolve(e: InvalidOrganizationUuidException) = ResponseEntity
        .status(HttpStatus.BAD_REQUEST)
        .headers(HttpHeadersBuilder().contentLanguage().build())
        .body(ErrorsDTO(commonMessageService["exception.InvalidOrganizationUuid"]))

    @ExceptionHandler
    fun resolve(e: InviteDoesNotExistException) = ResponseEntity
        .status(HttpStatus.NOT_FOUND)
        .headers(HttpHeadersBuilder().contentLanguage().build())
        .body(ErrorsDTO(commonMessageService["exception.InviteDoesNotExist"]))

    @ExceptionHandler
    fun resolve(e: UserAlreadyExistsException) = ResponseEntity
        .status(HttpStatus.BAD_REQUEST)
        .headers(HttpHeadersBuilder().contentLanguage().build())
        .body(ErrorsDTO(commonMessageService["exception.UserAlreadyExists", e.email]))

    @ExceptionHandler
    fun resolve(e: UserDoesNotExistException) = ResponseEntity
        .status(HttpStatus.NOT_FOUND)
        .headers(HttpHeadersBuilder().contentLanguage().build())
        .body(ErrorsDTO(commonMessageService["exception.UserDoesNotExist"]))
}
