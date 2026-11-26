package com.tsix.immocrm.services.perimetre;

import com.tsix.immocrm.domain.RoleAgence;
import com.tsix.immocrm.domain.RolesExemptesPerimetre;
import com.tsix.immocrm.dto.perimetre.ProjetPerimetreDto;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.adm.AdmUserHabilitation;
import com.tsix.immocrm.entities.cfg.CfgProject;
import com.tsix.immocrm.entities.cfg.CfgTier;
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
import com.tsix.immocrm.repositories.cfg.CfgProjectRepository;
import com.tsix.immocrm.repositories.com.ComFolderRepository;
import com.tsix.immocrm.repositories.com.ComProspectionRepository;
import com.tsix.immocrm.repositories.stl.StlSettlementRepository;
import com.tsix.immocrm.security.AuthenticatedUser;
import com.tsix.immocrm.services.prospection.DiscriminantPreProspection;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
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
    private static final String COM_PROSPECTION = "comProspection";
    private static final String CFG_COMMERCIAL = "cfgCommercial";

    private final AdmUserHabilitationRepository habilitationRepository;
    private final AgenceDuCompte agenceDuCompte;
    private final AdmUserRepository userRepository;
    private final CfgProjectRepository projectRepository;
    private final ComFolderRepository folderRepository;
    private final ComProspectionRepository prospectionRepository;
    private final StlSettlementRepository settlementRepository;

    public PerimetreCommercialService(AdmUserHabilitationRepository habilitationRepository,
                                       AgenceDuCompte agenceDuCompte,
                                       AdmUserRepository userRepository,
                                       CfgProjectRepository projectRepository,
                                       ComFolderRepository folderRepository,
                                       ComProspectionRepository prospectionRepository,
                                       StlSettlementRepository settlementRepository) {
        this.habilitationRepository = habilitationRepository;
        this.agenceDuCompte = agenceDuCompte;
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
        return admUserId == null ? Optional.empty() : perimetreDe(admUserId, agenceDuCompte.roles(admUserId));
    }

    /**
     * Un compte agence est toujours restreint : sans projet ou incomplet, {@code [AUCUN]} (R-V1, R-V5, R-INS3),
     * qu'un {@code IN} lit comme « rien » chez tous les appelants.
     */
    private Optional<List<UUID>> perimetreDe(UUID admUserId, List<String> roles) {
        boolean agence = RoleAgence.porte(roles);
        if (!agence && RolesExemptesPerimetre.exempte(roles)) {
            return Optional.empty();
        }
        List<UUID> projetIds = habilitationRepository.findProjetIdsByAdmUserId(admUserId).stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (agence) {
            boolean incomplet = projetIds.isEmpty()
                    || agenceDuCompte.vendeursDeLAgence(admUserId).contains(AgenceDuCompte.AUCUN);
            return Optional.of(incomplet ? List.of(AgenceDuCompte.AUCUN) : projetIds);
        }
        return projetIds.isEmpty() ? Optional.empty() : Optional.of(projetIds);
    }

    /** Rôles lus dans {@code AdmUserRole}, jamais {@code AdmUser.WebRoleId} (§6.2) ; agence ⇒ jamais exempté (R-V3). */
    public boolean estExempte(UUID admUserId) {
        List<String> codes = agenceDuCompte.roles(admUserId);
        return !RoleAgence.porte(codes) && RolesExemptesPerimetre.exempte(codes);
    }

    /** Rôle Agence du compte courant lu en base, vendeur et projets ignorés (FEATURE-agences-vente.md R-V9). */
    public boolean estAgenceCourant() {
        return agenceCourante();
    }

    /** Même règle lue sur la source : les écrans d'administration n'y empruntent aucune connexion de la copie. */
    @Transactional
    public boolean estAgenceCourantSurSource() {
        return agenceCourante();
    }

    private boolean agenceCourante() {
        return admUserIdCourant().map(id -> RoleAgence.porte(agenceDuCompte.roles(id))).orElse(false);
    }

    /** Vendeurs de l'agence du compte courant ; absent pour un compte du siège (FEATURE-agences-vente.md §4). */
    public Optional<List<UUID>> vendeursAgenceCourante() {
        return admUserIdCourant().flatMap(agenceDuCompte::vendeurs);
    }

    /**
     * « Projet visible » du compte courant, lu une fois : projet nul visible (§6.4) sauf pour une agence (R-V4),
     * sinon dans le périmètre.
     */
    public java.util.function.Predicate<UUID> projetVisible() {
        Optional<UUID> compte = admUserIdCourant();
        if (compte.isEmpty()) {
            return projetId -> true;
        }
        List<String> roles = agenceDuCompte.roles(compte.get());
        boolean agence = RoleAgence.porte(roles);
        Optional<List<UUID>> projets = perimetreDe(compte.get(), roles);
        return projetId -> projetId == null ? !agence : projets.map(ids -> ids.contains(projetId)).orElse(true);
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
        return filtreDossiers(porteeCourante());
    }

    private static Specification<ComFolder> filtreDossiers(Portee portee) {
        return (root, query, cb) -> {
            Join<ComFolder, StkItem> article = root.join(STK_ITEM, JoinType.LEFT);
            Join<StkItem, CfgTranche> tranche = article.join(STK_HIERARCHY, JoinType.LEFT);
            Join<CfgTranche, CfgProject> projet = tranche.join(CFG_PROJECT, JoinType.LEFT);
            return portee.appliquer(VisibiliteEntites.depuisProjet(projet, cb), projet.get("id"),
                    root.get(CFG_COMMERCIAL).get("id"), cb);
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
        return filtreProspections(porteeCourante());
    }

    /** Agence : une pré-prospection reste invisible, même assignée à un de ses vendeurs (R-V8, V5). */
    private static Specification<ComProspection> filtreProspections(Portee portee) {
        return (root, query, cb) -> {
            Join<ComProspection, CfgProject> projet = root.join(STK_HIERARCHY, JoinType.LEFT)
                    .join(CFG_PROJECT, JoinType.LEFT);
            Predicate filtre = portee.appliquer(VisibiliteEntites.depuisProjet(projet, cb), projet.get("id"),
                    root.get(CFG_COMMERCIAL).get("id"), cb);
            return portee.agence() ? cb.and(filtre, DiscriminantPreProspection.nEstPasPreProspection(root, query, cb))
                    : filtre;
        };
    }

    /** Ligne portée par une association directe vers {@code CfgTranche} : projet par la tranche, jointures LEFT. */
    public <T> Specification<T> filtreParTranche(String associationTranche) {
        Portee portee = porteeCourante();
        return (root, query, cb) -> {
            Join<T, CfgTranche> tranche = root.join(associationTranche, JoinType.LEFT);
            Join<CfgTranche, CfgProject> projet = tranche.join(CFG_PROJECT, JoinType.LEFT);
            return portee.appliquer(VisibiliteEntites.depuisProjet(projet, cb), projet.get("id"), null, cb);
        };
    }

    /**
     * Visible si une ligne au moins vise un dossier d'un projet du périmètre, ou si aucune ligne ne
     * résout de projet (§6.4). Agence : une ligne au moins sur un dossier de l'agence (R-V8).
     */
    public Specification<StlSettlement> filtreEncaissements() {
        return filtreEncaissements(porteeCourante());
    }

    private static Specification<StlSettlement> filtreEncaissements(Portee portee) {
        return (root, query, cb) -> {
            // Visibilité par la FK société directe (exacte, voir VisibiliteEntites) ; le périmètre,
            // lui, passe par les lignes.
            Predicate visible = VisibiliteEntites.depuisSocieteDirecte(root, "cfgCompany", cb);
            if (portee.projets().isEmpty()) {
                return visible;
            }
            List<UUID> projetIds = portee.projets().get();

            Subquery<UUID> dansPerimetre = query.subquery(UUID.class);
            var ligne = dansPerimetre.from(StlSettlementLine.class);
            var dossier = ligne.get("comDocument").get(COM_FOLDER);
            Predicate ligneDansPerimetre = dossier.get(STK_ITEM).get(STK_HIERARCHY).get(CFG_PROJECT).get("id")
                    .in(projetIds);
            if (portee.agence()) {
                ligneDansPerimetre = cb.and(ligneDansPerimetre,
                        dossier.get(CFG_COMMERCIAL).get("id").in(portee.vendeurs()));
            }
            dansPerimetre.select(ligne.get("id")).where(cb.and(
                    cb.equal(ligne.get("stlSettlement"), root), ligneDansPerimetre));
            if (portee.agence()) {
                return cb.and(visible, cb.exists(dansPerimetre));
            }

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
        Portee portee = porteeCourante();
        if (!portee.agence()) {
            return filtreParTranche("cfgTranche");
        }
        // Agence : par la prospection de l'offre (R-V8, V7), même règle que la liste des prospections.
        return (root, query, cb) -> {
            Subquery<UUID> visibles = query.subquery(UUID.class);
            Root<ComProspection> prospection = visibles.from(ComProspection.class);
            visibles.select(prospection.get("id")).where(
                    cb.equal(prospection.get("id"), root.get(COM_PROSPECTION).get("id")),
                    filtreProspections(portee).toPredicate(prospection, query, cb));
            return cb.exists(visibles);
        };
    }

    /**
     * Interactions de la hotline : projet par la prospection, sinon par le dossier ; §6.4 (visible)
     * seulement une fois les deux chemins épuisés (§9ter.3). Jointures LEFT pour garder les orphelines.
     */
    public Specification<ComActionMessage> filtreInteractions() {
        Portee portee = porteeCourante();
        return (root, query, cb) -> {
            Join<ComActionMessage, ComProspection> prospection = root.join(COM_PROSPECTION, JoinType.LEFT);
            Join<ComProspection, CfgTranche> trancheProspection = prospection.join(STK_HIERARCHY, JoinType.LEFT);
            Join<CfgTranche, CfgProject> projetProspection = trancheProspection.join(CFG_PROJECT, JoinType.LEFT);

            Join<ComActionMessage, ComFolder> dossier = root.join(COM_FOLDER, JoinType.LEFT);
            Join<ComFolder, StkItem> article = dossier.join(STK_ITEM, JoinType.LEFT);
            Join<StkItem, CfgTranche> trancheDossier = article.join(STK_HIERARCHY, JoinType.LEFT);
            Join<CfgTranche, CfgProject> projetDossier = trancheDossier.join(CFG_PROJECT, JoinType.LEFT);

            // coalesce = "prospection d'abord, dossier ensuite" ; NULL = les deux chemins épuisés.
            Expression<UUID> projet = cb.coalesce(projetProspection.get("id"), projetDossier.get("id"));
            Predicate visible = VisibiliteEntites.depuisDeuxProjets(projetProspection, projetDossier, cb);
            if (!portee.agence()) {
                return portee.appliquer(visible, projet, null, cb);
            }
            Expression<UUID> commercial = cb.coalesce(prospection.get(CFG_COMMERCIAL).get("id"),
                    dossier.get(CFG_COMMERCIAL).get("id"));
            return cb.and(portee.appliquer(visible, projet, commercial, cb),
                    cb.or(cb.isNull(prospection.get("id")),
                            DiscriminantPreProspection.nEstPasPreProspection(prospection, query, cb)));
        };
    }

    /**
     * Prospects et clients (V6) : siège sans filtre ; agence = au moins une prospection ou un dossier (client) visible
     * par {@link #filtreProspections()} / {@link #filtreDossiers()}. Un prospect suivi par personne est invisible.
     */
    public Specification<CfgTier> filtreProspects() {
        Portee portee = porteeCourante();
        if (!portee.agence()) {
            return (root, query, cb) -> cb.conjunction();
        }
        return (root, query, cb) -> {
            Subquery<UUID> prospections = query.subquery(UUID.class);
            Root<ComProspection> prospection = prospections.from(ComProspection.class);
            prospections.select(prospection.get("id")).where(cb.equal(prospection.get("cfgProspect"), root),
                    filtreProspections(portee).toPredicate(prospection, query, cb));
            Subquery<UUID> dossiers = query.subquery(UUID.class);
            Root<ComFolder> dossier = dossiers.from(ComFolder.class);
            dossiers.select(dossier.get("id")).where(cb.equal(dossier.get("cfgCustomer"), root),
                    filtreDossiers(portee).toPredicate(dossier, query, cb));
            return cb.or(cb.exists(prospections), cb.exists(dossiers));
        };
    }

    /**
     * Projets du périmètre (vide = non restreint) et, pour un compte agence, les vendeurs de son agence (vide = siège),
     * lus une fois par filtre.
     */
    private record Portee(Optional<List<UUID>> projets, List<UUID> vendeurs) {

        boolean agence() {
            return !vendeurs.isEmpty();
        }

        /**
         * Visibilité puis périmètre sur la <b>même</b> jointure (deux {@code Specification} dupliqueraient les
         * {@code LEFT JOIN}). Siège : projet nul visible (§6.4) ; agence : projet nul invisible (R-V4) et, quand
         * {@code commercial} est fourni, commercial de l'agence, commercial nul invisible (R-V8).
         */
        Predicate appliquer(Predicate visibilite, Expression<UUID> projet, Expression<UUID> commercial,
                            CriteriaBuilder cb) {
            if (projets.isEmpty()) {
                return visibilite;
            }
            Predicate dansProjets = projet.in(projets.get());
            if (!agence()) {
                return cb.and(visibilite, cb.or(dansProjets, cb.isNull(projet)));
            }
            return commercial == null ? cb.and(visibilite, dansProjets)
                    : cb.and(visibilite, dansProjets, cb.isNotNull(commercial), commercial.in(vendeurs));
        }
    }

    private Portee porteeCourante() {
        Optional<UUID> compte = admUserIdCourant();
        if (compte.isEmpty()) {
            return new Portee(Optional.empty(), List.of());
        }
        List<String> roles = agenceDuCompte.roles(compte.get());
        List<UUID> vendeurs = RoleAgence.porte(roles) ? agenceDuCompte.vendeursDeLAgence(compte.get()) : List.of();
        return new Portee(perimetreDe(compte.get(), roles), vendeurs);
    }

    // ---------------------------------------------------------------- contrôle d'accès par id

    /**
     * Appelé par {@code PerimetreInterceptor} pour <b>toute</b> route {@code /api/dossiers/{id}/…}, sinon
     * le filtre se contourne par les ids. Hors périmètre ou masqué ⇒ 404, jamais 403 (§6.5).
     */
    public void verifierAccesDossier(UUID dossierId) {
        Portee portee = porteeCourante();
        verifier(portee, () -> existeParmi(folderRepository, filtreDossiers(portee), dossierId),
                projetIds -> folderRepository.existsDansPerimetre(dossierId, projetIds),
                () -> folderRepository.existsVisible(dossierId),
                "Dossier introuvable: " + dossierId);
    }

    /** Idem {@link #verifierAccesDossier} pour une prospection ou une pré-prospection. */
    public void verifierAccesProspection(UUID prospectionId) {
        Portee portee = porteeCourante();
        verifier(portee, () -> existeParmi(prospectionRepository, filtreProspections(portee), prospectionId),
                projetIds -> prospectionRepository.existsDansPerimetre(prospectionId, projetIds),
                () -> prospectionRepository.existsVisible(prospectionId),
                "Prospection introuvable: " + prospectionId);
    }

    /** Idem {@link #verifierAccesDossier} pour un encaissement (règlement). */
    public void verifierAccesEncaissement(UUID encaissementId) {
        Portee portee = porteeCourante();
        verifier(portee, () -> existeParmi(settlementRepository, filtreEncaissements(portee), encaissementId),
                projetIds -> settlementRepository.existsDansPerimetre(encaissementId, projetIds),
                () -> settlementRepository.existsVisible(encaissementId),
                "Encaissement introuvable: " + encaissementId);
    }

    /**
     * Toujours une requête, même sans périmètre : la visibilité par entité vaut pour tous
     * ({@code FEATURE-visibilite-transversale-entites.md} R4). Agence : la règle de la liste (R-V8).
     */
    private static void verifier(Portee portee, java.util.function.BooleanSupplier visibleAgence,
                                 java.util.function.Predicate<List<UUID>> dansPerimetre,
                                 java.util.function.BooleanSupplier visible, String messageSiAbsent) {
        boolean autorise;
        if (portee.agence()) {
            autorise = visibleAgence.getAsBoolean();
        } else {
            autorise = portee.projets().map(dansPerimetre::test).orElseGet(visible::getAsBoolean);
        }
        if (!autorise) {
            throw new ResourceNotFoundException(messageSiAbsent);
        }
    }

    private static <T> boolean existeParmi(JpaSpecificationExecutor<T> depot, Specification<T> filtre, UUID id) {
        return depot.exists(filtre.and((root, query, cb) -> cb.equal(root.get("id"), id)));
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
