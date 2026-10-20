package com.tsix.immocrm.entities.stk;

import com.tsix.immocrm.entities.AuditTrieOmran;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/** Fonction spécifique d'une unité de base : surface comptée par le Verif de l'étude de faisabilité. */
@Getter
@Setter
@Entity
@Table(name = "SpecificFunction")
public class SpecificFunction extends AuditTrieOmran {
    @Column(name = "IsTotal")
    private Boolean isTotal;

    @Column(name = "SaleAbleArea")
    private Boolean saleAbleArea;

    @Column(name = "CoveredArea")
    private Boolean coveredArea;

    @Column(name = "AreaNet")
    private Boolean areaNet;

    @Column(name = "AreaBrut")
    private Boolean areaBrut;

    @Column(name = "AreaCos")
    private Boolean areaCos;
}
