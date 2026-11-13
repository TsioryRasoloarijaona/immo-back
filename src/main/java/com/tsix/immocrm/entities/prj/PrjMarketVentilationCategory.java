package com.tsix.immocrm.entities.prj;

import com.tsix.immocrm.entities.LigneOmran;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

/** Ventilation d'un lot par catégorie de dépense, taux en fraction ; réécrite à chaque enregistrement d'un bordereau. */
@Getter
@Setter
@Entity
@Table(name = "PrjMarketVentilationCategory")
public class PrjMarketVentilationCategory extends LigneOmran {
    @Column(name = "PrjMarketId")
    private UUID prjMarketId;

    @Column(name = "StlCategoryId")
    private UUID stlCategoryId;

    @Column(name = "VentilationRate", precision = 26, scale = 6)
    private BigDecimal ventilationRate;
}
