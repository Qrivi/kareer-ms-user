package be.kommaboard.kareer.user.lib.dto.request

import be.kommaboard.kareer.user.lib.constraint.AssignableRole
import be.kommaboard.kareer.user.lib.constraint.NotCommon
import be.kommaboard.kareer.user.lib.constraint.NotSimple
import java.util.Optional
import javax.validation.constraints.Email
import javax.validation.constraints.NotBlank
import javax.validation.constraints.Pattern
import javax.validation.constraints.Size

data class UpdateUserDTO(

    val email: Optional<
        @NotBlank(message = "{UpdateUserDTO.email.NotBlank}")
        @Size(message = "{UpdateUserDTO.email.Size}", max = 100)
        @Email(message = "{UpdateUserDTO.email.Email}")
        String>?,

    val phone: Optional<
        @Size(message = "{UpdateUserDTO.phone.Size}", max = 20)
        @Pattern(message = "{UpdateUserDTO.phone.Pattern}", regexp = "^(00|\\+)\\d+\$")
        String>?,

    val password: Optional<
        @NotBlank(message = "{UpdateUserDTO.password.NotBlank}")
        @Size(message = "{UpdateUserDTO.password.size}", min = 8)
        @NotCommon(message = "{UpdateUserDTO.password.NotCommon}")
        @NotSimple(message = "{UpdateUserDTO.password.NotSimple}")
        String>?,

    val lastName: Optional<
        @NotBlank(message = "{UpdateUserDTO.lastName.NotBlank}")
        @Size(message = "{UpdateUserDTO.lastName.Size}", max = 100)
        String>?,

    val firstName: Optional<
        @NotBlank(message = "{UpdateUserDTO.firstName.NotBlank}")
        @Size(message = "{UpdateUserDTO.firstName.Size}", max = 100)
        String>?,

    val nickname: Optional<
        @Size(message = "{UpdateUserDTO.nickname.Size}", max = 100)
        String?>?,

    val role: Optional<
        @AssignableRole(message = "{UpdateUserDTO.role.AssignableRole}")
        String>?,
)
