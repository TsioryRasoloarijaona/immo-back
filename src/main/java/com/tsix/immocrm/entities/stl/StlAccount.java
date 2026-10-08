package com.tsix.immocrm.entities.stl;

import com.tsix.immocrm.entities.cfg.CfgCompany;
import com.tsix.immocrm.entities.cfg.CfgTier;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Nationalized;

import java.util.UUID;

/** Compte bancaire des règlements ; {@code Code} sert de libellé à la colonne « Compte » du légataire. */
@Getter
@Setter
@Entity
@Table(name = "StlAccount")
public class StlAccount {
    @Id
    @Column(name = "PKey", nullable = false)
    private UUID id;

    @Size(max = 255)
    @Nationalized
    @Column(name = "Code")
    private String code;

    @Size(max = 255)
    @Nationalized
    @Column(name = "Description")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CfgBankId")
    private CfgTier cfgBank;

    @Size(max = 255)
    @Nationalized
    @Column(name = "BankCode")
    private String bankCode;

    @Size(max = 255)
    @Nationalized
    @Column(name = "AgencyCode")
    private String agencyCode;

    @Size(max = 255)
    @Nationalized
    @Column(name = "AccountNumber")
    private String accountNumber;

    @Size(max = 255)
    @Nationalized
    @Column(name = "Iban")
    private String iban;

    @Size(max = 255)
    @Nationalized
    @Column(name = "CodeBic")
    private String codeBic;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CfgCompanyId")
    private CfgCompany cfgCompany;

    // Clé RIB. StlAccountView.RIB est une colonne calculée de la vue, pas de la table : le RIB est
    // recomposé depuis BankCode/AgencyCode/AccountNumber/AccountKey (AutorisationVersementService.rib()).
    @Size(max = 5)
    @Nationalized
    @Column(name = "AccountKey", length = 5)
    private String accountKey;
}
