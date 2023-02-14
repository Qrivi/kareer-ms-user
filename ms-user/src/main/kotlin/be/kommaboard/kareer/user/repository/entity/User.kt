package be.kommaboard.kareer.user.repository.entity

import be.kommaboard.kareer.authorization.Role
import be.kommaboard.kareer.authorization.Status
import be.kommaboard.kareer.user.lib.dto.response.UserDTO
import org.bouncycastle.asn1.x500.style.RFC4519Style.title
import org.hibernate.annotations.GenericGenerator
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.UUID
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.EnumType
import javax.persistence.Enumerated
import javax.persistence.GeneratedValue
import javax.persistence.Id
import javax.persistence.PrimaryKeyJoinColumn
import javax.persistence.SecondaryTable
import javax.persistence.Table

@Entity
@Table(name = "user")
@SecondaryTable(name = "user_preferences", pkJoinColumns = [PrimaryKeyJoinColumn(name = "user_uuid")])
class User(

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(
        name = "UUID",
        strategy = "org.hibernate.id.UUIDGenerator",
    )
    @Column(name = "uuid")
    val uuid: UUID? = null,

    @Column(name = "slug")
    var slug: String?,

    @Column(name = "organization_uuid")
    val organizationUuid: UUID?,

    @Column(name = "creation_date")
    val creationDate: ZonedDateTime,

    @Column(name = "role")
    @Enumerated(EnumType.STRING)
    var role: Role,

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    var status: Status,

    @Column(name = "email")
    var email: String,

    @Column(name = "phone")
    var phone: String?,

    @Column(name = "password")
    var password: String,

    @Column(name = "last_name")
    var lastName: String,

    @Column(name = "first_name")
    var firstName: String,

    @Column(name = "nickname")
    var nickname: String?,

    @Column(name = "title")
    var title: String,

    @Column(name = "birthday")
    var birthday: LocalDate?,

    @Column(name = "avatar_reference")
    var avatarReference: String?,

    @Column(name = "banner_reference")
    var bannerReference: String?,

    @Column(name = "keywords")
    var keywords: String,

    @Column(name = "preferences", table = "user_preferences")
    var preferences: String?,
) {

    fun fullName() = "$firstName $lastName"

    fun toDTO(avatarUrl: String? = null, bannerUrl: String? = null) = UserDTO(
        uuid = uuid!!,
        slug = slug,
        creationDate = creationDate,
        organizationUuid = organizationUuid,
        role = role.name,
        status = status.name,
        email = email,
        phone = phone,
        lastName = lastName,
        firstName = firstName,
        nickname = nickname,
        title = title,
        birthday = birthday,
        avatarUrl = avatarUrl,
        bannerUrl = bannerUrl,
        preferences = preferences,
    )
}
