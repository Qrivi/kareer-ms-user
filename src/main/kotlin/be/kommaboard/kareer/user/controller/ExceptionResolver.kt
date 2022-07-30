package be.kommaboard.kareer.user.controller

import be.kommaboard.kareer.common.dto.response.ErrorDTO
import be.kommaboard.kareer.common.service.MessageService
import be.kommaboard.kareer.user.service.exception.UserAlreadyExistsException
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler

@ControllerAdvice
class ExceptionResolver(
    private val messageService: MessageService,
) {

    @ExceptionHandler
    fun resolve(e: UserAlreadyExistsException) =
        ErrorDTO.BadRequest(
            errors = listOf(messageService["exception.UserAlreadyExistsException", e.email]),
        ).buildEntity()
}
