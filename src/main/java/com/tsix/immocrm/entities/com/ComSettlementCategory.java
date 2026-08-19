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

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * dbo_ComSettlementCategory — the real "Catégorie" lookup for échéancier lines
 * ({@code ComDocument.ComSettlementCategoryId}), e.g. AUTO-FINANCEMENT / AVANCE / FRAIS SYNDIC.
 * Confirmed against real data: {@code ComDocument.StlSettlementTypeId} is NULL on every real
 * échéance row — {@code StlSettlementType} is unrelated (it's the payment-method lookup used by
 * {@code StlSettlement}, a different entity). Categories are scoped per {@code StkVocation}
 * (e.g. "AVANCE" only exists for vocation VENTE) and the boolean flags below let business logic
 * find "the" advance/auto-financement/syndic category without hardcoding a UUID.
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

    @Column(name = "IsSyndic")
    private Boolean isSyndic;

    @Column(name = "ExcludeFromPrice")
    private Boolean excludeFromPrice;

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
