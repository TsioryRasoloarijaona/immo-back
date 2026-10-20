package com.tsix.immocrm.services.perimetre;

import com.tsix.immocrm.domain.RolesExemptesPerimetre;
import com.tsix.immocrm.dto.perimetre.ProjetPerimetreDto;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.adm.AdmUserHabilitation;
import com.tsix.immocrm.entities.cfg.CfgProject;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComActionMessage;
import com.tsix.immocrm.entities.com.ComDocument;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.com.ComProspection;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.entities.stl.StlQuote;
import com.tsix.immocrm.entities.stl.StlSettlement;
import com.tsix.immocrm.entities.stl.StlSettlementLine;
import com.tsix.immocrm.entities.str.StrFolderStatusHistory;
import com.tsix.immocrm.exceptions.ForbiddenException;
import com.tsix.immocrm.exceptions.InvalidRequestException;
import com.tsix.immocrm.exceptions.ResourceNotFoundException;
import com.tsix.immocrm.repositories.adm.AdmUserHabilitationRepository;
import com.tsix.immocrm.repositories.adm.AdmUserRepository;
import com.tsix.immocrm.repositories.adm.AdmUserRoleRepository;
import com.tsix.immocrm.repositories.cfg.CfgProjectRepository;
import com.tsix.immocrm.repositories.com.ComFolderRepository;
import com.tsix.immocrm.repositories.com.ComProspectionRepository;
import com.tsix.immocrm.repositories.stl.StlSettlementRepository;
import com.tsix.immocrm.security.AuthenticatedUser;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Périmètre commercial par projets affectés ({@code FEATURE-perimetre-commercial.md} §6) : vide ⇒ aucune
 * restriction, ligne sans projet ⇒ visible de tous, relu à chaque requête (jamais depuis le JWT).
 * Chaque {@code filtreXxx()}/{@code verifierAccesXxx()} applique aussi {@link VisibiliteEntites}, à tous.
 */
@Service
@Transactional(readOnly = true)
public class PerimetreCommercialService {

    private static final String STK_ITEM = "stkItem";
    private static final String STK_HIERARCHY = "stkHierarchy";
    private static final String CFG_PROJECT = "cfgProject";
    private static final String COM_FOLDER = "comFolder";

    private final AdmUserHabilitationRepository habilitationRepository;
    private final AdmUserRoleRepository userRoleRepository;
    private final AdmUserRepository userRepository;
    private final CfgProjectRepository projectRepository;
    private final ComFolderRepository folderRepository;
    private final ComProspectionRepository prospectionRepository;
    private final StlSettlementRepository settlementRepository;

    public PerimetreCommercialService(AdmUserHabilitationRepository habilitationRepository,
                                       AdmUserRoleRepository userRoleRepository,
                                       AdmUserRepository userRepository,
                                       CfgProjectRepository projectRepository,
                                       ComFolderRepository folderRepository,
                                       ComProspectionRepository prospectionRepository,
                                       StlSettlementRepository settlementRepository) {
        this.habilitationRepository = habilitationRepository;
        this.userRoleRepository = userRoleRepository;
        this.userRepository = userRepository;
        this.projectRepository = projectRepository;
        this.folderRepository = folderRepository;
        this.prospectionRepository = prospectionRepository;
        this.settlementRepository = settlementRepository;
    }

    // ---------------------------------------------------------------- résolution du périmètre

    /**
     * Vide si aucune restriction : appelant non identifiable, aucun projet affecté (§6.3) ou rôle
     * exempté (§6.2). Jamais une liste vide, qui rendrait aveugle un compte non affecté.
     */
    public Optional<List<UUID>> perimetreCourant() {
        return admUserIdCourant().flatMap(this::perimetreDe);
    }

    public Optional<List<UUID>> perimetreDe(UUID admUserId) {
        if (admUserId == null || estExempte(admUserId)) {
            return Optional.empty();
        }
        List<UUID> projetIds = habilitationRepository.findProjetIdsByAdmUserId(admUserId).stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        return projetIds.isEmpty() ? Optional.empty() : Optional.of(projetIds);
    }

