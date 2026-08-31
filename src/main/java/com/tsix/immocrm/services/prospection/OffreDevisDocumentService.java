package com.tsix.immocrm.services.prospection;

import com.tsix.immocrm.dto.prospection.MontantsOffreDto;
import com.tsix.immocrm.dto.prospection.OffreDevisData;
import com.tsix.immocrm.dto.prospection.OffreDevisData.SousArticleLigne;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.entities.stl.StlQuote;
import com.tsix.immocrm.entities.stl.StlQuoteItem;
import com.tsix.immocrm.exceptions.ResourceNotFoundException;
import com.tsix.immocrm.repositories.stl.StlQuoteItemRepository;
import com.tsix.immocrm.repositories.stl.StlQuoteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.UUID;

/**
 * Gathers the data behind the "Fiche Offre vente" document (see {@code OffreDevisPdfRenderer} for
 * the actual PDF rendering), modeled on the legacy template
 * ({@code instructions/templates/priceOffer/OFFRE DE PRIX.pdf}).
 */
@Service
@Transactional(readOnly = true)
public class OffreDevisDocumentService {

    private final StlQuoteRepository quoteRepository;
    private final StlQuoteItemRepository quoteItemRepository;
    private final DevisService devisService;

    public OffreDevisDocumentService(StlQuoteRepository quoteRepository, StlQuoteItemRepository quoteItemRepository,
                                      DevisService devisService) {
        this.quoteRepository = quoteRepository;
        this.quoteItemRepository = quoteItemRepository;
        this.devisService = devisService;
    }

    public OffreDevisData collect(UUID prospectionId, UUID devisId) {
        StlQuote quote = quoteRepository.findById(devisId)
                .orElseThrow(() -> new ResourceNotFoundException("Devis introuvable: " + devisId));
        if (quote.getComProspection() == null || !prospectionId.equals(quote.getComProspection().getId())) {
            throw new ResourceNotFoundException("Devis introuvable: " + devisId);
        }

        StkItem article = quote.getStkItem();
        CfgTranche tranche = quote.getCfgTranche();
        List<StlQuoteItem> lignes = quoteItemRepository.findByStlQuoteIdOrderByCodeAsc(devisId);

        return new OffreDevisData(
                quote.getCode(),
                quote.getDocDate() != null ? quote.getDocDate().toLocalDate() : null,
                nomTier(quote.getCfgProspect()),
                tranche != null ? tranche.getDescription() : null,
                article != null ? article.getCode() : null,
                article != null ? article.getArea() : null,
                article != null && article.getStkItemCategory() != null ? article.getStkItemCategory().getDescription() : null,
                article != null && article.getStkItemType() != null ? article.getStkItemType().getDescription() : null,
                nomTier(quote.getCfgCommercial()),
                quote.getComPaymentMethod() != null ? quote.getComPaymentMethod().getDescription() : null,
                toMontantsDto(quote),
                lignes.stream().map(OffreDevisDocumentService::toLigne).toList(),
                devisService.calculerSchemaFinancement(quote)
        );
    }

    private static MontantsOffreDto toMontantsDto(StlQuote q) {
        return new MontantsOffreDto(
                q.getVatRatio(),
                q.getUnitCostHT(), q.getUnitCost(),
                q.getItemPriceHT(), q.getItemPrice(),
                q.getUnderItemsPriceHT(), q.getUnderItemsPrice(),
                q.getPriceBeforeDiscountHT(), q.getPriceBeforeDiscount(),
                q.getDiscount(), q.getDiscountAmountHT(), q.getDiscountAmount(),
                q.getUnitCostAfterDiscountHT(), q.getUnitCostAfterDiscount(),
                q.getItemPriceAfterDiscountHT(), q.getItemPriceAfterDiscount(),
                q.getUnderItemsPriceAfterDiscountHT(), q.getUnderItemsPriceAfterDiscount(),
                q.getPriceAfterDiscountHT(), q.getPriceAfterDiscount(),
                q.getCommissionRate()
        );
    }

    private static SousArticleLigne toLigne(StlQuoteItem item) {
        return new SousArticleLigne(
                item.getCode(),
                item.getStkItemCategory() != null ? item.getStkItemCategory().getDescription() : null,
                Boolean.TRUE.equals(item.getAlwaysAffected()),
                item.getArea(),
                item.getPriceBeforeDiscount(),
                item.getDiscount(),
                item.getDiscountAmount(),
                item.getPriceAfterDiscount()
        );
    }

    private static String nomTier(CfgTier tier) {
        return com.tsix.immocrm.utils.TierNomFormatter.nom(tier);
    }
}
