package com.tsix.immocrm.services.equipe;

import com.tsix.immocrm.domain.TypeActionRecente;
import com.tsix.immocrm.dto.equipe.AffectationsEquipeDto.MembreAffectationDto;
import com.tsix.immocrm.dto.equipe.DistribuerRequest;
import com.tsix.immocrm.dto.equipe.ModifierAffectationsRequest;
import com.tsix.immocrm.dto.equipe.ReaffecterRequest;
import com.tsix.immocrm.dto.equipe.ResultatLotDto;
import com.tsix.immocrm.dto.equipe.ResultatLotDto.LigneLotDto;
import com.tsix.immocrm.dto.equipe.RepartirRequest;
import com.tsix.immocrm.dto.equipe.ResultatRepartitionDto;
import com.tsix.immocrm.dto.equipe.ResultatRepartitionDto.AttributionDto;
import com.tsix.immocrm.dto.equipe.ResultatRepartitionDto.NonAttribueeDto;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.adm.AdmUserHabilitation;
import com.tsix.immocrm.entities.cfg.CfgProject;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComProspection;
import com.tsix.immocrm.exceptions.ConflictException;
import com.tsix.immocrm.exceptions.ForbiddenException;
import com.tsix.immocrm.exceptions.InvalidRequestException;
import com.tsix.immocrm.exceptions.ResourceNotFoundException;
import com.tsix.immocrm.repositories.adm.AdmUserHabilitationRepository;
import com.tsix.immocrm.repositories.adm.AdmUserRepository;
import com.tsix.immocrm.repositories.cfg.CfgProjectRepository;
import com.tsix.immocrm.repositories.cfg.CfgTierRepository;
import com.tsix.immocrm.repositories.com.ComProspectionRepository;
import com.tsix.immocrm.services.perimetre.PerimetreCommercialService;
import com.tsix.immocrm.services.prospection.DiscriminantPreProspection;
import com.tsix.immocrm.services.prospection.HistoriqueProspection;
import com.tsix.immocrm.services.prospection.ProspectionService;
import com.tsix.immocrm.utils.TierNomFormatter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Écritures de « Mon équipe », directement dans les tables d'OMRAN. Chaque lot est validé en entier (403 puis 409)
 * avant la première écriture, dans une seule transaction : un lot partiellement invalide n'écrit rien.
 */
@Service
@Transactional
public class EquipeEcrituresService {

    private static final String LIGNE = "Ligne ";

    private final EquipeCommercialeService equipeService;
    private final HistoriqueProspection historique;
    private final ComProspectionRepository prospectionRepository;
    private final CfgTierRepository tierRepository;
    private final CfgProjectRepository projectRepository;
    private final AdmUserRepository userRepository;
    private final AdmUserHabilitationRepository habilitationRepository;
    private final RepartitionLeadsService repartition;
    private final ADistribuerService aDistribuer;

    public EquipeEcrituresService(EquipeCommercialeService equipeService,
                                  ADistribuerService aDistribuer,
                                  HistoriqueProspection historique,
                                  ComProspectionRepository prospectionRepository,
                                  CfgTierRepository tierRepository,
                                  CfgProjectRepository projectRepository,
                                  AdmUserRepository userRepository,
                                  AdmUserHabilitationRepository habilitationRepository,
                                  RepartitionLeadsService repartition) {
        this.equipeService = equipeService;
        this.aDistribuer = aDistribuer;
        this.repartition = repartition;
        this.historique = historique;
        this.prospectionRepository = prospectionRepository;
        this.tierRepository = tierRepository;
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
        this.habilitationRepository = habilitationRepository;
    }

    // ---------------------------------------------------------------- distribuer

