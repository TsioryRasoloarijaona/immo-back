package com.tsix.immocrm.services.equipe;

import com.tsix.immocrm.domain.TypeActionRecente;
import com.tsix.immocrm.dto.equipe.AffectationsEquipeDto.MembreAffectationDto;
import com.tsix.immocrm.dto.equipe.DistribuerRequest;
import com.tsix.immocrm.dto.equipe.ModifierAffectationsRequest;
import com.tsix.immocrm.dto.equipe.ReaffecterRequest;
import com.tsix.immocrm.dto.equipe.ResultatLotDto;
import com.tsix.immocrm.dto.equipe.ResultatLotDto.LigneLotDto;
import com.tsix.immocrm.dto.prospection.TransformerProspectionRequest;
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
import com.tsix.immocrm.repositories.cfg.CfgTrancheRepository;
import com.tsix.immocrm.repositories.com.ComProspectionRepository;
import com.tsix.immocrm.services.perimetre.PerimetreCommercialService;
import com.tsix.immocrm.services.prospection.DiscriminantPreProspection;
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
    private final ProspectionService prospectionService;
    private final ComProspectionRepository prospectionRepository;
    private final CfgTrancheRepository trancheRepository;
    private final CfgTierRepository tierRepository;
    private final CfgProjectRepository projectRepository;
    private final AdmUserRepository userRepository;
    private final AdmUserHabilitationRepository habilitationRepository;

    public EquipeEcrituresService(EquipeCommercialeService equipeService,
                                  ProspectionService prospectionService,
                                  ComProspectionRepository prospectionRepository,
                                  CfgTrancheRepository trancheRepository,
                                  CfgTierRepository tierRepository,
                                  CfgProjectRepository projectRepository,
                                  AdmUserRepository userRepository,
                                  AdmUserHabilitationRepository habilitationRepository) {
        this.equipeService = equipeService;
        this.prospectionService = prospectionService;
        this.prospectionRepository = prospectionRepository;
        this.trancheRepository = trancheRepository;
        this.tierRepository = tierRepository;
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
        this.habilitationRepository = habilitationRepository;
    }

    // ---------------------------------------------------------------- distribuer

    /** Chaque ligne suit ensuite {@link ProspectionService#transformer} (prospect requis, unicité, tranche). */
    public ResultatLotDto distribuer(DistribuerRequest request) {
        List<ComProspection> lignes = lignesDemandees(request.preProspectionIds());
        exiger(request.commercialId(), "commercialId est requis");
        PerimetreEquipe perimetre = equipeService.perimetre();

        CfgTranche tranche = null;
        if (request.trancheId() != null) {
            tranche = trancheRepository.findById(request.trancheId())
                    .orElseThrow(() -> new InvalidRequestException("CfgTranche introuvable: " + request.trancheId()));
            perimetre.verifierProjet(projetDe(tranche), "Tranche " + tranche.getCode());
        }
        for (ComProspection ligne : lignes) {
            UUID projetActuel = projetDe(ligne.getStkHierarchy());
            // Une ligne sans projet n'est à personne : elle se distribue avec une tranche du chef.
            if (projetActuel != null || tranche == null) {
                perimetre.verifierProjet(projetActuel, LIGNE + ligne.getCode());
            }
        }
        UUID projetTranche = projetDe(tranche);
        for (ComProspection ligne : lignes) {
            ProspectionService.verifierEncorePreProspection(ligne);
            UUID projetCible = projetTranche != null ? projetTranche : projetDe(ligne.getStkHierarchy());
            perimetre.destinataire(request.commercialId(), projetCible, ligne.getCode());
        }

        List<LigneLotDto> ecrites = new ArrayList<>();
        for (ComProspection ligne : lignes) {
            String ancien = TierNomFormatter.nom(ligne.getCfgCommercial());
            prospectionService.transformer(ligne.getId(),
                    new TransformerProspectionRequest(request.commercialId(), request.trancheId(), null));
            ecrites.add(new LigneLotDto(ligne.getId(), ligne.getCode(), ancien));
        }
        return resultat(request.commercialId(), ecrites);
    }

    // ---------------------------------------------------------------- réaffecter

    /** Change {@code CfgCommercialId} seul : le commercial référent du prospect n'est pas touché. */
    public ResultatLotDto reaffecter(ReaffecterRequest request) {
        List<ComProspection> lignes = lignesDemandees(request.prospectionIds());
        exiger(request.commercialId(), "commercialId est requis");
        PerimetreEquipe perimetre = equipeService.perimetre();

        for (ComProspection ligne : lignes) {
            perimetre.verifierProjet(projetDe(ligne.getStkHierarchy()), LIGNE + ligne.getCode());
        }
        for (ComProspection ligne : lignes) {
            if (DiscriminantPreProspection.estPreProspection(ligne)) {
                throw new ConflictException(LIGNE + ligne.getCode()
                        + " : une pré-prospection se distribue, elle ne se réaffecte pas.");
            }
            if (Boolean.TRUE.equals(ligne.getIsClosed())) {
                throw new ConflictException(LIGNE + ligne.getCode() + " : prospection clôturée.");
            }
            perimetre.destinataire(request.commercialId(), projetDe(ligne.getStkHierarchy()), ligne.getCode());
        }

        CfgTier nouveau = tierRepository.findById(request.commercialId())
                .orElseThrow(() -> new InvalidRequestException("Commercial (CfgTier) introuvable: " + request.commercialId()));
        String motif = StringUtils.hasText(request.motif()) ? " — " + request.motif().strip() : "";
        List<LigneLotDto> ecrites = new ArrayList<>();
        for (ComProspection ligne : lignes) {
            CfgTier ancien = ligne.getCfgCommercial();
            if (ancien != null && ancien.getId().equals(nouveau.getId())) {
                continue;
            }
            String ancienNom = TierNomFormatter.nom(ancien);
            ligne.setCfgCommercial(nouveau);
            prospectionRepository.save(ligne);
            prospectionService.enregistrerHistorique(ligne.getId(), TypeActionRecente.COMMERCIAL_REAFFECTE,
                    (ancienNom != null ? ancienNom : "aucun") + " → " + TierNomFormatter.nom(nouveau) + motif);
            ecrites.add(new LigneLotDto(ligne.getId(), ligne.getCode(), ancienNom));
        }
        return resultat(nouveau.getId(), ecrites);
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

        retirerProjets(admUserId, retirer.stream().filter(cible.projetIds()::contains).toList());
        ajouter.stream()
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

    private ResultatLotDto resultat(UUID commercialId, List<LigneLotDto> lignes) {
        String nom = tierRepository.findById(commercialId).map(TierNomFormatter::nom).orElse(null);
        return new ResultatLotDto(lignes.size(), commercialId, nom, lignes);
    }
}
