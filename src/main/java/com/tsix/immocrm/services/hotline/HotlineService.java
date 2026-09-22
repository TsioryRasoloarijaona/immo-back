package com.tsix.immocrm.services.hotline;

import com.tsix.immocrm.dto.hotline.HotlineFicheDto;
import com.tsix.immocrm.dto.hotline.HotlineResultDto;
import com.tsix.immocrm.dto.prospection.InteractionDto;
import com.tsix.immocrm.dto.prospection.ProspectionListItemDto;
import com.tsix.immocrm.entities.cfg.CfgProject;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComActionMessage;
import com.tsix.immocrm.entities.com.ComProspection;
import com.tsix.immocrm.exceptions.InvalidRequestException;
import com.tsix.immocrm.repositories.cfg.CfgTierRepository;
import com.tsix.immocrm.repositories.com.ComActionMessageRepository;
import com.tsix.immocrm.repositories.com.ComProspectionRepository;
import com.tsix.immocrm.services.perimetre.PerimetreCommercialService;
import com.tsix.immocrm.services.prospect.ProspectService;
import com.tsix.immocrm.services.prospection.ContexteInteraction;
import com.tsix.immocrm.services.prospection.ProspectionService;
import com.tsix.immocrm.services.prospect.ProspectSubResourcesService;
import com.tsix.immocrm.utils.PhoneNumberUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Backs {@code GET /api/hotline/recherche} — the "who is calling?" lookup a hotline agent runs
 * the instant a caller gives their phone number: are they already known, and if so, what is
 * their entire history (general info, prospections, dossiers, règlements, interactions,
 * statut history)? See {@link HotlineResultDto} for the full response shape.
 */
@Service
@Transactional(readOnly = true)
public class HotlineService {

    /**
     * Cap applied to every history list embedded in a {@link HotlineFicheDto} (prospections,
     * dossiers, règlements, interactions, historique) — this endpoint is meant for an instant,
     * single-round-trip overview while someone is on the phone, not exhaustive pagination. Once
     * a tier is identified via {@code tier.id}, the dedicated {@code /api/prospects/{id}/...}
     * endpoints give full, paginated access to any of these lists.
     */
    static final int HISTORIQUE_LIMIT = 50;

    /**
     * Nombre maximal de fiches construites pour une recherche par NOM.
     *
     * <p>Un numéro identifie une personne ; un nom en identifie des centaines — « MOHAMED »
     * remonte 555 lignes en base réelle. Comme chaque fiche déclenche cinq requêtes, servir
     * l'ensemble coûterait ~2 775 requêtes pour une frappe. Ce plafond borne le pire cas à 100,
     * et {@code tronque} avertit l'agent qu'il doit préciser sa recherche.
     *
     * <p>La recherche par téléphone n'est délibérément <b>pas</b> plafonnée : elle remonte 0 à 2
     * fiches (un couple peut partager un fixe), et lui imposer une limite changerait un
     * comportement qui n'a jamais posé problème.
     */
    public static final int FICHES_MAX_PAR_NOM = 20;

    /** En deçà, une seule lettre ramènerait la moitié de la base pour n'en afficher que 20. */
    static final int LONGUEUR_MIN_NOM = 2;

    private final CfgTierRepository tierRepository;
    private final ComProspectionRepository prospectionRepository;
    private final ComActionMessageRepository actionMessageRepository;
    private final ProspectService prospectService;
    private final ProspectSubResourcesService subResourcesService;
    private final PerimetreCommercialService perimetreService;

    public HotlineService(CfgTierRepository tierRepository,
                           ComProspectionRepository prospectionRepository,
                           ComActionMessageRepository actionMessageRepository,
                           ProspectService prospectService,
                           ProspectSubResourcesService subResourcesService,
                           PerimetreCommercialService perimetreService) {
        this.perimetreService = perimetreService;
        this.tierRepository = tierRepository;
        this.prospectionRepository = prospectionRepository;
        this.actionMessageRepository = actionMessageRepository;
        this.prospectService = prospectService;
        this.subResourcesService = subResourcesService;
    }