    /**
     * Comme OMRAN : le commercial est posé sur la pré-prospection, qui reste une pré-prospection (ni prospect ni
     * tranche exigés) ; la transformer reste le geste du commercial (FEATURE-equipe-commerciale.md §9 D1).
     */
    public ResultatLotDto distribuer(DistribuerRequest request) {
        List<ComProspection> lignes = lignesDemandees(request.preProspectionIds());
        exiger(request.commercialId(), "commercialId est requis");
        PerimetreEquipe perimetre = equipeService.perimetre();

        verifierPerimetre(lignes, perimetre);
        for (ComProspection ligne : lignes) {
            ProspectionService.verifierEncorePreProspection(ligne);
            verifierOuverte(ligne);
            perimetre.destinataire(request.commercialId(), projetDe(ligne.getStkHierarchy()), ligne.getCode());
        }
        return ecrire(lignes, request.commercialId(), "");
    }

    // ---------------------------------------------------------------- réaffecter

    /** Change {@code CfgCommercialId} seul : le commercial référent du prospect n'est pas touché. */
    public ResultatLotDto reaffecter(ReaffecterRequest request) {
        List<ComProspection> lignes = lignesDemandees(request.prospectionIds());
        exiger(request.commercialId(), "commercialId est requis");
        PerimetreEquipe perimetre = equipeService.perimetre();

        verifierPerimetre(lignes, perimetre);
        for (ComProspection ligne : lignes) {
            if (DiscriminantPreProspection.estPreProspection(ligne)) {
                throw new ConflictException(LIGNE + ligne.getCode()
                        + " : une pré-prospection se distribue, elle ne se réaffecte pas.");
            }
            verifierOuverte(ligne);
            perimetre.destinataire(request.commercialId(), projetDe(ligne.getStkHierarchy()), ligne.getCode());
        }
        String motif = StringUtils.hasText(request.motif()) ? " — " + request.motif().strip() : "";
        return ecrire(lignes, request.commercialId(), motif);
    }

    // ---------------------------------------------------------------- répartir

    /**
     * Répartition automatique d'un lot (FEATURE-repartition-notifications.md §3.2) : même contrôle de périmètre que
     * distribuer (403 puis 409), puis {@link RepartitionLeadsService#decider} ligne par ligne ; une ligne déjà
     * attribuée n'est jamais reprise. {@code simulation} (défaut vrai) n'écrit rien.
     */
    public ResultatRepartitionDto repartir(RepartirRequest request) {
        List<ComProspection> lignes = lotARepartir(request);
        boolean simulation = !Boolean.FALSE.equals(request.simulation());
        PerimetreEquipe perimetre = equipeService.perimetre();

        verifierPerimetre(lignes, perimetre);
        for (ComProspection ligne : lignes) {
            ProspectionService.verifierEncorePreProspection(ligne);
            verifierOuverte(ligne);
        }
        RepartitionLeadsService.Lot lot = repartition.nouveauLot();
        List<RepartitionLeadsService.Decision> decisions = new ArrayList<>();
        List<AttributionDto> attributions = new ArrayList<>();
        List<NonAttribueeDto> nonAttribuees = new ArrayList<>();
        List<ComProspection> ecrites = new ArrayList<>();
        for (ComProspection ligne : lignes) {
            RepartitionLeadsService.Decision d = repartition.decider(ligne, lot);
            decisions.add(d);
            if (!d.estAttribuee()) {
                nonAttribuees.add(new NonAttribueeDto(ligne.getId(), ligne.getCode(), d.raison()));
                continue;
            }
            attributions.add(new AttributionDto(ligne.getId(), ligne.getCode(), d.commercialId(), d.commercialNom(),
                    d.motif()));
            if (!simulation) {
                repartition.appliquer(ligne, d);
                ecrites.add(ligne);
            }
        }
        if (!simulation) {
            repartition.publier(perimetre.appelantId(), ecrites, List.of());
        }
        return new ResultatRepartitionDto(simulation, attributions, nonAttribuees,
                RepartitionLeadsService.parCommercial(decisions));
    }

    /** Exactement un des deux : les ids cochés, ou toutes les lignes du filtre de la carte (§3.3). */
    private List<ComProspection> lotARepartir(RepartirRequest request) {
        boolean parIds = !demandes(request.preProspectionIds()).isEmpty();
        if (parIds == (request.filtre() != null)) {
            throw new InvalidRequestException("Donner soit preProspectionIds (non vide), soit filtre — pas les deux.");
        }
        return parIds ? lignesDemandees(request.preProspectionIds()) : aDistribuer.toutesLesLignes(request.filtre());
    }

