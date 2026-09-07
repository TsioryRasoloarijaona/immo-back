package com.tsix.immocrm.services.perimetre;

import com.tsix.immocrm.domain.RolesExemptesPerimetre;
import com.tsix.immocrm.dto.perimetre.ProjetPerimetreDto;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.adm.AdmUserHabilitation;
import com.tsix.immocrm.entities.cfg.CfgProject;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComActionMessage;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.com.ComProspection;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.entities.stl.StlQuote;
import com.tsix.immocrm.entities.stl.StlSettlement;
import com.tsix.immocrm.entities.stl.StlSettlementLine;
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
 * Périmètre commercial — point d'entrée unique de la feature "affectation de projets"
 * ({@code instructions/FEATURE-perimetre-commercial.md}).
 *
 * <p>Un administrateur affecte un ou plusieurs <b>projets</b> à un compte ; ce compte ne voit alors
 * plus que les prospections, pré-prospections, dossiers, encaissements et agrégats de tableau de
 * bord rattachés à ces projets. Le filtrage est intégralement côté serveur.
 *
 * <p>Trois règles structurantes, toutes mesurées sur les données réelles :
 * <ul>
 *   <li><b>Périmètre vide ⇒ aucune restriction</b> (§6.3). Le filtre ne s'active que lorsqu'un
 *       administrateur a explicitement affecté au moins un projet — déploiement opt-in, compte par
 *       compte, sans rien casser le jour de la mise en production. Un "deny by default" aurait
 *       laissé la quasi-totalité des comptes existants devant des écrans vides.</li>
 *   <li><b>Ligne sans projet résoluble ⇒ visible de tous</b> (§6.4). Le prédicat est toujours
 *       {@code projet ∈ périmètre OR projet IS NULL}, jamais une simple jointure interne : une
 *       pré-prospection sans projet n'appartient à personne, la masquer reviendrait à perdre le
 *       lead pour protéger une donnée rattachée à aucun périmètre.</li>
 *   <li><b>Résolution par requête, jamais depuis le JWT</b> (§6.1). Un token survit à une
 *       modification d'affectation par l'administrateur ; y embarquer le périmètre servirait une
 *       vue périmée. Aucun nouveau claim n'a été ajouté.</li>
 * </ul>
 *
 * <p>La jointure se fait toujours sur {@code AdmUserId}, jamais via {@code CfgEmployeeId} : cette
 * dernière colonne n'est pas unique en base réelle (un tier porté par deux comptes), et une
 * résolution par tier duplique les lignes.
 *
 * <h2>Deux restrictions, pas une</h2>
 *
 * <p>Les méthodes {@code filtreXxx()} et {@code verifierAccesXxx()} de cette classe portent
 * <b>deux</b> restrictions superposées, et leur nom ne dit que la première :
 * <ol>
 *   <li>le <b>périmètre commercial</b> décrit ci-dessus — par compte, opt-in ;</li>
 *   <li>la <b>visibilité par entité juridique</b> ({@link VisibiliteEntites}) — globale,
 *       permanente, appliquée y compris aux rôles exemptés de périmètre.</li>
 * </ol>
 *
 * <p>Elles sont assemblées ici plutôt que sur chacun des neuf sites d'appel, qui héritent donc du
 * filtre d'entité sans avoir été modifiés. Les deux partagent la même chaîne de jointures
 * ({@code … → CfgTranche → CfgProject}, prolongée d'un cran vers {@code CfgCompany}), d'où
 * l'assemblage dans une seule lambda par méthode — voir {@link #combine}.
 *
 * <p>Elles partagent aussi la même règle sur les lignes non résolues (§6.4 côté périmètre, R2 côté
 * entité) : une ligne dont le projet — donc la société — ne se résout pas reste visible de tous.
 * C'est ce qui permet de les composer sans qu'elles se contredisent.
 */
@Service
@Transactional(readOnly = true)
public class PerimetreCommercialService {

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
     * Le périmètre effectivement applicable à l'appelant courant, ou {@link Optional#empty()}
     * lorsqu'aucune restriction ne s'applique — c'est-à-dire dans l'un de ces cas :
     * appelant non identifiable, aucun projet affecté (§6.3), ou rôle d'exemption (§6.2).
     * Ne renvoie jamais une liste vide : "vide" et "pas de restriction" sont le même cas, et les
     * confondre transformerait un compte non affecté en compte aveugle.
     */
    public Optional<List<UUID>> perimetreCourant() {
        return admUserIdCourant().flatMap(this::perimetreDe);
    }

    /**
     * Idem {@link #perimetreCourant()} pour un compte donné — utilisé par {@code GET /api/mon-compte}
     * et {@code POST /api/auth/connexion} pour renseigner {@code perimetreRestreint}.
     */
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

    /**
     * true si l'un des rôles du compte figure dans {@link RolesExemptesPerimetre} — les rôles sont
     * relus depuis {@code AdmUserRole} (m2m), source de vérité, jamais depuis
     * {@code AdmUser.WebRoleId} : les deux mécanismes coexistent et divergent en base réelle.
     */
    public boolean estExempte(UUID admUserId) {
        List<String> codes = userRoleRepository.findByAdmUserId(admUserId).stream()
                .map(ur -> ur.getAdmRole() != null ? ur.getAdmRole().getCode() : null)
                .filter(Objects::nonNull)
                .toList();
        return RolesExemptesPerimetre.exempte(codes);
    }

    /**
     * Les projets <b>effectivement appliqués</b> à ce compte, pour {@code UtilisateurCourantDto} —
     * liste vide dès que le compte n'est pas restreint (aucune affectation, ou rôle d'exemption).
     * À ne pas confondre avec {@link #projetsAffectes(UUID)}, qui est la vue d'administration et
     * renvoie les affectations brutes même pour un administrateur exempté.
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
     * Le principal courant, s'il s'agit bien d'un {@link AuthenticatedUser}. Un contexte de sécurité
     * portant un autre type de principal (tests {@code @WithMockUser}, endpoints de génération de
     * documents ouverts sans authentification) est traité comme "non restreint" — cohérent avec le
     * déploiement opt-in de §6.3, et c'est {@code SecurityConfig} qui décide qui a le droit
     * d'atteindre l'endpoint, pas ce service.
     */
    private static Optional<UUID> admUserIdCourant() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof AuthenticatedUser user) {
            return Optional.ofNullable(user.admUserId());
        }
        return Optional.empty();
    }

    // ---------------------------------------------------------------- filtres de liste

    /**
     * Prédicat de périmètre à combiner ({@code .and(...)}) au {@code Specification} de
     * {@code DossierService#findAll}. Chaîne de résolution du projet d'un dossier :
     * {@code ComFolder.StkItemId} → {@code StkItem.StkHierarchyId} → {@code CfgTranche.CfgProjectId}.
     * <p>
     * Deux pièges évités ici : {@code StkItem.CfgProjectId} est mort en base réelle (0/1068), le
     * projet passe donc obligatoirement par la tranche ; et {@code StkHierarchyId} est bien une FK
     * vers {@code CfgTranche} malgré son nom (aucune table {@code StkHierarchy} n'existe).
     * Les jointures sont en {@code LEFT} et le prédicat tolère {@code IS NULL} (§6.4).
     */
    public Specification<ComFolder> filtreDossiers() {
        Optional<List<UUID>> perimetre = perimetreCourant();
        return (root, query, cb) -> {
            Join<ComFolder, StkItem> article = root.join("stkItem", JoinType.LEFT);
            Join<StkItem, CfgTranche> tranche = article.join("stkHierarchy", JoinType.LEFT);
            Join<CfgTranche, CfgProject> projet = tranche.join("cfgProject", JoinType.LEFT);
            return combine(VisibiliteEntites.depuisProjet(projet, cb), projet, perimetre, cb);
        };
    }

    /**
     * Prédicat de périmètre pour {@code ProspectionService} (prospections <i>et</i>
     * pré-prospections). Chaîne <b>directe</b> {@code ComProspection.StkHierarchyId} →
     * {@code CfgTranche.CfgProjectId}, sans repli par {@code StkItemId} : cette colonne n'est
     * renseignée que sur 4,61 % des lignes et n'apporte aucune ligne supplémentaire (mesuré).
     * <p>
     * Rappel : une pré-prospection a par construction un commercial {@code NULL} (0 sur 2800), donc
     * seul le projet peut la scoper — aucune règle "le commercial voit ses propres
     * pré-prospections" n'est possible, elle renverrait l'ensemble vide.
     */
    public Specification<ComProspection> filtreProspections() {
        Optional<List<UUID>> perimetre = perimetreCourant();
        return (root, query, cb) -> {
            Join<ComProspection, CfgTranche> tranche = root.join("stkHierarchy", JoinType.LEFT);
            Join<CfgTranche, CfgProject> projet = tranche.join("cfgProject", JoinType.LEFT);
            return combine(VisibiliteEntites.depuisProjet(projet, cb), projet, perimetre, cb);
        };
    }

    /**
     * Prédicat de périmètre pour {@code EncaissementService} : un règlement est visible s'il porte
     * au moins une ligne rattachée à un dossier d'un projet du périmètre, ou si aucune de ses
     * lignes ne résout de projet (§6.4). Même motif {@code EXISTS} que le filtre {@code dossierId}
     * déjà en place.
     */
    public Specification<StlSettlement> filtreEncaissements() {
        Optional<List<UUID>> perimetre = perimetreCourant();
        return (root, query, cb) -> {
            // Visibilité : FK société directe, mesurée exacte (voir VisibiliteEntites) — pas de
            // sous-requête ici, contrairement au périmètre qui doit passer par les lignes.
            Predicate visible = VisibiliteEntites.depuisSocieteDirecte(root, "cfgCompany", cb);
            if (perimetre.isEmpty()) {
                return visible;
            }
            List<UUID> projetIds = perimetre.get();

            Subquery<UUID> dansPerimetre = query.subquery(UUID.class);
            var ligne = dansPerimetre.from(StlSettlementLine.class);
            dansPerimetre.select(ligne.get("id")).where(cb.and(
                    cb.equal(ligne.get("stlSettlement"), root),
                    ligne.get("comDocument").get("comFolder").get("stkItem")
                            .get("stkHierarchy").get("cfgProject").get("id").in(projetIds)));

            Subquery<UUID> avecProjet = query.subquery(UUID.class);
            var ligne2 = avecProjet.from(StlSettlementLine.class);
            avecProjet.select(ligne2.get("id")).where(cb.and(
                    cb.equal(ligne2.get("stlSettlement"), root),
                    cb.isNotNull(ligne2.get("comDocument").get("comFolder").get("stkItem")
                            .get("stkHierarchy").get("cfgProject").get("id"))));

            return cb.and(visible, cb.or(cb.exists(dansPerimetre), cb.not(cb.exists(avecProjet))));
        };
    }

    /**
     * Prédicat de périmètre pour {@code GET /api/devis} (liste globale des offres). {@code StlQuote}
     * porte une FK directe vers la tranche, donc le projet s'en déduit sans passer par la
     * prospection. Les offres sans tranche restent visibles de tous (§6.4).
     * <p>
     * {@code GET /api/prospections/{id}/devis} n'a pas besoin de ce filtre : il est déjà couvert
     * par le contrôle d'accès sur la prospection porteuse ({@code PerimetreInterceptor}).
     */
    public Specification<StlQuote> filtreDevis() {
        Optional<List<UUID>> perimetre = perimetreCourant();
        return (root, query, cb) -> {
            Join<StlQuote, CfgTranche> tranche = root.join("cfgTranche", JoinType.LEFT);
            Join<CfgTranche, CfgProject> projet = tranche.join("cfgProject", JoinType.LEFT);
            return combine(VisibiliteEntites.depuisProjet(projet, cb), projet, perimetre, cb);
        };
    }

    /**
     * Prédicat de périmètre sur une interaction ({@code ComActionMessage}), pour les blocs
     * "interactions" de {@code GET /api/hotline/recherche}.
     *
     * <p><b>Deux chemins de résolution, dans cet ordre : prospection puis dossier.</b> Une première
     * version ne lisait que {@code ComProspectionId} et concluait "projet non résolu ⇒ visible"
     * (§6.4) dès qu'il était nul — or {@code ComActionMessage} porte aussi un {@code ComFolderId}
     * d'où le projet se résout parfaitement (mesuré : 13 lignes de {@code yesRems} ont exactement
     * cette forme, et les 13 résolvent un projet). La règle §6.4 ne s'applique qu'une fois les
     * <b>deux</b> chemins épuisés, ce qu'exprime ici le {@code coalesce}.
     *
     * <p>Toutes les jointures sont en {@code LEFT} : une interaction rattachée ni à une prospection
     * ni à un dossier doit rester visible, pas disparaître dans une jointure interne.
     */
    public Specification<ComActionMessage> filtreInteractions() {
        Optional<List<UUID>> perimetre = perimetreCourant();
        return (root, query, cb) -> {
            Join<ComActionMessage, ComProspection> prospection = root.join("comProspection", JoinType.LEFT);
            Join<ComProspection, CfgTranche> trancheProspection = prospection.join("stkHierarchy", JoinType.LEFT);
            Join<CfgTranche, CfgProject> projetProspection = trancheProspection.join("cfgProject", JoinType.LEFT);

            Join<ComActionMessage, ComFolder> dossier = root.join("comFolder", JoinType.LEFT);
            Join<ComFolder, StkItem> article = dossier.join("stkItem", JoinType.LEFT);
            Join<StkItem, CfgTranche> trancheDossier = article.join("stkHierarchy", JoinType.LEFT);
            Join<CfgTranche, CfgProject> projetDossier = trancheDossier.join("cfgProject", JoinType.LEFT);

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
     * Assemble les deux restrictions autour d'une <b>même</b> chaîne de jointures : la visibilité
     * par entité (toujours), puis le périmètre commercial (seulement si l'appelant est restreint).
     *
     * <p>Volontairement pas un {@code Specification.and(...)} de deux spécifications : chacune
     * reposerait ses propres {@code LEFT JOIN} sur le même chemin, et la requête SQL porterait la
     * chaîne article → tranche → projet en double.
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
     * Point de contrôle unique du périmètre en lecture par id — appelé par
     * {@code PerimetreInterceptor} pour <b>toute</b> route sous {@code /api/dossiers/{id}/…}
     * (échéancier, règlements, pièces jointes, générateurs de documents…), pas seulement le détail.
     * Sans ce contrôle, le périmètre se contournerait en énumérant les ids.
     * <p>
     * Une ressource hors périmètre est indiscernable d'une ressource inexistante : 404, jamais 403.
     * N'exécute aucune requête lorsque l'appelant n'est pas restreint.
     */
    public void verifierAccesDossier(UUID dossierId) {
        verifier(dossierId,
                projetIds -> folderRepository.existsDansPerimetre(dossierId, projetIds),
                () -> folderRepository.existsVisible(dossierId),
                "Dossier introuvable: " + dossierId);
    }

    /** Idem {@link #verifierAccesDossier} pour une prospection ou une pré-prospection. */
    public void verifierAccesProspection(UUID prospectionId) {
        verifier(prospectionId,
                projetIds -> prospectionRepository.existsDansPerimetre(prospectionId, projetIds),
                () -> prospectionRepository.existsVisible(prospectionId),
                "Prospection introuvable: " + prospectionId);
    }

    /** Idem {@link #verifierAccesDossier} pour un encaissement (règlement). */
    public void verifierAccesEncaissement(UUID encaissementId) {
        verifier(encaissementId,
                projetIds -> settlementRepository.existsDansPerimetre(encaissementId, projetIds),
                () -> settlementRepository.existsVisible(encaissementId),
                "Encaissement introuvable: " + encaissementId);
    }

    /**
     * Le squelette commun des trois contrôles ci-dessus : une requête, toujours, mais laquelle
     * dépend de l'appelant.
     *
     * <p><b>Ce contrôle n'est plus conditionnel.</b> Il ne s'exécutait autrefois que pour un compte
     * restreint par un périmètre ; la visibilité par entité juridique, elle, s'applique à tout le
     * monde ({@code VisibiliteEntites}), donc une ressource doit être vérifiée même quand aucun
     * périmètre n'est en jeu. Sans cela le filtre d'entité ne tiendrait que sur les listes et se
     * contournerait en énumérant les ids — détail du dossier, échéancier, pièces jointes et
     * générateurs de documents inclus.
     *
     * <p>La variante « dans le périmètre » porte déjà les deux restrictions, d'où l'alternative
     * plutôt qu'un cumul.
     */
    private void verifier(UUID id, java.util.function.Predicate<List<UUID>> dansPerimetre,
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
     * Refuse (403) une écriture qui rattacherait une ressource à un projet hors périmètre. Ici la
     * ressource est nommée par l'appelant lui-même : il n'y a rien à cacher, un 403 explicite vaut
     * mieux qu'un 404 trompeur. Sans ce contrôle la frontière serait purement décorative.
     * <p>
     * Un projet non résolu ({@code null}) est accepté — cohérent avec §6.4.
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

    /**
     * {@code GET /api/utilisateurs/{admUserId}/projets} — les projets actuellement affectés à ce
     * compte. Ne remonte que les lignes de portée projet : les habilitations société / tranche /
     * PrjProject du légataire ne sont ni exposées ni modifiables ici.
     */
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
     * {@code PUT /api/utilisateurs/{admUserId}/projets} — remplacement intégral du jeu
     * d'affectations <b>projet</b> de ce compte.
     *
     * <p><b>Contrainte impérative — le point le plus dangereux de cette feature.</b> La table est
     * polymorphe, et une même ligne peut porter <b>plusieurs</b> portées à la fois : mesuré sur
     * {@code yesRems}, 2 lignes du compte {@code Mkamara} portent à la fois {@code CfgCompanyId} et
     * {@code CfgProjectId}. Supprimer toute ligne dont {@code CfgProjectId} est non nul — ce que
     * faisait la première version — effaçait donc au passage des habilitations société de
     * l'application de bureau légataire, toujours en service. La règle correcte distingue les deux
     * formes :
     * <ul>
     *   <li><b>ligne à portée projet exclusive</b> (aucune autre colonne renseignée) → supprimée ;</li>
     *   <li><b>ligne mixte</b> (porte aussi une société, une tranche ou un {@code PrjProject}) →
     *       seul {@code CfgProjectId} est mis à {@code NULL}, la ligne survit avec ses autres
     *       portées.</li>
     * </ul>
     * Verrouillé par {@code putProjets_preserveLaPorteeSocieteDuneLigneMixte}.
     *
     * <p>{@code projetIds} <b>doit être présent</b> : un corps {@code {}} ou
     * {@code {"projetIds": null}} est rejeté en 400 plutôt qu'interprété comme une liste vide. Une
     * levée de restriction doit être explicite ({@code {"projetIds": []}}) — sinon un corps tronqué
     * par l'UI d'administration rendrait silencieusement un compte non restreint. Une liste
     * explicitement vide, elle, est valide et signifie "plus aucune restriction" (§6.3), jamais
     * "ne voit rien".
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

    /**
     * Nombre de projets affectés par compte, pour la colonne {@code nombreProjets} de
     * {@code GET /api/utilisateurs} — agrégé côté serveur, une seule requête pour toute la page.
     */
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

    /**
     * true si cette ligne d'habilitation porte, en plus du projet, au moins une portée appartenant
     * à l'application de bureau légataire — auquel cas elle doit survivre au remplacement du
     * périmètre projet (voir {@link #remplacerProjets}).
     */
    private static boolean porteUneAutrePortee(AdmUserHabilitation habilitation) {
        return habilitation.getCfgCompany() != null
                || habilitation.getCfgTranche() != null
                || habilitation.getPrjProjectId() != null;
    }

    private static ProjetPerimetreDto toProjetDto(CfgProject projet) {
        return new ProjetPerimetreDto(projet.getId(), projet.getCode(), projet.getDescription());
    }
}
