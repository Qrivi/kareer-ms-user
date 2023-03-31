package be.kommaboard.kareer.user.lib.dto.response

import java.time.LocalDate
import java.util.UUID

data class UserDetailsDTO(
    val organizationUuid: UUID?,
    val startDate: LocalDate,
    val phone: String?,
    val locationAddress: String?,
    val locationAddress2: String?,
    val locationCode: String?,
    val locationCity: String?,
    val locationCountry: String?,
    val title: String,
    val skills: List<String>,
    val experienceDate: LocalDate,
    val birthday: LocalDate?,
    val about: String,
)
