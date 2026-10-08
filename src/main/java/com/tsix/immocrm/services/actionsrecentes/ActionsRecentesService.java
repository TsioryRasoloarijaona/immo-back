package com.tsix.immocrm.services.actionsrecentes;

import com.tsix.immocrm.domain.CibleActionRecente;
import com.tsix.immocrm.domain.PorteeActionsRecentes;
import com.tsix.immocrm.domain.StatutDossier;
import com.tsix.immocrm.domain.TypeActionRecente;
import com.tsix.immocrm.dto.actionsrecentes.ActionRecenteDto;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.com.ComActionMessage;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.com.ComProspection;
import com.tsix.immocrm.entities.str.StrHistory;
import com.tsix.immocrm.exceptions.InvalidRequestException;
import com.tsix.immocrm.security.AuthenticatedUser;
import com.tsix.immocrm.services.perimetre.PerimetreCommercialService;
import com.tsix.immocrm.services.prospection.DiscriminantPreProspection;
import com.tsix.immocrm.services.prospection.ProspectionService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Tuple;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Fetch;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

/**
 * Flux « Actions récentes » : historique des prospections, historique des statuts de dossier et
 * interactions, chacun filtré par le périmètre existant, puis fusionnés (FEATURE-actions-recentes.md).
 */
@Service
@Transactional(readOnly = true)
public class ActionsRecentesService {

    public static final int LIMITE_MAX = 100;
    private static final int LIBELLE_INTERACTION_MAX = 120;

    /**
     * Ordre total du flux, le même en SQL (par source) et en Java (fusion) : date décroissante, puis
     * id en texte majuscule décroissant. Jamais {@code UUID.compareTo} : SQL Server trie un
     * {@code uniqueidentifier} autrement (voir FEATURE-actions-recentes.md §10.2).
     */
    static final Comparator<ActionRecenteDto> ORDRE = Comparator
            .comparing(ActionRecenteDto::date, Comparator.reverseOrder())
            .thenComparing(a -> cleId(a.id()), Comparator.reverseOrder());

    private final EntityManager em;
    private final PerimetreCommercialService perimetreService;

    public ActionsRecentesService(EntityManager em, PerimetreCommercialService perimetreService) {
        this.em = em;
        this.perimetreService = perimetreService;
    }

    static String cleId(UUID id) {
        return id.toString().toUpperCase(Locale.ROOT);
    }

    /** Filtres communs aux trois sources ; {@code auteur} nul = pas de filtre d'auteur (EQUIPE). */
    private record Filtre(UUID auteur, LocalDateTime avant, UUID avantId, int limite) {
    }

    public List<ActionRecenteDto> lister(PorteeActionsRecentes portee, int limite, LocalDateTime avant, UUID avantId) {
        if (limite < 1 || limite > LIMITE_MAX) {
            throw new InvalidRequestException("limite doit être comprise entre 1 et " + LIMITE_MAX + " : " + limite);
        }
        if (avantId != null && avant == null) {
            throw new InvalidRequestException("avantId exige avant");
        }
        UUID auteur = null;
        if (portee != PorteeActionsRecentes.EQUIPE) {
            auteur = admUserIdCourant();
            if (auteur == null) {
                return List.of();
            }
        }
        Filtre filtre = new Filtre(auteur, avant, avantId, limite);

        List<ActionRecenteDto> toutes = new ArrayList<>();
        toutes.addAll(historiqueProspections(filtre));
        toutes.addAll(historiqueDossiers(filtre));
        toutes.addAll(interactions(filtre));
        return toutes.stream().sorted(ORDRE).limit(limite).toList();
    }

    // ---------------------------------------------------------------- sources

    private List<ActionRecenteDto> historiqueProspections(Filtre filtre) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Tuple> cq = cb.createTupleQuery();
        Root<StrHistory> h = cq.from(StrHistory.class);
        Root<ComProspection> p = cq.from(ComProspection.class);

        List<Predicate> predicats = predicatsHistorique(cb, h, filtre);
        predicats.add(cb.equal(p.get("id"), h.get("objectId")));
        predicats.add(cb.isNull(h.get("strConfigEntityId")));
        ajouter(predicats, perimetreService.filtreProspections(), p, cq, cb);

