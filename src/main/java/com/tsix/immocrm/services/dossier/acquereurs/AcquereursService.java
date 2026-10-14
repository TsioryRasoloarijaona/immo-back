package com.tsix.immocrm.services.dossier.acquereurs;

import com.tsix.immocrm.dto.dossier.acquereurs.AcquereurDto;
import com.tsix.immocrm.dto.dossier.acquereurs.AcquereursDto;
import com.tsix.immocrm.dto.dossier.acquereurs.ProspectionAArchiverDto;
import com.tsix.immocrm.dto.dossier.acquereurs.RepartitionAcquereursRequest;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.com.ComFolderCustomer;
import com.tsix.immocrm.entities.com.ComProspection;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.exceptions.InvalidRequestException;
import com.tsix.immocrm.repositories.cfg.CfgTierRepository;
import com.tsix.immocrm.repositories.com.ComFolderCustomerRepository;
import com.tsix.immocrm.repositories.com.ComProspectionRepository;
import com.tsix.immocrm.services.dossier.DossierService;
import com.tsix.immocrm.services.dossier.HistoriqueDossier;
import com.tsix.immocrm.services.dossier.TransformationProspectClient;
import com.tsix.immocrm.services.dossier.fiche.EcritureDossier;
import com.tsix.immocrm.services.prospection.AlerteRelanceService;
import com.tsix.immocrm.utils.Champs;
import com.tsix.immocrm.utils.Requis;
import com.tsix.immocrm.utils.TierNomFormatter;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import static java.util.Objects.requireNonNullElse;

/**
 * Onglet Acquéreurs : le client principal ({@code ComFolder.PartPercent}) et les co-acquéreurs ({@code ComFolderCustomer}),
 * répartition contrôlée comme {@code Sale.cs:Verif} (part du principal &gt; 0, Σ = 100 %), prospections à archiver
 * ({@code GetProspectionsToArchive}). Voir FEATURE-vente-echeancier.md §2.
 */
@Service
@Transactional(readOnly = true)
public class AcquereursService {

    static final String PART_PRINCIPAL = "La répartition du client principal doit être > 0 !";
    static final String TOTAL_CENT = "Le total des répartitions sur les acquéreurs doit être égale à 100% !";
    static final String DEJA_PRINCIPAL = "Cet tier est déjà affecté en tant que client principal !";
    private static final BigDecimal CENT = BigDecimal.valueOf(100);
    /** Échelle de {@code PartPercent} decimal(26,6) : Σ = 1 se vérifie sur les valeurs écrites. */
    private static final int ECHELLE_RATIO = 6;
    private static final String NON_AFFECTE = "Non affecté";

    private final EcritureDossier ecriture;
    private final ComFolderCustomerRepository acquereurRepository;
    private final CfgTierRepository tierRepository;
    private final ComProspectionRepository prospectionRepository;
    private final TransformationProspectClient transformation;
    private final HistoriqueDossier historique;
    private final AlerteRelanceService alerteRelanceService;
    private final EntityManager entityManager;

    public AcquereursService(EcritureDossier ecriture, DepotsAcquereurs depots,
                             TransformationProspectClient transformation, HistoriqueDossier historique,
                             AlerteRelanceService alerteRelanceService, EntityManager entityManager) {
        this.entityManager = entityManager;
        this.ecriture = ecriture;
        this.acquereurRepository = depots.acquereurs();
        this.tierRepository = depots.tiers();
        this.prospectionRepository = depots.prospections();
        this.transformation = transformation;
        this.historique = historique;
        this.alerteRelanceService = alerteRelanceService;
    }

    public AcquereursDto lister(UUID dossierId) {
        return dto(ecriture.charger(dossierId), List.of());
    }

