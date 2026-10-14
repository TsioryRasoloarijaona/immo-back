package com.tsix.immocrm.services.dossier.modifications;

import com.tsix.immocrm.domain.EtatModification;
import com.tsix.immocrm.dto.dossier.modifications.ModificationDto;
import com.tsix.immocrm.dto.dossier.modifications.ModificationRequest;
import com.tsix.immocrm.dto.dossier.modifications.ModificationsDto;
import com.tsix.immocrm.entities.com.ComDocument;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.com.ComModification;
import com.tsix.immocrm.entities.com.ComSettlementCategory;
import com.tsix.immocrm.exceptions.ResourceNotFoundException;
import com.tsix.immocrm.repositories.com.ComDocumentRepository;
import com.tsix.immocrm.repositories.com.ComModificationRepository;
import com.tsix.immocrm.services.dossier.TotauxDossierEcheancier;
import com.tsix.immocrm.services.dossier.fiche.EcritureDossier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

import static java.util.Objects.requireNonNullElse;

/**
 * Onglet Modifications ({@code Yes.Com.Sales.Modification}) : chaque écriture recalcule {@code ModificationsAmount(HT)}
 * ({@code Sale.cs}), le prix de vente si les modifications y sont intégrées, puis les totaux du dossier.
 * Voir FEATURE-vente-echeancier.md §2.
 */
@Service
@Transactional(readOnly = true)
public class ModificationsService {

    private final EcritureDossier ecriture;
    private final ComModificationRepository modificationRepository;
    private final ComDocumentRepository documentRepository;
    private final TotauxDossierEcheancier totaux;

    public ModificationsService(EcritureDossier ecriture, ComModificationRepository modificationRepository,
                                ComDocumentRepository documentRepository, TotauxDossierEcheancier totaux) {
        this.ecriture = ecriture;
        this.modificationRepository = modificationRepository;
        this.documentRepository = documentRepository;
        this.totaux = totaux;
    }

    public ModificationsDto lister(UUID dossierId) {
        return dto(ecriture.charger(dossierId));
    }

    @Transactional
    public ModificationsDto ajouter(UUID dossierId, ModificationRequest request) {
        ComFolder dossier = ecriture.charger(dossierId);
        ComModification modification = new ComModification();
        modification.setId(UUID.randomUUID());
        modification.setComFolder(dossier);
        appliquer(modification, request);
        modificationRepository.save(modification);
        return recalculer(dossier);
    }

    @Transactional
    public ModificationsDto modifier(UUID dossierId, UUID modificationId, ModificationRequest request) {
        ComFolder dossier = ecriture.charger(dossierId);
        ComModification modification = trouver(dossierId, modificationId);
        appliquer(modification, request);
        modificationRepository.save(modification);
        return recalculer(dossier);
    }

    @Transactional
    public ModificationsDto supprimer(UUID dossierId, UUID modificationId) {
        ComFolder dossier = ecriture.charger(dossierId);
        modificationRepository.delete(trouver(dossierId, modificationId));
        modificationRepository.flush();
        return recalculer(dossier);
    }

    private ComModification trouver(UUID dossierId, UUID modificationId) {
        return modificationRepository.findById(modificationId)
                .filter(m -> m.getComFolder() != null && dossierId.equals(m.getComFolder().getId()))
                .orElseThrow(() -> new ResourceNotFoundException("Modification introuvable pour ce dossier: " + modificationId));
    }

    private void appliquer(ComModification m, ModificationRequest r) {
        m.setDescription(requireNonNullElse(r.description(), ""));
        m.setDemandDate(minuit(r.dateDemande() != null ? r.dateDemande() : ecriture.maintenant()));
        m.setTechnicalValidationDate(minuit(r.dateValidationTechnique()));
        m.setCustomerValidationDate(minuit(r.dateValidationClient()));
        m.setCost(requireNonNullElse(r.cout(), BigDecimal.ZERO));
        m.setCommercialAmountHT(requireNonNullElse(r.prixHT(), BigDecimal.ZERO));
        m.setCommercialAmount(requireNonNullElse(r.prixTTC(), BigDecimal.ZERO));
        m.setStatus(requireNonNullElse(r.etat(), EtatModification.ENCOURS).name());
        m.setNotes(requireNonNullElse(r.notes(), ""));
    }

