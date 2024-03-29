package be.kommaboard.kareer.user.lib.dto.request

import be.kommaboard.kareer.user.lib.constraint.NotCommonPassword
import be.kommaboard.kareer.user.lib.constraint.NotSimplePassword
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class CreateAdminDTO(

    @get:NotBlank(message = "{CreateAdminDTO.email.NotBlank}")
    @get:Size(message = "{CreateAdminDTO.email.Size}", max = 100)
    @get:Email(message = "{CreateAdminDTO.email.Email}")
    val email: String?,

    @get:NotBlank(message = "{CreateAdminDTO.password.NotBlank}")
    @get:Size(message = "{CreateAdminDTO.password.Size}", min = 8)
    @get:NotCommonPassword(message = "{CreateAdminDTO.password.NotCommonPassword}")
    @get:NotSimplePassword(message = "{CreateAdminDTO.password.NotSimplePassword}")
    val password: String?,

    @get:NotBlank(message = "{CreateAdminDTO.lastName.NotBlank}")
    @get:Size(message = "{CreateAdminDTO.lastName.Size}", max = 100)
    val lastName: String?,

    @get:NotBlank(message = "{CreateAdminDTO.firstName.NotBlank}")
    @get:Size(message = "{CreateAdminDTO.firstName.Size}", max = 100)
    val firstName: String?,
)
