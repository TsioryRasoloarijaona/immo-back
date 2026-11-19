package com.tsix.immocrm.entities.buy;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Nationalized;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/** Modalité de paiement d'une demande interne d'achat ; table sans colonne {@code Description}. */
@Getter
@Setter
@Entity
@Table(name = "BuySelectionPaymentTerm")
public class BuySelectionPaymentTerm {
    @Id
    @Column(name = "PKey", nullable = false)
    private UUID id;

    @Column(name = "BuySelectionId")
    private UUID buySelectionId;

    @Column(name = "Sorting")
    private Integer sorting;

    @Size(max = 255)
    @Nationalized
    @Column(name = "Code")
    private String code;

    @Column(name = "Ratio", precision = 26, scale = 6)
    private BigDecimal ratio;

    @Size(max = 255)
    @Nationalized
    @Column(name = "UserCreate")
    private String userCreate;

    @Column(name = "UserCreateDateTime")
    private LocalDateTime userCreateDateTime;
}
