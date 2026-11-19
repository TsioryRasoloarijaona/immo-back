package com.tsix.immocrm.entities.buy;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Nationalized;

import java.time.LocalDateTime;
import java.util.UUID;

/** Fournisseur présélectionné sur une demande interne d'achat (onglet Présélection). */
@Getter
@Setter
@Entity
@Table(name = "BuySelectionSupplier")
public class BuySelectionSupplier {
    @Id
    @Column(name = "PKey", nullable = false)
    private UUID id;

    @Column(name = "BuySelectionId")
    private UUID buySelectionId;

    @Column(name = "CfgTierId")
    private UUID cfgTierId;

    @Size(max = 255)
    @Nationalized
    @Column(name = "OfficeOrderRef")
    private String officeOrderRef;

    @Column(name = "SelectionLetterSendDate")
    private LocalDateTime selectionLetterSendDate;

    @Column(name = "TenderAcquisitionDate")
    private LocalDateTime tenderAcquisitionDate;

    @Column(name = "TenderDepositDate")
    private LocalDateTime tenderDepositDate;

    @Column(name = "IsRejected")
    private Boolean isRejected;

    @Size(max = 1000)
    @Nationalized
    @Column(name = "Notes")
    private String notes;

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
