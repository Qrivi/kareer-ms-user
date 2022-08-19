package be.kommaboard.kareer.user.controller

import be.kommaboard.kareer.common.dto.ErrorsDTO
import be.kommaboard.kareer.common.service.MessageService
import be.kommaboard.kareer.common.util.HttpHeadersBuilder
import be.kommaboard.kareer.user.service.exception.IncorrectCredentialsException
import be.kommaboard.kareer.user.service.exception.InvalidInviteStatusException
import be.kommaboard.kareer.user.service.exception.InviteDoesNotExistException
import be.kommaboard.kareer.user.service.exception.OrganizationDoesNotExistException
import be.kommaboard.kareer.user.service.exception.UserAlreadyExistsException
import be.kommaboard.kareer.user.service.exception.UserDoesNotExistException
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler

@ControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
class ExceptionResolver(
    private val messageService: MessageService,
) {

    @ExceptionHandler
    fun resolve(e: IncorrectCredentialsException) = ResponseEntity
        .status(HttpStatus.BAD_REQUEST)
        .headers(HttpHeadersBuilder().contentLanguage().build())
        .body(ErrorsDTO(messageService["exception.IncorrectCredentials"]))

    @ExceptionHandler
    fun resolve(e: InvalidInviteStatusException) = ResponseEntity
        .status(HttpStatus.BAD_REQUEST)
        .headers(HttpHeadersBuilder().contentLanguage().build())
        .body(ErrorsDTO(messageService["exception.InvalidInviteStatus", e.value]))

    @ExceptionHandler
    fun resolve(e: InviteDoesNotExistException) = ResponseEntity
        .status(HttpStatus.NOT_FOUND)
        .headers(HttpHeadersBuilder().contentLanguage().build())
        .body(ErrorsDTO(messageService["exception.InviteDoesNotExist"]))

    @ExceptionHandler
    fun resolve(e: OrganizationDoesNotExistException) = ResponseEntity
        .status(HttpStatus.BAD_REQUEST)
        .headers(HttpHeadersBuilder().contentLanguage().build())
        .body(ErrorsDTO(messageService["exception.OrganizationDoesNotExist"]))

    @ExceptionHandler
    fun resolve(e: UserAlreadyExistsException) = ResponseEntity
        .status(HttpStatus.BAD_REQUEST)
        .headers(HttpHeadersBuilder().contentLanguage().build())
        .body(ErrorsDTO(messageService["exception.UserAlreadyExists", e.email]))

    @ExceptionHandler
    fun resolve(e: UserDoesNotExistException) = ResponseEntity
        .status(HttpStatus.NOT_FOUND)
        .headers(HttpHeadersBuilder().contentLanguage().build())
        .body(ErrorsDTO(messageService["exception.UserDoesNotExist"]))
}
