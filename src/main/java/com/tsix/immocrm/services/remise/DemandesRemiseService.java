package com.tsix.immocrm.services.remise;

import com.tsix.immocrm.domain.StatutDemandeRemise;
import com.tsix.immocrm.dto.remise.AccepterRemiseRequest;
import com.tsix.immocrm.dto.remise.DemandeRemiseDto;
import com.tsix.immocrm.dto.remise.DemanderRemiseRequest;
import com.tsix.immocrm.dto.remise.RefuserRemiseRequest;
import com.tsix.immocrm.entities.app.AppDemandeRemise;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.exceptions.ConflictException;
import com.tsix.immocrm.exceptions.InvalidRequestException;
import com.tsix.immocrm.exceptions.ResourceNotFoundException;
import com.tsix.immocrm.repositories.app.AppDemandeRemiseRepository;
import com.tsix.immocrm.repositories.com.ComFolderRepository;
import com.tsix.immocrm.services.dossier.DossierSorti;
import com.tsix.immocrm.services.dossier.edition.DossierEditionService;
import com.tsix.immocrm.services.dossier.edition.ValidationRemise;
import com.tsix.immocrm.services.perimetre.PerimetreCommercialService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Demandes de remise des agences — voir FEATURE-agences-vente.md (A3). */
@Service
public class DemandesRemiseService {

    private static final int LONGUEUR_MOTIF = 1000;
    private static final BigDecimal CENT = BigDecimal.valueOf(100);
    private static final String DEJA_EN_ATTENTE = "Une demande de remise est déjà en attente sur ce dossier.";

    private final AppDemandeRemiseRepository demandes;
    private final ComFolderRepository dossiers;
    private final PerimetreCommercialService perimetre;
    private final ValidationRemise validation;
    private final DossierEditionService edition;
    private final DemandeRemiseMapper mapper;
    private final Clock clock;

    public DemandesRemiseService(AppDemandeRemiseRepository demandes, ComFolderRepository dossiers,
                                 PerimetreCommercialService perimetre, ValidationRemise validation,
                                 DossierEditionService edition, DemandeRemiseMapper mapper, Clock clock) {
        this.demandes = demandes;
        this.dossiers = dossiers;
        this.perimetre = perimetre;
        this.validation = validation;
        this.edition = edition;
        this.mapper = mapper;
        this.clock = clock;
    }

    @Transactional
    public DemandeRemiseDto demander(UUID dossierId, DemanderRemiseRequest requete) {
        ComFolder dossier = dossierVisible(dossierId);
        UUID compte = validation.exigerAgence();
        BigDecimal taux = requete == null ? null : arrondi(requete.taux());
        if (taux == null || taux.signum() <= 0 || taux.compareTo(CENT) > 0) {
            throw new InvalidRequestException("taux : obligatoire, supérieur à 0 et au plus 100.");
        }
        String motif = motifObligatoire(requete.motif());
        if (DossierSorti.estSorti(dossier)) {
            throw new ConflictException(DossierEditionService.MESSAGE_SORTI);
        }
        if (taux.compareTo(edition.tauxRemise(dossier)) <= 0) {
            throw new InvalidRequestException("taux : doit dépasser le taux de remise actuel du dossier.");
        }
        if (demandes.findByDossierIdOrderByDemandeLeDesc(dossierId).stream()
                .anyMatch(d -> d.getStatut() == StatutDemandeRemise.EN_ATTENTE)) {
            throw new ConflictException(DEJA_EN_ATTENTE);
        }
        AppDemandeRemise demande = new AppDemandeRemise();
        demande.setId(UUID.randomUUID());
        demande.setDossierId(dossierId);
        demande.setTauxDemande(taux);
        demande.setMotif(motif);
        demande.setStatut(StatutDemandeRemise.EN_ATTENTE);
        demande.setDemandeParUserId(compte);
        demande.setDemandeLe(LocalDateTime.now(clock));
        try {
            demandes.saveAndFlush(demande);
        } catch (DataIntegrityViolationException e) {
            throw new ConflictException(DEJA_EN_ATTENTE);
        }
        return mapper.dto(demande, dossier);
    }

    @Transactional(readOnly = true)
    public List<DemandeRemiseDto> historique(UUID dossierId) {
        ComFolder dossier = dossierVisible(dossierId);
        return mapper.dtos(demandes.findByDossierIdOrderByDemandeLeDesc(dossierId), Map.of(dossierId, dossier));
    }

