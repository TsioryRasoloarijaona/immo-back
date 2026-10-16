package com.tsix.immocrm.services.dossier.cadeaux;

import com.tsix.immocrm.dto.dossier.cadeaux.CadeauCatalogueDto;
import com.tsix.immocrm.dto.dossier.cadeaux.CadeauDto;
import com.tsix.immocrm.dto.dossier.cadeaux.CadeauRequest;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.com.ComFolderGift;
import com.tsix.immocrm.entities.com.ComGift;
import com.tsix.immocrm.entities.com.ComGiftReason;
import com.tsix.immocrm.exceptions.ForbiddenException;
import com.tsix.immocrm.exceptions.InvalidRequestException;
import com.tsix.immocrm.exceptions.ResourceNotFoundException;
import com.tsix.immocrm.repositories.com.ComFolderGiftRepository;
import com.tsix.immocrm.repositories.com.ComGiftReasonRepository;
import com.tsix.immocrm.repositories.com.ComGiftRepository;
import com.tsix.immocrm.services.dossier.fiche.EcritureDossier;
import com.tsix.immocrm.utils.Champs;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import static java.util.Objects.requireNonNullElse;

/**
 * Onglet Cadeaux ({@code Yes.Com.Sales.FolderGift}, écran {@code Yes.Com.Front.Gift}) : prix et disponibilité repris du
 * catalogue au choix du cadeau, date de livraison obligatoire si livré. Voir FEATURE-vente-echeancier.md §2.
 */
@Service
@Transactional(readOnly = true)
public class CadeauxService {

    static final String DATE_LIVRAISON = "Veuillez saisir une date de livraison !";

    private final EcritureDossier ecriture;
    private final ComFolderGiftRepository cadeauRepository;
    private final ComGiftRepository catalogueRepository;
    private final ComGiftReasonRepository raisonRepository;

    public CadeauxService(EcritureDossier ecriture, ComFolderGiftRepository cadeauRepository,
                          ComGiftRepository catalogueRepository, ComGiftReasonRepository raisonRepository) {
        this.ecriture = ecriture;
        this.cadeauRepository = cadeauRepository;
        this.catalogueRepository = catalogueRepository;
        this.raisonRepository = raisonRepository;
    }

    public List<CadeauDto> lister(UUID dossierId) {
        ecriture.charger(dossierId);
        return cadeaux(dossierId);
    }

