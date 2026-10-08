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
import jakarta.persistence.criteria.JoinType;
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

/** « Qui appelle ? » : fiches et historique d'un tiers retrouvé par téléphone ou par nom. */
@Service
@Transactional(readOnly = true)
public class HotlineService {

    /** Vue d'ensemble en un aller-retour ; la pagination complète est sous {@code /api/prospects/{id}/…}. */
    static final int HISTORIQUE_LIMIT = 50;

    /**
     * Un nom remonte des centaines de tiers et chaque fiche coûte cinq requêtes : plafond, et
     * {@code tronque} invite à préciser. Le téléphone (0 à 2 fiches) n'est pas plafonné.
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
        // Périmètre DANS la requête, avant le plafond : filtrer la page chargée évincerait les lignes
        // du périmètre (FEATURE-perimetre-commercial.md §9ter.2).
        List<ComProspection> preProspections = perimetreService.perimetreCourant()
                .map(projetIds -> prospectionRepository.findByNormalizedTempTelDansPerimetre(
                        normalise, projetIds, preProspectionsPage))
                .orElseGet(() -> prospectionRepository.findByNormalizedTempTel(normalise, preProspectionsPage))
                .getContent();

        return assembler("TELEPHONE", telephoneBrut, normalise, null, tiers, preProspections, false);
    }

    /** Une ligne de plus que le plafond : de quoi savoir que c'est tronqué sans tout compter. */
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

        // Même périmètre que par téléphone, sinon le nom montrerait ce que le numéro cache.
        List<ComProspection> preProspections = perimetreService.perimetreCourant()
                .map(projetIds -> prospectionRepository.rechercherPreProspectionsParNomDansPerimetre(
                        motif, projetIds, HISTORIQUE_LIMIT))
                .orElseGet(() -> prospectionRepository.rechercherPreProspectionsParNom(motif, HISTORIQUE_LIMIT));

        return assembler("NOM", null, null, nomBrut, tiers, preProspections, tronque);
    }

    /** Commun aux deux recherches, dont l'exclusion des doublons ci-dessous. */
    private HotlineResultDto assembler(String critere, String telephoneBrut, String telephoneNormalise,
                                       String nomRecherche, List<CfgTier> tiers,
                                       List<ComProspection> preProspections, boolean tronque) {
        List<HotlineFicheDto> fiches = tiers.stream().map(t -> toFiche(t.getId())).toList();
        Set<UUID> tierIds = tiers.stream().map(CfgTier::getId).collect(Collectors.toSet());

        List<ProspectionListItemDto> appelsSansFiche = preProspections.stream()
                // Une pré-prospection peut avoir été rattachée depuis à l'un des tiers trouvés.
                .filter(p -> p.getCfgProspect() == null || !tierIds.contains(p.getCfgProspect().getId()))
                .map(HotlineService::toProspectionListItem)
                .toList();

        return new HotlineResultDto(critere, telephoneBrut, telephoneNormalise, nomRecherche, tronque,
                !fiches.isEmpty(), fiches, appelsSansFiche);
    }

    /**
     * Blocs plafonnés : périmètre dans chaque requête, jamais sur le résultat (§9ter.2). Prospections,
     * dossiers, règlements filtrés par {@code ProspectSubResourcesService} ; interactions ici.
     */
    private HotlineFicheDto toFiche(UUID tierId) {
        Pageable recentSorted = PageRequest.of(0, HISTORIQUE_LIMIT, Sort.by(Sort.Direction.DESC, "docDate"));
        Pageable recent = PageRequest.of(0, HISTORIQUE_LIMIT);

        // Cascade en lecture, sans réécrire CfgTierId : une interaction saisie avant « Créer prospect »
        // n'a que sa prospection. Jointure LEFT, sinon les interactions sans prospection disparaissent.
        // Voir FIX-tracabilite-historique-hotline.md §5.
        Specification<ComActionMessage> interactionsDuTier = Specification
                .<ComActionMessage>where((root, query, cb) -> cb.or(
                        cb.equal(root.get("cfgTier").get("id"), tierId),
                        cb.equal(root.join("comProspection", JoinType.LEFT).get("cfgProspect").get("id"), tierId)))
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

    /** Champs d'alerte nuls ici : seules les listes de prospections les chargent (requête groupée). */
    private static ProspectionListItemDto toProspectionListItem(ComProspection p) {
        return ProspectionService.toListItem(p);
    }

    private static InteractionDto toInteractionDto(ComActionMessage m) {
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
                // Employé, puis UserName (FIX-auteur-interactions.md).
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
