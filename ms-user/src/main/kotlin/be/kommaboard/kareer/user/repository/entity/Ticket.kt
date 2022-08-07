package be.kommaboard.kareer.user.repository.entity

import org.hibernate.annotations.GenericGenerator
import java.time.ZonedDateTime
import java.util.UUID
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.FetchType
import javax.persistence.GeneratedValue
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne

@Entity
class Ticket(

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(
        name = "UUID",
        strategy = "org.hibernate.id.UUIDGenerator",
    )
    @Column(name = "uuid")
    val uuid: UUID? = null,

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_uuid")
    val user: User,

    @Column(name = "creation_date")
    val creationDate: ZonedDateTime,

    @Column(name = "token")
    val token: String,

    @Column(name = "kind")
    val kind: Kind,

    @Column(name = "used")
    var used: Boolean = false,
) {

    enum class Kind {
        CONFIRM_EMAIL,
        RESET_PASSWORD,
    }
}
