package com.tsix.immocrm.entities.com;

import com.tsix.immocrm.entities.adm.AdmCity;
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

/** dbo_ComProspectionCity — a "recherché" city on a Prospection (GAP-04, "Villes" tab). */
@Getter
@Setter
@Entity
@Table(name = "ComProspectionCity")
public class ComProspectionCity {
    @Id
    @Column(name = "PKey", nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ComProspectionId")
    private ComProspection comProspection;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "AdmCityId")
    private AdmCity admCity;
}
