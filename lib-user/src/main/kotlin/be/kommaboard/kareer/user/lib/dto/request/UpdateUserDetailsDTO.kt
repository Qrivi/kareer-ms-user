package be.kommaboard.kareer.user.lib.dto.request

import be.kommaboard.kareer.user.lib.constraint.ItemSize
import jakarta.validation.constraints.Past
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import java.time.LocalDate

data class UpdateUserDetailsDTO(

    @get:Size(message = "{UpdateUserDetailsDTO.phone.Size}", max = 20)
    @get:Pattern(message = "{UpdateUserDetailsDTO.phone.Pattern}", regexp = "^\\+\\d+\$")
    val phone: String?,

    @get:Pattern(message = "{UpdateUserDetailsDTO.locationAddress.NotBlank}", regexp = "^(?!\\s*$).+")
    @get:Size(message = "{UpdateUserDetailsDTO.locationAddress.Size}", max = 100)
    val locationAddress: String?,

    @get:Size(message = "{UpdateUserDetailsDTO.locationAddress2.Size}", max = 100)
    val locationAddress2: String?,

    @get:Pattern(message = "{UpdateUserDetailsDTO.locationCode.NotBlank}", regexp = "^(?!\\s*$).+")
    @get:Size(message = "{UpdateUserDetailsDTO.locationCode.Size}", max = 100)
    val locationCode: String?,

    @get:Pattern(message = "{UpdateUserDetailsDTO.locationCity.NotBlank}", regexp = "^(?!\\s*$).+")
    @get:Size(message = "{UpdateUserDetailsDTO.locationCity.Size}", max = 100)
    val locationCity: String?,

    @get:Pattern(message = "{UpdateUserDetailsDTO.locationCountry.NotBlank}", regexp = "^(?!\\s*$).+")
    @get:Size(message = "{UpdateUserDetailsDTO.locationCountry.Size}", max = 2)
    val locationCountry: String?,

    @get:Pattern(message = "{UpdateUserDetailsDTO.title.NotBlank}", regexp = "^(?!\\s*$).+")
    @get:Size(message = "{UpdateUserDetailsDTO.title.Size}", max = 100)
    val title: String?,

    @get:Size(message = "{UpdateUserDetailsDTO.skills.Size}", max = 15)
    @get:ItemSize(message = "{UpdateUserDetailsDTO.skills.ItemSize}", min = 1, max = 25)
    val skills: List<String>?,

    @get:Past(message = "{UpdateUserDetailsDTO.experienceDate.Past}")
    val experienceDate: LocalDate?,

    @get:Past(message = "{UpdateUserDetailsDTO.birthday.Past}")
    val birthday: LocalDate?,

    @get:Past(message = "{UpdateUserDetailsDTO.startDate.Past}")
    val startDate: LocalDate?,

    @get:Size(message = "{UpdateUserDetailsDTO.about.Size}", max = 1500)
    val about: String?,
)
