package com.tsix.immocrm.entities.prj;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/** Tiers invité à une réception ; table sans colonnes d'audit. */
@Getter
@Setter
@Entity
@Table(name = "PrjMarketReceptionCfgTier")
public class PrjMarketReceptionCfgTier {
    @Id
    @Column(name = "PKey", nullable = false)
    private UUID id;

    @Column(name = "PrjMarketReceptionId")
    private UUID prjMarketReceptionId;

    @Column(name = "sorting")
    private Integer sorting;

    @Column(name = "CfgTierId")
    private UUID cfgTierId;
}
