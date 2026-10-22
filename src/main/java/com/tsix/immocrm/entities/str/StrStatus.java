package com.tsix.immocrm.entities.str;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Nationalized;

import java.util.UUID;

/** Statut générique, partagé par plusieurs modules : ceux des dossiers sont filtrés par {@code StrConfigEntityId}. */
@Getter
@Setter
@Entity
@Table(name = "StrStatus")
public class StrStatus {
    @Id
    @Column(name = "PKey", nullable = false)
    private UUID id;

    @Size(max = 255)
    @Nationalized
    @Column(name = "Code")
    private String code;

    @Size(max = 255)
    @Nationalized
    @Column(name = "Description")
    private String description;

    @Column(name = "Sorting")
    private Integer sorting;

    @Size(max = 255)
    @Nationalized
    @Column(name = "ColorName")
    private String colorName;

    @Column(name = "StrConfigEntityId")
    private UUID strConfigEntityId;

    // OMRAN « Transforme le prospect en client » ; NULL vaut faux.
    @Column(name = "BoolField011")
    private Boolean transformeProspectEnClient;

    // OMRAN « Vente effective » : faux pour la réservation, l'annulation et le désistement.
    @Column(name = "BoolField001")
    private Boolean venteEffective;

    // OMRAN « Annulation » / « Résiliation » : un dossier à ce statut n'est plus actif ; NULL vaut faux.
    @Column(name = "BoolField002")
    private Boolean annulation;

    @Column(name = "BoolField003")
    private Boolean resiliation;

    /** Jours de validité du statut ; {@code StatusValidityDate} n'est posée que si positif. */
    @Column(name = "Delay")
    private Integer delai;

    @Column(name = "StrStatusTypeId")
    private UUID strStatusTypeId;

    @Column(name = "IsReadOnly")
    private Boolean lectureSeule;

    @Column(name = "CanDelete")
    private Boolean suppressionPermise;

    // OMRAN « Vente au comptant » / « Vente à crédit » (contrôle du crédit dans Sale.cs:VerifThreshold).
    @Column(name = "BoolField004")
    private Boolean venteAuComptant;

    @Column(name = "BoolField005")
    private Boolean venteACredit;

    // OMRAN « Peut effectuer un désistement ».
    @Column(name = "BoolField010")
    private Boolean peutDesister;

    // OMRAN « Avec vérification du schéma de financement » (Sale.cs:VerifSchema).
    @Column(name = "BoolField012")
    private Boolean verificationSchema;

    // OMRAN « Avec affectation de gestionnaire » (Sale.cs:ManageSaleManager).
    @Column(name = "BoolField015")
    private Boolean affectationGestionnaire;

    /** « Date associée » : {@code StrField} dont le {@code Code} nomme la colonne date du dossier. */
    @Column(name = "StrFieldDateId")
    private UUID strFieldDateId;

    /** Ni annulation ni résiliation : le statut d'un dossier encore en cours chez OMRAN. */
    public boolean estActif() {
        return !Boolean.TRUE.equals(annulation) && !Boolean.TRUE.equals(resiliation);
    }
}
