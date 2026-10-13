package com.tsix.immocrm.entities.app;

import com.tsix.immocrm.entities.adm.AdmUser;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Nationalized;

import java.time.LocalDateTime;
import java.util.UUID;

/** Notification de la cloche, une par compte destinataire (table applicative, FEATURE-repartition-notifications.md §3.3). */
@Getter
@Setter
@Entity
@Table(name = "AppNotification")
public class AppNotification {

    @Id
    @Column(name = "PKey", nullable = false)
    private UUID id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "AdmUserId", nullable = false)
    private AdmUser admUser;

    /** Code {@link com.tsix.immocrm.domain.TypeNotification}. */
    @NotNull
    @Nationalized
    @Size(max = 40)
    @Column(name = "Type", nullable = false, length = 40)
    private String type;

    @NotNull
    @Nationalized
    @Size(max = 200)
    @Column(name = "Titre", nullable = false, length = 200)
    private String titre;

    @NotNull
    @Nationalized
    @Size(max = 1000)
    @Column(name = "Message", nullable = false, length = 1000)
    private String message;

    @Nationalized
    @Size(max = 300)
    @Column(name = "Lien", length = 300)
    private String lien;

    @NotNull
    @Column(name = "CreeLe", nullable = false)
    private LocalDateTime creeLe;

    @Column(name = "LueLe")
    private LocalDateTime lueLe;
}
