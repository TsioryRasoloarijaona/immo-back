package com.tsix.immocrm.entities.cfg;

import com.tsix.immocrm.entities.com.ComSettlementCategory;
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

/** Ligne de schéma de financement propre à une tranche : montant TTC fixe, ou pourcentage du prix de vente. */
@Getter
@Setter
@Entity
@Table(name = "CfgTrancheSettlementCategory")
public class CfgTrancheSettlementCategory {
    @Id
    @Column(name = "PKey", nullable = false)
    private UUID id;

    @Column(name = "Sorting")
    private Integer sorting;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ComSettlementCategoryId")
    private ComSettlementCategory comSettlementCategory;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CfgtrancheId")
    private CfgTranche cfgTranche;

    @Column(name = "AmountHT", precision = 26, scale = 6)
    private BigDecimal amountHT;

    @Column(name = "VatRatio", precision = 26, scale = 6)
    private BigDecimal vatRatio;

    @Column(name = "AmountTTC", precision = 26, scale = 6)
    private BigDecimal amountTTC;

    @Column(name = "PercentOf", precision = 26, scale = 6)
    private BigDecimal percentOf;

    @Size(max = 500)
    @Nationalized
    @Column(name = "Notes", length = 500)
    private String notes;

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
