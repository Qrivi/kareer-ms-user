package be.kommaboard.kareer.user.controller

import be.kommaboard.kareer.common.dto.ErrorsDTO
import be.kommaboard.kareer.common.service.CommonMessageService
import be.kommaboard.kareer.common.util.HttpHeadersBuilder
import be.kommaboard.kareer.user.service.exception.IncorrectCredentialsException
import be.kommaboard.kareer.user.service.exception.InvitationDoesNotExistException
import be.kommaboard.kareer.user.service.exception.InvitationInvalidException
import be.kommaboard.kareer.user.service.exception.InvitationMismatchException
import be.kommaboard.kareer.user.service.exception.InvitationStatusInvalidException
import be.kommaboard.kareer.user.service.exception.SkillLimitException
import be.kommaboard.kareer.user.service.exception.TicketAlreadyUsedException
import be.kommaboard.kareer.user.service.exception.TicketDoesNotExistException
import be.kommaboard.kareer.user.service.exception.TicketExpiredException
import be.kommaboard.kareer.user.service.exception.TicketInvalidException
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
    fun resolve(e: InvitationDoesNotExistException) = ResponseEntity
        .status(HttpStatus.NOT_FOUND)
        .headers(HttpHeadersBuilder().contentLanguage().build())
        .body(ErrorsDTO(commonMessageService["exception.InvitationDoesNotExist"]))

    @ExceptionHandler
    fun resolve(e: InvitationInvalidException) = ResponseEntity
        .status(HttpStatus.BAD_REQUEST)
        .headers(HttpHeadersBuilder().contentLanguage().build())
        .body(ErrorsDTO(commonMessageService["exception.InvitationInvalid"]))

    @ExceptionHandler
    fun resolve(e: InvitationMismatchException) = ResponseEntity
        .status(HttpStatus.BAD_REQUEST)
        .headers(HttpHeadersBuilder().contentLanguage().build())
        .body(ErrorsDTO(commonMessageService["exception.InvitationMismatch"]))

    @ExceptionHandler
    fun resolve(e: InvitationStatusInvalidException) = ResponseEntity
        .status(HttpStatus.BAD_REQUEST)
        .headers(HttpHeadersBuilder().contentLanguage().build())
        .body(ErrorsDTO(commonMessageService["exception.InvitationStatusInvalid", e.value]))

    @ExceptionHandler
    fun resolve(e: SkillLimitException) = ResponseEntity
        .status(HttpStatus.BAD_REQUEST)
        .headers(HttpHeadersBuilder().contentLanguage().build())
        .body(ErrorsDTO(commonMessageService["exception.SkillLimitException", e.limit]))

    @ExceptionHandler
    fun resolve(e: TicketAlreadyUsedException) = ResponseEntity
        .status(HttpStatus.BAD_REQUEST)
        .headers(HttpHeadersBuilder().contentLanguage().build())
        .body(ErrorsDTO(commonMessageService["exception.TicketAlreadyUsed"]))

    @ExceptionHandler
    fun resolve(e: TicketDoesNotExistException) = ResponseEntity
        .status(HttpStatus.BAD_REQUEST)
        .headers(HttpHeadersBuilder().contentLanguage().build())
        .body(ErrorsDTO(commonMessageService["exception.TicketDoesNotExist"]))

    @ExceptionHandler
    fun resolve(e: TicketExpiredException) = ResponseEntity
        .status(HttpStatus.NOT_FOUND)
        .headers(HttpHeadersBuilder().contentLanguage().build())
        .body(ErrorsDTO(commonMessageService["exception.TicketExpired"]))

    @ExceptionHandler
    fun resolve(e: TicketInvalidException) = ResponseEntity
        .status(HttpStatus.NOT_FOUND)
        .headers(HttpHeadersBuilder().contentLanguage().build())
        .body(ErrorsDTO(commonMessageService["exception.TicketInvalid"]))

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
