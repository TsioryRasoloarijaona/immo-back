package com.tsix.immocrm.entities.adm;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Nationalized;

import java.time.LocalDateTime;
import java.util.UUID;

/** Note OMRAN (bouton « Notes » d'une fiche) ; {@code CategoryId} = PKey du porteur, sans clé étrangère. */
@Getter
@Setter
@Entity
@Table(name = "AdmMessage")
public class AdmMessage {
    @Id
    @Column(name = "PKey", nullable = false)
    private UUID id;

    @Column(name = "DocDate")
    private LocalDateTime docDate;

    /** HTML assaini (voir {@code NoteService}). */
    @Nationalized
    @Column(name = "Notes")
    private String notes;

    @Column(name = "CategoryId")
    private UUID categoryId;

    @Size(max = 255)
    @Nationalized
    @Column(name = "Description")
    private String description;

    @Size(max = 255)
    @Nationalized
    @Column(name = "UserName")
    private String userName;

    @Column(name = "IsIn")
    private Boolean isIn;

    @Size(max = 255)
    @Nationalized
    @Column(name = "Origin")
    private String origin;

    @Column(name = "ReminderDate")
    private LocalDateTime reminderDate;
}
