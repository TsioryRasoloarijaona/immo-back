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
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
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
 * {@code GET /api/suivi-actions[/groupes]} — l'écran OMRAN « Suivi des actions ». Voir
 * {@code instructions/FEATURE-suivi-actions.md}.
 *
 * <h2>Pourquoi pas la vue OMRAN</h2>
 *
 * <p>L'écran lit {@code ComActionMessageView}. Mesurée : 6,6 s la page de 50, 9,3 s le comptage —
 * son {@code DISTINCT} et son rang corrélé sont évalués sur toutes les lignes. Les mêmes jointures
 * écrites sur {@code ComActionMessage} : 23 ms le comptage, 139 ms la page. D'où ce découpage :
 * <ol>
 *   <li>la page d'identifiants par {@code Specification} (filtres + périmètre) ;</li>
 *   <li><b>une</b> requête de projection pour les colonnes de ces lignes — jamais de chargement
 *       paresseux association par association ;</li>
 *   <li>pour les colonnes calculées de la vue (rang, alerte active, état), une requête groupée
 *       sur les identifiants de la page, et seulement si la colonne est demandée.</li>
 * </ol>
 *
 * <h2>Périmètre</h2>
 *
 * <p>{@code /api/suivi-actions} n'est pas une collection de {@code PerimetreInterceptor} : le
 * service applique lui-même {@link PerimetreCommercialService#filtreInteractions()} — le prédicat
 * de la hotline et des relances — à la liste <b>et</b> aux groupes, pour que la somme des effectifs
 * retombe sur le total de la liste.
 */
@Service
@Transactional(readOnly = true)
public class SuiviActionService {

    private final ComActionMessageRepository messageRepository;
    private final PerimetreCommercialService perimetreService;
    private final GroupementService groupementService;
    private final UtilisateurAffiche utilisateurAffiche;
    private final EntityManager em;

    /** Rôle qui voit le suivi <b>général</b> — celui qui garde déjà {@code GET /api/kpi/commerciaux}. */
    static final String ROLE_ADMINISTRATEUR = "ROLE_Administrateur";

    /**
     * {@code UserName} des actions générées. 1 083 actions liées au compte de Hamza ATCHANY — et
     * d'autres pour 11 comptes — portent cette valeur : générées pendant sa session, OMRAN les
     * affiche sous « Système », pas sous son nom.
     */
    static final String UTILISATEUR_SYSTEME = "Système";

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

    /** Les filtres de l'écran. Tous optionnels, combinés en ET. */
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
        Page<ComActionMessage> page = messageRepository.findAll(spec, pageable);
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

    /** Replie la liste en groupes — mêmes filtres, même périmètre. */
    public ResultatGroupement grouper(Filtres filtres, ColonneSuiviAction groupeBy, Granularite granularite,
                                      List<AgregatDemande> agregats, TriGroupe tri,
                                      boolean inclureSansValeur, Pageable pageable) {
        Specification<ComActionMessage> spec = spec(filtres).and(portee())
                .and(perimetreService.filtreInteractions());
        return groupementService.grouper(ComActionMessage.class, spec, groupeBy.chemin(), granularite,
                agregats, tri, inclureSansValeur, pageable);
    }

    /**
     * <b>Qui voit quoi.</b> L'administrateur voit le suivi général ; tout autre utilisateur ne voit
     * que <b>ses</b> actions — le filtre {@code utilisateur} n'élargit rien, il se combine en ET.
     * Voir {@code instructions/FEATURE-suivi-actions.md} §3bis.
     *
     * <p>« Mes actions » = {@code UserName} égal à mon nom (la règle de l'écran OMRAN, et le
     * libellé que {@link UtilisateurAffiche} écrit même quand le compte manque en base
     * d'écriture), <b>ou</b> mon compte ({@code AdmUserId}) hors lignes « Système ». Le compte
     * rattrape les 36 actions réelles écrites sous l'identifiant de connexion
     * ({@code c.aittagadirt}) au lieu du nom ; l'exclusion de « Système » écarte les actions
     * générées pendant la session, qu'OMRAN n'attribue pas à l'utilisateur non plus.
     *
     * <p>Un appelant non administrateur sans identité exploitable (hors jeton applicatif) ne voit
     * rien — jamais tout.
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
                miennes.add(cb.equal(root.get("userName"), monNom));
            }
            if (monCompte != null) {
                miennes.add(cb.and(
                        cb.equal(root.get("admUser").get("id"), monCompte),
                        cb.or(cb.isNull(root.get("userName")),
                                cb.notEqual(root.get("userName"), UTILISATEUR_SYSTEME))));
            }
            return miennes.isEmpty() ? cb.disjunction() : cb.or(miennes.toArray(new Predicate[0]));
        };
    }

    /**
     * Les filtres de l'écran OMRAN. Période <b>fin exclue</b> ({@code DocDate < dateFin}), comme sa
     * requête ; utilisateur en égalité exacte sur {@code UserName}. Type et catégorie sont en SQL
     * ici, là où OMRAN les applique dans sa grille — même résultat, sans rapatrier les lignes.
     */
    private static Specification<ComActionMessage> spec(Filtres f) {
        return (root, query, cb) -> {
            List<Predicate> p = new ArrayList<>();
            if (f.dateDebut() != null) {
                p.add(cb.greaterThanOrEqualTo(root.get("docDate"), f.dateDebut()));
            }
            if (f.dateFin() != null) {
                p.add(cb.lessThan(root.get("docDate"), f.dateFin()));
            }
            if (f.trancheIds() != null && !f.trancheIds().isEmpty()) {
                p.add(root.join("trancheResolue", JoinType.LEFT).get("id").in(f.trancheIds()));
            }
            if (f.utilisateurs() != null && !f.utilisateurs().isEmpty()) {
                p.add(root.get("userName").in(f.utilisateurs()));
            }
            if (f.typeIds() != null && !f.typeIds().isEmpty()) {
                p.add(root.get("comActionMessageType").get("id").in(f.typeIds()));
            }
            if (f.categorieIds() != null && !f.categorieIds().isEmpty()) {
                p.add(root.get("comActionMessageCategory").get("id").in(f.categorieIds()));
            }
            if (f.systeme() != null) {
                p.add(f.systeme()
                        ? cb.isTrue(root.get("isSystem"))
                        : cb.or(cb.isFalse(root.get("isSystem")), cb.isNull(root.get("isSystem"))));
            }
            return cb.and(p.toArray(new Predicate[0]));
        };
    }

    /** Toutes les colonnes de la page en une requête — une jointure LEFT par colonne affichée. */
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
                LEFT JOIN m.comProspection p
                LEFT JOIN m.comFolder f LEFT JOIN f.strStatus fs
                LEFT JOIN m.stkItem i
                LEFT JOIN m.trancheResolue tr LEFT JOIN tr.cfgProject pr LEFT JOIN pr.cfgCompany so
                WHERE m.id IN :ids""", Tuple.class)
                .setParameter("ids", ids)
                .getResultList();
        return tuples.stream().collect(Collectors.toMap(t -> t.get("id", UUID.class), Function.identity()));
    }

    /**
     * Rang de la vue : 1 + le nombre d'actions <b>plus récentes</b> du même tiers et du même type,
     * hors actions système et impayées. Une action sans tiers ou sans type a le rang 1 — l'égalité
     * SQL avec {@code NULL} ne compte rien, exactement comme dans la vue.
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

    /**
     * Pour l'état d'une action de prospection : le statut du dossier <b>né de cette prospection</b>
     * ({@code ComFolder.ComProspectionId}), s'il existe — la jointure {@code Pfolder} de la vue.
     */
    private Map<UUID, String> etatsDesDossiersNesDeProspection(Collection<Tuple> lignes) {
        List<UUID> prospectionIds = lignes.stream()
                .filter(t -> t.get("dossierId") == null && t.get("prospectionId") != null)
                .map(t -> t.get("prospectionId", UUID.class))
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
                t.get("tiersNom", String.class),
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
                t.get("prospectionId", UUID.class),
                t.get("dossierId", UUID.class),
                t.get("trancheId", UUID.class),
                t.get("creePar", String.class),
                t.get("creeLe", LocalDateTime.class),
                t.get("modifiePar", String.class),
                t.get("modifieLe", LocalDateTime.class));
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
        if (t.get("dossierId") != null) {
            return t.get("statutDossier", String.class);
        }
        UUID prospectionId = t.get("prospectionId", UUID.class);
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