    @Transactional(readOnly = true)
    public List<DemandeRemiseDto> file(StatutDemandeRemise statut) {
        validation.exigerValideur();
        List<AppDemandeRemise> liste = demandes.findByStatutOrderByDemandeLeAsc(statut);
        if (liste.isEmpty()) {
            return List.of();
        }
        List<UUID> ids = liste.stream().map(AppDemandeRemise::getDossierId).distinct().toList();
        Specification<ComFolder> parmi = (root, query, cb) -> root.get("id").in(ids);
        Map<UUID, ComFolder> visibles = dossiers.findAll(perimetre.filtreDossiers().and(parmi)).stream()
                .collect(Collectors.toMap(ComFolder::getId, Function.identity()));
        return mapper.dtos(liste, visibles);
    }

    @Transactional
    public DemandeRemiseDto accepter(UUID demandeId, AccepterRemiseRequest requete) {
        validation.exigerValideur();
        AppDemandeRemise demande = enAttente(demandeVisible(demandeId));
        BigDecimal taux = arrondi(requete == null || requete.taux() == null
                ? demande.getTauxDemande() : requete.taux());
        if (taux.signum() <= 0 || taux.compareTo(demande.getTauxDemande()) > 0) {
            throw new InvalidRequestException("taux : supérieur à 0 et au plus le taux demandé.");
        }
        ComFolder dossier = dossierVisible(demande.getDossierId());
        if (taux.compareTo(edition.tauxRemise(dossier)) <= 0) {
            throw new InvalidRequestException("La remise actuelle du dossier est déjà supérieure ou égale.");
        }
        edition.accorderRemise(demande.getDossierId(), taux);
        demande.setTauxAccorde(taux);
        return traiter(demande, StatutDemandeRemise.ACCEPTEE);
    }

    @Transactional
    public DemandeRemiseDto refuser(UUID demandeId, RefuserRemiseRequest requete) {
        validation.exigerValideur();
        AppDemandeRemise demande = enAttente(demandeVisible(demandeId));
        demande.setMotifRefus(motifObligatoire(requete == null ? null : requete.motif()));
        return traiter(demande, StatutDemandeRemise.REFUSEE);
    }

    @Transactional
    public DemandeRemiseDto annuler(UUID demandeId) {
        AppDemandeRemise demande = demandeVisible(demandeId);
        validation.exigerAgence();
        return traiter(enAttente(demande), StatutDemandeRemise.ANNULEE);
    }

    private DemandeRemiseDto traiter(AppDemandeRemise demande, StatutDemandeRemise statut) {
        demande.setStatut(statut);
        demande.setTraiteParUserId(PerimetreCommercialService.admUserIdCourant().orElse(null));
        demande.setTraiteLe(LocalDateTime.now(clock));
        return mapper.dto(demande, dossierVisible(demande.getDossierId()));
    }

    /** 404 si la demande est inconnue ou si son dossier est hors du périmètre de l'appelant. */
    private AppDemandeRemise demandeVisible(UUID demandeId) {
        AppDemandeRemise demande = demandes.findById(demandeId)
                .orElseThrow(() -> new ResourceNotFoundException("Demande de remise introuvable: " + demandeId));
        perimetre.verifierAccesDossier(demande.getDossierId());
        return demande;
    }

    private ComFolder dossierVisible(UUID dossierId) {
        perimetre.verifierAccesDossier(dossierId);
        return dossiers.findById(dossierId)
                .orElseThrow(() -> new ResourceNotFoundException("Dossier introuvable: " + dossierId));
    }

    private static BigDecimal arrondi(BigDecimal taux) {
        return taux == null ? null : taux.setScale(4, java.math.RoundingMode.HALF_UP);
    }

    private static AppDemandeRemise enAttente(AppDemandeRemise demande) {
        if (demande.getStatut() != StatutDemandeRemise.EN_ATTENTE) {
            throw new ConflictException("La demande n'est plus en attente (statut " + demande.getStatut() + ").");
        }
        return demande;
    }

    private static String motifObligatoire(String motif) {
        if (motif == null || motif.isBlank() || motif.length() > LONGUEUR_MOTIF) {
            throw new InvalidRequestException("motif : obligatoire, " + LONGUEUR_MOTIF + " caractères au plus.");
        }
        return motif.strip();
    }
}
