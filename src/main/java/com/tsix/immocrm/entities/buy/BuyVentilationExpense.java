package com.tsix.immocrm.entities.buy;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

/** Ventilation d'un document à payer par projet de facturation ({@code PrjProject}) ; Σ des taux = 1. */
@Getter
@Setter
@Entity
@Table(name = "BuyVentilationExpense")
public class BuyVentilationExpense {
    @Id
    @Column(name = "Pkey", nullable = false)
    private UUID id;
    @Column(name = "PrjProjectId")
    private UUID prjProjectId;
    @Column(name = "BuyDocumentId")
    private UUID buyDocumentId;
    @Column(name = "VentilationRate", precision = 26, scale = 6)
    private BigDecimal ventilationRate;
}
