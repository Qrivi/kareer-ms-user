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

    @Column(name = "creation_date")
    val creationDate: ZonedDateTime,

    @Column(name = "email")
    val email: String,

    @Column(name = "password")
    val password: String,

    @Column(name = "last_name")
    val lastName: String,

    @Column(name = "first_name")
    val firstName: String,

    @Column(name = "nickname")
    val nickname: String,

    @Column(name = "organization_uuid")
    val organizationUuid: UUID?,

    @Column(name = "role")
    @Enumerated(EnumType.STRING)
    val role: Role,

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    var status: Status,
) {

    fun fullName() = "$firstName $lastName"

    fun toDTO() = UserDTO(
        uuid = uuid!!,
        creationDate = creationDate,
        email = email,
        lastName = lastName,
        firstName = firstName,
        nickname = nickname,
        organizationUuid = organizationUuid,
        role = role.name,
        status = status.name,
    )
}
