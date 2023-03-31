package be.kommaboard.kareer.user.repository.entity

import be.kommaboard.kareer.user.lib.dto.response.UserDetailsDTO
import jakarta.persistence.CollectionTable
import jakarta.persistence.Column
import jakarta.persistence.ElementCollection
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.MapKeyColumn
import jakarta.persistence.Table
import org.hibernate.annotations.GenericGenerator
import java.time.LocalDate
import java.util.UUID

@Entity
@Table(name = "details")
class UserDetails(

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(
        name = "UUID",
        strategy = "org.hibernate.id.UUIDGenerator",
    )
    @Column(name = "uuid")
    val uuid: UUID? = null,

    @Column(name = "organization_uuid")
    val organizationUuid: UUID,

    @Column(name = "phone")
    var phone: String?,

    @Column(name = "location_address")
    var locationAddress: String?,

    @Column(name = "location_address2")
    var locationAddress2: String?,

    @Column(name = "location_code")
    var locationCode: String?,

    @Column(name = "location_city")
    var locationCity: String?,

    @Column(name = "location_country")
    var locationCountry: String?,

    @Column(name = "title")
    var title: String,

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
        name = "user_skills",
        joinColumns = [JoinColumn(name = "user_uuid", referencedColumnName = "uuid")],
    )
    @MapKeyColumn(name = "skill_key")
    @Column(name = "skill_value")
    var skills: MutableMap<String, String>,

    @Column(name = "experience")
    var experience: LocalDate,

    @Column(name = "birthday")
    var birthday: LocalDate?,

    @Column(name = "start_date")
    val startDate: LocalDate,

    @Column(name = "about")
    var about: String,
) {

    fun toDTO() = UserDetailsDTO(
        organizationUuid = organizationUuid,
        phone = phone,
        locationAddress = locationAddress,
        locationAddress2 = locationAddress2,
        locationCode = locationCode,
        locationCity = locationCity,
        locationCountry = locationCountry,
        title = title,
        skills = skills.values.toList(),
        experience = experience,
        birthday = birthday,
        startDate = startDate,
        about = about,
    )
}
