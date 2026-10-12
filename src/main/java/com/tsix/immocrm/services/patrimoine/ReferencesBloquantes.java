package com.tsix.immocrm.services.patrimoine;

import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Lignes qui empêchent une suppression : les clés étrangères NO_ACTION de la production vers le programme et ses zones
 * (FEATURE-patrimoine-tranche-fiche.md §3.3-3.4), et vers la tranche (FEATURE-patrimoine-tranche-onglets.md §4). Requêtes constantes ; une table absente du schéma courant (CI,
 * {@code yesRemsTest}) ne bloque rien, d'où le test {@code OBJECT_ID} avant chaque comptage.
 */
@Component
class ReferencesBloquantes {

    /** {@code sql} compte les lignes qui référencent {@code :id}. */
    record Reference(String table, String sql, String libelle) {
    }

    private static final String TABLE_EXISTE = "SELECT CASE WHEN OBJECT_ID(:nom, 'U') IS NULL THEN 0 ELSE 1 END";
    private static final String ZONES_DU_PROGRAMME = "(SELECT PKey FROM CfgProjectZone WHERE CfgProjectId = :id)";

    static final List<Reference> PROGRAMME = List.of(
            new Reference("CfgTranche", "SELECT COUNT(*) FROM CfgTranche WHERE CfgProjectId = :id", "tranche(s)"),
            new Reference("StkItem", "SELECT COUNT(*) FROM StkItem WHERE CfgProjectId = :id", "article(s)"),
            new Reference("ActStockVariationCoef",
                    "SELECT COUNT(*) FROM ActStockVariationCoef WHERE CfgProjectId = :id",
                    "coefficient(s) de variation de stock"),
            new Reference("ActStockVariationExclusion",
                    "SELECT COUNT(*) FROM ActStockVariationExclusion WHERE CfgProjectId = :id",
                    "exclusion(s) de variation de stock"),
            new Reference("SavServiceExpense", "SELECT COUNT(*) FROM SavServiceExpense WHERE CfgProjectId = :id",
                    "dépense(s) SAV"),
            new Reference("StlItemPricing", "SELECT COUNT(*) FROM StlItemPricing WHERE CfgProjectId = :id",
                    "tarif(s) d'article"),
            new Reference("SavClaim",
                    "SELECT COUNT(*) FROM SavClaim WHERE CfgProjectZoneId IN " + ZONES_DU_PROGRAMME,
                    "réclamation(s) SAV sur ses zones"),
            new Reference("GrhWorkedDay",
                    "SELECT COUNT(*) FROM GrhWorkedDay WHERE CfgProjectZoneId IN " + ZONES_DU_PROGRAMME,
                    "journée(s) travaillée(s) sur ses zones"));

