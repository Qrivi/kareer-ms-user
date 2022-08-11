package be.kommaboard.kareer.user.repository.entity

import be.kommaboard.kareer.user.lib.dto.response.InviteDTO
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
    @JoinColumn(name = "inviter_uuid")
    val inviter: User,

    @Column(name = "creation_date")
    val creationDate: ZonedDateTime,

    @Column(name = "invitee_email")
    val inviteeEmail: String,

    @Column(name = "invitee_last_name")
    val inviteeLastName: String,

    @Column(name = "invitee_first_name")
    val inviteeFirstName: String,

    @Column(name = "used")
    var used: Boolean = false,
) {

    fun toDTO() = InviteDTO(
        uuid = uuid!!,
        inviterUuid = inviter.uuid!!,
        creationDate = creationDate,
        inviteeEmail = inviteeEmail,
        inviteeLastName = inviteeLastName,
        inviteeFirstName = inviteeFirstName,
        used = used,
    )
}
