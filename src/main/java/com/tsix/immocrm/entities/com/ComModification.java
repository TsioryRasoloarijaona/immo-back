package com.tsix.immocrm.entities.com;

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

/** Demande de modification d'un dossier de vente ({@code Yes.Com.Sales.Modification}) ; pas de colonnes User*. */
@Getter
@Setter
@Entity
@Table(name = "ComModification")
public class ComModification {
    @Id
    @Column(name = "PKey", nullable = false)
    private UUID id;

    @Size(max = 255)
    @Nationalized
    @Column(name = "Description")
    private String description;

    @Column(name = "DemandDate")
    private LocalDateTime demandDate;

    @Column(name = "TechnicalValidationDate")
    private LocalDateTime technicalValidationDate;

    @Column(name = "CustomerValidationDate")
    private LocalDateTime customerValidationDate;

    @Column(name = "Cost", precision = 26, scale = 6)
    private BigDecimal cost;

    @Column(name = "CommercialAmount", precision = 26, scale = 6)
    private BigDecimal commercialAmount;

    @Column(name = "CommercialAmountHT", precision = 26, scale = 6)
    private BigDecimal commercialAmountHT;

    // Nom d'énumération OMRAN (ENCOURS, ACCEPTEE, REALISED…), voir domain.EtatModification.
    @Size(max = 255)
    @Nationalized
    @Column(name = "Status")
    private String status;

    @Size(max = 1000)
    @Nationalized
    @Column(name = "Notes", length = 1000)
    private String notes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ComFolderId")
    private ComFolder comFolder;
}
