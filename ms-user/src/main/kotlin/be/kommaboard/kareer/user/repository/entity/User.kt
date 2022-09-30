package be.kommaboard.kareer.user.repository.entity

import be.kommaboard.kareer.authorization.Role
import be.kommaboard.kareer.authorization.Status
import be.kommaboard.kareer.user.lib.dto.response.UserDTO
import org.hibernate.annotations.GenericGenerator
import java.time.ZonedDateTime
import java.util.UUID
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.EnumType
import javax.persistence.Enumerated
import javax.persistence.GeneratedValue
import javax.persistence.Id

@Entity
class User(

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(
        name = "UUID",
        strategy = "org.hibernate.id.UUIDGenerator",
    )
    @Column(name = "uuid")
    val uuid: UUID? = null,

    @Column(name = "organization_uuid")
    val organizationUuid: UUID?,

    @Column(name = "creation_date")
    val creationDate: ZonedDateTime,

    @Column(name = "email")
    var email: String,

    @Column(name = "password")
    var password: String,

    @Column(name = "last_name")
    var lastName: String,

    @Column(name = "first_name")
    var firstName: String,

    @Column(name = "nickname")
    var nickname: String,

    @Column(name = "role")
    @Enumerated(EnumType.STRING)
    var role: Role,

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    var status: Status,

    @Column(name = "avatar_reference")
    var avatarReference: String?,

    @Column(name = "banner_reference")
    var bannerReference: String?,

    @Column(name = "keywords")
    var keywords: String,
) {

    fun fullName() = "$firstName $lastName"

    fun toDTO(avatarUrl: String?, bannerUrl: String?) = UserDTO(
        uuid = uuid!!,
        creationDate = creationDate,
        email = email,
        lastName = lastName,
        firstName = firstName,
        nickname = nickname,
        organizationUuid = organizationUuid,
        role = role.name,
        status = status.name,
        avatarUrl = avatarUrl,
        bannerUrl = bannerUrl,
    )
}
