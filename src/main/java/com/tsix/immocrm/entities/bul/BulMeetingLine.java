package com.tsix.immocrm.entities.bul;

import com.tsix.immocrm.entities.LigneOmran;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Décision d'une réunion ; {@code BulMeetingLineId} = décision d'origine quand elle est reportée. Description et Note sont
 * en {@code varchar} (pas de {@code @Nationalized}) ; {@code IsClosed} n'est pas mappé, OMRAN ne le lit pas.
 */
@Getter
@Setter
@Entity
@Table(name = "BulMeetingLine")
public class BulMeetingLine extends LigneOmran {
    @Column(name = "Sorting")
    private Integer sorting;

    @Column(name = "DueDate")
    private LocalDateTime dueDate;

    @Size(max = 500)
    @Column(name = "Description")
    private String description;

    @Size(max = 500)
    @Column(name = "Note")
    private String note;

    @Column(name = "IsInternal")
    private Boolean isInternal;

    @Column(name = "CfgSupplierId")
    private UUID cfgSupplierId;

    @Column(name = "PrjMarketId")
    private UUID prjMarketId;

    @Column(name = "BulMeetingId")
    private UUID bulMeetingId;

    @Column(name = "BulMeetingLineId")
    private UUID bulMeetingLineId;

    @Column(name = "BulObservationStatusId")
    private UUID bulObservationStatusId;
}
