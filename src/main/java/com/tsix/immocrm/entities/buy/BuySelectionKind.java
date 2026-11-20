package com.tsix.immocrm.entities.buy;

import com.tsix.immocrm.entities.AuditOmran;
import com.tsix.immocrm.entities.str.StrEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Nationalized;

import java.math.BigDecimal;

/** Procédure d'achat d'une demande interne d'achat (référentiel {@code procedures-achat}, FEATURE-n6-demandes-achat.md). */
@Getter
@Setter
@Entity
@Table(name = "BuySelectionKind")
public class BuySelectionKind extends AuditOmran {

    @Size(max = 1000)
    @Nationalized
    @Column(name = "Notes")
    private String notes;

    @Column(name = "MaxAmount", precision = 26, scale = 6)
    private BigDecimal maxAmount;

    @Column(name = "IsConfidential")
    private Boolean isConfidential;

    // Faute d'orthographe réelle de la colonne (« Managment »).
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "StrEntityManagmentId")
    private StrEntity strEntityManagment;

    @Column(name = "IsMultiSupplier")
    private Boolean isMultiSupplier;

    @Column(name = "IsWithoutVerification")
    private Boolean isWithoutVerification;
}
