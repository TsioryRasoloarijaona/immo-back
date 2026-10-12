package com.tsix.immocrm.entities.buy;

import com.tsix.immocrm.entities.AuditOmran;
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
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.Nationalized;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Pièce d'achat (facture de décompte ici). Colonnes utiles seulement (87 en base). Le lien vers le
 * décompte part de {@code PrjWorkingOut.BuyDocumentId} ; {@code PrjWorkingOutId} ne sert qu'aux avances.
 */
@Getter
@Setter
@Entity
@Immutable
@Table(name = "BuyDocument")
public class BuyDocument extends AuditOmran {

    @Id
    @Column(name = "PKey", nullable = false)
    private UUID id;

    @Size(max = 255)
    @Nationalized
    @Column(name = "Code")
    private String code;

    @Column(name = "DocDate")
    private LocalDateTime docDate;

    @Column(name = "MaturityDate")
    private LocalDateTime maturityDate;

    @Size(max = 255)
    @Nationalized
    @Column(name = "DocType")
    private String docType;

    @Size(max = 255)
    @Nationalized
    @Column(name = "ReferenceType")
    private String referenceType;

    @Column(name = "AmountHT", precision = 26, scale = 6)
    private BigDecimal amountHT;

    @Column(name = "AmountTTC", precision = 26, scale = 6)
    private BigDecimal amountTTC;

    @Column(name = "AmountToPay", precision = 26, scale = 6)
    private BigDecimal amountToPay;

    @Column(name = "AmountRegul", precision = 26, scale = 6)
    private BigDecimal amountRegul;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "BuyDocumentStatusId")
    private BuyDocumentStatus buyDocumentStatus;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CfgTierId")
    private CfgTier cfgTier;

    @Column(name = "PrjMarketId")
    private UUID prjMarketId;

    @Column(name = "PrjProjectId")
    private UUID prjProjectId;

    @Column(name = "PrjWorkingOutId")
    private UUID prjWorkingOutId;

    @Column(name = "StrEntityId")
    private UUID strEntityId;
}
