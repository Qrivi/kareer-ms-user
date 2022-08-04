package be.kommaboard.kareer.user.repository.entity

import be.kommaboard.kareer.common.security.Role
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

    @Column(name = "full_name")
    val fullName: String,

    @Column(name = "short_name")
    val shortName: String,

    @Column(name = "organization_uuid")
    val organizationUuid: UUID?,

    @Column(name = "role")
    @Enumerated(EnumType.STRING)
    val role: Role,

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    var status: Status,

) {

    enum class Status {
        REGISTERED,
        ACTIVATED,
        DEACTIVATED,
    }
}
