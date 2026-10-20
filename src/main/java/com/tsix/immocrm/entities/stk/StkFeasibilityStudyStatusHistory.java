package com.tsix.immocrm.entities.stk;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/** Historique des états d'une étude : la ligne initiale est écrite à la création. */
@Getter
@Setter
@Entity
@Table(name = "StkFeasibilityStudyStatusHistory")
public class StkFeasibilityStudyStatusHistory {
    @Id
    @Column(name = "PKey", nullable = false)
    private UUID id;

    @Column(name = "DocDate")
    private LocalDateTime docDate;

    @Column(name = "StkFeasibilityStudyStatusId")
    private UUID statusId;

    @Column(name = "StkFeasibilityStudyId")
    private UUID feasibilityStudyId;

    @Column(name = "AdmUserId")
    private UUID admUserId;
}
