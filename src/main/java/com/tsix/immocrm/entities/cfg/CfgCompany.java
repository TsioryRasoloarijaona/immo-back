package com.tsix.immocrm.entities.cfg;

import com.tsix.immocrm.entities.adm.AdmCountry;
import com.tsix.immocrm.entities.stl.StlCurrency;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entité juridique d'un projet ou d'un règlement. Les documents générés la lisent : ne jamais écrire
 * un nom de société en dur dans un renderer.
 */
@Getter
@Setter
@Entity
@Table(name = "CfgCompany")
public class CfgCompany {
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

    /**
     * Décide de la visibilité de l'entité et de ses projets, tranches et articles ; modifiable par
     * {@code PATCH /api/entites-juridiques/{id}}. {@code null} vaut inactif : ne rien exposer par défaut.
     */
    @Column(name = "IsActive")
    private Boolean isActive;

    @Size(max = 255)
    @Nationalized
    @Column(name = "Address")
    private String address;

    @Size(max = 255)
    @Nationalized
    @Column(name = "Address2")
    private String address2;

    @Size(max = 255)
    @Nationalized
    @Column(name = "Address3")
    private String address3;

    @Size(max = 255)
    @Nationalized
    @Column(name = "City")
    private String city;

    /** Équivalent de {@code StlSettlementView.CfgComPanyViewLegalForm}. */
    @Size(max = 255)
    @Nationalized
    @Column(name = "LegalForm")
    private String legalForm;

    /** Pour « {Ville} le, {date} » en pied de document. */
    @Size(max = 255)
    @Nationalized
    @Column(name = "SignatureCity")
    private String signatureCity;

    @Size(max = 255)
    @Nationalized
    @Column(name = "ZipCode")
    private String zipCode;

    @Size(max = 255)
    @Nationalized
    @Column(name = "Phone")
    private String phone;

    @Size(max = 255)
    @Nationalized
    @Column(name = "Mobile")
    private String mobile;

    @Size(max = 255)
    @Nationalized
    @Column(name = "Fax")
    private String fax;

    @Size(max = 255)
    @Nationalized
    @Column(name = "Email")
    private String email;

    @Size(max = 255)
    @Nationalized
    @Column(name = "WebSite")
    private String webSite;

    @Size(max = 1000)
    @Nationalized
    @Column(name = "DescriptionLong", length = 1000)
    private String descriptionLong;

    @Size(max = 255)
    @Nationalized
    @Column(name = "FiscalNumber")
    private String fiscalNumber;

    @Size(max = 255)
    @Nationalized
    @Column(name = "CommercialRegister")
    private String commercialRegister;

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

    @Column(name = "CreationDate")
    private LocalDateTime creationDate;

    @Column(name = "Capital", precision = 26, scale = 6)
    private BigDecimal capital;

    /** « Nombre actionnaires » saisi : doit égaler le nombre de lignes de répartition (OMRAN {@code Verif}). */
    @Column(name = "TotalSharing")
    private Integer totalSharing;

    /** « Nombre de parts (actions) » : doit égaler la somme des parts de la répartition. */
    @Column(name = "SharingNumber")
    private Integer sharingNumber;

    /** Fraction (0,25 = 25 %), échelle 16 en base. */
    @Column(name = "HomePart", precision = 26, scale = 16)
    private BigDecimal homePart;

    @Column(name = "LiquidationDate")
    private LocalDateTime liquidationDate;

    /** Piège : la FK de production est ON DELETE CASCADE — supprimer le groupe supprime la société. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CfgGroupShareholderId")
    private CfgGroupShareholder cfgGroupShareholder;

    /** Obligatoire à l'écriture (contrôle du service), nullable en base. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "StlCurrencyId")
    private StlCurrency stlCurrency;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "AdmCountryId")
    private AdmCountry admCountry;

    /** « Représentant légal » de l'entité, repris dans les listes programme et tranche. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CfgTierId")
    private CfgTier cfgTier;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "AdmSignatoryId")
    private CfgTier admSignatory;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CfgTechnicalManagerId")
    private CfgTier cfgTechnicalManager;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CfgSalesManagerId")
    private CfgTier cfgSalesManager;

    @Size(max = 255)
    @Nationalized
    @Column(name = "UserCreate")
    private String userCreate;

    @Column(name = "UserCreateDateTime")
    private LocalDateTime userCreateDateTime;

    @Size(max = 255)
    @Nationalized
    @Column(name = "UserUpdate")
    private String userUpdate;

    @Column(name = "UserUpdateDateTime")
    private LocalDateTime userUpdateDateTime;
}
