package com.tsix.immocrm.entities.buy;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

/** Ventilation d'une facture par catégorie comptable ; Σ des taux = 1 ({@code DocumentBase.Verif}). */
@Getter
@Setter
@Entity
@Table(name = "BuyVentilationCategory")
public class BuyVentilationCategory {
    @Id
    @Column(name = "PKey", nullable = false)
    private UUID id;
    @Column(name = "BuyDocumentId")
    private UUID buyDocumentId;
    @Column(name = "StlCategoryId")
    private UUID stlCategoryId;
    @Column(name = "VentilationRate", precision = 26, scale = 6)
    private BigDecimal ventilationRate;
}
