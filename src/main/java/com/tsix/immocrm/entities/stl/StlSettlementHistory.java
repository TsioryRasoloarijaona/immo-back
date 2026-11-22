package com.tsix.immocrm.entities.stl;

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

/** Historique des échéances, types et références d'un règlement (OMRAN {@code Settlement.cs:Historize}). */
@Getter
@Setter
@Entity
@Table(name = "StlSettlementHistory")
public class StlSettlementHistory {
    @Id
    @Column(name = "PKey", nullable = false)
    private UUID id;

    @Column(name = "AdmUserId")
    private UUID admUserId;

    @Column(name = "DocDate")
    private LocalDateTime docDate;

    @Column(name = "EcheanceDate")
    private LocalDateTime echeanceDate;

    @Column(name = "StlSettlementId")
    private UUID stlSettlementId;

    @Column(name = "StlSettlementTypeId")
    private UUID stlSettlementTypeId;

    @Size(max = 255)
    @Nationalized
    @Column(name = "Reference")
    private String reference;

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
