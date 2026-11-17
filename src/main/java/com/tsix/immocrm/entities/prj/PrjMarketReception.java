package com.tsix.immocrm.entities.prj;

import com.tsix.immocrm.entities.AuditOmran;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Nationalized;

import java.time.LocalDateTime;
import java.util.UUID;

/** Réception d'un lot ; son genre ({@code PrjMarketReceptionKind.IsFinal}) fixe la date de réception définitive. */
@Getter
@Setter
@Entity
@Table(name = "PrjMarketReception")
public class PrjMarketReception extends AuditOmran {
    @Column(name = "PrjMarketId")
    private UUID prjMarketId;

    @Column(name = "sorting")
    private Integer sorting;

    @Column(name = "DocDate")
    private LocalDateTime docDate;

    @Size(max = 255)
    @Nationalized
    @Column(name = "MeetingPlace")
    private String meetingPlace;

    @Column(name = "RequiredBySupplier")
    private Boolean requiredBySupplier;

    @Column(name = "PrjMarketReceptionTypeId")
    private UUID prjMarketReceptionTypeId;

    @Column(name = "PrjMarketReceptionKindId")
    private UUID prjMarketReceptionKindId;
}
