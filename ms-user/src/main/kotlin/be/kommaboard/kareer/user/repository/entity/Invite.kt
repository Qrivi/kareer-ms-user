package be.kommaboard.kareer.user.repository.entity

import be.kommaboard.kareer.user.lib.dto.response.InviteDTO
import be.kommaboard.kareer.user.service.exception.InvalidInviteStatusException
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
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
@Table(name = "invite")
class Invite(

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
    val inviter: User,

    @Column(name = "creation_date")
    val creationDate: ZonedDateTime,

    @Column(name = "invitee_email")
    val inviteeEmail: String,

    @Column(name = "invitee_last_name")
    val inviteeLastName: String,

    @Column(name = "invitee_first_name")
    val inviteeFirstName: String,

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    var status: Status,
) {

    enum class Status {
        PENDING,
        ACCEPTED,
        DECLINED,
        RETRACTED,
    }

    fun toDTO() = InviteDTO(
        uuid = uuid!!,
        inviterUuid = inviter.uuid!!,
        creationDate = creationDate,
        inviteeEmail = inviteeEmail,
        inviteeLastName = inviteeLastName,
        inviteeFirstName = inviteeFirstName,
        status = status.name,
    )
}

fun String.toInviteStatus(): Invite.Status = try {
    Invite.Status.valueOf(this.trim().uppercase())
} catch (e: IllegalArgumentException) {
    throw InvalidInviteStatusException(this.trim())
}