    public List<CadeauCatalogueDto> catalogue(UUID dossierId) {
        ecriture.charger(dossierId);
        Map<UUID, Integer> stock = stock();
        return catalogueRepository.findAll().stream()
                .sorted(Comparator.comparing(ComGift::getSorting, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(ComGift::getCode, Comparator.nullsLast(Comparator.naturalOrder())))
                .map(g -> new CadeauCatalogueDto(g.getId(), g.getCode(), g.getDescription(), g.getPrice(),
                        stock.getOrDefault(g.getId(), 0)))
                .toList();
    }

    public List<CadeauCatalogueDto> raisons(UUID dossierId) {
        ecriture.charger(dossierId);
        return raisonRepository.findAll().stream()
                .sorted(Comparator.comparing(ComGiftReason::getCode, Comparator.nullsLast(Comparator.naturalOrder())))
                .map(r -> new CadeauCatalogueDto(r.getId(), r.getCode(), r.getDescription(), null, null))
                .toList();
    }

    @Transactional
    public List<CadeauDto> ajouter(UUID dossierId, CadeauRequest request) {
        ComFolder dossier = ecriture.charger(dossierId);
        ComFolderGift cadeau = new ComFolderGift();
        cadeau.setId(UUID.randomUUID());
        cadeau.setComFolder(dossier);
        cadeau.setUserCreate(ecriture.auteur().nom());
        cadeau.setUserCreateDateTime(ecriture.maintenant());
        appliquer(cadeau, request);
        return enregistrer(dossier, cadeau);
    }

    /** {@code raisonPermise} : seul le droit « ajouter / supprimer des cadeaux » change la raison ({@code Gift.cs}). */
    @Transactional
    public List<CadeauDto> modifier(UUID dossierId, UUID cadeauId, CadeauRequest request, boolean raisonPermise) {
        ComFolder dossier = ecriture.charger(dossierId);
        ComFolderGift cadeau = trouver(dossierId, cadeauId);
        UUID raisonAvant = Champs.de(cadeau.getComGiftReason(), ComGiftReason::getId);
        if (!raisonPermise && !Objects.equals(raisonAvant, request.raisonId())) {
            throw new ForbiddenException("Seuls l'Administrateur et le Commercial Chef changent la raison d'un cadeau.");
        }
        appliquer(cadeau, request);
        return enregistrer(dossier, cadeau);
    }

    @Transactional
    public List<CadeauDto> supprimer(UUID dossierId, UUID cadeauId) {
        ComFolder dossier = ecriture.charger(dossierId);
        cadeauRepository.delete(trouver(dossierId, cadeauId));
        cadeauRepository.flush();
        ecriture.signer(dossier);
        return cadeaux(dossierId);
    }

    private List<CadeauDto> enregistrer(ComFolder dossier, ComFolderGift cadeau) {
        cadeau.setUserUpdate(ecriture.auteur().nom());
        cadeau.setUserUpdateDateTime(ecriture.maintenant());
        cadeauRepository.save(cadeau);
        ecriture.signer(dossier);
        return cadeaux(dossier.getId());
    }

    private ComFolderGift trouver(UUID dossierId, UUID cadeauId) {
        return cadeauRepository.findById(cadeauId)
                .filter(c -> c.getComFolder() != null && dossierId.equals(c.getComFolder().getId()))
                .orElseThrow(() -> new ResourceNotFoundException("Cadeau introuvable pour ce dossier: " + cadeauId));
    }

    /** {@code FolderGift.cs:SetPrice} : un nouveau cadeau reprend le prix du catalogue et sa disponibilité. */
    private void appliquer(ComFolderGift c, CadeauRequest r) {
        boolean livre = Boolean.TRUE.equals(r.livre());
        if (livre && r.dateLivraison() == null) {
            throw new InvalidRequestException(DATE_LIVRAISON);
        }
        UUID avant = Champs.de(c.getComGift(), ComGift::getId);
        ComGift choisi = r.cadeauId() == null ? null : catalogueRepository.findById(r.cadeauId())
                .orElseThrow(() -> new InvalidRequestException("Cadeau du catalogue introuvable: " + r.cadeauId()));
        c.setComGift(choisi);
        if (choisi != null && !choisi.getId().equals(avant)) {
            c.setPrice(choisi.getPrice());
            c.setIsAvailable(stock().getOrDefault(choisi.getId(), 0) > 0);
        }
        if (r.prix() != null) {
            c.setPrice(r.prix());
        }
        c.setPrice(requireNonNullElse(c.getPrice(), BigDecimal.ZERO));
        c.setIsAvailable(Boolean.TRUE.equals(c.getIsAvailable()));
        c.setComGiftReason(r.raisonId() == null ? null : raisonRepository.findById(r.raisonId())
                .orElseThrow(() -> new InvalidRequestException("Raison de cadeau introuvable: " + r.raisonId())));
        c.setDelivered(livre);
        c.setVoucherDate(minuit(r.dateBon()));
        c.setDeliveryDate(minuit(r.dateLivraison()));
        c.setNotes(requireNonNullElse(r.notes(), ""));
    }

    private Map<UUID, Integer> stock() {
        Map<UUID, Integer> stock = new HashMap<>();
        for (Object[] ligne : cadeauRepository.stockParCadeau()) {
            stock.put(UUID.fromString(ligne[0].toString()), ((Number) ligne[1]).intValue());
        }
        return stock;
    }

    private List<CadeauDto> cadeaux(UUID dossierId) {
        return cadeauRepository.findByComFolderIdOrderByUserCreateDateTimeAsc(dossierId).stream()
                .map(c -> new CadeauDto(c.getId(), Champs.de(c.getComGift(), ComGift::getId),
                        Champs.de(c.getComGift(), ComGift::getDescription), c.getPrice(), Boolean.TRUE.equals(c.getDelivered()),
                        Boolean.TRUE.equals(c.getIsAvailable()), Champs.de(c.getComGiftReason(), ComGiftReason::getId),
                        Champs.de(c.getComGiftReason(), ComGiftReason::getDescription), c.getVoucherDate(),
                        c.getDeliveryDate(), c.getNotes()))
                .toList();
    }

    private static LocalDateTime minuit(LocalDateTime date) {
        return date == null ? null : date.toLocalDate().atStartOfDay();
    }
}
