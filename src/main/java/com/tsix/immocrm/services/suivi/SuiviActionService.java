package com.tsix.immocrm.services.suivi;

import com.tsix.immocrm.domain.AgregatDemande;
import com.tsix.immocrm.domain.ColonneSuiviAction;
import com.tsix.immocrm.domain.Granularite;
import com.tsix.immocrm.domain.TriGroupe;
import com.tsix.immocrm.dto.suivi.SuiviActionDto;
import com.tsix.immocrm.entities.com.ComActionMessage;
import com.tsix.immocrm.repositories.com.ComActionMessageRepository;
import com.tsix.immocrm.security.AuthenticatedUser;
import com.tsix.immocrm.security.UtilisateurAffiche;
import com.tsix.immocrm.services.groupement.GroupementService;
import com.tsix.immocrm.services.groupement.GroupementService.ResultatGroupement;
import com.tsix.immocrm.services.perimetre.PerimetreCommercialService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Tuple;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Écran OMRAN « Suivi des actions », sans sa vue {@code ComActionMessageView}, trop lente
 * ({@code FEATURE-suivi-actions.md} §1) : page d'ids, une projection, colonnes calculées à la demande.
 * Hors {@code PerimetreInterceptor} : {@code filtreInteractions()} appliqué ici, liste <b>et</b> groupes.
 */
@Service
@Transactional(readOnly = true)
public class SuiviActionService {

    private final ComActionMessageRepository messageRepository;
    private final PerimetreCommercialService perimetreService;
    private final GroupementService groupementService;
    private final UtilisateurAffiche utilisateurAffiche;
    private final EntityManager em;

    /** Seul ce rôle voit le suivi <b>général</b>. */
    static final String ROLE_ADMINISTRATEUR = "ROLE_Administrateur";

    /** Actions générées pendant une session : OMRAN les affiche sous « Système », pas sous l'utilisateur. */
    static final String UTILISATEUR_SYSTEME = "Système";

    private static final String USER_NAME = "userName";
    private static final String IS_SYSTEM = "isSystem";
    private static final String PROSPECTION_ID = "prospectionId";
    private static final String DOSSIER_ID = "dossierId";

    public SuiviActionService(ComActionMessageRepository messageRepository,
                              PerimetreCommercialService perimetreService,
                              GroupementService groupementService,
                              UtilisateurAffiche utilisateurAffiche, EntityManager em) {
        this.messageRepository = messageRepository;
        this.perimetreService = perimetreService;
        this.groupementService = groupementService;
        this.utilisateurAffiche = utilisateurAffiche;
        this.em = em;
    }

    public record Filtres(LocalDateTime dateDebut, LocalDateTime dateFin, List<UUID> trancheIds,
                          List<String> utilisateurs, List<UUID> typeIds, List<UUID> categorieIds,
                          Boolean systeme) {
    }

    public Page<SuiviActionDto> findAll(Filtres filtres, ColonneSuiviAction groupeColonne,
                                        String groupeValeur, Granularite granularite,
                                        Set<ColonneSuiviAction> colonnes, Pageable pageable) {
        Specification<ComActionMessage> spec = spec(filtres).and(portee())
                .and(perimetreService.filtreInteractions());
        if (groupeColonne != null) {
            spec = spec.and((root, query, cb) -> GroupementService.predicatDeGroupe(
                    root, cb, groupeColonne.chemin(), granularite, groupeValeur));
        }
        return servir(messageRepository.findAll(spec, pageable), colonnes);
    }

    /**
     * Panneau « Actions » d'une fiche article (FEATURE-patrimoine-audit-actions.md §3) : toutes les actions de
     * l'article ({@code StkItemId}), visibilité par entité appliquée, sans la portée « mes actions » du suivi.
     */
    public Page<SuiviActionDto> parArticle(UUID articleId, Pageable pageable) {
        Specification<ComActionMessage> deLArticle = (root, query, cb) ->
                cb.equal(root.get("stkItem").get("id"), articleId);
        return servir(messageRepository.findAll(deLArticle.and(perimetreService.filtreInteractions()), pageable),
                ColonneSuiviAction.resoudre(null));
    }

    /** Une action, projetée comme une ligne du suivi (colonnes par défaut). */
    public SuiviActionDto parId(UUID id) {
        return servir(new PageImpl<>(List.of(messageRepository.getReferenceById(id))),
                ColonneSuiviAction.resoudre(null)).getContent().getFirst();
    }

