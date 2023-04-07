package be.kommaboard.kareer.user.repository.entity

import be.kommaboard.kareer.authorization.Role
import be.kommaboard.kareer.authorization.Status
import be.kommaboard.kareer.user.lib.dto.response.UserDTO
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToOne
import jakarta.persistence.PrimaryKeyJoinColumn
import jakarta.persistence.SecondaryTable
import jakarta.persistence.Table
import org.hibernate.annotations.GenericGenerator
import java.time.ZonedDateTime
import java.util.UUID

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

    @Column(name = "password")
    var password: String,

    @Column(name = "last_name")
    var lastName: String,

    @Column(name = "first_name")
    var firstName: String,

    @Column(name = "nickname")
    var nickname: String?,

    @Column(name = "slug")
    var slug: String?,

    @OneToOne(fetch = FetchType.EAGER, cascade = [CascadeType.ALL])
    @JoinColumn(name = "details_uuid")
    val details: UserDetails?,

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
        creationDate = creationDate,
        role = role.name,
        status = status.name,
        email = email,
        lastName = lastName,
        firstName = firstName,
        nickname = nickname,
        slug = slug,
        details = details?.toDTO(),
        avatarUrl = avatarUrl,
        bannerUrl = bannerUrl,
        preferences = preferences,
    )
}
