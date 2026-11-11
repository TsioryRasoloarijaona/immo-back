package com.tsix.immocrm.services.achat.config;

import com.tsix.immocrm.dto.achat.config.RoleTransitionDto;
import com.tsix.immocrm.dto.achat.config.StatutAchatConfigDto;
import com.tsix.immocrm.dto.achat.config.TransitionAchatDto;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/** Statuts et transitions des pièces d'achat (FEATURE-achat-regle-commune.md §4) : une requête par liste, plus les rôles. */
@Service
@Transactional(readOnly = true)
public class ConfigurationAchatService {

    private static final String SANS_GESTION = "Sans gestion";
    private static final String STATUTS = """
            SELECT s.PKey, s.Code, s.Description, s.Sorting, t.Description, s.ColorName, s.IsOkForPayement,
                   s.IsValidatedOnAccounting, s.IsReadOnly, s.[Delay], COALESCE(n.Pieces, 0)
            FROM BuyDocumentStatus s
            LEFT JOIN AdmWorkFlowStateType t ON t.PKey = s.AdmWorkFlowStateTypeId
            LEFT JOIN (SELECT BuyDocumentStatusId, COUNT(*) AS Pieces FROM BuyDocument GROUP BY BuyDocumentStatusId) n
                   ON n.BuyDocumentStatusId = s.PKey
            ORDER BY s.Sorting ASC, s.Code ASC""";
    // Passages comptés par identifiant de transition, quelle que soit la gestion actuelle de la pièce (R14).
    private static final String TRANSITIONS = """
            SELECT w.PKey, w.Sorting, w.Description, e.Code,
                   COALESCE(e.Description COLLATE DATABASE_DEFAULT, e.Code COLLATE DATABASE_DEFAULT),
                   w.StartStatusId, sd.Code, w.EndStatusId, sa.Code, w.NotesMandatory, w.IsValidatedByCfgTierTrigger,
                   COALESCE(h.Passages, 0), h.Dernier
            FROM BuyDocumentWorkFlow w
            LEFT JOIN StrEntity e ON e.PKey = w.StrEntityId
            LEFT JOIN BuyDocumentStatus sd ON sd.PKey = w.StartStatusId
            LEFT JOIN BuyDocumentStatus sa ON sa.PKey = w.EndStatusId
            LEFT JOIN (SELECT buyDocumentWorkFlowId AS Transition, COUNT(*) AS Passages, MAX(DocDate) AS Dernier
                       FROM BuyDocumentStatusHistory WHERE buyDocumentWorkFlowId IS NOT NULL
                       GROUP BY buyDocumentWorkFlowId) h ON h.Transition = w.PKey
            WHERE :utilisees = 0 OR h.Passages > 0
            ORDER BY CASE WHEN e.PKey IS NULL THEN 1 ELSE 0 END ASC,
                     COALESCE(e.Description COLLATE DATABASE_DEFAULT, e.Code COLLATE DATABASE_DEFAULT) ASC,
                     w.Sorting ASC, w.Description ASC""";
    private static final String ROLES = """
            SELECT l.WorkFlowId, r.PKey, r.Code FROM BuyDocumentWorkFlowAdmRole l JOIN AdmRole r ON r.PKey = l.AdmRoleId
            WHERE l.WorkFlowId IS NOT NULL ORDER BY r.Code ASC""";

    private final EntityManager em;

    public ConfigurationAchatService(EntityManager em) {
        this.em = em;
    }

    public List<StatutAchatConfigDto> statuts() {
        return lignes(em.createNativeQuery(STATUTS).getResultList()).stream()
                .map(r -> new StatutAchatConfigDto(uuid(r[0]), texte(r[1]), texte(r[2]), (Integer) r[3], texte(r[4]),
                        texte(r[5]), vrai(r[6]), vrai(r[7]), vrai(r[8]), (Integer) r[9], ((Number) r[10]).longValue()))
                .toList();
    }

    public List<TransitionAchatDto> transitions(boolean utilisees) {
        List<Object[]> transitions = lignes(em.createNativeQuery(TRANSITIONS)
                .setParameter("utilisees", utilisees ? 1 : 0).getResultList());
        Map<UUID, List<RoleTransitionDto>> roles = lignes(em.createNativeQuery(ROLES).getResultList()).stream()
                .collect(Collectors.groupingBy(r -> uuid(r[0]),
                        Collectors.mapping(r -> new RoleTransitionDto(uuid(r[1]), texte(r[2])), Collectors.toList())));
        return transitions.stream().map(r -> {
            UUID id = uuid(r[0]);
            String gestion = texte(r[4]);
            return new TransitionAchatDto(id, (Integer) r[1], texte(r[2]), texte(r[3]),
                    gestion == null ? SANS_GESTION : gestion, uuid(r[5]), texte(r[6]), uuid(r[7]), texte(r[8]), vrai(r[9]),
                    vrai(r[10]), roles.getOrDefault(id, List.of()), ((Number) r[11]).longValue(),
                    r[12] == null ? null : ((LocalDateTime) r[12]).toLocalDate());
        }).toList();
    }

    @SuppressWarnings("unchecked")
    private static List<Object[]> lignes(List<?> brut) {
        return (List<Object[]>) brut;
    }

    private static UUID uuid(Object v) {
        return v == null ? null : UUID.fromString(v.toString());
    }

    private static String texte(Object v) {
        return v == null ? null : v.toString();
    }

    private static boolean vrai(Object v) {
        return Boolean.TRUE.equals(v);
    }
}
