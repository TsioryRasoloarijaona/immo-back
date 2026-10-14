package com.tsix.immocrm.services.dossier.modalite;

import com.tsix.immocrm.dto.dossier.modalite.CreerModalitePaiementRequest;
import com.tsix.immocrm.dto.dossier.modalite.ModalitePaiementDto;
import com.tsix.immocrm.entities.com.ComDocument;
import com.tsix.immocrm.entities.com.ComDocumentPaymentMethod;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.com.ComSettlementCategory;
import com.tsix.immocrm.entities.str.StrStatus;
import com.tsix.immocrm.exceptions.InvalidRequestException;
import com.tsix.immocrm.exceptions.ResourceNotFoundException;
import com.tsix.immocrm.repositories.com.ComDocumentPaymentMethodRepository;
import com.tsix.immocrm.repositories.com.ComDocumentRepository;
import com.tsix.immocrm.repositories.com.ComSettlementCategoryRepository;
import com.tsix.immocrm.services.dossier.LigneEcheancierOmran;
import com.tsix.immocrm.services.dossier.SocieteDossier;
import com.tsix.immocrm.services.dossier.fiche.EcritureDossier;
import com.tsix.immocrm.utils.Champs;
import com.tsix.immocrm.utils.Requis;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static java.util.Objects.requireNonNullElse;

/**
 * « Modalité de paiement » du dossier ({@code ComDocumentPaymentMethod}) : le schéma de financement tel qu'il était à la
 * vente. Copie à la demande ({@code Sale.cs:UpdateDocumentPaymentMethod}) ou automatique au premier statut effectif
 * ({@code CreateDocumentPaymentMethod}, drapeau {@code BoolField001}). Voir FEATURE-vente-echeancier.md §2.
 */
@Service
@Transactional(readOnly = true)
public class ModalitePaiementService {

    private static final BigDecimal TVA_PAR_DEFAUT = new BigDecimal("0.2");
    /** Drapeau 001 « vente effective » du statut ; non mappé sur {@code StrStatus}, lu en SQL. */
    private static final String STATUT_EFFECTIF = "SELECT COUNT(*) FROM StrStatus WHERE PKey = :id AND BoolField001 = 1";

    private final EcritureDossier ecriture;
    private final ComDocumentPaymentMethodRepository modaliteRepository;
    private final ComDocumentRepository documentRepository;
    private final ComSettlementCategoryRepository categorieRepository;
    private final EntityManager entityManager;

    public ModalitePaiementService(EcritureDossier ecriture, ComDocumentPaymentMethodRepository modaliteRepository,
                                   ComDocumentRepository documentRepository,
                                   ComSettlementCategoryRepository categorieRepository, EntityManager entityManager) {
        this.ecriture = ecriture;
        this.modaliteRepository = modaliteRepository;
        this.documentRepository = documentRepository;
        this.categorieRepository = categorieRepository;
        this.entityManager = entityManager;
    }

    public List<ModalitePaiementDto> lister(UUID dossierId) {
        ecriture.charger(dossierId);
        return lignes(dossierId);
    }

    @Transactional
    public List<ModalitePaiementDto> ajouter(UUID dossierId, CreerModalitePaiementRequest request) {
        ComFolder dossier = ecriture.charger(dossierId);
        LocalDateTime date = Requis.champ(request.date(), "date est requise");
        BigDecimal montant = Requis.champ(request.montant(), "montant est requis");
        ComSettlementCategory categorie = categorieRepository.findById(Requis.champ(request.categorieId(), "categorieId est requis"))
                .orElseThrow(() -> new InvalidRequestException("ComSettlementCategory introuvable: " + request.categorieId()));
        BigDecimal tva = requireNonNullElse(categorie.getVatRatio(), TVA_PAR_DEFAUT);
        ComDocumentPaymentMethod ligne = nouvelle(dossier, categorie, date, montant, BigDecimal.ZERO, tva);
        ligne.setCfgCompany(SocieteDossier.de(dossier));
        ligne.setNotes(requireNonNullElse(request.notes(), ""));
        modaliteRepository.save(ligne);
        return lignes(dossierId);
    }