    private Page<SuiviActionDto> servir(Page<ComActionMessage> page, Set<ColonneSuiviAction> colonnes) {
        if (page.isEmpty()) {
            return page.map(m -> null);
        }
        List<UUID> ids = page.getContent().stream().map(ComActionMessage::getId).toList();
        Map<UUID, Tuple> lignes = projeter(ids);
        Map<UUID, Integer> rangs = colonnes.contains(ColonneSuiviAction.RANG) ? rangs(ids) : Map.of();
        // null = colonne non demandée : le champ reste nul plutôt que de dire « inactive » à tort.
        Map<UUID, Boolean> alertes = colonnes.contains(ColonneSuiviAction.ALERTE_ACTIVE) ? alertes(ids) : null;
        Map<UUID, String> etatsParProspection = colonnes.contains(ColonneSuiviAction.ETAT)
                ? etatsDesDossiersNesDeProspection(lignes.values()) : Map.of();
        return page.map(m -> versDto(lignes.get(m.getId()), rangs, alertes, etatsParProspection));
    }

    /** Mêmes filtres, même portée, même périmètre que {@link #findAll}. */
    public ResultatGroupement grouper(Filtres filtres, ColonneSuiviAction groupeBy, Granularite granularite,
                                      List<AgregatDemande> agregats, TriGroupe tri,
                                      boolean inclureSansValeur, Pageable pageable) {
        Specification<ComActionMessage> spec = spec(filtres).and(portee())
                .and(perimetreService.filtreInteractions());
        return groupementService.grouper(ComActionMessage.class, spec, groupeBy.chemin(), granularite,
                agregats, tri, inclureSansValeur, pageable);
    }

