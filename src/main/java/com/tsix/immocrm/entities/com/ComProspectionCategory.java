package com.tsix.immocrm.entities.com;

import com.tsix.immocrm.entities.stk.StkItemCategory;
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

/** Catégorie d'article recherchée par une prospection. */
@Getter
@Setter
@Entity
@Table(name = "ComProspectionCategory")
public class ComProspectionCategory {
    @Id
    @Column(name = "PKey", nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ComProspectionId")
    private ComProspection comProspection;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "StkItemCategoryId")
    private StkItemCategory stkItemCategory;
}
