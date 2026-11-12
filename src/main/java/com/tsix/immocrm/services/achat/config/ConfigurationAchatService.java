package com.tsix.immocrm.services.achat.config;

import com.tsix.immocrm.dto.achat.config.CatalogueChampsStatutDto;
import com.tsix.immocrm.dto.achat.config.DocumentAJoindreAchatDto;
import com.tsix.immocrm.dto.achat.config.ImageStatutDto;
import com.tsix.immocrm.dto.achat.config.RoleTransitionDto;
import com.tsix.immocrm.dto.achat.config.StatutAchatConfigDto;
import com.tsix.immocrm.dto.achat.config.TransitionAchatDto;
import com.tsix.immocrm.dto.achat.config.TypeAchatDto;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/** Statuts et transitions des pièces d'achat (FEATURE-achat-regle-commune.md §4) : une requête par liste, plus les rôles. */
@Service
@Transactional(readOnly = true)
public class ConfigurationAchatService {

    private static final String SANS_GESTION = "Sans gestion";
    private static final String STATUTS = """
            SELECT s.PKey, s.Code, s.Description, s.Sorting, t.Description, s.ColorName, s.IsOkForPayement,
                   s.IsValidatedOnAccounting, s.IsReadOnly, s.[Delay], COALESCE(n.Pieces, 0), s.AdmWorkFlowStateTypeId,
                   s.CfgImageId, s.Notes, s.MandatoryFields, s.SupplierMandatoryFields, COALESCE(w.Transitions, 0),
                   COALESCE(h.Historiques, 0)
            FROM BuyDocumentStatus s
            LEFT JOIN AdmWorkFlowStateType t ON t.PKey = s.AdmWorkFlowStateTypeId
            LEFT JOIN (SELECT BuyDocumentStatusId, COUNT(*) AS Pieces FROM BuyDocument GROUP BY BuyDocumentStatusId) n
                   ON n.BuyDocumentStatusId = s.PKey
            LEFT JOIN (SELECT Statut, COUNT(*) AS Transitions
                       FROM (SELECT PKey, StartStatusId AS Statut FROM BuyDocumentWorkFlow
                             UNION SELECT PKey, EndStatusId FROM BuyDocumentWorkFlow) x
                       GROUP BY Statut) w ON w.Statut = s.PKey
            LEFT JOIN (SELECT BuyDocumentStatusId, COUNT(*) AS Historiques FROM BuyDocumentStatusHistory
                       GROUP BY BuyDocumentStatusId) h ON h.BuyDocumentStatusId = s.PKey
            ORDER BY s.Sorting ASC, s.Code ASC""";
    private static final String DOCUMENTS = """
            SELECT d.BuyDocumentStatusId, d.AdmAttachedFileTypeId, COALESCE(f.Description, f.Code), d.IsMandatory, d.WithTitle
            FROM BuyDocumentStatusDocumentToAttach d
            LEFT JOIN AdmAttachedFileType f ON f.PKey = d.AdmAttachedFileTypeId
            ORDER BY f.Description ASC""";
    private static final String TYPES = "SELECT PKey, Code, Description FROM AdmWorkFlowStateType ORDER BY Sorting ASC, Code ASC";
    private static final String TYPES_DOCUMENTS = "SELECT PKey, Code, Description FROM AdmAttachedFileType ORDER BY Description ASC";
    private static final String IMAGES = "SELECT PKey, Code, COALESCE(DATALENGTH(ImageData), 0) FROM CfgImage ORDER BY Code ASC";
    // Passages comptés par identifiant de transition, quelle que soit la gestion actuelle de la pièce (R14).
    private static final String TRANSITIONS = """
            SELECT w.PKey, w.Sorting, w.Description, e.Code,
                   COALESCE(e.Description COLLATE DATABASE_DEFAULT, e.Code COLLATE DATABASE_DEFAULT),
                   w.StartStatusId, sd.Code, w.EndStatusId, sa.Code, w.NotesMandatory, w.IsValidatedByCfgTierTrigger,
                   COALESCE(h.Passages, 0), h.Dernier, e.PKey
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
        Map<UUID, List<DocumentAJoindreAchatDto>> documents = lignes(em.createNativeQuery(DOCUMENTS).getResultList())
                .stream().collect(Collectors.groupingBy(r -> uuid(r[0]), Collectors.mapping(
                        r -> new DocumentAJoindreAchatDto(uuid(r[1]), texte(r[2]), vrai(r[3]), vrai(r[4])),
                        Collectors.toList())));
        return lignes(em.createNativeQuery(STATUTS).getResultList()).stream().map(r -> {
            UUID id = uuid(r[0]);
            return new StatutAchatConfigDto(id, texte(r[1]), texte(r[2]), (Integer) r[3], texte(r[4]), nonVide(r[5]),
                    vrai(r[6]), vrai(r[7]), vrai(r[8]), (Integer) r[9], nombre(r[10]), uuid(r[11]), uuid(r[12]),
                    nonVide(r[13]), CatalogueStatutAchat.noms(texte(r[14])), CatalogueStatutAchat.noms(texte(r[15])),
                    documents.getOrDefault(id, List.of()), nombre(r[16]), nombre(r[17]));
        }).toList();
    }

    /** Un statut tel que la liste le rend (mêmes comptages), pour les réponses d'écriture et le refus de suppression. */
    public Optional<StatutAchatConfigDto> statut(UUID id) {
        return statuts().stream().filter(s -> s.id().equals(id)).findFirst();
    }

    public List<TypeAchatDto> types() {
        return types(TYPES);
    }

    public List<TypeAchatDto> typesDocuments() {
        return types(TYPES_DOCUMENTS);
    }

    public CatalogueChampsStatutDto champsObligatoires() {
        return CatalogueStatutAchat.CHAMPS;
    }

    public List<ImageStatutDto> images() {
        return lignes(em.createNativeQuery(IMAGES).getResultList()).stream()
                .map(r -> new ImageStatutDto(uuid(r[0]), texte(r[1]), nombre(r[2]))).toList();
    }

    public Optional<byte[]> contenuImage(UUID id) {
        return em.createNativeQuery("SELECT ImageData FROM CfgImage WHERE PKey = :id").setParameter("id", id)
                .getResultList().stream().filter(byte[].class::isInstance).map(byte[].class::cast).findFirst();
    }

    private List<TypeAchatDto> types(String requete) {
        return lignes(em.createNativeQuery(requete).getResultList()).stream()
                .map(r -> new TypeAchatDto(uuid(r[0]), texte(r[1]), texte(r[2]))).toList();
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
                    r[12] == null ? null : ((LocalDateTime) r[12]).toLocalDate(), uuid(r[13]));
        }).toList();
    }

    public Optional<TransitionAchatDto> transition(UUID id) {
        return transitions(false).stream().filter(t -> t.id().equals(id)).findFirst();
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

    private static long nombre(Object v) {
        return ((Number) v).longValue();
    }

    private static String nonVide(Object v) {
        return v == null || v.toString().isBlank() ? null : v.toString();
    }
}
