package com.tsix.immocrm.entities.prj;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

/** Ventilation d'un projet par tranche (onglet OMRAN « Ventilation par tranches »), taux en fraction. */
@Getter
@Setter
@Entity
@Table(name = "PrjProjectCfgTrancheVentilation")
public class PrjProjectCfgTrancheVentilation {
    @Id
    @Column(name = "PKey", nullable = false)
    private UUID id;

    @Column(name = "PrjProjectId")
    private UUID prjProjectId;

    @Column(name = "CfgTrancheId")
    private UUID cfgTrancheId;

    @Column(name = "VentilationRate", precision = 26, scale = 6)
    private BigDecimal ventilationRate;
}