    /**
     * Remplace la répartition. Les tiers ajoutés deviennent clients via {@link TransformationProspectClient} ; leurs
     * prospections ouvertes sont renvoyées pour l'archivage.
     */
    @Transactional
    public AcquereursDto enregistrer(UUID dossierId, RepartitionAcquereursRequest request) {
        ComFolder dossier = ecriture.charger(dossierId);
        BigDecimal partPrincipal = ratio(Requis.champ(request.quotePartPrincipal(), "quotePartPrincipal est requise"));
        List<RepartitionAcquereursRequest.CoAcquereur> saisis = requireNonNullElse(request.coAcquereurs(), List.of());
        Map<UUID, BigDecimal> parts = verifier(dossier, partPrincipal, saisis);

        List<ComFolderCustomer> existants = acquereurRepository.findByComFolderId(dossierId);
        Set<UUID> avant = new HashSet<>();
        for (ComFolderCustomer ligne : existants) {
            UUID tiersId = ligne.getCfgTier().getId();
            avant.add(tiersId);
            BigDecimal part = parts.get(tiersId);
            if (part == null) {
                acquereurRepository.delete(ligne);
            } else {
                ligne.setPartPercent(part);
                signer(ligne, false);
            }
        }
        List<UUID> nouveaux = parts.keySet().stream().filter(t -> !avant.contains(t)).toList();
        for (UUID tiersId : nouveaux) {
            ComFolderCustomer ligne = new ComFolderCustomer();
            ligne.setId(UUID.randomUUID());
            ligne.setComFolder(dossier);
            ligne.setCfgTier(tierRepository.findById(tiersId).orElseThrow());
            ligne.setPartPercent(parts.get(tiersId));
            signer(ligne, true);
        }
        dossier.setPartPercent(partPrincipal);
        ecriture.signer(dossier);
        acquereurRepository.flush();
        transformation.synchroniser(dossier).forEach(mention -> historique.evenement(dossier, mention));
        return dto(dossier, prospectionsOuvertes(dossier, nouveaux));
    }

    /** Ferme les prospections cochées, comme OMRAN à l'enregistrement ; seules celles des acquéreurs du dossier. */
    @Transactional
    public void archiverProspections(UUID dossierId, List<UUID> prospectionIds) {
        ComFolder dossier = ecriture.charger(dossierId);
        Set<UUID> permises = new HashSet<>();
        prospectionsOuvertes(dossier, tiersDuDossier(dossier)).forEach(p -> permises.add(p.id()));
        List<UUID> demandees = requireNonNullElse(prospectionIds, List.of());
        if (!permises.containsAll(demandees)) {
            throw new InvalidRequestException("Seules les prospections ouvertes des acquéreurs de ce dossier s'archivent ici.");
        }
        for (ComProspection prospection : prospectionRepository.findAllById(demandees)) {
            prospection.setIsClosed(true);
            prospection.setClosingDate(ecriture.maintenant());
            prospection.setClosingReasonText(DossierService.TEXTE_CLOTURE_VENTE);
            prospectionRepository.save(prospection);
            alerteRelanceService.desarmerToutes(prospection.getId());
        }
    }

    private Map<UUID, BigDecimal> verifier(ComFolder dossier, BigDecimal partPrincipal,
                                           List<RepartitionAcquereursRequest.CoAcquereur> saisis) {
        if (partPrincipal.signum() <= 0) {
            throw new InvalidRequestException(PART_PRINCIPAL);
        }
        UUID principalId = Champs.de(dossier.getCfgCustomer(), CfgTier::getId);
        Map<UUID, BigDecimal> parts = new HashMap<>();
        BigDecimal total = partPrincipal;
        for (RepartitionAcquereursRequest.CoAcquereur co : saisis) {
            UUID tiersId = Requis.champ(co.tiersId(), "tiersId est requis pour chaque co-acquéreur");
            BigDecimal part = ratio(Requis.champ(co.quotePart(), "quotePart est requise pour chaque co-acquéreur"));
            if (tiersId.equals(principalId)) {
                throw new InvalidRequestException(DEJA_PRINCIPAL);
            }
            if (part.signum() < 0 || parts.put(tiersId, part) != null) {
                throw new InvalidRequestException("Co-acquéreur en double ou part négative : " + tiersId);
            }
            if (!tierRepository.existsById(tiersId)) {
                throw new InvalidRequestException("Tiers introuvable: " + tiersId);
            }
            total = total.add(part);
        }
        if (total.compareTo(BigDecimal.ONE) != 0) {
            throw new InvalidRequestException(TOTAL_CENT);
        }
        return parts;
    }

