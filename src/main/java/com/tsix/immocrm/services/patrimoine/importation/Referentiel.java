package com.tsix.immocrm.services.patrimoine.importation;

import com.tsix.immocrm.services.patrimoine.ReferencesPatrimoine;
import jakarta.persistence.EntityManager;
import jakarta.persistence.FlushModeType;
import jakarta.persistence.TypedQuery;

import java.util.List;
import java.util.UUID;

/**
 * Référentiels des colonnes REFERENCE de l'import générique : lignes (id, code, libellé) des objets <b>visibles</b>
 * (R4 : une entité inactive et ses programmes sont « inexistents »), feuille d'aide du modèle si {@code aide}.
 */
enum Referentiel {
    ENTITE("Entités", "select x.id, x.code, x.description from CfgCompany x where x.isActive = true order by x.code"),
    PROGRAMME("Programmes", "select x.id, x.code, x.description from CfgProject x where x.cfgCompany.isActive = true "
            + "order by x.code"),
    PAYS("Pays", "select x.id, x.code, x.description from AdmCountry x order by x.code"),
    VILLE("Villes", "select x.id, x.code, x.description from AdmCity x order by x.code"),
    DEVISE("Devises", "select x.id, x.code, x.description from StlCurrency x order by x.code"),
    GROUPE_ACTIONNAIRES("Groupes d'actionnaires", "select x.id, x.code, x.description from CfgGroupShareholder x "
            + "order by x.code"),
    PERSONNEL("Personnels", ReferencesPatrimoine.TYPE_EMPLOYE),
    FOURNISSEUR("Fournisseurs", ReferencesPatrimoine.TYPE_FOURNISSEUR),
    /** Courtier, notaire : tout tiers ; pas de feuille d'aide (la table porte aussi les clients). */
    TIERS(null, "select x.id, x.code, x.description from CfgTier x order by x.code"),
    STANDING("Standings", "select x.id, x.code, x.description from CfgTrancheStanding x order by x.code"),
    NIVEAU("Niveaux", "select x.id, x.code, x.description from LndLevel x order by x.code"),
    RUBRIQUE("Rubriques", "select x.id, x.code, x.description from StkTrancheHeading x order by x.code"),
    PLAN_COMPTABLE("Plans comptables", "select x.id, x.code, x.description from ActAccount x order by x.code"),
    VOCATION("Vocations", "select x.id, x.code, x.description from LndVocation x order by x.code"),
    ORIENTATION("Orientations", "select x.id, x.code, x.description from StkOrientation x order by x.code"),
    CATEGORIE("Catégories", "select x.id, x.code, x.description from StkItemCategory x where x.stkKind.code = 'PROS TER' "
            + "order by x.code"),
    TYPE_TERRAIN("Types", "select x.id, x.code, x.description from StkItemType x where x.stkKind.code = 'TER' "
            + "order by x.code");

    private static final String TIERS_DU_TYPE = "select x.id, x.code, x.description from CfgTier x "
            + "where x.cfgTierType.id = :type order by x.code";

    /** Nom de la feuille d'aide ; nul = pas de feuille. */
    final String aide;
    private final String jpql;
    private final UUID typeTiers;

    Referentiel(String aide, String jpql) {
        this.aide = aide;
        this.jpql = jpql;
        this.typeTiers = null;
    }

    Referentiel(String aide, UUID typeTiers) {
        this.aide = aide;
        this.jpql = TIERS_DU_TYPE;
        this.typeTiers = typeTiers;
    }

    /** Une ligne : {@code [id, code, libellé]}, par code. */
    List<Object[]> lignes(EntityManager em) {
        TypedQuery<Object[]> requete = em.createQuery(jpql, Object[].class).setFlushMode(FlushModeType.COMMIT);
        if (typeTiers != null) {
            requete.setParameter("type", typeTiers);
        }
        return requete.getResultList();
    }
}