    /** {@code GET /api/hotline/recherche?telephone=...} — see {@link HotlineResultDto}. */
    public HotlineResultDto rechercherParTelephone(String telephoneBrut) {
        if (!StringUtils.hasText(telephoneBrut)) {
            throw new InvalidRequestException("Le paramètre 'telephone' est requis");
        }
        String normalise = PhoneNumberUtils.normalize(telephoneBrut);
        if (normalise.isEmpty()) {
            throw new InvalidRequestException("Le paramètre 'telephone' ne contient aucun chiffre exploitable");
        }

        List<CfgTier> tiers = tierRepository.findByNormalizedPhone(normalise);

        Pageable preProspectionsPage = PageRequest.of(0, HISTORIQUE_LIMIT);
        // Périmètre commercial : le filtre est appliqué DANS la requête, avant le plafond de 50 —
        // filtrer la page déjà chargée reviendrait à évincer les lignes du périmètre au profit de
        // celles qui l'ont saturée (voir §9ter.2 et la doc du repository).
        List<ComProspection> preProspections = perimetreService.perimetreCourant()
                .map(projetIds -> prospectionRepository.findByNormalizedTempTelDansPerimetre(
                        normalise, projetIds, preProspectionsPage))
                .orElseGet(() -> prospectionRepository.findByNormalizedTempTel(normalise, preProspectionsPage))
                .getContent();

        return assembler("TELEPHONE", telephoneBrut, normalise, null, tiers, preProspections, false);
    }

    /**
     * {@code GET /api/hotline/recherche?nom=...} — même résultat, autre porte d'entrée : l'agent
     * a le nom mais pas le numéro (appel masqué, ou l'interlocuteur se présente).
     *
     * <p><b>Plafonnée, contrairement à la recherche par téléphone</b>, pour la raison expliquée
     * sur {@link #FICHES_MAX_PAR_NOM}. On demande volontairement une ligne de plus que le
     * plafond : c'est ce qui permet de dire à l'agent que sa recherche a été coupée, sans avoir
     * à compter la totalité des correspondances.
     */
    public HotlineResultDto rechercherParNom(String nomBrut) {
        if (!StringUtils.hasText(nomBrut)) {
            throw new InvalidRequestException("Le paramètre 'nom' est requis");
        }
        String terme = nomBrut.trim();
        if (terme.length() < LONGUEUR_MIN_NOM) {
            throw new InvalidRequestException(
                    "Le paramètre 'nom' doit contenir au moins " + LONGUEUR_MIN_NOM + " caractères");
        }
        String motif = "%" + terme + "%";

        List<CfgTier> trouves = tierRepository.rechercherParNom(motif, FICHES_MAX_PAR_NOM + 1);
        boolean tronque = trouves.size() > FICHES_MAX_PAR_NOM;
        List<CfgTier> tiers = tronque ? trouves.subList(0, FICHES_MAX_PAR_NOM) : trouves;

        // Même exigence de périmètre que par téléphone : sans cette variante, la recherche par
        // nom deviendrait un contournement — on verrait par le nom ce que le numéro cache.
        List<ComProspection> preProspections = perimetreService.perimetreCourant()
                .map(projetIds -> prospectionRepository.rechercherPreProspectionsParNomDansPerimetre(
                        motif, projetIds, HISTORIQUE_LIMIT))
                .orElseGet(() -> prospectionRepository.rechercherPreProspectionsParNom(motif, HISTORIQUE_LIMIT));

        return assembler("NOM", null, null, nomBrut, tiers, preProspections, tronque);
    }

    /**
     * Tout ce que les deux recherches ont en commun : construire les fiches, écarter les
     * pré-prospections déjà rattachées à l'une d'elles, assembler la réponse.
     *
     * <p>Cette partie était mêlée à la recherche par téléphone. L'en extraire est ce qui fait
     * hériter la recherche par nom des règles déjà éprouvées — en particulier l'exclusion des
     * doublons ci-dessous, qu'on aurait fatalement oubliée en dupliquant la méthode.
     */
    private HotlineResultDto assembler(String critere, String telephoneBrut, String telephoneNormalise,
                                       String nomRecherche, List<CfgTier> tiers,
                                       List<ComProspection> preProspections, boolean tronque) {
        List<HotlineFicheDto> fiches = tiers.stream().map(t -> toFiche(t.getId())).toList();
        Set<UUID> tierIds = tiers.stream().map(CfgTier::getId).collect(Collectors.toSet());

        List<ProspectionListItemDto> appelsSansFiche = preProspections.stream()
                // A match can, on messy legacy data, already belong to a prospection that's since
                // been linked to one of the tiers above — don't show it twice.
                .filter(p -> p.getCfgProspect() == null || !tierIds.contains(p.getCfgProspect().getId()))
                .map(HotlineService::toProspectionListItem)
                .toList();

        return new HotlineResultDto(critere, telephoneBrut, telephoneNormalise, nomRecherche, tronque,
                !fiches.isEmpty(), fiches, appelsSansFiche);
    }

