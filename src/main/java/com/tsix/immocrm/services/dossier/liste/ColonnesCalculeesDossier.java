package com.tsix.immocrm.services.dossier.liste;

import com.tsix.immocrm.domain.ColonneDossier;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Colonnes de la grille OMRAN calculées par {@code ComFolderView} (sous-requêtes), lues en une requête
 * native par page de dossiers. Formules recopiées de la vue, voir FEATURE-vente-liste.md §1.
 */
@Component
public class ColonnesCalculeesDossier {

    /** Ce qui exige la requête native. */
    public static final Set<ColonneDossier> COLONNES = EnumSet.of(
            ColonneDossier.NOMBRE_ETATS_MODIFICATION,
            ColonneDossier.NOMBRE_CADEAUX, ColonneDossier.PRIX_VENTE_M2_TTC, ColonneDossier.PRIX_VENTE_M2_HT,
            ColonneDossier.ALERTE, ColonneDossier.NOMBRE_ACTIONS, ColonneDossier.DERNIERE_COMMUNICATION_DATE,
            ColonneDossier.DERNIERE_COMMUNICATION_RESULTAT, ColonneDossier.DERNIERE_COMMUNICATION_MESSAGE,
            ColonneDossier.INSTRUCTION_NON_LUE, ColonneDossier.SOUS_ARTICLES_CODES,
            ColonneDossier.SOUS_ARTICLES_LIBELLES, ColonneDossier.AUTRES_CLIENTS, ColonneDossier.CLIENT_VILLE,
            ColonneDossier.CLIENT_PAYS);

    /** Aucun compte n'a cet id : sans utilisateur connecté, aucune instruction ne le vise. */
    private static final UUID AUCUN_UTILISATEUR = new UUID(0L, 0L);

    /** Ids par requête, sous la limite de 2 100 paramètres de SQL Server (les deux autres paramètres compris). */
    static final int TAILLE_TRANCHE = 2_000;

    // Catégories « communication » d'OMRAN : 1111 communication, 2222 réservation, 3333 livraison, 9999 annulation.
    private static final String REQUETE = """
            SELECT f.PKey,
              (SELECT COUNT(*) FROM ComModification m WHERE m.Status = 'ENCOURS' AND m.ComFolderId = f.PKey),
              (SELECT COUNT(*) FROM ComFolderGift g WHERE g.ComFolderId = f.PKey),
              (SELECT SUM(ISNULL(i.Area, 0)) FROM ComFolderItem i
                 WHERE i.ComFolderId = f.PKey AND i.IsUnderItem = 1 AND i.IsUnderItemOrigin = 0),
              (SELECT TOP (1) a.AlertDate FROM AdmAlertData a
                 INNER JOIN ComActionMessage m ON m.PKey = a.RelatedKey
                 WHERE a.Active = 1
                   AND a.AlertDate <= DATEADD(day, ISNULL((SELECT p.CodeValue FROM AdmParamsInteger p
                                                            WHERE p.Code = 'DefaultAlertDelai'), 0), :maintenant)
                   AND m.ComProspectionId = f.ComProspectionId
                   AND m.ComActionMessageCategoryId IN ('69f8e7d6-1111-1111-1111-1aa8b1f02008',
                       '69f8e7d6-2222-2222-2222-1aa8b1f02008', '69f8e7d6-3333-3333-3333-1aa8b1f02008',
                       '69f8e7d6-9999-9999-9999-1aa8b1f02008')
                 ORDER BY a.AlertDate DESC),
              (SELECT COUNT(m.DocDate) FROM ComActionMessage m
                 WHERE m.ComActionMessageCategoryId IN ('69f8e7d6-2222-2222-2222-1aa8b1f02008',
                       '69f8e7d6-3333-3333-3333-1aa8b1f02008', '69f8e7d6-9999-9999-9999-1aa8b1f02008')
                   AND m.ComFolderId = f.PKey AND m.IsUnpaid = 0 AND m.DocDate <= :maintenant),
              dc.DocDate, dc.Resultat, dc.Message,
              CASE WHEN ins.CfgEmployeeId IS NOT NULL
                    AND ins.CfgEmployeeId = (SELECT u.CfgEmployeeId FROM AdmUser u WHERE u.PKey = :utilisateur)
                   THEN 1 ELSE 0 END,
              LTRIM(STUFF((SELECT DISTINCT ', ' + c.Code FROM ComFolderItem c WHERE c.ComFolderId = f.PKey
                 FOR XML PATH(''), TYPE).value('.', 'nvarchar(max)'), 1, 1, '')),
              LTRIM(STUFF((SELECT DISTINCT ', ' + c.Description FROM ComFolderItem c WHERE c.ComFolderId = f.PKey
                 FOR XML PATH(''), TYPE).value('.', 'nvarchar(max)'), 1, 1, '')),
              LTRIM(STUFF((SELECT DISTINCT ', ' + t.Description FROM ComFolderCustomer fc
                 INNER JOIN CfgTier t ON fc.CfgTierId = t.PKey WHERE fc.ComFolderId = f.PKey
                 FOR XML PATH(''), TYPE).value('.', 'nvarchar(max)'), 1, 1, '')),
              COALESCE(ci.Description, NULLIF(LTRIM(RTRIM(cl.City)), '')), co.Description
            FROM ComFolder f
            LEFT JOIN CfgTier cl ON cl.PKey = f.CfgCustomerId
            LEFT JOIN AdmCity ci ON ci.PKey = cl.AdmCityId
            LEFT JOIN AdmCountry co ON co.PKey = cl.AdmCountryId
            OUTER APPLY (SELECT TOP (1) m.DocDate, sc.Description AS Resultat,
                           IIF(ISNULL(m.AlertMessage, '') = '', sc.Description, m.AlertMessage) AS Message
                         FROM ComActionMessage m
                         LEFT JOIN ComActionMessageSubCategory sc ON m.ComActionMessageSubCategoryId = sc.PKey
                         WHERE m.ComActionMessageCategoryId IN ('69f8e7d6-1111-1111-1111-1aa8b1f02008',
                               '69f8e7d6-2222-2222-2222-1aa8b1f02008', '69f8e7d6-3333-3333-3333-1aa8b1f02008',
                               '69f8e7d6-9999-9999-9999-1aa8b1f02008')
                           AND m.DocDate <= :maintenant AND m.IsSystem = 0
                           AND m.ComProspectionId = f.ComProspectionId
                         ORDER BY m.DocDate DESC) dc
            OUTER APPLY (SELECT TOP (1) m.CfgEmployeeId FROM ComActionMessage m
                         WHERE m.ComActionMessageCategoryId IN ('69f8e7d6-1111-1111-1111-1aa8b1f02008',
                               '69f8e7d6-2222-2222-2222-1aa8b1f02008', '69f8e7d6-3333-3333-3333-1aa8b1f02008',
                               '69f8e7d6-9999-9999-9999-1aa8b1f02008')
                           AND m.IsNotRead = 1 AND m.IsSystem = 0 AND m.ComProspectionId = f.ComProspectionId
                         ORDER BY m.DocDate DESC) ins
            WHERE f.PKey IN (:ids)""";

