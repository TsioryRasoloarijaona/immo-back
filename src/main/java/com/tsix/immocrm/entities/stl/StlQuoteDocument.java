package com.tsix.immocrm.entities.stl;

import com.tsix.immocrm.entities.com.ComSettlementCategory;
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
import java.util.UUID;

/** Ligne du schéma de financement saisie sur une offre dans OMRAN ; Arpent la lit et la supprime, sans en créer. */
@Getter
@Setter
@Entity
@Table(name = "StlQuoteDocument")
public class StlQuoteDocument {
    @Id
    @Column(name = "PKey", nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "StlQuoteId")
    private StlQuote stlQuote;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ComSettlementCategoryId")
    private ComSettlementCategory comSettlementCategory;

    @Size(max = 255)
    @Nationalized
    @Column(name = "Code")
    private String code;

    @Size(max = 255)
    @Nationalized
    @Column(name = "Description")
    private String description;

    @Column(name = "Amount", precision = 26, scale = 6)
    private BigDecimal amount;
}
