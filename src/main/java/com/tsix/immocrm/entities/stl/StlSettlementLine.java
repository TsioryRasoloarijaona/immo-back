package com.tsix.immocrm.entities.stl;

import com.tsix.immocrm.entities.com.ComDocument;
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
@Table(name = "StlSettlementLine")
public class StlSettlementLine {
    @Id
    @Column(name = "PKey", nullable = false)
    private UUID id;

    @Column(name = "Amount", precision = 26, scale = 6)
    private BigDecimal amount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "StlSettlementId")
    private StlSettlement stlSettlement;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ComDocumentId")
    private ComDocument comDocument;

    // Facture fournisseur réglée (module Chantier) ; id brut, BuyDocument n'est lu qu'en lecture seule.
    @Column(name = "BuyDocumentId")
    private UUID buyDocumentId;

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