package com.tsix.immocrm.entities.bul;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/** Maître d'ouvrage d'une réunion. */
@Getter
@Setter
@Entity
@Table(name = "BulMeetingContact")
public class BulMeetingContact extends ContactReunionOmran {
    @Column(name = "BulMeetingId")
    private UUID bulMeetingId;
}