        cq.multiselect(h, p).where(predicats.toArray(Predicate[]::new)).orderBy(ordre(cb, h.get("docDate"), h.get("id")));
        return em.createQuery(cq).setMaxResults(filtre.limite()).getResultList().stream()
                .map(t -> depuisHistoriqueProspection(t.get(0, StrHistory.class), t.get(1, ComProspection.class)))
                .toList();
    }

    private List<ActionRecenteDto> historiqueDossiers(Filtre filtre) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Tuple> cq = cb.createTupleQuery();
        Root<StrHistory> h = cq.from(StrHistory.class);
        Root<ComFolder> f = cq.from(ComFolder.class);

        List<Predicate> predicats = predicatsHistorique(cb, h, filtre);
        predicats.add(cb.equal(f.get("id"), h.get("objectId")));
        predicats.add(cb.equal(h.get("strConfigEntityId"), StatutDossier.CONFIG_ENTITY_ID));
        ajouter(predicats, perimetreService.filtreDossiers(), f, cq, cb);

        cq.multiselect(h, f).where(predicats.toArray(Predicate[]::new)).orderBy(ordre(cb, h.get("docDate"), h.get("id")));
        return em.createQuery(cq).setMaxResults(filtre.limite()).getResultList().stream()
                .map(t -> depuisHistoriqueDossier(t.get(0, StrHistory.class), t.get(1, ComFolder.class)))
                .toList();
    }

    private List<ActionRecenteDto> interactions(Filtre filtre) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<ComActionMessage> cq = cb.createQuery(ComActionMessage.class);
        Root<ComActionMessage> m = cq.from(ComActionMessage.class);
        m.fetch("admUser", JoinType.LEFT);
        m.fetch("comActionMessageType", JoinType.LEFT);
        m.fetch("comFolder", JoinType.LEFT);
        Fetch<ComActionMessage, ComProspection> prospection = m.fetch("comProspection", JoinType.LEFT);
        prospection.fetch("cfgProspect", JoinType.LEFT);

        Expression<LocalDateTime> date = cb.coalesce(m.get("userCreateDateTime"), m.<LocalDateTime>get("docDate"));
        List<Predicate> predicats = new ArrayList<>();
        predicats.add(cb.isNotNull(date));
        // Sans fiche, rien à ouvrir : ignorée comme une ligne orpheline (R8).
        predicats.add(cb.or(cb.isNotNull(m.get("comProspection")), cb.isNotNull(m.get("comFolder"))));
        if (filtre.auteur() != null) {
            predicats.add(cb.equal(m.get("admUser").get("id"), filtre.auteur()));
        }
        ajouterCurseur(predicats, cb, date, m.get("id"), filtre);
        ajouter(predicats, perimetreService.filtreInteractions(), m, cq, cb);

        cq.select(m).where(predicats.toArray(Predicate[]::new)).orderBy(ordre(cb, date, m.get("id")));
        return em.createQuery(cq).setMaxResults(filtre.limite()).getResultList().stream()
                .map(ActionsRecentesService::depuisInteraction)
                .filter(Objects::nonNull)
                .toList();
    }

    private static List<Predicate> predicatsHistorique(CriteriaBuilder cb, Root<StrHistory> h, Filtre filtre) {
        List<Predicate> predicats = new ArrayList<>();
        predicats.add(cb.isNotNull(h.get("docDate")));
        if (filtre.auteur() != null) {
            predicats.add(cb.equal(h.get("admUserId"), filtre.auteur()));
        }
        ajouterCurseur(predicats, cb, h.get("docDate"), h.get("id"), filtre);
        return predicats;
    }

    private static List<Order> ordre(CriteriaBuilder cb, Expression<LocalDateTime> date, Path<UUID> id) {
        return List.of(cb.desc(date), cb.desc(idTexte(id)));
    }

    // cast() émet un vrai CAST SQL ; as() ne fait que retyper et laisse trier le uniqueidentifier.
    private static Expression<String> idTexte(Path<UUID> id) {
        return id.cast(String.class);
    }

    /**
     * Page suivante : {@code date < avant}, ou même date et id plus petit (ordre de {@link #ORDRE}).
     * Une colonne datetime (pas de 1/300 s) ne vaut jamais le paramètre datetime2 relu arrondi à la
     * ms : « même date » = à ±1 ms, moins que l'écart entre deux pas (FEATURE-actions-recentes.md §10.2).
     */
    private static void ajouterCurseur(List<Predicate> predicats, CriteriaBuilder cb,
                                       Expression<LocalDateTime> date, Path<UUID> id, Filtre filtre) {
        if (filtre.avant() == null) {
            return;
        }
        LocalDateTime debutPas = filtre.avant().minus(1, ChronoUnit.MILLIS);
        Predicate anterieure = cb.lessThan(date, debutPas);
        if (filtre.avantId() == null) {
            predicats.add(anterieure);
            return;
        }
        Predicate memePas = cb.and(
                cb.between(date, debutPas, filtre.avant().plus(1, ChronoUnit.MILLIS)),
                cb.lessThan(idTexte(id), cleId(filtre.avantId())));
        predicats.add(cb.or(anterieure, memePas));
    }

    private static <T> void ajouter(List<Predicate> predicats, Specification<T> spec, Root<T> root,
                                    CriteriaQuery<?> cq, CriteriaBuilder cb) {
        Predicate predicat = spec.toPredicate(root, cq, cb);
        if (predicat != null) {
            predicats.add(predicat);
        }
    }

    // ---------------------------------------------------------------- correspondances

    private static ActionRecenteDto depuisHistoriqueProspection(StrHistory h, ComProspection p) {
        return new ActionRecenteDto(
                h.getId(),
                TypeActionRecente.depuisHistoriqueProspection(h.getDescription()),
                h.getDocDate(),
                h.getDescription(),
                h.getAdmUserId(),
                videEnNull(h.getAdmUserDescription()),
                cibleProspection(p),
                p.getId(),
                libelleProspection(p),
                p.getCfgProspect() != null ? p.getCfgProspect().getId() : null);
    }

    private static ActionRecenteDto depuisHistoriqueDossier(StrHistory h, ComFolder f) {
        return new ActionRecenteDto(
                h.getId(),
                TypeActionRecente.DOSSIER_STATUT,
                h.getDocDate(),
                StringUtils.hasText(h.getDescription()) ? h.getDescription() : h.getStrStatusDescription(),
                h.getAdmUserId(),
                videEnNull(h.getAdmUserDescription()),
                CibleActionRecente.DOSSIER,
                f.getId(),
                f.getCode(),
                null);
    }

    /**
     * Rattachée aux deux : la prospection l'emporte, dans l'ordre de {@code filtreInteractions()} —
     * sinon un dossier hors périmètre serait exposé (FEATURE-actions-recentes.md §10.1).
     */
    static ActionRecenteDto depuisInteraction(ComActionMessage m) {
        AdmUser auteur = m.getAdmUser();
        ComProspection p = m.getComProspection();
        ComFolder dossier = p == null ? m.getComFolder() : null;
        // Le filtre teste la colonne de lien, pas la ligne : une fiche supprimée laisse les deux à null (R8).
        if (p == null && dossier == null) {
            return null;
        }
        LocalDateTime date = m.getUserCreateDateTime() != null ? m.getUserCreateDateTime() : m.getDocDate();
        return new ActionRecenteDto(
                m.getId(),
                TypeActionRecente.INTERACTION,
                date,
                libelleInteraction(m),
                auteur != null ? auteur.getId() : null,
                auteur != null ? nomUtilisateur(auteur) : null,
                dossier != null ? CibleActionRecente.DOSSIER : cibleProspection(p),
                dossier != null ? dossier.getId() : p.getId(),
                dossier != null ? dossier.getCode() : libelleProspection(p),
                dossier == null && p.getCfgProspect() != null ? p.getCfgProspect().getId() : null);
    }

    private static CibleActionRecente cibleProspection(ComProspection p) {
        return DiscriminantPreProspection.estPreProspection(p)
                ? CibleActionRecente.PRE_PROSPECTION : CibleActionRecente.PROSPECTION;
    }

    private static String libelleProspection(ComProspection p) {
        String nom = ProspectionService.nomProspect(p.getCfgProspect(), p);
        return nom != null ? nom : p.getCode();
    }

    private static String libelleInteraction(ComActionMessage m) {
        String type = m.getComActionMessageType() != null ? videEnNull(m.getComActionMessageType().getDescription()) : null;
        String message = StringUtils.hasText(m.getAlertMessage())
                ? m.getAlertMessage().strip().replaceAll("\\s+", " ") : null;
        String libelle;
        if (type != null && message != null) {
            libelle = type + " — " + message;
        } else if (type != null) {
            libelle = type;
        } else {
            libelle = message != null ? message : "Interaction";
        }
        return libelle.length() <= LIBELLE_INTERACTION_MAX
                ? libelle : libelle.substring(0, LIBELLE_INTERACTION_MAX - 1) + "…";
    }

    private static String nomUtilisateur(AdmUser u) {
        return StringUtils.hasText(u.getDescription()) ? u.getDescription().trim() : u.getCode();
    }

    private static String videEnNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    private static UUID admUserIdCourant() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof AuthenticatedUser user) {
            return user.admUserId();
        }
        return null;
    }
}