    /** Clés NO_ACTION vers {@code CfgTranche}, sauf {@code CfgTrancheStkItem} que la suppression retire elle-même. */
    static final List<Reference> TRANCHE = List.of(
            new Reference("StkItem", "SELECT COUNT(*) FROM StkItem WHERE StkHierarchyId = :id", "article(s)"),
            new Reference("ComProspection", "SELECT COUNT(*) FROM ComProspection WHERE StkHierarchyId = :id",
                    "prospection(s)"),
            new Reference("ComProspectionProject",
                    "SELECT COUNT(*) FROM ComProspectionProject WHERE StkHierarchyId = :id",
                    "projet(s) de prospection"),
            new Reference("ComOffer", "SELECT COUNT(*) FROM ComOffer WHERE StkHierarchyId = :id", "offre(s)"),
            new Reference("StkPresentationType",
                    "SELECT COUNT(*) FROM StkPresentationType WHERE StkHierarchyId = :id", "type(s) de présentation"),
            new Reference("StkHierarchyPlan", "SELECT COUNT(*) FROM StkHierarchyPlan WHERE StkHierarchyId = :id",
                    "plan(s)"),
            new Reference("CfgPrevision", "SELECT COUNT(*) FROM CfgPrevision WHERE StkHierarchyId = :id",
                    "prévision(s) de base"),
            new Reference("ComPrevision", "SELECT COUNT(*) FROM ComPrevision WHERE StkHierarchyId = :id",
                    "prévision(s) commerciale(s)"),
            new Reference("ComPrevisionVersion",
                    "SELECT COUNT(*) FROM ComPrevisionVersion WHERE StkHierarchyId = :id", "version(s) de prévision"),
            new Reference("ComPrevisionByPeriod",
                    "SELECT COUNT(*) FROM ComPrevisionByPeriod WHERE CfgTrancheId = :id", "prévision(s) par période"),
            new Reference("PrmRequestOffer", "SELECT COUNT(*) FROM PrmRequestOffer WHERE CfgTrancheId = :id",
                    "demande(s) d'offre"),
            new Reference("SavClaimGroup", "SELECT COUNT(*) FROM SavClaimGroup WHERE CfgTrancheId = :id",
                    "groupe(s) de réclamations SAV"),
            new Reference("SavServiceExpense", "SELECT COUNT(*) FROM SavServiceExpense WHERE CfgTrancheId = :id",
                    "dépense(s) SAV"),
            new Reference("StlQuote", "SELECT COUNT(*) FROM StlQuote WHERE CfgTrancheId = :id", "devis"),
            new Reference("StlItemPricing", "SELECT COUNT(*) FROM StlItemPricing WHERE CfgTrancheId = :id",
                    "tarif(s) d'article"),
            new Reference("StlSalePrevisionManual",
                    "SELECT COUNT(*) FROM StlSalePrevisionManual WHERE CfgTrancheId = :id",
                    "prévision(s) de vente manuelle(s)"),
            new Reference("StlTrancheVentilationItem",
                    "SELECT COUNT(*) FROM StlTrancheVentilationItem WHERE CfgTrancheId = :id",
                    "ventilation(s) de tranche"),
            new Reference("PrjProjectCfgTrancheVentilation",
                    "SELECT COUNT(*) FROM PrjProjectCfgTrancheVentilation WHERE CfgTrancheId = :id",
                    "ventilation(s) de projet chantier"),
            new Reference("StkFeasibilityStudyCfgTranche",
                    "SELECT COUNT(*) FROM StkFeasibilityStudyCfgTranche WHERE CfgTrancheId = :id",
                    "étude(s) de faisabilité"),
            new Reference("ActStockVariationCoef",
                    "SELECT COUNT(*) FROM ActStockVariationCoef WHERE CfgTrancheId = :id",
                    "coefficient(s) de variation de stock"),
            new Reference("AdmEvent", "SELECT COUNT(*) FROM AdmEvent WHERE CfgTrancheId = :id",
                    "évènement(s) d'agenda"),
            new Reference("ComCommissionLine", "SELECT COUNT(*) FROM ComCommissionLine WHERE CfgTrancheId = :id",
                    "ligne(s) de commission"));

    static final List<Reference> ZONE = List.of(
            new Reference("CfgProjectZone", "SELECT COUNT(*) FROM CfgProjectZone WHERE ParentId = :id",
                    "sous-zone(s)"),
            new Reference("SavClaim", "SELECT COUNT(*) FROM SavClaim WHERE CfgProjectZoneId = :id",
                    "réclamation(s) SAV"),
            new Reference("GrhWorkedDay", "SELECT COUNT(*) FROM GrhWorkedDay WHERE CfgProjectZoneId = :id",
                    "journée(s) travaillée(s)"));

    private final EntityManager em;

    ReferencesBloquantes(EntityManager em) {
        this.em = em;
    }

    /** « 2 tranche(s) », « 1 article(s) »… ; vide si rien ne bloque. */
    List<String> compter(List<Reference> references, UUID id) {
        List<String> bloquantes = new ArrayList<>();
        for (Reference reference : references) {
            long nombre = tableExiste(reference.table()) ? nombre(reference.sql(), id) : 0;
            if (nombre > 0) {
                bloquantes.add(nombre + " " + reference.libelle());
            }
        }
        return bloquantes;
    }

    /** {@code AdmMessage} n'a pas d'entité : suppression native, si la table existe. */
    void supprimerMessages(UUID categoryId) {
        if (tableExiste("AdmMessage")) {
            em.createNativeQuery("DELETE FROM AdmMessage WHERE CategoryId = :id")
                    .setParameter("id", categoryId)
                    .executeUpdate();
        }
    }

    private boolean tableExiste(String table) {
        Number existe = (Number) em.createNativeQuery(TABLE_EXISTE)
                .setParameter("nom", "dbo." + table)
                .getSingleResult();
        return existe.intValue() == 1;
    }

    private long nombre(String sql, UUID id) {
        return ((Number) em.createNativeQuery(sql).setParameter("id", id).getSingleResult()).longValue();
    }
}
