package com.tsix.immocrm.entities.cfg;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Entity
@Table(name = "CfgGroupShareholderLine")
public class CfgGroupShareholderLine extends LigneActionnaireOmran {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CfgGroupShareholderId")
    private CfgGroupShareholder cfgGroupShareholder;

    /** Pourcentage en fraction (0,25 = 25 %), malgré son nom. */
    @Column(name = "SharingNumber", precision = 26, scale = 6)
    private BigDecimal sharingNumber;
}
