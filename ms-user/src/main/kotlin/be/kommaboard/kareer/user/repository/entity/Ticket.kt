package be.kommaboard.kareer.user.repository.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.hibernate.annotations.GenericGenerator
import java.time.ZonedDateTime
import java.util.UUID

@Entity
@Table(name = "ticket")
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
