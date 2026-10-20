package com.tsix.immocrm.entities.stk;

import com.tsix.immocrm.entities.LigneOmran;
import com.tsix.immocrm.entities.cfg.CfgTier;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Nationalized;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Frais d'un terrain ; la clé de la table s'appelle {@code Pkey}, pas {@code PKey}. */
@Getter
@Setter
@Entity
@Table(name = "StkExpense")
@AttributeOverride(name = "id", column = @Column(name = "Pkey", nullable = false))
public class StkExpense extends LigneOmran {
    @Column(name = "\"Date\"")
    private LocalDate date;

    @Column(name = "Amount", precision = 26, scale = 6)
    private BigDecimal amount;

    @NotNull
    @Column(name = "IsValidated", nullable = false)
    private Boolean isValidated;

    @Size(max = 1000)
    @Nationalized
    @Column(name = "Notes", length = 1000)
    private String notes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CfgSupplierId")
    private CfgTier cfgSupplier;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "StkExpenseTypeId")
    private StkExpenseType stkExpenseType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "StkItemId")
    private StkItem stkItem;
}
