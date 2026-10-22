package com.tsix.immocrm.entities.stk;

import com.tsix.immocrm.entities.LigneOmran;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/** Remboursement d'un crédit d'étude (« Remboursement de crédit »). */
@Getter
@Setter
@Entity
@Table(name = "FSReimbursement")
public class FSReimbursement extends LigneOmran {
    @Column(name = "FSFundingId")
    private UUID fsFundingId;

    @Column(name = "paymentDate")
    private LocalDateTime paymentDate;

    @Column(name = "amountBase", precision = 26, scale = 6)
    private BigDecimal amountBase;

    @Column(name = "interestRate", precision = 26, scale = 6)
    private BigDecimal interestRate;

    @Column(name = "amountInterest", precision = 26, scale = 6)
    private BigDecimal amountInterest;

    @Column(name = "amountToPay", precision = 26, scale = 6)
    private BigDecimal amountToPay;
}
