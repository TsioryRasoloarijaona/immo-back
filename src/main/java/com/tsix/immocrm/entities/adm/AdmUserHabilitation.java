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
 * dbo.AdmUserHabilitation — table légataire (pré-existante, jamais créée par cette application)
 * qui porte le périmètre d'habilitation d'un compte {@link AdmUser}. Elle est **polymorphe : une
 * portée par ligne** — 98,6 % des 1071 lignes réelles n'ont qu'une seule des quatre colonnes
 * {@code CfgCompanyId}/{@code CfgProjectId}/{@code CfgTrancheId}/{@code PrjProjectId} renseignée
 * (346 tranche, 302 PrjProject, 237 société, 196 projet).
 *
 * <p>La feature "Périmètre commercial" n'exploite que les lignes <b>projet</b>
 * ({@code CfgProjectId IS NOT NULL}). Les trois autres portées sont lues mais ignorées par le
 * filtrage, et surtout <b>jamais supprimées ni modifiées</b> : elles appartiennent à l'application
 * de bureau légataire (OMRAN Estate), toujours en service — les effacer casserait ses
 * habilitations. Voir {@code PerimetreCommercialService#remplacerProjets}.
 *
 * <p>{@code PrjProjectId} est mappé en {@link UUID} brut plutôt qu'en association : aucune entité
 * JPA ne modélise {@code PrjProject} (module "Projets travaux" du légataire, hors périmètre de
 * cette application). Même convention que {@code StkItem.comFolderId}.
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
     * Le compte habilité. C'est la <b>seule</b> clé de jointure valide pour résoudre un périmètre :
     * {@code AdmUser.CfgEmployeeId} n'est pas unique (59 renseignés pour 58 valeurs distinctes,
     * un tier porté par 2 comptes), donc une résolution par tier duplique les lignes.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "AdmUserId")
    private AdmUser admUser;

    /** Portée "société" — lue, jamais appliquée ni modifiée par cette application. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CfgCompanyId")
    private CfgCompany cfgCompany;

    /** Portée "projet" — la seule exploitée par le périmètre commercial. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CfgProjectId")
    private CfgProject cfgProject;

    /** Portée "tranche" — lue, jamais appliquée ni modifiée par cette application. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CfgTrancheId")
    private CfgTranche cfgTranche;

    /** Portée "projet travaux" (PrjProject, non mappé) — lue, jamais appliquée ni modifiée. */
    @Column(name = "PrjProjectId")
    private UUID prjProjectId;
}