    /**
     * Admin : tout ; sinon <b>ses</b> actions seulement, le filtre {@code utilisateur} s'y ajoute en ET
     * (§3bis) — {@code UserName} = mon nom, ou mon compte hors « Système ». Sans identité ⇒ rien,
     * jamais tout.
     */
    private Specification<ComActionMessage> portee() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean administrateur = auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> ROLE_ADMINISTRATEUR.equals(a.getAuthority()));
        if (administrateur) {
            return (root, query, cb) -> cb.conjunction();
        }
        if (auth == null || !(auth.getPrincipal() instanceof AuthenticatedUser moi)) {
            return (root, query, cb) -> cb.disjunction();
        }
        String monNom = utilisateurAffiche.nom(moi);
        UUID monCompte = moi.admUserId();
        return (root, query, cb) -> {
            List<Predicate> miennes = new ArrayList<>();
            if (StringUtils.hasText(monNom)) {
                miennes.add(cb.equal(root.get(USER_NAME), monNom));
            }
            if (monCompte != null) {
                miennes.add(cb.and(
                        cb.equal(root.get("admUser").get("id"), monCompte),
                        cb.or(cb.isNull(root.get(USER_NAME)),
                                cb.notEqual(root.get(USER_NAME), UTILISATEUR_SYSTEME))));
            }
            return miennes.isEmpty() ? cb.disjunction() : cb.or(miennes.toArray(new Predicate[0]));
        };
    }

    /** Comme OMRAN : fin exclue, {@code UserName} en égalité exacte ; type et catégorie en SQL plutôt qu'en grille. */
    private static Specification<ComActionMessage> spec(Filtres f) {
        return (root, query, cb) -> {
            List<Predicate> p = new ArrayList<>();
            if (f.dateDebut() != null) {
                p.add(cb.greaterThanOrEqualTo(root.get("docDate"), f.dateDebut()));
            }
            if (f.dateFin() != null) {
                p.add(cb.lessThan(root.get("docDate"), f.dateFin()));
            }
            if (renseignee(f.trancheIds())) {
                p.add(root.join("trancheResolue", JoinType.LEFT).get("id").in(f.trancheIds()));
            }
            if (renseignee(f.utilisateurs())) {
                p.add(root.get(USER_NAME).in(f.utilisateurs()));
            }
            if (renseignee(f.typeIds())) {
                p.add(root.get("comActionMessageType").get("id").in(f.typeIds()));
            }
            if (renseignee(f.categorieIds())) {
                p.add(root.get("comActionMessageCategory").get("id").in(f.categorieIds()));
            }
            if (f.systeme() != null) {
                p.add(predicatSysteme(Boolean.TRUE.equals(f.systeme()), root, cb));
            }
            return cb.and(p.toArray(new Predicate[0]));
        };
    }

    private static boolean renseignee(Collection<?> valeurs) {
        return valeurs != null && !valeurs.isEmpty();
    }

    /** Non système = {@code IsSystem} à 0 ou NULL. */
    private static Predicate predicatSysteme(boolean systeme, Root<ComActionMessage> root, CriteriaBuilder cb) {
        return systeme
                ? cb.isTrue(root.get(IS_SYSTEM))
                : cb.or(cb.isFalse(root.get(IS_SYSTEM)), cb.isNull(root.get(IS_SYSTEM)));
    }

    /** Une requête pour toute la page, jamais de chargement paresseux association par association. */
    private Map<UUID, Tuple> projeter(List<UUID> ids) {
        List<Tuple> tuples = em.createQuery("""
                SELECT m.id AS id, m.userName AS utilisateur, m.docDate AS dateAction,
                       m.alertValidityDate AS dateAlerte, m.alertMessage AS message,
                       m.isUnpaid AS impaye, m.isSystem AS systeme, m.isIn AS entrante,
                       m.userCreate AS creePar, m.userCreateDateTime AS creeLe,
                       m.userUpdate AS modifiePar, m.userUpdateDateTime AS modifieLe,
                       t.id AS typeId, t.description AS typeAction,
                       c.id AS categorieId, c.description AS categorie,
                       sc.description AS resultat,
                       ce.description AS commercial,
                       au.description AS compte, aue.description AS employeDuCompte,
                       ti.id AS tiersId, ti.description AS tiersNom,
                       pp.description AS prospectNom, p.tempDescription AS prospectionNomSaisi,
                       p.id AS prospectionId, p.code AS prospectionCode,
                       f.id AS dossierId, f.code AS dossierCode, fs.description AS statutDossier,
                       i.code AS articleCode, i.description AS articleLibelle,
                       tr.id AS trancheId, tr.code AS trancheCode, tr.description AS trancheLibelle,
                       pr.description AS programme, so.description AS entite
                FROM ComActionMessage m
                LEFT JOIN m.comActionMessageType t
                LEFT JOIN m.comActionMessageCategory c
                LEFT JOIN m.comActionMessageSubCategory sc
                LEFT JOIN m.cfgEmployee ce
                LEFT JOIN m.admUser au LEFT JOIN au.cfgEmployee aue
                LEFT JOIN m.cfgTier ti
                LEFT JOIN m.comProspection p LEFT JOIN p.cfgProspect pp
                LEFT JOIN m.comFolder f LEFT JOIN f.strStatus fs
                LEFT JOIN m.stkItem i
                LEFT JOIN m.trancheResolue tr LEFT JOIN tr.cfgProject pr LEFT JOIN pr.cfgCompany so
                WHERE m.id IN :ids""", Tuple.class)
                .setParameter("ids", ids)
                .getResultList();
        return tuples.stream().collect(Collectors.toMap(t -> t.get("id", UUID.class), Function.identity()));
    }

    /**
     * Rang de la vue : 1 + actions plus récentes, même tiers et même type, hors système et impayées.
     * Sans tiers ou sans type ⇒ rang 1, l'égalité avec NULL ne comptant rien, comme dans la vue.
     */
    private Map<UUID, Integer> rangs(List<UUID> ids) {
        List<?> lignes = em.createNativeQuery("""
                SELECT m.PKey, (SELECT COUNT(*) + 1 FROM ComActionMessage v
                                WHERE v.PKey <> m.PKey AND v.CfgTierId = m.CfgTierId
                                  AND v.ComActionMessageTypeId = m.ComActionMessageTypeId
                                  AND v.DocDate > m.DocDate AND v.IsSystem = 0 AND v.IsUnpaid = 0)
                FROM ComActionMessage m WHERE m.PKey IN (:ids)""")
                .setParameter("ids", ids)
                .getResultList();
        Map<UUID, Integer> rangs = new HashMap<>();
        for (Object ligne : lignes) {
            Object[] l = (Object[]) ligne;
            rangs.put(uuid(l[0]), ((Number) l[1]).intValue());
        }
        return rangs;
    }

    /** {@code AdmAlertData.Active} par action — absente = inactive, comme l'{@code ISNULL(…, 0)} de la vue. */
    private Map<UUID, Boolean> alertes(List<UUID> ids) {
        List<?> lignes = em.createNativeQuery("""
                SELECT RelatedKey, MAX(CAST(Active AS int)) FROM AdmAlertData
                WHERE RelatedKey IN (:ids) GROUP BY RelatedKey""")
                .setParameter("ids", ids)
                .getResultList();
        Map<UUID, Boolean> actives = new HashMap<>();
        for (Object ligne : lignes) {
            Object[] l = (Object[]) ligne;
            actives.put(uuid(l[0]), l[1] != null && ((Number) l[1]).intValue() == 1);
        }
        return actives;
    }

    /** Statut du dossier né de la prospection ({@code ComFolder.ComProspectionId}) : la jointure {@code Pfolder} de la vue. */
    private Map<UUID, String> etatsDesDossiersNesDeProspection(Collection<Tuple> lignes) {
        List<UUID> prospectionIds = lignes.stream()
                .filter(t -> t.get(DOSSIER_ID) == null && t.get(PROSPECTION_ID) != null)
                .map(t -> t.get(PROSPECTION_ID, UUID.class))
                .distinct()
                .toList();
        if (prospectionIds.isEmpty()) {
            return Map.of();
        }
        Map<UUID, String> etats = new HashMap<>();
        em.createQuery("""
                SELECT f.comProspection.id, s.description FROM ComFolder f LEFT JOIN f.strStatus s
                WHERE f.comProspection.id IN :ids""", Tuple.class)
                .setParameter("ids", prospectionIds)
                .getResultList()
                .forEach(t -> etats.putIfAbsent(t.get(0, UUID.class), t.get(1, String.class)));
        return etats;
    }

    private static SuiviActionDto versDto(Tuple t, Map<UUID, Integer> rangs, Map<UUID, Boolean> alertes,
                                          Map<UUID, String> etatsParProspection) {
        UUID id = t.get("id", UUID.class);
        return new SuiviActionDto(
                id,
                t.get("utilisateur", String.class),
                t.get("typeAction", String.class),
                rangs.get(id),
                t.get("dateAction", LocalDateTime.class),
                t.get("dateAlerte", LocalDateTime.class),
                alertes != null ? alertes.getOrDefault(id, false) : null,
                t.get("categorie", String.class),
                t.get("resultat", String.class),
                t.get("message", String.class),
                t.get("impaye", Boolean.class),
                t.get("systeme", Boolean.class),
                t.get("entrante", Boolean.class),
                t.get("commercial", String.class),
                personnel(t),
                tiersNom(t),
                t.get("prospectionCode", String.class),
                t.get("dossierCode", String.class),
                etat(t, etatsParProspection),
                t.get("articleCode", String.class),
                t.get("articleLibelle", String.class),
                t.get("trancheCode", String.class),
                t.get("trancheLibelle", String.class),
                t.get("programme", String.class),
                t.get("entite", String.class),
                t.get("typeId", UUID.class),
                t.get("categorieId", UUID.class),
                t.get("tiersId", UUID.class),
                t.get(PROSPECTION_ID, UUID.class),
                t.get(DOSSIER_ID, UUID.class),
                t.get("trancheId", UUID.class),
                t.get("creePar", String.class),
                t.get("creeLe", LocalDateTime.class),
                t.get("modifiePar", String.class),
                t.get("modifieLe", LocalDateTime.class));
    }

    /**
     * Tiers, sinon prospect, sinon nom saisi ({@code FEATURE-nom-pre-prospection-relances.md} §2). Rejoue
     * exactement la clé de groupe de {@link ColonneSuiviAction#TIERS_NOM} : ligne « X » ⇒ groupe « X ».
     */
    private static String tiersNom(Tuple t) {
        for (String alias : List.of("tiersNom", "prospectNom", "prospectionNomSaisi")) {
            String v = t.get(alias, String.class);
            if (StringUtils.hasText(v)) {
                return v.trim();
            }
        }
        return null;
    }

    /** {@code ISNULL(employé du compte, ISNULL(compte, ISNULL(commercial, '')))} — la vue. */
    private static String personnel(Tuple t) {
        for (String alias : List.of("employeDuCompte", "compte", "commercial")) {
            String v = t.get(alias, String.class);
            if (v != null) {
                return v;
            }
        }
        return null;
    }

    /**
     * {@code FolderStatusDescription} de la vue : dossier → son statut ; prospection → statut du
     * dossier né d'elle, sinon la catégorie ; sinon la catégorie.
     */
    private static String etat(Tuple t, Map<UUID, String> etatsParProspection) {
        if (t.get(DOSSIER_ID) != null) {
            return t.get("statutDossier", String.class);
        }
        UUID prospectionId = t.get(PROSPECTION_ID, UUID.class);
        if (prospectionId != null && etatsParProspection.containsKey(prospectionId)) {
            return etatsParProspection.get(prospectionId);
        }
        String categorie = t.get("categorie", String.class);
        return StringUtils.hasText(categorie) ? categorie : null;
    }

    /** Le pilote mssql-jdbc livre un {@code uniqueidentifier} natif en {@code String}. */
    private static UUID uuid(Object brut) {
        return brut instanceof UUID u ? u : UUID.fromString(brut.toString());
    }
}