    private final EntityManager em;
    private final Clock clock;

    public ColonnesCalculeesDossier(EntityManager em, Clock clock) {
        this.em = em;
        this.clock = clock;
    }

    public static boolean requises(Set<ColonneDossier> colonnes) {
        return colonnes.stream().anyMatch(COLONNES::contains);
    }

    public Map<UUID, ValeursCalculees> pour(Collection<UUID> ids, UUID admUserId) {
        List<UUID> tous = List.copyOf(ids);
        Map<UUID, ValeursCalculees> parDossier = new HashMap<>();
        LocalDateTime maintenant = LocalDateTime.now(clock);
        // SQL Server refuse plus de 2 100 paramètres liés par requête : l'export (20 000 lignes) passe par tranches.
        for (int debut = 0; debut < tous.size(); debut += TAILLE_TRANCHE) {
            List<UUID> tranche = tous.subList(debut, Math.min(debut + TAILLE_TRANCHE, tous.size()));
            lire(tranche, admUserId, maintenant, parDossier);
        }
        return parDossier;
    }

    private void lire(List<UUID> ids, UUID admUserId, LocalDateTime maintenant, Map<UUID, ValeursCalculees> parDossier) {
        List<?> lignes = em.createNativeQuery(REQUETE)
                .setParameter("ids", ids)
                .setParameter("maintenant", maintenant)
                .setParameter("utilisateur", admUserId != null ? admUserId : AUCUN_UTILISATEUR)
                .getResultList();
        for (Object ligne : lignes) {
            Object[] l = (Object[]) ligne;
            parDossier.put(uuid(l[0]), new ValeursCalculees(
                    entier(l[1]), entier(l[2]), decimal(l[3]), date(l[4]), entier(l[5]),
                    date(l[6]), (String) l[7], (String) l[8], entier(l[9]) == 1,
                    vide(l[10]), vide(l[11]), vide(l[12]), vide(l[13]), vide(l[14])));
        }
    }

    private static UUID uuid(Object brut) {
        return brut instanceof UUID u ? u : UUID.fromString(brut.toString());
    }

    private static Integer entier(Object brut) {
        return brut instanceof Number n ? n.intValue() : null;
    }

    private static BigDecimal decimal(Object brut) {
        if (brut instanceof BigDecimal b) {
            return b;
        }
        return brut instanceof Number n ? BigDecimal.valueOf(n.doubleValue()) : null;
    }

    private static LocalDateTime date(Object brut) {
        if (brut instanceof Timestamp t) {
            return t.toLocalDateTime();
        }
        return brut instanceof LocalDateTime d ? d : null;
    }

    /** {@code STUFF} sur un ensemble vide rend NULL ; une chaîne vide n'apporte rien de plus. */
    private static String vide(Object brut) {
        return brut instanceof String s && !s.isBlank() ? s : null;
    }
}
