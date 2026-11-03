package com.tsix.immocrm.entities.stl;

import com.tsix.immocrm.entities.CodeLibelleOmran;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.Nationalized;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/** Pièce de recette OMRAN, lue par l'onglet Recettes d'un tiers ; jamais écrite par nous. */
@Getter
@Entity
@Immutable
@Table(name = "StlDocument")
public class StlDocument extends CodeLibelleOmran {
    @Column(name = "DocDate")
    private LocalDateTime docDate;
    @Column(name = "MaturityDate")
    private LocalDateTime maturityDate;
    @Size(max = 255)
    @Nationalized
    @Column(name = "DocRef")
    private String docRef;
    @Column(name = "AmountToPay", precision = 26, scale = 6)
    private BigDecimal amountToPay;
    @Size(max = 1000)
    @Nationalized
    @Column(name = "Notes", length = 1000)
    private String notes;
    @Column(name = "CfgTierId")
    private UUID cfgTierId;
}