    /** {@code ModificationsAmount(HT)}, puis {@code Price}/{@code PriceHT} = prix après remise (+ modifications si intégrées). */
    private ModificationsDto recalculer(ComFolder dossier) {
        List<ComModification> modifications = modificationRepository.findByComFolderIdOrderByDemandDateDesc(dossier.getId());
        BigDecimal ttc = somme(modifications, ComModification::getCommercialAmount);
        BigDecimal ht = somme(modifications, ComModification::getCommercialAmountHT);
        dossier.setModificationsAmount(ttc);
        dossier.setModificationsAmountHT(ht);
        boolean integre = Boolean.TRUE.equals(dossier.getPriceWithModification());
        // Prix après remise inconnu (dossier incomplet) : le prix stocké n'est pas réécrit.
        if (dossier.getPriceAfterDiscount() != null) {
            dossier.setPrice(integre ? dossier.getPriceAfterDiscount().add(ttc) : dossier.getPriceAfterDiscount());
        }
        if (dossier.getPriceAfterDiscountHT() != null) {
            dossier.setPriceHT(integre ? dossier.getPriceAfterDiscountHT().add(ht) : dossier.getPriceAfterDiscountHT());
        }
        totaux.rafraichir(dossier);
        return dto(dossier);
    }

    private static BigDecimal somme(List<ComModification> modifications, Function<ComModification, BigDecimal> montant) {
        return modifications.stream()
                .filter(m -> EtatModification.compte(m.getStatus()))
                .map(m -> requireNonNullElse(montant.apply(m), BigDecimal.ZERO))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private ModificationsDto dto(ComFolder dossier) {
        List<ModificationDto> lignes = modificationRepository.findByComFolderIdOrderByDemandDateDesc(dossier.getId())
                .stream().map(ModificationsService::ligne).toList();
        boolean integre = Boolean.TRUE.equals(dossier.getPriceWithModification());
        return new ModificationsDto(lignes, dossier.getModificationsAmount(), dossier.getModificationsAmountHT(), integre,
                integre ? null : ecartSchema(dossier));
    }

    /** {@code Sale.cs:VerifSchema} : hors prix, les modifications doivent figurer au schéma (lignes « Modifications » au crédit). */
    private BigDecimal ecartSchema(ComFolder dossier) {
        BigDecimal auSchema = BigDecimal.ZERO;
        for (ComDocument ligne : documentRepository.findByComFolderIdOrderByDocDateAsc(dossier.getId())) {
            ComSettlementCategory c = ligne.getComSettlementCategory();
            if (c != null && Boolean.TRUE.equals(c.getIsSaleModification()) && Boolean.TRUE.equals(c.getIsCredit())) {
                auSchema = auSchema.add(requireNonNullElse(ligne.getAmount(), BigDecimal.ZERO));
            }
        }
        return requireNonNullElse(dossier.getModificationsAmount(), BigDecimal.ZERO).subtract(auSchema)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private static ModificationDto ligne(ComModification m) {
        EtatModification etat = Arrays.stream(EtatModification.values())
                .filter(e -> e.name().equals(m.getStatus())).findFirst().orElse(null);
        return new ModificationDto(m.getId(), m.getDescription(), m.getDemandDate(), m.getTechnicalValidationDate(),
                m.getCustomerValidationDate(), m.getCost(), m.getCommercialAmountHT(), m.getCommercialAmount(), etat,
                etat != null ? etat.libelle() : m.getStatus(), m.getNotes());
    }

    private static java.time.LocalDateTime minuit(java.time.LocalDateTime date) {
        return date == null ? null : date.toLocalDate().atStartOfDay();
    }
}