    /** Rôles lus dans {@code AdmUserRole}, jamais {@code AdmUser.WebRoleId} : les deux divergent (§6.2). */
    public boolean estExempte(UUID admUserId) {
        List<String> codes = userRoleRepository.findByAdmUserId(admUserId).stream()
                .map(ur -> ur.getAdmRole() != null ? ur.getAdmRole().getCode() : null)
                .filter(Objects::nonNull)
                .toList();
        return RolesExemptesPerimetre.exempte(codes);
    }

    /**
     * Projets <b>effectivement appliqués</b> (vide si non restreint ou exempté) — à ne pas confondre avec
     * {@link #projetsAffectes(UUID)}, qui renvoie les affectations brutes même pour un exempté.
     */
    public List<ProjetPerimetreDto> projetsDuPerimetre(UUID admUserId) {
        return perimetreDe(admUserId)
                .map(ids -> projectRepository.findAllById(ids).stream()
                        .map(PerimetreCommercialService::toProjetDto)
                        .sorted((a, b) -> Objects.toString(a.code(), "").compareTo(Objects.toString(b.code(), "")))
                        .toList())
                .orElseGet(List::of);
    }

    /**
     * Un principal d'un autre type ({@code @WithMockUser}, route anonyme) est « non restreint » (§6.3) :
     * c'est {@code SecurityConfig} qui décide qui atteint l'endpoint, pas ce service.
     */
    public static Optional<UUID> admUserIdCourant() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof AuthenticatedUser user) {
            return Optional.ofNullable(user.admUserId());
        }
        return Optional.empty();
    }

    // ---------------------------------------------------------------- filtres de liste

    /**
     * Projet par la tranche : {@code StkItem.CfgProjectId} est vide en base, et {@code StkHierarchyId}
     * est une FK vers {@code CfgTranche} malgré son nom (§4). Jointures LEFT : le NULL reste visible (§6.4).
     */
    public Specification<ComFolder> filtreDossiers() {
        Optional<List<UUID>> perimetre = perimetreCourant();
        return (root, query, cb) -> {
            Join<ComFolder, StkItem> article = root.join(STK_ITEM, JoinType.LEFT);
            Join<StkItem, CfgTranche> tranche = article.join(STK_HIERARCHY, JoinType.LEFT);
            Join<CfgTranche, CfgProject> projet = tranche.join(CFG_PROJECT, JoinType.LEFT);
            return combine(VisibiliteEntites.depuisProjet(projet, cb), projet, perimetre, cb);
        };
    }

    /**
     * Échéances ({@code ComDocument}) : visible ssi son dossier l'est par {@link #filtreDossiers()}, appliqué
     * tel quel dans une sous-requête. Une échéance sans dossier n'est jamais visible.
     */
    public Specification<ComDocument> filtreEcheances() {
        return parDossierVisible(false);
    }

    /** Toute ligne portée par une association {@code comFolder} : visible ssi son dossier l'est, comme une échéance. */
    public <T> Specification<T> filtreParDossier() {
        return parDossierVisible(false);
    }

    /** Ligne {@code StrFolderStatusHistory} : visible sans dossier de vente, sinon comme son dossier. */
    public Specification<StrFolderStatusHistory> filtreHistoriqueStatuts() {
        return parDossierVisible(true);
    }

    private <T> Specification<T> parDossierVisible(boolean visibleSansDossier) {
        Specification<ComFolder> dossiers = filtreDossiers();
        return (root, query, cb) -> {
            Subquery<UUID> visibles = query.subquery(UUID.class);
            Root<ComFolder> dossier = visibles.from(ComFolder.class);
            visibles.select(dossier.get("id")).where(
                    cb.equal(dossier.get("id"), root.get(COM_FOLDER).get("id")),
                    dossiers.toPredicate(dossier, query, cb));
            Predicate visible = cb.exists(visibles);
            return visibleSansDossier ? cb.or(cb.isNull(root.get(COM_FOLDER)), visible) : visible;
        };
    }

    /**
     * Prospections et pré-prospections, par la tranche directe, sans repli par {@code StkItemId} (§4).
     * Une pré-prospection n'a jamais de commercial : seul le projet peut la restreindre (§5).
     */
    public Specification<ComProspection> filtreProspections() {
        return filtreParTranche(STK_HIERARCHY);
    }

    /** Ligne portée par une association directe vers {@code CfgTranche} : projet par la tranche, jointures LEFT. */
    public <T> Specification<T> filtreParTranche(String associationTranche) {
        Optional<List<UUID>> perimetre = perimetreCourant();
        return (root, query, cb) -> {
            Join<T, CfgTranche> tranche = root.join(associationTranche, JoinType.LEFT);
            Join<CfgTranche, CfgProject> projet = tranche.join(CFG_PROJECT, JoinType.LEFT);
            return combine(VisibiliteEntites.depuisProjet(projet, cb), projet, perimetre, cb);
        };
    }

    /**
     * Visible si une ligne au moins vise un dossier d'un projet du périmètre, ou si aucune ligne ne
     * résout de projet (§6.4).
     */
    public Specification<StlSettlement> filtreEncaissements() {
        Optional<List<UUID>> perimetre = perimetreCourant();
        return (root, query, cb) -> {
            // Visibilité par la FK société directe (exacte, voir VisibiliteEntites) ; le périmètre,
            // lui, passe par les lignes.
            Predicate visible = VisibiliteEntites.depuisSocieteDirecte(root, "cfgCompany", cb);
            if (perimetre.isEmpty()) {
                return visible;
            }
            List<UUID> projetIds = perimetre.get();

            Subquery<UUID> dansPerimetre = query.subquery(UUID.class);
            var ligne = dansPerimetre.from(StlSettlementLine.class);
            dansPerimetre.select(ligne.get("id")).where(cb.and(
                    cb.equal(ligne.get("stlSettlement"), root),
                    ligne.get("comDocument").get(COM_FOLDER).get(STK_ITEM)
                            .get(STK_HIERARCHY).get(CFG_PROJECT).get("id").in(projetIds)));

            Subquery<UUID> avecProjet = query.subquery(UUID.class);
            var ligne2 = avecProjet.from(StlSettlementLine.class);
            avecProjet.select(ligne2.get("id")).where(cb.and(
                    cb.equal(ligne2.get("stlSettlement"), root),
                    cb.isNotNull(ligne2.get("comDocument").get(COM_FOLDER).get(STK_ITEM)
                            .get(STK_HIERARCHY).get(CFG_PROJECT).get("id"))));

            return cb.and(visible, cb.or(cb.exists(dansPerimetre), cb.not(cb.exists(avecProjet))));
        };
    }

    /**
     * Liste globale {@code GET /api/devis}, par la tranche de l'offre. {@code /prospections/{id}/devis}
     * n'en a pas besoin : {@code PerimetreInterceptor} contrôle déjà la prospection porteuse.
     */
    public Specification<StlQuote> filtreDevis() {
        return filtreParTranche("cfgTranche");
    }

    /**
     * Interactions de la hotline : projet par la prospection, sinon par le dossier ; §6.4 (visible)
     * seulement une fois les deux chemins épuisés (§9ter.3). Jointures LEFT pour garder les orphelines.
     */
    public Specification<ComActionMessage> filtreInteractions() {
        Optional<List<UUID>> perimetre = perimetreCourant();
        return (root, query, cb) -> {
            Join<ComActionMessage, ComProspection> prospection = root.join("comProspection", JoinType.LEFT);
            Join<ComProspection, CfgTranche> trancheProspection = prospection.join(STK_HIERARCHY, JoinType.LEFT);
            Join<CfgTranche, CfgProject> projetProspection = trancheProspection.join(CFG_PROJECT, JoinType.LEFT);

            Join<ComActionMessage, ComFolder> dossier = root.join(COM_FOLDER, JoinType.LEFT);
            Join<ComFolder, StkItem> article = dossier.join(STK_ITEM, JoinType.LEFT);
            Join<StkItem, CfgTranche> trancheDossier = article.join(STK_HIERARCHY, JoinType.LEFT);
            Join<CfgTranche, CfgProject> projetDossier = trancheDossier.join(CFG_PROJECT, JoinType.LEFT);

            // coalesce = "prospection d'abord, dossier ensuite" ; NULL = les deux chemins épuisés.
            Expression<UUID> projet = cb.coalesce(projetProspection.get("id"), projetDossier.get("id"));
            Predicate visible = VisibiliteEntites.depuisDeuxProjets(projetProspection, projetDossier, cb);
            if (perimetre.isEmpty()) {
                return visible;
            }
            return cb.and(visible, cb.or(projet.in(perimetre.get()), cb.isNull(projet)));
        };
    }

    /**
     * Visibilité (toujours) puis périmètre (si restreint) sur la <b>même</b> jointure : deux
     * {@code Specification} combinées dupliqueraient les {@code LEFT JOIN} en SQL.
     */
    private static Predicate combine(Predicate visibilite, Join<?, CfgProject> projet,
                                     Optional<List<UUID>> perimetre, CriteriaBuilder cb) {
        return perimetre
                .map(projetIds -> cb.and(visibilite,
                        cb.or(projet.get("id").in(projetIds), cb.isNull(projet.get("id")))))
                .orElse(visibilite);
    }

    // ---------------------------------------------------------------- contrôle d'accès par id

    /**
     * Appelé par {@code PerimetreInterceptor} pour <b>toute</b> route {@code /api/dossiers/{id}/…}, sinon
     * le filtre se contourne par les ids. Hors périmètre ou masqué ⇒ 404, jamais 403 (§6.5).
     */
    public void verifierAccesDossier(UUID dossierId) {
        verifier(projetIds -> folderRepository.existsDansPerimetre(dossierId, projetIds),
                () -> folderRepository.existsVisible(dossierId),
                "Dossier introuvable: " + dossierId);
    }

    /** Idem {@link #verifierAccesDossier} pour une prospection ou une pré-prospection. */
    public void verifierAccesProspection(UUID prospectionId) {
        verifier(projetIds -> prospectionRepository.existsDansPerimetre(prospectionId, projetIds),
                () -> prospectionRepository.existsVisible(prospectionId),
                "Prospection introuvable: " + prospectionId);
    }

    /** Idem {@link #verifierAccesDossier} pour un encaissement (règlement). */
    public void verifierAccesEncaissement(UUID encaissementId) {
        verifier(projetIds -> settlementRepository.existsDansPerimetre(encaissementId, projetIds),
                () -> settlementRepository.existsVisible(encaissementId),
                "Encaissement introuvable: " + encaissementId);
    }

    /**
     * Toujours une requête, même sans périmètre : la visibilité par entité vaut pour tous
     * ({@code FEATURE-visibilite-transversale-entites.md} R4). « Dans le périmètre » porte déjà les deux.
     */
    private void verifier(java.util.function.Predicate<List<UUID>> dansPerimetre,
                          java.util.function.BooleanSupplier visible, String messageSiAbsent) {
        boolean autorise = perimetreCourant()
                .map(dansPerimetre::test)
                .orElseGet(visible::getAsBoolean);
        if (!autorise) {
            throw new ResourceNotFoundException(messageSiAbsent);
        }
    }

    // ---------------------------------------------------------------- contrôle d'accès en écriture

    /**
     * 403 et non 404 : l'appelant nomme lui-même le projet, il n'y a rien à cacher (§6.5). Un projet
     * {@code null} est accepté (§6.4).
     */
    public void verifierEcritureSurProjet(UUID projetId, String description) {
        perimetreCourant().ifPresent(projetIds -> {
            if (projetId != null && !projetIds.contains(projetId)) {
                throw new ForbiddenException(
                        description + " : ce projet n'est pas dans votre périmètre commercial.");
            }
        });
    }

    /** Variante de {@link #verifierEcritureSurProjet} à partir d'une tranche (projet = son parent). */
    public void verifierEcritureSurTranche(CfgTranche tranche, String description) {
        UUID projetId = tranche != null && tranche.getCfgProject() != null ? tranche.getCfgProject().getId() : null;
        verifierEcritureSurProjet(projetId, description);
    }

    // ---------------------------------------------------------------- administration des affectations

    /** Portée projet seulement : les habilitations société / tranche / PrjProject du légataire restent hors API. */
    public List<ProjetPerimetreDto> projetsAffectes(UUID admUserId) {
        if (!userRepository.existsById(admUserId)) {
            throw new ResourceNotFoundException("Utilisateur introuvable: " + admUserId);
        }
        return habilitationRepository.findProjetsByAdmUserId(admUserId).stream()
                .map(row -> new ProjetPerimetreDto((UUID) row[0], (String) row[1], (String) row[2]))
                .distinct()
                .toList();
    }

    /**
     * Une ligne d'habilitation peut porter plusieurs portées : une ligne mixte perd seulement son
     * {@code CfgProjectId}, sinon on effacerait des droits du légataire (§9bis.4). {@code null} ⇒ 400,
     * seul {@code []} lève la restriction : un corps tronqué ne doit pas la lever (§9bis.6).
     */
    @Transactional
    public List<ProjetPerimetreDto> remplacerProjets(UUID admUserId, List<UUID> projetIds) {
        AdmUser user = userRepository.findById(admUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable: " + admUserId));

        if (projetIds == null) {
            throw new InvalidRequestException(
                    "projetIds est requis — envoyez une liste vide [] pour retirer toute restriction.");
        }
        List<UUID> demandes = projetIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        List<CfgProject> projets = projectRepository.findAllById(demandes);
        if (projets.size() != demandes.size()) {
            List<UUID> connus = projets.stream().map(CfgProject::getId).toList();
            List<UUID> inconnus = demandes.stream().filter(id -> !connus.contains(id)).toList();
            throw new InvalidRequestException("Projet(s) inconnu(s): " + inconnus);
        }

        for (AdmUserHabilitation existante : habilitationRepository.findByAdmUserIdAndCfgProjectIsNotNull(admUserId)) {
            if (porteUneAutrePortee(existante)) {
                existante.setCfgProject(null);
                habilitationRepository.save(existante);
            } else {
                habilitationRepository.delete(existante);
            }
        }

        for (CfgProject projet : projets) {
            AdmUserHabilitation habilitation = new AdmUserHabilitation();
            habilitation.setId(UUID.randomUUID());
            habilitation.setAdmUser(user);
            habilitation.setCfgProject(projet);
            habilitationRepository.save(habilitation);
        }

        return projets.stream()
                .map(PerimetreCommercialService::toProjetDto)
                .sorted((a, b) -> Objects.toString(a.code(), "").compareTo(Objects.toString(b.code(), "")))
                .toList();
    }

    /** Une seule requête pour toute la page de {@code GET /api/utilisateurs}. */
    public Map<UUID, Integer> nombreProjetsParUtilisateur(List<UUID> admUserIds) {
        if (admUserIds == null || admUserIds.isEmpty()) {
            return Map.of();
        }
        Map<UUID, Integer> resultat = new LinkedHashMap<>();
        for (Object[] row : habilitationRepository.countProjetsGroupedByAdmUserId(admUserIds)) {
            resultat.put((UUID) row[0], ((Number) row[1]).intValue());
        }
        return resultat;
    }

    /** Ligne mixte : elle porte aussi une portée du légataire et doit survivre ({@link #remplacerProjets}). */
    public static boolean porteUneAutrePortee(AdmUserHabilitation habilitation) {
        return habilitation.getCfgCompany() != null
                || habilitation.getCfgTranche() != null
                || habilitation.getPrjProjectId() != null;
    }

    private static ProjetPerimetreDto toProjetDto(CfgProject projet) {
        return new ProjetPerimetreDto(projet.getId(), projet.getCode(), projet.getDescription());
    }
}
