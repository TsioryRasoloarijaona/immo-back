package com.tsix.immocrm.entities.com;

import com.tsix.immocrm.entities.adm.AdmCountry;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.entities.stk.StkVocation;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Nationalized;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "ComProspection")
public class ComProspection {
    @Id
    @Column(name = "PKey", nullable = false)
    private UUID id;

    @Column(name = "DocDate")
    private LocalDateTime docDate;

    @Column(name = "Area", precision = 26, scale = 6)
    private BigDecimal area;

    @Column(name = "Price", precision = 26, scale = 6)
    private BigDecimal price;

    @Column(name = "AdvanceAmount", precision = 26, scale = 6)
    private BigDecimal advanceAmount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ComProspectionOriginId")
    private ComProspectionOrigin comProspectionOrigin;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CfgProspectId")
    private CfgTier cfgProspect;

    @Size(max = 1000)
    @Nationalized
    @Column(name = "Notes", length = 1000)
    private String notes;

    @Column(name = "AreaTo", precision = 26, scale = 6)
    private BigDecimal areaTo;

    @Column(name = "PriceTo", precision = 26, scale = 6)
    private BigDecimal priceTo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "StkVocationId")
    private StkVocation stkVocation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ComProspectionKindId")
    private ComProspectionKind comProspectionKind;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ComActionMessageTypeId")
    private ComActionMessageType comActionMessageType;

    @Column(name = "FundingOwnBase", precision = 26, scale = 6)
    private BigDecimal fundingOwnBase;

    @Column(name = "FundingCredit", precision = 26, scale = 6)
    private BigDecimal fundingCredit;

    @Column(name = "FundingOther", precision = 26, scale = 6)
    private BigDecimal fundingOther;

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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ComActionId")
    private ComAction comAction;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ComProspectionOriginSupportId")
    private ComProspectionOriginSupport comProspectionOriginSupport;

    @Column(name = "AlertValidityDate")
    private LocalDateTime alertValidityDate;

    @Size(max = 255)
    @Nationalized
    @Column(name = "AlertMessage")
    private String alertMessage;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CfgCommercialId")
    private CfgTier cfgCommercial;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "StkHierarchyId")
    private CfgTranche stkHierarchy;

    /**
     * Entité structurelle du légataire, qui distingue une <strong>prospection de vente</strong>
     * ({@code Code = 'ComProspection'}) d'une <strong>pré-prospection</strong>
     * ({@code 'ComPreProspection'}), d'une prospection de location, etc.
     *
     * <p>C'est le discriminant réel du logiciel de bureau, et ses vues font un
     * {@code INNER JOIN} dessus : une ligne dont {@code StrEntityId} est {@code NULL}
     * <strong>n'apparaît dans aucune vue</strong>, donc dans aucun écran du logiciel de bureau.
     * En base réelle, 4 lignes sont dans ce cas — deux injectées par {@code n8n_api}, une créée
     * par notre API, une par un script de test.
     *
     * <p>Mappé en UUID brut plutôt qu'en {@code @ManyToOne} : {@code StrEntity} n'a pas d'entité
     * JPA dans ce dépôt, et {@code StrStatus.StrConfigEntityId} suit déjà exactement cette
     * convention.
     *
     * <p>Mappé pour que les tests puissent produire des prospections visibles de
     * {@code ComSaleProspectionView}. <strong>{@code ProspectionService.creerProspection} ne le
     * renseigne toujours pas</strong> — voir {@code FEATURE-vues-legacy-prospection.md} §8.
     */
    @Column(name = "StrEntityId")
    private UUID strEntityId;

    /**
     * Pays du prospect, tel que la vue légataire l'expose sous {@code AdmCountryId} — renseigné
     * sur <strong>27,3 %</strong> des 14 118 prospections réelles.
     *
     * <p>La colonne existait en base sans être mappée : c'était le seul manque de
     * {@code INVENTAIRE-colonnes-prospection.md} §2 qui exigeait autre chose qu'une entrée au
     * catalogue.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "AdmCountryId")
    private AdmCountry admCountry;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "StkItemId")
    private StkItem stkItem;

    @Column(name = "IsClosed")
    private Boolean isClosed;

    @Size(max = 255)
    @Nationalized
    @Column(name = "Code")
    private String code;

    @Column(name = "ClosingDate")
    private LocalDateTime closingDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ComProspectionClosingReasonId")
    private ComProspectionClosingReason comProspectionClosingReason;

    @Size(max = 255)
    @Nationalized
    @Column(name = "ClosingReasonText")
    private String closingReasonText;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CfgProposerId")
    private CfgTier cfgProposer;

    @Column(name = "IsOldCustomer")
    private Boolean isOldCustomer;

    @Size(max = 255)
    @Nationalized
    @Column(name = "TempDescription")
    private String tempDescription;

    @Size(max = 255)
    @Nationalized
    @Column(name = "TempEMail")
    private String tempEMail;

    @Size(max = 255)
    @Nationalized
    @Column(name = "TempTel")
    private String tempTel;

    @Size(max = 255)
    @Nationalized
    @Column(name = "TempField1")
    private String tempField1;

    @Size(max = 255)
    @Nationalized
    @Column(name = "TempField2")
    private String tempField2;

    @Size(max = 255)
    @Nationalized
    @Column(name = "TempField3")
    private String tempField3;

    @Size(max = 255)
    @Nationalized
    @Column(name = "TempField4")
    private String tempField4;

    @Size(max = 255)
    @Nationalized
    @Column(name = "TempField5")
    private String tempField5;

    @Size(max = 255)
    @Nationalized
    @Column(name = "TempField6")
    private String tempField6;

    @Size(max = 255)
    @Nationalized
    @Column(name = "TempField7")
    private String tempField7;

    @Size(max = 255)
    @Nationalized
    @Column(name = "TempField8")
    private String tempField8;

    @Size(max = 255)
    @Nationalized
    @Column(name = "TempField9")
    private String tempField9;

    @Size(max = 255)
    @Nationalized
    @Column(name = "TempField10")
    private String tempField10;

    @Column(name = "MapLatitude", precision = 20, scale = 16)
    private BigDecimal mapLatitude;

    @Column(name = "MapLongitude", precision = 20, scale = 16)
    private BigDecimal mapLongitude;

    @Column(name = "MapRadius", precision = 20, scale = 16)
    private BigDecimal mapRadius;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CfgIntermediateId")
    private CfgTier cfgIntermediate;


}