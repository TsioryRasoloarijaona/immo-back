package com.tsix.immocrm.entities.prj;

import com.tsix.immocrm.entities.CodeLibelleOmran;
import com.tsix.immocrm.entities.cfg.CfgCompany;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/** Projet du module Projet (pas un programme {@code CfgProject}) : « Projet de facturation pour les dépenses ». */
@Getter
@Setter
@Entity
@Table(name = "PrjProject")
public class PrjProject extends CodeLibelleOmran {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CfgCompanyId")
    private CfgCompany cfgCompany;
}
