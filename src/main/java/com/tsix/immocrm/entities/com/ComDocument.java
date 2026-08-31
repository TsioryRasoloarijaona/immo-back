package com.tsix.immocrm.entities.com;

import com.tsix.immocrm.entities.stl.StlSettlementType;
import com.tsix.immocrm.entities.cfg.CfgTier;
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
@Table(name = "ComDocument")
public class ComDocument {
    @Id
    @Column(name = "PKey", nullable = false)
    private UUID id;

    @Column(name = "DocDate")
    private LocalDateTime docDate;

    @Column(name = "Amount", precision = 26, scale = 6)
    private BigDecimal amount;

    @Column(name = "ExcludeFromPrice")
    private Boolean excludeFromPrice;

    @Column(name = "AmountHT", precision = 26, scale = 6)
    private BigDecimal amountHT;

    @Column(name = "AmountTTC", precision = 26, scale = 6)
    private BigDecimal amountTTC;

    @Column(name = "VATRatio", precision = 26, scale = 6)
    private BigDecimal vATRatio;

    @Size(max = 255)
    @Nationalized
    @Column(name = "Description")
    private String description;

    @Size(max = 255)
    @Nationalized
    @Column(name = "QuittanceCode")
    private String quittanceCode;

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
    @JoinColumn(name = "ComFolderId")
    private ComFolder comFolder;

    @Column(name = "DiscountRatio", precision = 26, scale = 16)
    private BigDecimal discountRatio;

    @Size(max = 1000)
    @Nationalized
    @Column(name = "Notes", length = 1000)
    private String notes;

    // NOT the "Catégorie" shown on the échéancier (Omran "Schéma de financement" tab) — that's
    // ComSettlementCategoryId below. StlSettlementTypeId is NULL on every real échéance row;
    // StlSettlementType is the payment-method lookup used by StlSettlement, not ComDocument.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "StlSettlementTypeId")
    private StlSettlementType stlSettlementType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ComSettlementCategoryId")
    private ComSettlementCategory comSettlementCategory;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CfgContactId")
    private CfgTier cfgContact;

    @Column(name = "PaymentAuthorizationDate")
    private LocalDateTime paymentAuthorizationDate;

    // Compte destinataire du versement — confirmé colonne réelle (INFORMATION_SCHEMA), mais quasi
    // jamais renseigné en données réelles (2 lignes sur toute la base) contrairement à son
    // équivalent StlSettlement.StlAccountId (76% renseigné) — c'est ce dernier qui alimente le RIB
    // sur l'Autorisation d'Encaissement (voir AutorisationVersementService), pas ce champ-ci.
    // Laissé mappé (colonne réelle) même si non lu ailleurs pour l'instant.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "StlAccountId")
    private com.tsix.immocrm.entities.stl.StlAccount stlAccount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CfgDebtCollectorId")
    private CfgTier cfgDebtCollector;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ComPenaltyDocumentId")
    private ComDocument comPenaltyDocument;

    @Size(max = 255)
    @Nationalized
    @Column(name = "Code")
    private String code;


}