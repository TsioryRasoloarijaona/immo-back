package com.tsix.immocrm.entities.com;

import com.tsix.immocrm.entities.stk.StkVocation;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Nationalized;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * « Catégorie » d'une échéance (AVANCE, AUTO-FINANCEMENT…), propre à chaque {@code StkVocation} ;
 * les drapeaux {@code IsSale*} la désignent sans UUID en dur. Pas {@code StlSettlementType}, toujours NULL sur les échéances.
 */
@Getter
@Setter
@Entity
@Table(name = "ComSettlementCategory")
public class ComSettlementCategory {
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

    @Column(name = "IsCredit")
    private Boolean isCredit;

    @Column(name = "IsSaleExpense")
    private Boolean isSaleExpense;

    @Column(name = "IsSaleAdvance")
    private Boolean isSaleAdvance;

    @Column(name = "IsSaleAutoFinancment")
    private Boolean isSaleAutoFinancment;

    // Pas isCredit (crédit/débit comptable) : le « Crédit » bancaire du bloc MONTANTS de la fiche dossier.
    @Column(name = "IsSaleCredit")
    private Boolean isSaleCredit;

    @Column(name = "IsSaleModification")
    private Boolean isSaleModification;

    @Column(name = "IsSalePenality")
    private Boolean isSalePenality;

    // Pénalité de retard possible sur cette catégorie (ComDocument.cs:ManagePenalty) ; pas IsSalePenality.
    @Column(name = "IsWithPenalty")
    private Boolean isWithPenalty;

    @Column(name = "IsSaleResiliation")
    private Boolean isSaleResiliation;

    // OMRAN « Toujours payé » : son reste dû est retenu sur le remboursement d'un désistement.
    @Column(name = "IsAlwaysPayed")
    private Boolean isAlwaysPayed;

    @Column(name = "IsSyndic")
    private Boolean isSyndic;

    @Column(name = "ExcludeFromPrice")
    private Boolean excludeFromPrice;

    // « Mettre à jour le schéma de financement » : l'écart est reporté sur le dernier autofinancement, sinon crédit.
    @Column(name = "CanUpdateDocuments")
    private Boolean canUpdateDocuments;

    @Column(name = "IsSaleBeforeReservation")
    private Boolean isSaleBeforeReservation;

    // Ligne datée au plus tôt du début des finalisations de la tranche.
    @Column(name = "IsConfermentBasedDate")
    private Boolean isConfermentBasedDate;

    @Column(name = "VatRatio", precision = 26, scale = 16)
    private BigDecimal vatRatio;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "StkVocationId")
    private StkVocation stkVocation;

    @Size(max = 255)
    @Nationalized
    @Column(name = "UserCreate")
    private String userCreate;

    @Column(name = "UserCreateDateTime")
    private LocalDateTime userCreateDateTime;

    @Size(max = 255)
    @Nationalized
    @Column(name = "UserUpdate")
    private String userUpdate;

    @Column(name = "UserUpdateDateTime")
    private LocalDateTime userUpdateDateTime;
}