    private static void verifierPerimetre(List<ComProspection> lignes, PerimetreEquipe perimetre) {
        for (ComProspection ligne : lignes) {
            perimetre.verifierProjet(projetDe(ligne.getStkHierarchy()), LIGNE + ligne.getCode());
        }
    }

    private static void verifierOuverte(ComProspection ligne) {
        if (Boolean.TRUE.equals(ligne.getIsClosed())) {
            throw new ConflictException(LIGNE + ligne.getCode() + " : ligne clôturée.");
        }
    }

    /**
     * Seule écriture de distribuer et réaffecter : {@code CfgCommercialId} + une ligne d'historique (« Commercial
     * assigné : B » ou « Commercial réaffecté : A → B »). Le référent du prospect n'est pas touché ; ligne déjà au
     * destinataire : ignorée.
     */
    private ResultatLotDto ecrire(List<ComProspection> lignes, UUID commercialId, String motif) {
        CfgTier nouveau = tierRepository.findById(commercialId)
                .orElseThrow(() -> new InvalidRequestException("Commercial (CfgTier) introuvable: " + commercialId));
        String nouveauNom = TierNomFormatter.nom(nouveau);
        List<LigneLotDto> ecrites = new ArrayList<>();
        List<ComProspection> modifiees = new ArrayList<>();
        for (ComProspection ligne : lignes) {
            CfgTier ancien = ligne.getCfgCommercial();
            if (ancien != null && ancien.getId().equals(nouveau.getId())) {
                continue;
            }
            String ancienNom = TierNomFormatter.nom(ancien);
            ligne.setCfgCommercial(nouveau);
            prospectionRepository.save(ligne);
            if (ancien == null) {
                historique.enregistrerHistorique(ligne.getId(), TypeActionRecente.COMMERCIAL_ASSIGNE,
                        nouveauNom + motif);
            } else {
                historique.enregistrerHistorique(ligne.getId(), TypeActionRecente.COMMERCIAL_REAFFECTE,
                        ancienNom + " → " + nouveauNom + motif);
            }
            ecrites.add(new LigneLotDto(ligne.getId(), ligne.getCode(), ancienNom));
            modifiees.add(ligne);
        }
        repartition.publier(PerimetreCommercialService.admUserIdCourant().orElse(null), modifiees, List.of());
        return new ResultatLotDto(ecrites.size(), nouveau.getId(), nouveauNom, ecrites);
    }

    // ---------------------------------------------------------------- affectations

