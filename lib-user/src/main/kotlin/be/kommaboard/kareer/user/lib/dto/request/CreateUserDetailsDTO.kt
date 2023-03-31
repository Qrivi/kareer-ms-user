package be.kommaboard.kareer.user.lib.dto.request

import be.kommaboard.kareer.user.lib.constraint.ItemSize
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Past
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import java.time.LocalDate

data class CreateUserDetailsDTO(
    @get:Size(message = "{CreateUserInviteDTO.phone.Size}", max = 20)
    @get:Pattern(message = "{CreateUserInviteDTO.phone.Pattern}", regexp = "^(00|\\+)\\d+\$")
    val phone: String?,

    @get:Size(message = "{CreateUserInviteDTO.locationAddress.Size}", max = 100)
    val locationAddress: String?,

    @get:Size(message = "{CreateUserInviteDTO.locationAddress2.Size}", max = 100)
    val locationAddress2: String?,

    @get:Size(message = "{CreateUserInviteDTO.locationCode.Size}", max = 100)
    val locationCode: String?,

    @get:Size(message = "{CreateUserInviteDTO.locationCity.Size}", max = 100)
    val locationCity: String?,

    @get:Size(message = "{CreateUserInviteDTO.locationCountry.Size}", max = 100)
    val locationCountry: String?,

    @get:NotBlank(message = "{CreateUserInviteDTO.title.NotBlank}")
    @get:Size(message = "{CreateUserInviteDTO.title.Size}", max = 100)
    val title: String?,

    @get:NotNull(message = "{CreateUserInviteDTO.skills.NotNull}")
    @get:Size(message = "{CreateUserInviteDTO.skills.Size}", min = 1)
    @get:ItemSize(message = "{CreateUserInviteDTO.skills.ItemSize}", min = 1, max = 25)
    val skills: List<String>?,

    @get:Past(message = "{CreateUserInviteDTO.experience.Past}")
    val experience: LocalDate?,

    @get:Past(message = "{CreateUserInviteDTO.birthday.Past}")
    val birthday: LocalDate?,

    @get:Size(message = "{CreateUserInviteDTO.about.Size}", max = 1500)
    val about: String?,
)