    /** Pourcentage saisi → ratio rangé en base, à l'échelle de la colonne. */
    private static BigDecimal ratio(BigDecimal pourcentage) {
        return pourcentage.divide(CENT, ECHELLE_RATIO, RoundingMode.HALF_UP);
    }

    private void signer(ComFolderCustomer ligne, boolean creation) {
        String auteur = ecriture.auteur().nom();
        if (creation) {
            ligne.setUserCreate(auteur);
            ligne.setUserCreateDateTime(ecriture.maintenant());
        }
        ligne.setUserUpdate(auteur);
        ligne.setUserUpdateDateTime(ecriture.maintenant());
        acquereurRepository.save(ligne);
    }

    private List<UUID> tiersDuDossier(ComFolder dossier) {
        List<UUID> tiers = new ArrayList<>();
        if (dossier.getCfgCustomer() != null) {
            tiers.add(dossier.getCfgCustomer().getId());
        }
        acquereurRepository.findByComFolderId(dossier.getId()).forEach(l -> tiers.add(l.getCfgTier().getId()));
        return tiers;
    }

    /** {@code Sale.cs:GetProspectionsToArchive} : ouvertes, des tiers donnés, hors la prospection du dossier. */
    private List<ProspectionAArchiverDto> prospectionsOuvertes(ComFolder dossier, Collection<UUID> tiers) {
        if (tiers.isEmpty()) {
            return List.of();
        }
        UUID propre = Champs.de(dossier.getComProspection(), ComProspection::getId);
        return entityManager.createQuery(
                        "SELECT p FROM ComProspection p WHERE p.cfgProspect.id IN :tiers AND p.isClosed = false",
                        ComProspection.class)
                .setParameter("tiers", tiers)
                .getResultList().stream()
                .filter(p -> !p.getId().equals(propre))
                .map(AcquereursService::aArchiver)
                .sorted(Comparator.comparing(ProspectionAArchiverDto::libelle, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
    }

    private static ProspectionAArchiverDto aArchiver(ComProspection p) {
        String tranche = Objects.requireNonNullElse(Champs.de(p.getStkHierarchy(), CfgTranche::getDescription), NON_AFFECTE);
        String article = Champs.de(p.getStkItem(), StkItem::getCode);
        String libelle = TierNomFormatter.nom(p.getCfgProspect()) + " => " + tranche
                + (article != null && !article.isBlank() ? " => " + article : "");
        return new ProspectionAArchiverDto(p.getId(), p.getCode(), libelle);
    }

    private AcquereursDto dto(ComFolder dossier, List<ProspectionAArchiverDto> aArchiver) {
        List<AcquereurDto> acquereurs = new ArrayList<>();
        CfgTier principal = dossier.getCfgCustomer();
        if (principal != null) {
            acquereurs.add(new AcquereurDto(null, principal.getId(), TierNomFormatter.nom(principal),
                    principal.getIDNumber(), pourcentage(dossier.getPartPercent()), true));
        }
        acquereurRepository.findByComFolderId(dossier.getId()).stream()
                .sorted(Comparator.comparing(l -> TierNomFormatter.nom(l.getCfgTier()), Comparator.nullsLast(Comparator.naturalOrder())))
                .forEach(l -> acquereurs.add(new AcquereurDto(l.getId(), l.getCfgTier().getId(),
                        TierNomFormatter.nom(l.getCfgTier()), l.getCfgTier().getIDNumber(), pourcentage(l.getPartPercent()),
                        false)));
        BigDecimal total = acquereurs.stream().map(AcquereurDto::quotePart).filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new AcquereursDto(acquereurs, total, aArchiver);
    }

    private static BigDecimal pourcentage(BigDecimal ratio) {
        return ratio == null ? null : ratio.multiply(CENT);
    }
}
