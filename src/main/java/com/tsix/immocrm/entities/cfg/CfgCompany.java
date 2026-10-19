package com.tsix.immocrm.entities.cfg;

import com.tsix.immocrm.entities.ResponsablesOmran;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Nationalized;

/**
 * Entité juridique d'un projet ou d'un règlement. Les documents générés la lisent : ne jamais écrire
 * un nom de société en dur dans un renderer.
 */
@Getter
@Setter
@Entity
@Table(name = "CfgCompany")
public class CfgCompany extends ResponsablesOmran {
    /**
     * Décide de la visibilité de l'entité et de ses projets, tranches et articles ; à entretenir en
     * base (005_CfgCompany_IsActive.sql). {@code null} vaut inactif : ne rien exposer par défaut.
     */
    @Column(name = "IsActive")
    private Boolean isActive;

    @Size(max = 1000)
    @Nationalized
    @Column(name = "DescriptionLong", length = 1000)
    private String descriptionLong;

    /** Représentant légal (CfgTrancheView.CfgTierDescription). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CfgTierId")
    private CfgTier representantLegal;

    /** Équivalent de {@code StlSettlementView.CfgComPanyViewLegalForm}. */
    @Size(max = 255)
    @Nationalized
    @Column(name = "LegalForm")
    private String legalForm;
}
