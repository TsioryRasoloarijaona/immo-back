package com.tsix.immocrm.entities.com;

import com.tsix.immocrm.entities.stk.StkItemType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/** dbo_ComProspectionType — a "recherché" article type on a Prospection (GAP-04, "Types de bien"). */
@Getter
@Setter
@Entity
@Table(name = "ComProspectionType")
public class ComProspectionType {
    @Id
    @Column(name = "PKey", nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ComProspectionId")
    private ComProspection comProspection;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "StkItemTypeId")
    private StkItemType stkItemType;
}