    /**
     * Chaque bloc de la fiche est plafonné à {@link #HISTORIQUE_LIMIT} lignes. Le périmètre
     * commercial doit donc être appliqué <b>dans chaque requête</b>, jamais sur le résultat :
     * filtrer après coup une page déjà plafonnée évince les lignes du périmètre (celles hors
     * périmètre ayant consommé la page) au lieu d'évincer celles hors périmètre — un tiers avec 50
     * interactions récentes sur un autre projet et une seule sur le mien renvoyait une liste vide.
     * Les prospections / dossiers / règlements passent par {@code ProspectSubResourcesService}, déjà
     * filtré par {@code Specification} ; les interactions sont filtrées ici par
     * {@code filtreInteractions()}, qui résout le projet par la prospection <i>puis</i> par le
     * dossier.
     */
    private HotlineFicheDto toFiche(UUID tierId) {
        Pageable recentSorted = PageRequest.of(0, HISTORIQUE_LIMIT, Sort.by(Sort.Direction.DESC, "docDate"));
        Pageable recent = PageRequest.of(0, HISTORIQUE_LIMIT);

        Specification<ComActionMessage> interactionsDuTier = Specification
                .<ComActionMessage>where((root, query, cb) -> cb.equal(root.get("cfgTier").get("id"), tierId))
                .and(perimetreService.filtreInteractions());

        return new HotlineFicheDto(
                prospectService.findById(tierId),
                subResourcesService.prospections(tierId, recentSorted).getContent(),
                subResourcesService.dossiers(tierId, recentSorted).getContent(),
                subResourcesService.reglements(tierId, recent).getContent(),
                actionMessageRepository.findAll(interactionsDuTier, recentSorted).stream()
                        .map(HotlineService::toInteractionDto).toList(),
                subResourcesService.historique(tierId, recent).getContent()
        );
    }

    /**
     * Délègue au mappeur commun de {@code ProspectionService} : ce bloc existait ici en copie
     * identique, et c'est cette duplication qui l'a fait diverger quand la liste a gagné le
     * contact et l'alerte. Les champs d'alerte restent nuls ici — ils ne sont résolus que par
     * les listes de prospections, qui les chargent en une requête groupée.
     */
    private static ProspectionListItemDto toProspectionListItem(ComProspection p) {
        return ProspectionService.toListItem(p);
    }

    private static InteractionDto toInteractionDto(ComActionMessage m) {
        // Même contexte projet/tranche que GET /api/prospections/{id}/interactions : la règle
        // est partagée pour que les deux endpoints ne divergent pas.
        CfgTranche tranche = ContexteInteraction.tranche(m);
        CfgProject projet = tranche != null ? tranche.getCfgProject() : null;
        return new InteractionDto(
                m.getId(),
                m.getDocDate(),
                // AlertMessage porte le texte du commercial ; Description n'est qu'un libellé
                // « date + auteur ». Voir instructions/FIX-interaction-note.md.
                m.getAlertMessage(),
                m.getDescription(),
                m.getIsIn(),
                m.getCfgEmployee() != null ? m.getCfgEmployee().getId() : null,
                // Même règle d'auteur que le journal de prospection — employé, puis UserName :
                // sans le repli, un quart des interactions s'affichait « Système ».
                ProspectionService.nomAuteur(m),
                m.getComActionMessageType() != null ? m.getComActionMessageType().getId() : null,
                m.getComActionMessageType() != null ? m.getComActionMessageType().getDescription() : null,
                m.getComActionMessageSubCategory() != null ? m.getComActionMessageSubCategory().getId() : null,
                m.getComActionMessageSubCategory() != null ? m.getComActionMessageSubCategory().getDescription() : null,
                m.getAlertValidityDate(),
                m.getAdmEvent() != null ? m.getAdmEvent().getStartHour() : null,
                m.getComActionMessageCategory() != null ? m.getComActionMessageCategory().getId() : null,
                m.getComActionMessageCategory() != null ? m.getComActionMessageCategory().getDescription() : null,
                tranche != null ? tranche.getId() : null,
                tranche != null ? tranche.getCode() : null,
                tranche != null ? tranche.getDescription() : null,
                projet != null ? projet.getId() : null,
                projet != null ? projet.getCode() : null,
                projet != null ? projet.getDescription() : null
        );
    }
}
