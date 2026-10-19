package com.tsix.immocrm.entities;

import com.tsix.immocrm.entities.cfg.CfgTier;
import jakarta.persistence.Column;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MappedSuperclass;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Nationalized;

/** {@link CoordonneesOmran} plus le courrier et les responsables, communs à l'entité et au programme. */
@Getter
@Setter
@MappedSuperclass
public abstract class ResponsablesOmran extends CoordonneesOmran {
    /** Pour « {Ville} le, {date} » en pied de document. */
    @Size(max = 255)
    @Nationalized
    @Column(name = "SignatureCity")
    private String signatureCity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "AdmSignatoryId")
    private CfgTier admSignatory;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CfgTechnicalManagerId")
    private CfgTier cfgTechnicalManager;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CfgSalesManagerId")
    private CfgTier cfgSalesManager;
}
