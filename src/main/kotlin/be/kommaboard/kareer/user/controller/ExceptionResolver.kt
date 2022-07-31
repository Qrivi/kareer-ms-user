package be.kommaboard.kareer.user.controller

import be.kommaboard.kareer.common.dto.response.ErrorDTO
import be.kommaboard.kareer.common.service.MessageService
import be.kommaboard.kareer.user.service.exception.IncorrectCredentialsException
import be.kommaboard.kareer.user.service.exception.UserAlreadyExistsException
import be.kommaboard.kareer.user.service.exception.UserDoesNotExistException
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler

@ControllerAdvice
class ExceptionResolver(
    private val messageService: MessageService,
) {

    @ExceptionHandler
    fun resolve(e: IncorrectCredentialsException) =
        ErrorDTO.BadRequest(
            errors = listOf(messageService["exception.IncorrectCredentials"]),
        ).buildEntity()

    @ExceptionHandler
    fun resolve(e: UserAlreadyExistsException) =
        ErrorDTO.BadRequest(
            errors = listOf(messageService["exception.UserAlreadyExists", e.email]),
        ).buildEntity()

    @ExceptionHandler
    fun resolve(e: UserDoesNotExistException) =
        ErrorDTO.NotFound(
            errors = listOf(messageService["exception.UserDoesNotExist"]),
        ).buildEntity()
}
