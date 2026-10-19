package com.tsix.immocrm.entities.cfg;

import com.tsix.immocrm.entities.ResponsablesOmran;
import com.tsix.immocrm.entities.stl.StlCurrency;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Nationalized;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entité juridique d'un projet ou d'un règlement. Les documents générés la lisent : ne jamais écrire
 * un nom de société en dur dans un renderer.
 */
@Getter
@Setter
@Entity
@Table(name = "CfgCompany")
public class CfgCompany extends ResponsablesOmran {
    /**
     * Décide de la visibilité de l'entité et de ses projets, tranches et articles ; basculé par
     * {@code PATCH /api/entites-juridiques/{id}}. {@code null} vaut inactif : ne rien exposer par défaut.
     */
    @Column(name = "IsActive")
    private Boolean isActive;

    @Size(max = 1000)
    @Nationalized
    @Column(name = "DescriptionLong", length = 1000)
    private String descriptionLong;

    /** Représentant légal (CfgTrancheView.CfgTierDescription). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CfgTierId")
    private CfgTier representantLegal;

    /** Équivalent de {@code StlSettlementView.CfgComPanyViewLegalForm}. */
    @Size(max = 255)
    @Nationalized
    @Column(name = "LegalForm")
    private String legalForm;

    @Size(max = 255)
    @Nationalized
    @Column(name = "FiscalNumber")
    private String fiscalNumber;

    @Size(max = 255)
    @Nationalized
    @Column(name = "CommercialRegister")
    private String commercialRegister;

    @Column(name = "CreationDate")
    private LocalDateTime creationDate;

    @Column(name = "Capital", precision = 26, scale = 6)
    private BigDecimal capital;

    @Size(max = 255)
    @Nationalized
    @Column(name = "Cnss")
    private String cnss;

    @Size(max = 255)
    @Nationalized
    @Column(name = "Patent")
    private String patent;

    @Size(max = 255)
    @Nationalized
    @Column(name = "ICE")
    private String ice;

    /** « Nombre actionnaires » : nombre de lignes de répartition attendues ({@code Verif}). */
    @Column(name = "TotalSharing")
    private Integer totalSharing;

    /** « Nombre de parts (actions) ». */
    @Column(name = "SharingNumber")
    private Integer sharingNumber;

    @Column(name = "LiquidationDate")
    private LocalDateTime liquidationDate;

    /** « Part maison » ; decimal(26,16) en base. */
    @Column(name = "HomePart", precision = 26, scale = 16)
    private BigDecimal homePart;

    /** Base : {@code CfgCompany → CfgGroupShareholder} ON DELETE CASCADE — supprimer le groupe supprime l'entité. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CfgGroupShareholderId")
    private CfgGroupShareholder cfgGroupShareholder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "StlCurrencyId")
    private StlCurrency stlCurrency;
}
