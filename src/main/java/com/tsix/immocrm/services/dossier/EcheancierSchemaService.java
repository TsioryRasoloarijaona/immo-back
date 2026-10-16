package com.tsix.immocrm.services.dossier;

import com.tsix.immocrm.domain.Periodicite;
import com.tsix.immocrm.dto.dossier.EcheanceDto;
import com.tsix.immocrm.dto.dossier.echeancier.EtatSchemaDto;
import com.tsix.immocrm.dto.dossier.echeancier.GenererPeriodiqueRequest;
import com.tsix.immocrm.entities.com.ComDocument;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.com.ComSettlementCategory;
import com.tsix.immocrm.exceptions.ConflictException;
import com.tsix.immocrm.exceptions.InvalidRequestException;
import com.tsix.immocrm.exceptions.ResourceNotFoundException;
import com.tsix.immocrm.repositories.com.ComDocumentRepository;
import com.tsix.immocrm.repositories.com.ComFolderRepository;
import com.tsix.immocrm.repositories.com.ComSettlementCategoryRepository;
import com.tsix.immocrm.repositories.stl.StlSettlementLineRepository;
import com.tsix.immocrm.utils.Requis;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNullElse;

/**
 * Suppression d'une échéance, périodicité et état du schéma ({@code Sale.cs} front : {@code DeleteDocument},
 * {@code GeneratePeriodicity}). Voir FEATURE-vente-echeancier.md §1.1, §1.5.
 */
@Service
@Transactional(readOnly = true)
public class EcheancierSchemaService {

    static final String ECHEANCE_LIEE = "Vous ne pourriez pas supprimer une échéance ayant déjà un règlement ou un "
            + "décompte de règlement ou qui soit déjà facturée !";
    private static final String DOSSIER_INTROUVABLE = "Dossier introuvable: ";

    private final ComFolderRepository folderRepository;
    private final ComDocumentRepository documentRepository;
    private final ComSettlementCategoryRepository categorieRepository;
    private final StlSettlementLineRepository settlementLineRepository;
    private final LigneEcheancierOmran ligneOmran;
    private final LiensEcheance liens;
    private final EcrituresSchema ecritures;
    private final EcheancierService echeancierService;

    public EcheancierSchemaService(ComFolderRepository folderRepository, ComDocumentRepository documentRepository,
                                   ComSettlementCategoryRepository categorieRepository,
                                   StlSettlementLineRepository settlementLineRepository,
                                   LigneEcheancierOmran ligneOmran, LiensEcheance liens, EcrituresSchema ecritures,
                                   EcheancierService echeancierService) {
        this.folderRepository = folderRepository;
        this.documentRepository = documentRepository;
        this.categorieRepository = categorieRepository;
        this.settlementLineRepository = settlementLineRepository;
        this.ligneOmran = ligneOmran;
        this.liens = liens;
        this.ecritures = ecritures;
        this.echeancierService = echeancierService;
    }

    public EtatSchemaDto etat(UUID dossierId) {
        ComFolder folder = dossier(dossierId);
        List<ComDocument> lignes = documentRepository.findByComFolderIdOrderByDocDateAsc(dossierId);
        SchemaEcheancier.Natures natures = SchemaEcheancier.natures(lignes);
        BigDecimal reste = SchemaEcheancier.autofinancementReste(lignes, regleParLigne(dossierId));
        return new EtatSchemaDto(folder.getPriceAfterDiscount(), natures.avance(), natures.credit(),
                natures.autofinancement(), SchemaEcheancier.ecart(folder.getPriceAfterDiscount(), lignes), reste,
                montantInitial(folder, natures, reste));
    }

    /** 409 si l'échéance est réglée, facturée, en décompte ou en recouvrement ; sinon ses alertes partent avec elle. */
    @Transactional
    public EtatSchemaDto supprimer(UUID dossierId, UUID echeanceId) {
        ComFolder folder = dossier(dossierId);
        ComDocument echeance = documentRepository.findById(echeanceId)
                .filter(d -> d.getComFolder() != null && dossierId.equals(d.getComFolder().getId()))
                .orElseThrow(() -> new ResourceNotFoundException("Échéance introuvable pour ce dossier: " + echeanceId));
        if (liens.estLiee(echeanceId)) {
            throw new ConflictException(ECHEANCE_LIEE);
        }
        ecritures.verification().avantSuppression(folder, echeance);
        effacer(echeance);
        ecritures.reequilibrage().reporter(folder, echeance);
        ecritures.totaux().rafraichir(folder);
        documentRepository.flush();
        return etat(dossierId);
    }

