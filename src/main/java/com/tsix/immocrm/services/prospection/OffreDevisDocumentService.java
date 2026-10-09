package com.tsix.immocrm.services.prospection;

import com.tsix.immocrm.domain.ConventionRemise;
import com.tsix.immocrm.dto.prospection.OffreDevisData;
import com.tsix.immocrm.dto.prospection.OffreDevisData.SousArticleLigne;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.entities.stk.StkItemCategory;
import com.tsix.immocrm.entities.stk.StkItemType;
import com.tsix.immocrm.entities.stl.StlQuote;
import com.tsix.immocrm.entities.stl.StlQuoteItem;
import com.tsix.immocrm.exceptions.ResourceNotFoundException;
import com.tsix.immocrm.repositories.stl.StlQuoteItemRepository;
import com.tsix.immocrm.repositories.stl.StlQuoteRepository;
import com.tsix.immocrm.utils.Champs;
import com.tsix.immocrm.utils.TierNomFormatter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.UUID;

/**
 * Gathers the data behind the "Fiche Offre vente" document (rendered by {@code OffreDevisPdfRenderer}),
 * modeled on {@code instructions/templates/priceOffer/OFFRE DE PRIX.pdf}.
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
                TierNomFormatter.nom(quote.getCfgProspect()),
                Champs.de(tranche, CfgTranche::getDescription),
                Champs.de(article, StkItem::getCode),
                Champs.de(article, StkItem::getArea),
                Champs.de(article, StkItem::getStkItemCategory, StkItemCategory::getDescription),
                Champs.de(article, StkItem::getStkItemType, StkItemType::getDescription),
                TierNomFormatter.nom(quote.getCfgCommercial()),
                quote.getComPaymentMethod() != null ? quote.getComPaymentMethod().getDescription() : null,
                DevisService.toMontantsDto(quote),
                lignes.stream().map(OffreDevisDocumentService::toLigne).toList(),
                devisService.calculerSchemaFinancement(quote)
        );
    }

    private static SousArticleLigne toLigne(StlQuoteItem item) {
        return new SousArticleLigne(
                item.getCode(),
                item.getStkItemCategory() != null ? item.getStkItemCategory().getDescription() : null,
                Boolean.TRUE.equals(item.getAlwaysAffected()),
                item.getArea(),
                item.getPriceBeforeDiscount(),
                ConventionRemise.pourcentage(item.getDiscount()),
                item.getDiscountAmount(),
                item.getPriceAfterDiscount()
        );
    }
}
