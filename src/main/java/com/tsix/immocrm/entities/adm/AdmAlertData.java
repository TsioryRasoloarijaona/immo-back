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

/**
 * Alerte de relance telle que l'écran du bureau la lit ({@code ComSaleProspectionView}, sur
 * {@code Active = 1}) : sans ligne ici, l'alerte n'existe pas. Une ligne par interaction, écrite
 * par le client, aucun trigger — voir FEATURE-alerte-admalertdata.md. Clé nommée {@code Pkey}.
 */
@Getter
@Setter
@Entity
@Table(name = "AdmAlertData")
public class AdmAlertData {

    public static final String DESCRIPTION_ALERTE = "Alerte";

    /** Dossiers : « Dossier de vente (…) ». */
    public static final String GROUPE_PROSPECTION = "Prospection";

    /** Catégorie d'interaction « communication », en minuscules comme le légataire l'écrit. */
    public static final String DROIT_COMMUNICATION = "69f8e7d6-1111-1111-1111-1aa8b1f02008";

    @Id
    @Column(name = "Pkey", nullable = false)
    private UUID id;

    @Size(max = 256)
    @Nationalized
    @Column(name = "Label")
    private String label;

    @Size(max = 256)
    @Nationalized
    @Column(name = "Code")
    private String code;

    @Size(max = 256)
    @Nationalized
    @Column(name = "Description")
    private String description;

    @Size(max = 1000)
    @Nationalized
    @Column(name = "Message")
    private String message;

    @Size(max = 256)
    @Nationalized
    @Column(name = "Groupping")
    private String groupping;

    @Column(name = "AlertDate")
    private LocalDateTime alertDate;

    /** Pour une prospection, toujours un {@code ComActionMessage}. */
    @Column(name = "RelatedKey")
    private UUID relatedKey;

    @Size(max = 256)
    @Nationalized
    @Column(name = "RightKey")
    private String rightKey;

    /** 0 = désarmée par une action postérieure. */
    @Column(name = "Active")
    private Boolean active;
}