    /**
     * Ajout / retrait de lignes {@code AdmUserHabilitation} de portée projet ; une ligne mixte garde sa portée du
     * légataire. Retirer le dernier projet ⇒ 409 : liste vide = « tous les projets » (§6.3 du périmètre).
     */
    public MembreAffectationDto modifierAffectations(UUID admUserId, ModifierAffectationsRequest request) {
        AdmUser compte = userRepository.findById(admUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable: " + admUserId));
        PerimetreEquipe perimetre = equipeService.perimetre();
        MembreEquipe cible = Stream.concat(perimetre.membres().stream(), perimetre.horsEquipe().stream())
                .filter(m -> m.admUserId().equals(admUserId))
                .findFirst()
                .orElseThrow(() -> new ForbiddenException(
                        "Le compte " + compte.getCode() + " n'est pas un commercial dont vous gérez les affectations."));
        if (!perimetre.exempte() && cible.estProtege()) {
            throw new ForbiddenException("Les affectations de " + cible.nom() + " sont gérées par l'administrateur.");
        }

        Set<UUID> ajouter = demandes(request == null ? null : request.ajouter());
        Set<UUID> retirer = demandes(request == null ? null : request.retirer());
        if (ajouter.isEmpty() && retirer.isEmpty()) {
            throw new InvalidRequestException("ajouter ou retirer doit contenir au moins un projet.");
        }
        Map<UUID, CfgProject> projets = projetsConnus(ajouter, retirer);
        Stream.concat(ajouter.stream(), retirer.stream())
                .forEach(id -> perimetre.verifierProjet(id, "Projet " + projets.get(id).getCode()));

        Set<UUID> finaux = new LinkedHashSet<>(cible.projetIds());
        finaux.addAll(ajouter);
        finaux.removeAll(retirer);
        if (finaux.isEmpty() && !cible.projetIds().isEmpty()) {
            throw new ConflictException("Impossible de retirer le dernier projet de " + cible.nom()
                    + " : sans projet, il verrait tous les projets.");
        }

        // Écrire exactement la différence avant / après : un projet à la fois ajouté et retiré est retiré.
        retirerProjets(admUserId, cible.projetIds().stream().filter(id -> !finaux.contains(id)).toList());
        finaux.stream()
                .filter(id -> !cible.projetIds().contains(id))
                .forEach(id -> ajouterProjet(compte, projets.get(id)));

        MembreEquipe apres = new MembreEquipe(cible.admUserId(), cible.code(), cible.nom(), cible.employeId(),
                cible.roles(), Set.copyOf(finaux));
        return EquipeCommercialeService.versAffectation(apres, perimetre);
    }

    private Map<UUID, CfgProject> projetsConnus(Set<UUID> ajouter, Set<UUID> retirer) {
        Set<UUID> tous = new HashSet<>(ajouter);
        tous.addAll(retirer);
        Map<UUID, CfgProject> projets = projectRepository.findAllById(tous).stream()
                .collect(Collectors.toMap(CfgProject::getId, Function.identity()));
        List<UUID> inconnus = tous.stream().filter(id -> !projets.containsKey(id)).toList();
        if (!inconnus.isEmpty()) {
            throw new InvalidRequestException("Projet(s) inconnu(s): " + inconnus);
        }
        return projets;
    }

    private void retirerProjets(UUID admUserId, List<UUID> projetIds) {
        if (projetIds.isEmpty()) {
            return;
        }
        for (AdmUserHabilitation h : habilitationRepository.findByAdmUserIdAndProjetIdIn(admUserId, projetIds)) {
            if (PerimetreCommercialService.porteUneAutrePortee(h)) {
                h.setCfgProject(null);
                habilitationRepository.save(h);
            } else {
                habilitationRepository.delete(h);
            }
        }
    }

    private void ajouterProjet(AdmUser compte, CfgProject projet) {
        AdmUserHabilitation habilitation = new AdmUserHabilitation();
        habilitation.setId(UUID.randomUUID());
        habilitation.setAdmUser(compte);
        habilitation.setCfgProject(projet);
        habilitationRepository.save(habilitation);
    }

    // ---------------------------------------------------------------- outils

    /** Ids distincts, tous connus (404 sinon) ; ordre de la demande conservé. */
    private List<ComProspection> lignesDemandees(List<UUID> ids) {
        Set<UUID> demandes = demandes(ids);
        if (demandes.isEmpty()) {
            throw new InvalidRequestException("Au moins une ligne est requise.");
        }
        Map<UUID, ComProspection> trouvees = prospectionRepository.findAllById(demandes).stream()
                .collect(Collectors.toMap(ComProspection::getId, Function.identity()));
        List<UUID> inconnues = demandes.stream().filter(id -> !trouvees.containsKey(id)).toList();
        if (!inconnues.isEmpty()) {
            throw new ResourceNotFoundException("Prospection(s) introuvable(s): " + inconnues);
        }
        return demandes.stream().map(trouvees::get).toList();
    }

    private static Set<UUID> demandes(List<UUID> ids) {
        Set<UUID> distincts = new LinkedHashSet<>();
        if (ids != null) {
            ids.stream().filter(Objects::nonNull).forEach(distincts::add);
        }
        return distincts;
    }

    private static void exiger(UUID valeur, String message) {
        if (valeur == null) {
            throw new InvalidRequestException(message);
        }
    }

    private static UUID projetDe(CfgTranche tranche) {
        return tranche != null && tranche.getCfgProject() != null ? tranche.getCfgProject().getId() : null;
    }
}
