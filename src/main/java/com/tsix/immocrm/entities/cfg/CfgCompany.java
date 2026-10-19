package com.tsix.immocrm.entities.cfg;

import com.tsix.immocrm.entities.adm.AdmCountry;
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
     * Décide de la visibilité de l'entité et de ses projets, tranches et articles ; à entretenir en
     * base (005_CfgCompany_IsActive.sql). {@code null} vaut inactif : ne rien exposer par défaut.
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

    @Size(max = 255)
    @Nationalized
    @Column(name = "ZipCode")
    private String zipCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "AdmCountryId")
    private AdmCountry admCountry;

    @Size(max = 255)
    @Nationalized
    @Column(name = "Phone")
    private String phone;

    @Size(max = 255)
    @Nationalized
    @Column(name = "Fax")
    private String fax;

    @Size(max = 255)
    @Nationalized
    @Column(name = "Mobile")
    private String mobile;

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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "AdmSignatoryId")
    private CfgTier admSignatory;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CfgTechnicalManagerId")
    private CfgTier cfgTechnicalManager;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CfgSalesManagerId")
    private CfgTier cfgSalesManager;

    /** Représentant légal (CfgTrancheView.CfgTierDescription). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CfgTierId")
    private CfgTier representantLegal;

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
