package com.tsix.immocrm.entities.adm;

import com.tsix.immocrm.entities.cfg.CfgCompany;
import com.tsix.immocrm.entities.cfg.CfgProject;
import com.tsix.immocrm.entities.cfg.CfgTranche;
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

/**
 * Périmètre d'habilitation d'un compte, polymorphe : une portée par ligne. Seules les lignes projet
 * sont exploitées ; les autres appartiennent au logiciel de bureau, toujours en service, et ne sont
 * jamais modifiées ni supprimées — voir FEATURE-perimetre-commercial.md.
 */
@Getter
@Setter
@Entity
@Table(name = "AdmUserHabilitation")
public class AdmUserHabilitation {

    @Id
    @Column(name = "PKey", nullable = false)
    private UUID id;

    /**
     * Seule clé de jointure valide pour un périmètre : {@code AdmUser.CfgEmployeeId} n'est pas
     * unique (un tier porté par deux comptes), une résolution par tier duplique les lignes.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "AdmUserId")
    private AdmUser admUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CfgCompanyId")
    private CfgCompany cfgCompany;

    /** La seule portée exploitée par le périmètre commercial. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CfgProjectId")
    private CfgProject cfgProject;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CfgTrancheId")
    private CfgTranche cfgTranche;

    /** UUID brut : {@code PrjProject} (module « Projets travaux ») n'est pas mappé. */
    @Column(name = "PrjProjectId")
    private UUID prjProjectId;
}
