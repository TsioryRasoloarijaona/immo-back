package com.tsix.immocrm.services.prospection;

import com.tsix.immocrm.dto.prospection.CreerDevisRequest;
import com.tsix.immocrm.dto.prospection.DevisDto;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComProspection;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.entities.stl.StlQuote;
import com.tsix.immocrm.exceptions.InvalidRequestException;
import com.tsix.immocrm.exceptions.ResourceNotFoundException;
import com.tsix.immocrm.repositories.cfg.CfgTrancheRepository;
import com.tsix.immocrm.repositories.com.ComProspectionRepository;
import com.tsix.immocrm.repositories.stk.StkItemRepository;
import com.tsix.immocrm.repositories.stl.StlQuoteRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/** GAP-07 (low priority) — Devis/offres proposées sur une Prospection ({@code StlQuote}). */
@Service
@Transactional(readOnly = true)
public class DevisService {

    private final ComProspectionRepository prospectionRepository;
    private final StlQuoteRepository quoteRepository;
    private final CfgTrancheRepository trancheRepository;
    private final StkItemRepository stkItemRepository;

    public DevisService(ComProspectionRepository prospectionRepository, StlQuoteRepository quoteRepository,
                         CfgTrancheRepository trancheRepository, StkItemRepository stkItemRepository) {
        this.prospectionRepository = prospectionRepository;
        this.quoteRepository = quoteRepository;
        this.trancheRepository = trancheRepository;
        this.stkItemRepository = stkItemRepository;
    }

    public Page<DevisDto> findByProspection(UUID prospectionId, Pageable pageable) {
        if (!prospectionRepository.existsById(prospectionId)) {
            throw new ResourceNotFoundException("Prospection introuvable: " + prospectionId);
        }
        return quoteRepository.findByComProspectionIdOrderByDocDateDesc(prospectionId, pageable)
                .map(DevisService::toDto);
    }

    @Transactional
    public DevisDto creerDevis(UUID prospectionId, CreerDevisRequest request) {
        ComProspection prospection = prospectionRepository.findById(prospectionId)
                .orElseThrow(() -> new ResourceNotFoundException("Prospection introuvable: " + prospectionId));

        if (request.trancheId() == null) {
            throw new InvalidRequestException("trancheId est requis");
        }
        if (request.prix() == null) {
            throw new InvalidRequestException("prix est requis");
        }
        CfgTranche tranche = trancheRepository.findById(request.trancheId())
                .orElseThrow(() -> new InvalidRequestException("CfgTranche introuvable: " + request.trancheId()));

        StlQuote quote = new StlQuote();
        quote.setId(UUID.randomUUID());
        quote.setDocDate(LocalDateTime.now());
        quote.setComProspection(prospection);
        quote.setCfgTranche(tranche);
        quote.setCfgProspect(prospection.getCfgProspect());
        quote.setCfgCommercial(prospection.getCfgCommercial());
        quote.setPriceAfterDiscount(request.prix());
        quote.setNotes(request.notes());
        if (request.articleId() != null) {
            StkItem article = stkItemRepository.findById(request.articleId())
                    .orElseThrow(() -> new InvalidRequestException("Article introuvable: " + request.articleId()));
            quote.setStkItem(article);
        }

        return toDto(quoteRepository.save(quote));
    }

    private static DevisDto toDto(StlQuote q) {
        return new DevisDto(
                q.getId(),
                q.getCode(),
                q.getDocDate(),
                q.getCfgTranche() != null ? q.getCfgTranche().getId() : null,
                q.getCfgTranche() != null ? q.getCfgTranche().getCode() : null,
                q.getStkItem() != null ? q.getStkItem().getId() : null,
                q.getStkItem() != null ? q.getStkItem().getCode() : null,
                q.getCfgProspect() != null ? q.getCfgProspect().getId() : null,
                q.getPriceAfterDiscount(),
                q.getNotes()
        );
    }
}
