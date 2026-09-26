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
 * dbo.AdmAlertData — l'alerte de relance <b>telle que l'écran du logiciel de bureau la voit</b>.
 *
 * <p>Une interaction ({@code ComActionMessage}) porte sa date d'alerte dans
 * {@code AlertValidityDate}, mais ce n'est pas cette colonne que l'écran lit : la vue
 * {@code ComSaleProspectionView} calcule {@code LastActiveAlertDate} depuis <b>cette</b> table,
 * sur {@code Active = 1}. Une alerte sans ligne ici n'existe pas pour le logiciel de bureau.
 *
 * <p>Une ligne par interaction ({@code RelatedKey} = {@code ComActionMessage.PKey}, 0 doublon
 * sur 39 599 lignes). Le reste est constant, mesuré sur la source de vérité le 26/09/2026 :
 * {@code Description = 'Alerte'}, {@code Groupping = 'Prospection'}, {@code Label} toujours nul,
 * {@code Code} = la clé en minuscules (39 599 / 39 599), {@code AlertDate} =
 * {@code AlertValidityDate} de l'interaction, {@code Message} = son {@code AlertMessage},
 * {@code RightKey} = la catégorie « communication » (38 041 / 38 041 pour la prospection).
 *
 * <p>Aucun trigger, aucune procédure ne remplit cette table : c'est le client qui l'écrit, en
 * trois ordres relevés dans sa trace d'audit. Voir {@code AlerteRelanceService}.
 *
 * <p><b>La colonne de clé s'appelle {@code Pkey}</b> (k minuscule), à l'inverse de tout le reste
 * du schéma. SQL Server ne fait pas la différence, la lecture du code si.
 */
@Getter
@Setter
@Entity
@Table(name = "AdmAlertData")
public class AdmAlertData {

    /** {@code Description} de toutes les alertes de prospection. */
    public static final String DESCRIPTION_ALERTE = "Alerte";

    /** {@code Groupping} des alertes portées par une prospection (dossiers : « Dossier de vente (…) »). */
    public static final String GROUPE_PROSPECTION = "Prospection";

    /**
     * {@code RightKey} de toutes les alertes de prospection : la catégorie d'interaction
     * « communication » ({@code ComActionMessageCategory}), en minuscules comme le légataire l'écrit.
     */
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

    /** Clé de l'objet alerté — pour une prospection, toujours un {@code ComActionMessage}. */
    @Column(name = "RelatedKey")
    private UUID relatedKey;

    @Size(max = 256)
    @Nationalized
    @Column(name = "RightKey")
    private String rightKey;

    /** 1 = affichée par l'écran du logiciel de bureau ; 0 = désarmée par une action postérieure. */
    @Column(name = "Active")
    private Boolean active;
}
