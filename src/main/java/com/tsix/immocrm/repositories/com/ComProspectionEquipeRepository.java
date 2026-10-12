package com.tsix.immocrm.repositories.com;

import com.tsix.immocrm.entities.com.ComProspection;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Agrégats de l'écran « Mon équipe » (FEATURE-equipe-commerciale.md §4). {@code tous = true} lève le filtre projet ;
 * {@code projetIds} n'est alors jamais lu mais doit rester non vide ({@code IN ()} est invalide).
 */
public interface ComProspectionEquipeRepository extends Repository<ComProspection, UUID> {

    /** Une ligne par lead de la cohorte : le service en tire le niveau atteint (§4.2). */
    @Query(value = """
            SELECT p.PKey AS id, p.DocDate AS dateArrivee,
                   CASE WHEN e.Code = 'ComProspection' THEN 1 ELSE 0 END AS transformee,
                   k.Code AS etapeCode,
                   CASE WHEN EXISTS (SELECT 1 FROM ComActionMessage m
                                     JOIN ComActionMessageType ty ON ty.PKey = m.ComActionMessageTypeId
                                     WHERE m.ComProspectionId = p.PKey AND ty.Code = :typeVisite)
                        THEN 1 ELSE 0 END AS visite,
                   CASE WHEN EXISTS (SELECT 1 FROM StlQuote q WHERE q.ComProspectionId = p.PKey)
                        THEN 1 ELSE 0 END AS offre,
                   (SELECT MIN(f.DocDate) FROM ComFolder f LEFT JOIN StrStatus s ON s.PKey = f.StrStatusId
                     WHERE f.ComProspectionId = p.PKey
                       AND (s.Code IS NULL OR s.Code NOT IN ('ANNULATION', 'DESISTEMENT'))) AS premierDossier
            FROM ComProspection p
            JOIN StrEntity e ON e.PKey = p.StrEntityId
            LEFT JOIN ComProspectionKind k ON k.PKey = p.ComProspectionKindId
            LEFT JOIN CfgTranche t ON t.PKey = p.StkHierarchyId
            LEFT JOIN CfgProject pr ON pr.PKey = t.CfgProjectId
            LEFT JOIN CfgCompany soc ON soc.PKey = pr.CfgCompanyId
            WHERE e.Code IN ('ComPreProspection', 'ComProspection')
              AND p.DocDate BETWEEN :debut AND :fin
              AND (soc.IsActive = 1 OR soc.PKey IS NULL)
              AND (:tous = 1 OR pr.PKey IN (:projetIds))""", nativeQuery = true)
    List<LeadCohorte> cohorte(@Param("debut") LocalDateTime debut, @Param("fin") LocalDateTime fin,
                              @Param("typeVisite") String typeVisite,
                              @Param("tous") boolean tous, @Param("projetIds") List<UUID> projetIds);

    /** Portefeuille ouvert par commercial (§4, « Par commercial ») ; relance = la dernière non désarmée. */
    @Query(value = """
            SELECT x.commercialId AS commercialId, COUNT(*) AS ouvertes,
                   SUM(x.inactive) AS sansAction, SUM(x.enRetard) AS relancesEnRetard
            FROM (
                SELECT p.CfgCommercialId AS commercialId,
                       CASE WHEN COALESCE((SELECT MAX(m.DocDate) FROM ComActionMessage m
                                           WHERE m.ComProspectionId = p.PKey), p.DocDate) < :seuilInactivite
                            THEN 1 ELSE 0 END AS inactive,
                       CASE WHEN (SELECT TOP 1 r.AlertValidityDate FROM ComActionMessage r
                                  WHERE r.ComProspectionId = p.PKey AND r.AlertValidityDate IS NOT NULL
                                    AND ISNULL(r.WithoutRevival, 0) = 0 AND r.ComDocumentId IS NULL
                                  ORDER BY r.DocDate DESC, r.AlertValidityDate DESC) < :debutJour
                            THEN 1 ELSE 0 END AS enRetard
                FROM ComProspection p
                LEFT JOIN CfgTranche t ON t.PKey = p.StkHierarchyId
                LEFT JOIN CfgProject pr ON pr.PKey = t.CfgProjectId
                LEFT JOIN CfgCompany soc ON soc.PKey = pr.CfgCompanyId
                WHERE p.CfgCommercialId IN (:commerciaux)
                  AND ISNULL(p.IsClosed, 0) = 0
                  AND (p.StrEntityId IS NULL
                       OR p.StrEntityId NOT IN (SELECT e.PKey FROM StrEntity e WHERE e.Code = 'ComPreProspection'))
                  AND (soc.IsActive = 1 OR soc.PKey IS NULL)
                  AND (:tous = 1 OR pr.PKey IN (:projetIds))
            ) x
            GROUP BY x.commercialId""", nativeQuery = true)
    List<PortefeuilleCommercial> portefeuilles(@Param("commerciaux") List<UUID> commerciaux,
                                               @Param("seuilInactivite") LocalDateTime seuilInactivite,
                                               @Param("debutJour") LocalDateTime debutJour,
                                               @Param("tous") boolean tous,
                                               @Param("projetIds") List<UUID> projetIds);

    /**
     * Pré-prospections ouvertes sans commercial reçues dans la période (DocDate) ; la liste elle-même =
     * {@code GET /api/pre-prospections} avec les mêmes bornes (FEATURE-equipe-commerciale.md §10).
     */
    @Query(value = """
            SELECT COUNT(*) AS nombre, MIN(p.DocDate) AS plusAncienne
            FROM ComProspection p
            LEFT JOIN CfgTranche t ON t.PKey = p.StkHierarchyId
            LEFT JOIN CfgProject pr ON pr.PKey = t.CfgProjectId
            LEFT JOIN CfgCompany soc ON soc.PKey = pr.CfgCompanyId
            WHERE p.StrEntityId IN (SELECT e.PKey FROM StrEntity e WHERE e.Code = 'ComPreProspection')
              AND p.CfgCommercialId IS NULL
              AND ISNULL(p.IsClosed, 0) = 0
              AND p.DocDate BETWEEN :debut AND :fin
              AND (soc.IsActive = 1 OR soc.PKey IS NULL)
              AND (:tous = 1 OR pr.PKey IN (:projetIds))""", nativeQuery = true)
    EnAttente aDistribuer(@Param("debut") LocalDateTime debut, @Param("fin") LocalDateTime fin,
                          @Param("tous") boolean tous, @Param("projetIds") List<UUID> projetIds);

    interface LeadCohorte {
        UUID getId();

        LocalDateTime getDateArrivee();

        Integer getTransformee();

        String getEtapeCode();

        Integer getVisite();

        Integer getOffre();

        LocalDateTime getPremierDossier();
    }

    interface PortefeuilleCommercial {
        UUID getCommercialId();

        Integer getOuvertes();

        Integer getSansAction();

        Integer getRelancesEnRetard();
    }

    interface EnAttente {
        Integer getNombre();

        LocalDateTime getPlusAncienne();
    }
}