    /**
     * N lignes de la catégorie choisie ; la dernière reporte l'écart du schéma, puis les lignes ramenées à zéro et
     * non réglées sont retirées, comme OMRAN.
     */
    @Transactional
    public List<EcheanceDto> genererPeriodique(UUID dossierId, GenererPeriodiqueRequest request) {
        ComFolder folder = dossier(dossierId);
        ComSettlementCategory categorie = categorieRepository.findById(
                        Requis.champ(request.categorieId(), EcheancierService.CATEGORIE_REQUISE))
                .orElseThrow(() -> new InvalidRequestException("ComSettlementCategory introuvable: " + request.categorieId()));
        Periodicite periodicite = Requis.champ(request.periodicite(), "periodicite est requise");
        BigDecimal total = Requis.champ(request.montantTotal(), "montantTotal est requis");
        int nombre = Requis.champ(request.nombre(), "nombre est requis");
        if (nombre < 1 || total.signum() <= 0) {
            throw new InvalidRequestException("nombre doit être >= 1 et montantTotal > 0");
        }
        BigDecimal initial = etat(dossierId).montantInitialPeriodicite();
        if (total.compareTo(initial) > 0) {
            throw new InvalidRequestException("Le montant à défalquer doit être <= " + initial.setScale(2, RoundingMode.HALF_UP));
        }

        List<Periodicite.Echeance> echeances = periodicite.repartir(total, nombre,
                Requis.champ(request.premiereDate(), "premiereDate est requise"));
        ecritures.verification().avantGeneration(folder, echeances.stream()
                .map(e -> apercu(categorie, e))
                .toList());
        List<ComDocument> creees = echeances.stream()
                .map(e -> nouvelleLigne(folder, categorie, e, request.notes()))
                .toList();
        ecritures.reequilibrage().reporter(folder, creees.get(creees.size() - 1));
        Set<UUID> retirees = retirerLignesNulles(dossierId);
        ecritures.alertes().synchroniser(creees.stream().filter(d -> !retirees.contains(d.getId())).toList());
        ecritures.totaux().rafraichir(folder);
        BigDecimal ecart = echeancierService.ecartSchema(folder);
        return creees.stream()
                .filter(d -> !retirees.contains(d.getId()))
                .map(d -> EcheancierService.avecEcart(echeancierService.buildDto(d, BigDecimal.ZERO), ecart))
                .toList();
    }

    /** Ligne de périodicité telle qu'elle sera créée, pour le contrôle du schéma ; jamais enregistrée. */
    private static ComDocument apercu(ComSettlementCategory categorie, Periodicite.Echeance e) {
        ComDocument ligne = new ComDocument();
        ligne.setId(UUID.randomUUID());
        ligne.setDocDate(e.date());
        ligne.setAmount(e.montant());
        ligne.setComSettlementCategory(categorie);
        return ligne;
    }

    private ComDocument nouvelleLigne(ComFolder folder, ComSettlementCategory categorie, Periodicite.Echeance e,
                                      String notes) {
        ComDocument ligne = new ComDocument();
        ligne.setId(UUID.randomUUID());
        ligne.setComFolder(folder);
        ligneOmran.signerCreation(ligne);
        ligne.setComSettlementCategory(categorie);
        ligne.setDescription(categorie.getDescription());
        ligne.setDocDate(e.date());
        ligne.setAmount(e.montant());
        ligne.setNotes(notes);
        ecritures.defauts().verifierDate(ligne, folder, e.montant());
        ligneOmran.completer(ligne, folder);
        return documentRepository.save(ligne);
    }

    /** {@code Math.Round(Amount, 0) = 0} et rien de réglé : la ligne disparaît du schéma. */
    private Set<UUID> retirerLignesNulles(UUID dossierId) {
        documentRepository.flush();
        Map<UUID, BigDecimal> regle = regleParLigne(dossierId);
        List<ComDocument> nulles = documentRepository.findByComFolderIdOrderByDocDateAsc(dossierId).stream()
                .filter(d -> requireNonNullElse(d.getAmount(), BigDecimal.ZERO).setScale(0, RoundingMode.HALF_EVEN).signum() == 0)
                .filter(d -> regle.getOrDefault(d.getId(), BigDecimal.ZERO).signum() == 0)
                .toList();
        nulles.forEach(this::effacer);
        return nulles.stream().map(ComDocument::getId).collect(Collectors.toSet());
    }

    private void effacer(ComDocument echeance) {
        liens.detacher(echeance);
        documentRepository.delete(echeance);
        documentRepository.flush();
    }

    /** {@code GeneratePeriodicity} : l'autofinancement restant, sinon prix − avance − crédit (loyers : 0). */
    private static BigDecimal montantInitial(ComFolder folder, SchemaEcheancier.Natures natures, BigDecimal reste) {
        if (reste.signum() != 0) {
            return reste;
        }
        return requireNonNullElse(folder.getPrice(), BigDecimal.ZERO).subtract(natures.avance()).subtract(natures.credit());
    }

    private Map<UUID, BigDecimal> regleParLigne(UUID dossierId) {
        Map<UUID, BigDecimal> regle = new HashMap<>();
        for (Object[] ligne : settlementLineRepository.sumAmountByComFolderIdGroupedByComDocument(dossierId)) {
            regle.put((UUID) ligne[0], (BigDecimal) ligne[1]);
        }
        return regle;
    }

    private ComFolder dossier(UUID dossierId) {
        return folderRepository.findById(dossierId)
                .orElseThrow(() -> new ResourceNotFoundException(DOSSIER_INTROUVABLE + dossierId));
    }
}
