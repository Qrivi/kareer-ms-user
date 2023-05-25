package be.kommaboard.kareer.user.lib.dto.request

import be.kommaboard.kareer.user.lib.constraint.ItemSize
import jakarta.validation.constraints.Past
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import java.time.LocalDate

data class CreateUserDetailsDTO(

    @get:Size(message = "{CreateUserDetailsDTO.phone.Size}", max = 20)
    @get:Pattern(message = "{CreateUserDetailsDTO.phone.Pattern}", regexp = "^\\+\\d+\$")
    val phone: String?,

    @get:Size(message = "{CreateUserDetailsDTO.locationAddress.Size}", max = 100)
    val locationAddress: String?,

    @get:Size(message = "{CreateUserDetailsDTO.locationAddress2.Size}", max = 100)
    val locationAddress2: String?,

    @get:Size(message = "{CreateUserDetailsDTO.locationCode.Size}", max = 100)
    val locationCode: String?,

    @get:Size(message = "{CreateUserDetailsDTO.locationCity.Size}", max = 100)
    val locationCity: String?,

    @get:Size(message = "{CreateUserDetailsDTO.locationCountry.Size}", max = 2)
    val locationCountry: String?,

    @get:Size(message = "{CreateUserDetailsDTO.title.Size}", max = 100)
    val title: String?,

    @get:Size(message = "{CreateUserDetailsDTO.skills.Size}", max = 15)
    @get:ItemSize(message = "{CreateUserDetailsDTO.skills.ItemSize}", min = 1, max = 25)
    val skills: List<String>?,

    @get:Past(message = "{CreateUserDetailsDTO.experienceDate.Past}")
    val experienceDate: LocalDate?,

    @get:Past(message = "{CreateUserDetailsDTO.birthday.Past}")
    val birthday: LocalDate?,

    @get:Size(message = "{CreateUserDetailsDTO.about.Size}", max = 1500)
    val about: String?,
)
