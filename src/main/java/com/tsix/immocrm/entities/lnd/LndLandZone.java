package com.tsix.immocrm.entities.lnd;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Nationalized;

import java.util.UUID;

/** Zone d'un terrain brut (relation OMRAN « Zones terrain brut », non historisée). */
@Getter
@Setter
@Entity
@Table(name = "LndLandZone")
public class LndLandZone extends SurfacesFoncieresOmran {
    @Size(max = 255)
    @Nationalized
    @Column(name = "Identification")
    private String identification;

    @Column(name = "ProspectionId")
    private UUID prospectionId;
}
