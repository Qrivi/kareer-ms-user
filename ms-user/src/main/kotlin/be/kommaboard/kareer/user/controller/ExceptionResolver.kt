package be.kommaboard.kareer.user.controller

import be.kommaboard.kareer.common.dto.ErrorsDTO
import be.kommaboard.kareer.common.service.CommonMessageService
import be.kommaboard.kareer.common.util.HttpHeadersBuilder
import be.kommaboard.kareer.user.service.exception.IncorrectCredentialsException
import be.kommaboard.kareer.user.service.exception.InvalidInvitationException
import be.kommaboard.kareer.user.service.exception.InvalidInvitationStatusException
import be.kommaboard.kareer.user.service.exception.InvitationDoesNotExistException
import be.kommaboard.kareer.user.service.exception.SkillLimitException
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
    fun resolve(e: InvalidInvitationException) = ResponseEntity
        .status(HttpStatus.BAD_REQUEST)
        .headers(HttpHeadersBuilder().contentLanguage().build())
        .body(ErrorsDTO(commonMessageService["exception.InvalidInvitation"]))

    @ExceptionHandler
    fun resolve(e: InvalidInvitationStatusException) = ResponseEntity
        .status(HttpStatus.BAD_REQUEST)
        .headers(HttpHeadersBuilder().contentLanguage().build())
        .body(ErrorsDTO(commonMessageService["exception.InvalidInvitationStatus", e.value]))

    @ExceptionHandler
    fun resolve(e: InvitationDoesNotExistException) = ResponseEntity
        .status(HttpStatus.NOT_FOUND)
        .headers(HttpHeadersBuilder().contentLanguage().build())
        .body(ErrorsDTO(commonMessageService["exception.InvitationDoesNotExist"]))

    @ExceptionHandler
    fun resolve(e: SkillLimitException) = ResponseEntity
        .status(HttpStatus.BAD_REQUEST)
        .headers(HttpHeadersBuilder().contentLanguage().build())
        .body(ErrorsDTO(commonMessageService["exception.SkillLimitException", e.limit]))

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