    @Transactional
    public List<ModalitePaiementDto> supprimer(UUID dossierId, UUID ligneId) {
        ecriture.charger(dossierId);
        ComDocumentPaymentMethod ligne = modaliteRepository.findById(ligneId)
                .filter(l -> l.getComFolder() != null && dossierId.equals(l.getComFolder().getId()))
                .orElseThrow(() -> new ResourceNotFoundException("Ligne de modalité introuvable pour ce dossier: " + ligneId));
        modaliteRepository.delete(ligne);
        modaliteRepository.flush();
        return lignes(dossierId);
    }

    /** « Copier schéma de financement » : remplace toutes les lignes par l'échéancier actuel. */
    @Transactional
    public List<ModalitePaiementDto> copierSchema(UUID dossierId) {
        ComFolder dossier = ecriture.charger(dossierId);
        modaliteRepository.deleteAll(modaliteRepository.findByComFolderIdOrderByDocDateDesc(dossierId));
        modaliteRepository.flush();
        copier(dossier);
        return lignes(dossierId);
    }

    /**
     * Instantané au passage à un statut effectif : rien si le dossier a déjà des lignes ou si son statut n'est pas
     * effectif. Appelé par le changement de statut, dans sa transaction ; rend {@code true} si la copie a eu lieu.
     */
    @Transactional
    public boolean instantaneSiEffectif(ComFolder dossier) {
        UUID statutId = Champs.de(dossier.getStrStatus(), StrStatus::getId);
        if (statutId == null || modaliteRepository.existsByComFolderId(dossier.getId()) || !estEffectif(statutId)) {
            return false;
        }
        copier(dossier);
        return true;
    }

    private boolean estEffectif(UUID statutId) {
        Number n = (Number) entityManager.createNativeQuery(STATUT_EFFECTIF).setParameter("id", statutId).getSingleResult();
        return n.intValue() > 0;
    }

    private void copier(ComFolder dossier) {
        for (ComDocument d : documentRepository.findByComFolderIdOrderByDocDateAsc(dossier.getId())) {
            ComDocumentPaymentMethod ligne = nouvelle(dossier, d.getComSettlementCategory(), d.getDocDate(),
                    requireNonNullElse(d.getAmount(), BigDecimal.ZERO), requireNonNullElse(d.getDiscountRatio(), BigDecimal.ZERO),
                    requireNonNullElse(d.getVATRatio(), TVA_PAR_DEFAUT));
            ligne.setCfgCompany(d.getCfgCompany());
            ligne.setNotes("");
            modaliteRepository.save(ligne);
        }
    }

    /** Montant posé en dernier comme OMRAN : HT = montant / ((1 − remise) × (1 + TVA)). */
    private ComDocumentPaymentMethod nouvelle(ComFolder dossier, ComSettlementCategory categorie, LocalDateTime date,
                                              BigDecimal montant, BigDecimal remise, BigDecimal tva) {
        ComDocumentPaymentMethod ligne = new ComDocumentPaymentMethod();
        ligne.setId(UUID.randomUUID());
        ligne.setComFolder(dossier);
        ligne.setComSettlementCategory(categorie);
        ligne.setDocDate(date != null ? date.toLocalDate().atStartOfDay() : null);
        ligne.setDiscountRatio(remise);
        ligne.setVatRatio(tva);
        ligne.setAmount(montant);
        ligne.setAmountTTC(montant);
        ligne.setAmountHT(LigneEcheancierOmran.montantHT(montant, remise, tva));
        String auteur = ecriture.auteur().nom();
        ligne.setUserCreate(auteur);
        ligne.setUserCreateDateTime(ecriture.maintenant());
        ligne.setUserUpdate(auteur);
        ligne.setUserUpdateDateTime(ecriture.maintenant());
        return ligne;
    }

    private List<ModalitePaiementDto> lignes(UUID dossierId) {
        return modaliteRepository.findByComFolderIdOrderByDocDateDesc(dossierId).stream()
                .map(l -> new ModalitePaiementDto(l.getId(), l.getDocDate(),
                        Champs.de(l.getComSettlementCategory(), ComSettlementCategory::getId),
                        Champs.de(l.getComSettlementCategory(), ComSettlementCategory::getCode),
                        l.getAmount(), l.getAmountHT(), l.getVatRatio(), l.getDiscountRatio(), l.getNotes()))
                .toList();
    }
}
