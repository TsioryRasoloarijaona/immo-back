package com.tsix.immocrm.entities.prj;

import com.tsix.immocrm.entities.LigneOmran;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Nationalized;

import java.math.BigDecimal;
import java.util.UUID;

/** Ligne d'avenant ; la clé s'écrit {@code Pkey} en base (collation insensible à la casse). */
@Getter
@Setter
@Entity
@Table(name = "PrjClauseLine")
public class PrjClauseLine extends LigneOmran {
    @Column(name = "PrjClauseId")
    private UUID prjClauseId;

    @Column(name = "Sorting")
    private Integer sorting;

    @Size(max = 255)
    @Nationalized
    @Column(name = "Code")
    private String code;

    @Size(max = 500)
    @Nationalized
    @Column(name = "Description")
    private String description;

    @Size(max = 255)
    @Nationalized
    @Column(name = "Unity")
    private String unity;

    @Column(name = "Quantity", precision = 26, scale = 6)
    private BigDecimal quantity;

    @Column(name = "UnitPriceHT", precision = 26, scale = 6)
    private BigDecimal unitPriceHT;

    @Column(name = "VatRatio", precision = 26, scale = 6)
    private BigDecimal vatRatio;

    @Column(name = "FODECRatio", precision = 26, scale = 16)
    private BigDecimal fodecRatio;

    @Column(name = "UnitPriceTTC", precision = 26, scale = 6)
    private BigDecimal unitPriceTTC;

    @Size(max = 1000)
    @Nationalized
    @Column(name = "Notes")
    private String notes;

    @Column(name = "StlCategoryId")
    private UUID stlCategoryId;

    @Column(name = "PrjMarketLineId")
    private UUID prjMarketLineId;
}
