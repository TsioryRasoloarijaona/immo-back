package com.tsix.immocrm.entities.com;

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

/** Ligne de commission d'un dossier (OMRAN {@code Yes.Com.Commission.CommissionLine}) ; montant sans arrondi. */
@Getter
@Setter
@Entity
@Table(name = "ComCommissionLine")
public class ComCommissionLine {
    @Id
    @Column(name = "PKey", nullable = false)
    private UUID id;

    @Column(name = "DocDate")
    private LocalDateTime docDate;

    @Column(name = "CfgEmployeeId")
    private UUID cfgEmployeeId;

    @Column(name = "ComFolderId")
    private UUID comFolderId;

    @Size(max = 255)
    @Nationalized
    @Column(name = "CommissionType")
    private String commissionType;

    @Column(name = "CommissionAmount", precision = 26, scale = 16)
    private BigDecimal commissionAmount;

    @Column(name = "StkItemCategoryId")
    private UUID stkItemCategoryId;

    @Column(name = "StkItemId")
    private UUID stkItemId;

    @Column(name = "CfgTrancheId")
    private UUID cfgTrancheId;

    @Column(name = "BuyDocumentId")
    private UUID buyDocumentId;

    @Size(max = 255)
    @Nationalized
    @Column(name = "CalculationMethod")
    private String calculationMethod;

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
