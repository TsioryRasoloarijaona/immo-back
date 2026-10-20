package com.tsix.immocrm.entities.buy;

import com.tsix.immocrm.entities.AuditOmran;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Nationalized;

import java.math.BigDecimal;
import java.util.UUID;

/** Ligne de facture fournisseur ; la clé de la table s'appelle {@code Pkey}, pas {@code PKey}. */
@Getter
@Setter
@Entity
@Table(name = "BuyDocumentLine")
@AttributeOverride(name = "id", column = @Column(name = "Pkey", nullable = false))
public class BuyDocumentLine extends AuditOmran {
    @Column(name = "BuyDocumentId")
    private UUID buyDocumentId;
    @Column(name = "StlCategoryId")
    private UUID stlCategoryId;

    @Size(max = 255)
    @Nationalized
    @Column(name = "Unity")
    private String unity;
    @Size(max = 1000)
    @Nationalized
    @Column(name = "Notes", length = 1000)
    private String notes;
    @Column(name = "Sorting")
    private Integer sorting;

    @Column(name = "Quantity", precision = 26, scale = 6)
    private BigDecimal quantity;
    @Column(name = "QuantityOld", precision = 26, scale = 6)
    private BigDecimal quantityOld;
    @Column(name = "QuantityOrigin", precision = 26, scale = 6)
    private BigDecimal quantityOrigin;
    @Column(name = "UnitPriceHT", precision = 26, scale = 6)
    private BigDecimal unitPriceHT;
    @Column(name = "UnitPriceTTC", precision = 26, scale = 6)
    private BigDecimal unitPriceTTC;
    @Column(name = "VatRatio", precision = 26, scale = 6)
    private BigDecimal vatRatio;
    @Column(name = "Coefficient", precision = 26, scale = 6)
    private BigDecimal coefficient;
    @Column(name = "DiscountRatio", precision = 26, scale = 16)
    private BigDecimal discountRatio;
    @Column(name = "FODECRatio", precision = 26, scale = 16)
    private BigDecimal fodecRatio;
}
