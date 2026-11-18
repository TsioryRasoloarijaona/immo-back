package com.tsix.immocrm.entities.bul;

import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/** Contact présent à une réunion ({@code BulMeetingContact}, {@code BulDelegateContact}) : sans colonnes d'audit. */
@Getter
@Setter
@MappedSuperclass
public abstract class ContactReunionOmran {
    @Id
    @Column(name = "PKey", nullable = false)
    private UUID id;

    @Column(name = "Sorting")
    private Integer sorting;

    @Column(name = "ContactId")
    private UUID contactId;

    @Column(name = "IsPresent")
    private Boolean isPresent;

    @Column(name = "Notify")
    private Boolean notify;
}
