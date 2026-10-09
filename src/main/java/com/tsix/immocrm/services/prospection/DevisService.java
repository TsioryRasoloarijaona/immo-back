package com.tsix.immocrm.services.prospection;

import com.tsix.immocrm.domain.PrixArticle;
import com.tsix.immocrm.domain.SchemaFinancementCalculator;
import com.tsix.immocrm.dto.dossier.LigneSchemaFinancementDto;
import com.tsix.immocrm.dto.prospection.CreerDevisRequest;
import com.tsix.immocrm.dto.prospection.DevisDto;
import com.tsix.immocrm.dto.prospection.MontantsOffreDto;
import com.tsix.immocrm.entities.adm.AdmSequence;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComPaymentMethod;
import com.tsix.immocrm.entities.com.ComProspection;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.entities.stk.StkItemCategory;
import com.tsix.immocrm.entities.stk.StkItemType;
import com.tsix.immocrm.entities.stl.StlQuote;
import com.tsix.immocrm.entities.stl.StlQuoteItem;
import com.tsix.immocrm.exceptions.InvalidRequestException;
import com.tsix.immocrm.exceptions.ResourceNotFoundException;
import com.tsix.immocrm.repositories.adm.AdmSequenceRepository;
import com.tsix.immocrm.repositories.cfg.CfgTierRepository;
import com.tsix.immocrm.repositories.cfg.CfgTrancheRepository;
import com.tsix.immocrm.repositories.com.ComPaymentMethodRepository;
import com.tsix.immocrm.repositories.com.ComProspectionRepository;
import com.tsix.immocrm.repositories.com.ComSettlementCategoryRepository;
import com.tsix.immocrm.repositories.stk.StkItemRepository;
import com.tsix.immocrm.repositories.stl.StlQuoteItemRepository;
import com.tsix.immocrm.repositories.stl.StlQuoteRepository;
import com.tsix.immocrm.services.perimetre.PerimetreCommercialService;
import com.tsix.immocrm.utils.Champs;
import com.tsix.immocrm.utils.Requis;
import com.tsix.immocrm.utils.TierNomFormatter;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Offres d'une prospection ({@code StlQuote}) : le formulaire Offre legacy, entête et onglet « Montants ». */
@Service
@Transactional(readOnly = true)
public class DevisService {

    private final ComProspectionRepository prospectionRepository;
    private final StlQuoteRepository quoteRepository;
    private final StlQuoteItemRepository quoteItemRepository;
    private final CfgTrancheRepository trancheRepository;
    private final StkItemRepository stkItemRepository;
    private final CfgTierRepository tierRepository;
    private final ComPaymentMethodRepository paymentMethodRepository;
    private final AdmSequenceRepository sequenceRepository;
    private final ComSettlementCategoryRepository settlementCategoryRepository;
    private final PerimetreCommercialService perimetreService;
    private final Clock clock;

    public DevisService(ComProspectionRepository prospectionRepository, StlQuoteRepository quoteRepository,
                         StlQuoteItemRepository quoteItemRepository, CfgTrancheRepository trancheRepository,
                         StkItemRepository stkItemRepository, CfgTierRepository tierRepository,
                         ComPaymentMethodRepository paymentMethodRepository, AdmSequenceRepository sequenceRepository,
                         ComSettlementCategoryRepository settlementCategoryRepository,
                         PerimetreCommercialService perimetreService, Clock clock) {
        this.clock = clock;
        this.perimetreService = perimetreService;
        this.prospectionRepository = prospectionRepository;
        this.quoteRepository = quoteRepository;
        this.quoteItemRepository = quoteItemRepository;
        this.trancheRepository = trancheRepository;
        this.stkItemRepository = stkItemRepository;
        this.tierRepository = tierRepository;
        this.paymentMethodRepository = paymentMethodRepository;
        this.sequenceRepository = sequenceRepository;
        this.settlementCategoryRepository = settlementCategoryRepository;
    }

    /**
     * Compteur {@code AdmSequence "Offre"} partagé avec OMRAN, global : jamais remis à zéro par
     * année, seule l'année affichée change ({@code BasedOnSystemYear}). Format {@code YY/NNNNN}.
     */
    private String genererCodeOffre() {
        AdmSequence sequence = sequenceRepository.findByCode("Offre").orElseGet(() -> {
            // Crée la ligne si elle manque (base de test fraîche) plutôt que de bloquer toute offre.
            AdmSequence nouvelle = new AdmSequence();
            nouvelle.setId(UUID.randomUUID());
            nouvelle.setCode("Offre");
            nouvelle.setSeqValue(0);
            nouvelle.setSeqValueNumber(5);
            nouvelle.setSeqYearNumber(2);
            nouvelle.setSeparator("/");
            nouvelle.setBasedOnSystemYear(true);
            return nouvelle;
        });
        int prochain = (sequence.getSeqValue() != null ? sequence.getSeqValue() : 0) + 1;
        sequence.setSeqValue(prochain);
        sequenceRepository.save(sequence);

        int largeurAnnee = sequence.getSeqYearNumber() != null ? sequence.getSeqYearNumber() : 2;
        int largeurNumero = sequence.getSeqValueNumber() != null ? sequence.getSeqValueNumber() : 5;
        String separateur = sequence.getSeparator() != null ? sequence.getSeparator() : "/";
        int annee = LocalDateTime.now(clock).getYear() % (int) Math.pow(10, largeurAnnee);
        String anneeFormatee = completerParZeros(annee, largeurAnnee);
        String numero = completerParZeros(prochain, largeurNumero);
        String prefixe = sequence.getSeqPrefix() != null ? sequence.getSeqPrefix() : "";
        String suffixe = sequence.getSeqSuffix() != null ? sequence.getSeqSuffix() : "";
        return prefixe + anneeFormatee + separateur + numero + suffixe;
    }

    private static String completerParZeros(int valeur, int largeur) {
        String format = "%0" + largeur + "d";
        return String.format(format, valeur);
    }

    public Page<DevisDto> findAll(String search, List<UUID> trancheIds, UUID commercial,
                                   LocalDateTime dateDebut, LocalDateTime dateFin, Pageable pageable) {
        Specification<StlQuote> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (StringUtils.hasText(search)) {
                predicates.add(cb.like(cb.lower(root.get("code")), "%" + search.toLowerCase() + "%"));
            }
            if (trancheIds != null && !trancheIds.isEmpty()) {
                predicates.add(root.get("cfgTranche").get("id").in(trancheIds));
            }
            if (commercial != null) {
                predicates.add(cb.equal(root.get("cfgCommercial").get("id"), commercial));
            }
            if (dateDebut != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("docDate"), dateDebut));
            }
            if (dateFin != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("docDate"), dateFin));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        // Périmètre filtré ici : PerimetreInterceptor ne couvre pas ce préfixe d'URL
        // (FEATURE-perimetre-commercial.md §9bis.2).
        return quoteRepository.findAll(spec.and(perimetreService.filtreDevis()), pageable).map(this::toDto);
    }

    public Page<DevisDto> findByProspection(UUID prospectionId, Pageable pageable) {
        if (!prospectionRepository.existsById(prospectionId)) {
            throw new ResourceNotFoundException("Prospection introuvable: " + prospectionId);
        }
        return quoteRepository.findByComProspectionIdOrderByDocDateDesc(prospectionId, pageable)
                .map(this::toDto);
    }

    public DevisDto findById(UUID prospectionId, UUID devisId) {
        StlQuote quote = quoteRepository.findById(devisId)
                .orElseThrow(() -> new ResourceNotFoundException("Devis introuvable: " + devisId));
        if (quote.getComProspection() == null || !prospectionId.equals(quote.getComProspection().getId())) {
            throw new ResourceNotFoundException("Devis introuvable: " + devisId);
        }
        return toDto(quote);
    }

    @Transactional
    public DevisDto creerDevis(UUID prospectionId, CreerDevisRequest request) {
        ComProspection prospection = prospectionRepository.findById(prospectionId)
                .orElseThrow(() -> new ResourceNotFoundException("Prospection introuvable: " + prospectionId));

        Requis.champ(request.trancheId(), "trancheId est requis");
        CfgTranche tranche = trancheRepository.findById(request.trancheId())
                .orElseThrow(() -> new InvalidRequestException("CfgTranche introuvable: " + request.trancheId()));

        StkItem article = null;
        if (request.articleId() != null) {
            article = stkItemRepository.findById(request.articleId())
                    .orElseThrow(() -> new InvalidRequestException("Article introuvable: " + request.articleId()));
        }
        if (article == null && request.prix() == null) {
            throw new InvalidRequestException("prix est requis quand articleId n'est pas renseigné");
        }

        List<StkItem> sousArticles = List.of();
        if (article != null && request.sousArticleIds() != null && !request.sousArticleIds().isEmpty()) {
            sousArticles = stkItemRepository.findAllById(request.sousArticleIds());
            if (sousArticles.size() != request.sousArticleIds().size()) {
                throw new InvalidRequestException("Un ou plusieurs sous-articles sont introuvables");
            }
        }

        CfgTier prospect = prospection.getCfgProspect();
        if (request.prospectId() != null) {
            prospect = tierRepository.findById(request.prospectId())
                    .orElseThrow(() -> new InvalidRequestException("Prospect introuvable: " + request.prospectId()));
        }
        CfgTier commercial = prospection.getCfgCommercial();
        if (request.commercialId() != null) {
            commercial = tierRepository.findById(request.commercialId())
                    .orElseThrow(() -> new InvalidRequestException("Commercial introuvable: " + request.commercialId()));
        }
        ComPaymentMethod modePaiement = null;
        if (request.modePaiementId() != null) {
            modePaiement = paymentMethodRepository.findById(request.modePaiementId())
                    .orElseThrow(() -> new InvalidRequestException("ComPaymentMethod introuvable: " + request.modePaiementId()));
        }

        BigDecimal remisePercent = request.remisePercent() != null ? request.remisePercent() : BigDecimal.ZERO;
        MontantsCalcules montants = calculerMontants(article, sousArticles, request.prix(), remisePercent);

        StlQuote quote = new StlQuote();
        quote.setId(UUID.randomUUID());
        quote.setCode(genererCodeOffre());
        quote.setDocDate(LocalDateTime.now(clock));
        quote.setComProspection(prospection);
        quote.setCfgTranche(tranche);
        quote.setStkItem(article);
        quote.setCfgProspect(prospect);
        quote.setCfgCommercial(commercial);
        quote.setComPaymentMethod(modePaiement);
        quote.setNotes(request.notes());
        appliquerMontants(quote, montants);

        quote = quoteRepository.save(quote);
        persisterLignesSousArticles(quote, sousArticles, remisePercent);

        return toDto(quote);
    }

    /**
     * Lignes du tableau « SOUS ARTICLES » de la fiche Offre ; comme le legacy, la remise en % de
     * l'offre s'applique telle quelle à chaque sous-article.
     */
    private void persisterLignesSousArticles(StlQuote quote, List<StkItem> sousArticles, BigDecimal remisePercent) {
        for (StkItem sousArticle : sousArticles) {
            BigDecimal vatRatio = tauxTvaSousArticle(sousArticle, quote);
            BigDecimal priceBeforeDiscount = prixTtcOuZero(sousArticle);
            BigDecimal priceBeforeDiscountHT = prixHtOuDeduit(sousArticle, vatRatio);
            BigDecimal discountAmount = pourcentage(priceBeforeDiscount, remisePercent);
            BigDecimal discountAmountHT = pourcentage(priceBeforeDiscountHT, remisePercent);
            BigDecimal priceAfterDiscount = priceBeforeDiscount.subtract(discountAmount);
            BigDecimal priceAfterDiscountHT = priceBeforeDiscountHT.subtract(discountAmountHT);
            BigDecimal area = sousArticle.getArea();
            BigDecimal unitCost = parSurface(priceBeforeDiscount, area);
            BigDecimal unitCostHT = parSurface(priceBeforeDiscountHT, area);
            BigDecimal unitCostAfterDiscount = parSurface(priceAfterDiscount, area);
            BigDecimal unitCostAfterDiscountHT = parSurface(priceAfterDiscountHT, area);

            StlQuoteItem item = new StlQuoteItem();
            item.setId(UUID.randomUUID());
            item.setStlQuote(quote);
            item.setStkItem(sousArticle);
            item.setStkItemType(sousArticle.getStkItemType());
            item.setStkItemCategory(sousArticle.getStkItemCategory());
            item.setCode(sousArticle.getCode());
            item.setDescription(sousArticle.getDescription());
            item.setArea(area);
            item.setVatRatio(vatRatio);
            item.setUnitCost(unitCost);
            item.setUnitCostHT(unitCostHT);
            item.setPriceBeforeDiscount(priceBeforeDiscount);
            item.setPriceBeforeDiscountHT(priceBeforeDiscountHT);
            item.setDiscount(remisePercent);
            item.setDiscountAmount(discountAmount);
            item.setDiscountAmountHT(discountAmountHT);
            item.setUnitCostAfterDiscount(unitCostAfterDiscount);
            item.setUnitCostAfterDiscountHT(unitCostAfterDiscountHT);
            item.setPriceAfterDiscount(priceAfterDiscount);
            item.setPriceAfterDiscountHT(priceAfterDiscountHT);
            item.setAlwaysAffected(sousArticle.getAlwaysAffected());
            item.setIsUnderItem(Boolean.TRUE);
            quoteItemRepository.save(item);
        }
    }

    /** TVA du sous-article, à défaut celle de l'offre, à défaut zéro. */
    private static BigDecimal tauxTvaSousArticle(StkItem sousArticle, StlQuote quote) {
        if (sousArticle.getVat() != null) {
            return sousArticle.getVat();
        }
        return quote.getVatRatio() != null ? quote.getVatRatio() : BigDecimal.ZERO;
    }

    private static BigDecimal prixTtcOuZero(StkItem article) {
        return PrixArticle.ttcOuZero(article);
    }

    /** Prix de référence HT de l'article (renseigné, sinon déduit du TTC par la TVA), 0 sans prix. */
    private static BigDecimal prixHtOuDeduit(StkItem article, BigDecimal vatRatio) {
        return PrixArticle.htOuZero(article, vatRatio);
    }

    private static BigDecimal pourcentage(BigDecimal montant, BigDecimal pourcent) {
        return montant.multiply(pourcent).divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP);
    }

    /** Montant au m², ou null sans surface exploitable. */
    private static BigDecimal parSurface(BigDecimal montant, BigDecimal area) {
        return area != null && area.signum() != 0 ? montant.divide(area, 6, RoundingMode.HALF_UP) : null;
    }

    private record MontantsCalcules(
            BigDecimal area, BigDecimal vatRatio,
            BigDecimal unitCostHT, BigDecimal unitCost,
            BigDecimal itemPriceHT, BigDecimal itemPrice,
            BigDecimal underItemsPriceHT, BigDecimal underItemsPrice,
            BigDecimal totalHT, BigDecimal totalTTC,
            BigDecimal discountPercent, BigDecimal discountAmountHT, BigDecimal discountAmountTTC,
            BigDecimal unitCostAfterDiscountHT, BigDecimal unitCostAfterDiscountTTC,
            BigDecimal itemPriceAfterDiscountHT, BigDecimal itemPriceAfterDiscountTTC,
            BigDecimal underItemsPriceAfterDiscountHT, BigDecimal underItemsPriceAfterDiscountTTC,
            BigDecimal priceAfterDiscountHT, BigDecimal priceAfterDiscountTTC
    ) {
    }

    /**
     * Onglet « Montants » : {@code prixOverride} remplace le prix TTC de l'article (HT recalculé
     * par sa TVA) ; sans article, il est le prix TTC brut, sans ventilation HT/m²/TVA.
     */
    private static MontantsCalcules calculerMontants(StkItem article, List<StkItem> sousArticles,
                                                       BigDecimal prixOverride, BigDecimal remisePercent) {
        if (article == null) {
            BigDecimal prixVenteTTC = prixOverride;
            return new MontantsCalcules(
                    null, null, null, null, null, prixVenteTTC, BigDecimal.ZERO, BigDecimal.ZERO,
                    null, prixVenteTTC, remisePercent, null, BigDecimal.ZERO,
                    null, null, null, prixVenteTTC, BigDecimal.ZERO, BigDecimal.ZERO,
                    null, prixVenteTTC);
        }

        BigDecimal vatRatio = article.getVat() != null ? article.getVat() : BigDecimal.ZERO;
        BigDecimal area = article.getArea();

        BigDecimal itemPrice;
        BigDecimal itemPriceHT;
        if (prixOverride != null) {
            itemPrice = prixOverride;
            itemPriceHT = diviserParTaux(itemPrice, vatRatio);
        } else {
            itemPrice = prixTtcOuZero(article);
            itemPriceHT = prixHtOuDeduit(article, vatRatio);
        }

        BigDecimal unitCostHT = parSurface(itemPriceHT, area);
        BigDecimal unitCost = parSurface(itemPrice, area);

        BigDecimal underItemsPriceHT = BigDecimal.ZERO;
        BigDecimal underItemsPrice = BigDecimal.ZERO;
        for (StkItem sousArticle : sousArticles) {
            BigDecimal prixSousArticle = prixTtcOuZero(sousArticle);
            BigDecimal prixSousArticleHT = prixHtOuDeduit(sousArticle, vatRatio);
            underItemsPrice = underItemsPrice.add(prixSousArticle);
            underItemsPriceHT = underItemsPriceHT.add(prixSousArticleHT);
        }

        BigDecimal totalHT = itemPriceHT.add(underItemsPriceHT);
        BigDecimal totalTTC = itemPrice.add(underItemsPrice);

        BigDecimal discountAmountHT = pourcentage(totalHT, remisePercent);
        BigDecimal discountAmountTTC = pourcentage(totalTTC, remisePercent);

        // Remise répartie au prorata article / sous-articles, comme le legacy.
        BigDecimal partArticle = totalHT.signum() != 0
                ? itemPriceHT.divide(totalHT, 10, RoundingMode.HALF_UP) : BigDecimal.ONE;
        BigDecimal itemPriceAfterDiscountHT = itemPriceHT.subtract(discountAmountHT.multiply(partArticle))
                .setScale(6, RoundingMode.HALF_UP);
        BigDecimal itemPriceAfterDiscountTTC = itemPrice.subtract(discountAmountTTC.multiply(partArticle))
                .setScale(6, RoundingMode.HALF_UP);
        BigDecimal underItemsPriceAfterDiscountHT = underItemsPriceHT.subtract(discountAmountHT.multiply(BigDecimal.ONE.subtract(partArticle)))
                .setScale(6, RoundingMode.HALF_UP);
        BigDecimal underItemsPriceAfterDiscountTTC = underItemsPrice.subtract(discountAmountTTC.multiply(BigDecimal.ONE.subtract(partArticle)))
                .setScale(6, RoundingMode.HALF_UP);

        BigDecimal unitCostAfterDiscountHT = parSurface(itemPriceAfterDiscountHT, area);
        BigDecimal unitCostAfterDiscountTTC = parSurface(itemPriceAfterDiscountTTC, area);

        BigDecimal priceAfterDiscountHT = totalHT.subtract(discountAmountHT);
        BigDecimal priceAfterDiscountTTC = totalTTC.subtract(discountAmountTTC);

        return new MontantsCalcules(
                area, vatRatio, unitCostHT, unitCost, itemPriceHT, itemPrice,
                underItemsPriceHT, underItemsPrice, totalHT, totalTTC,
                remisePercent, discountAmountHT, discountAmountTTC,
                unitCostAfterDiscountHT, unitCostAfterDiscountTTC,
                itemPriceAfterDiscountHT, itemPriceAfterDiscountTTC,
                underItemsPriceAfterDiscountHT, underItemsPriceAfterDiscountTTC,
                priceAfterDiscountHT, priceAfterDiscountTTC);
    }

    private static BigDecimal diviserParTaux(BigDecimal montantTTC, BigDecimal vatRatio) {
        return PrixArticle.diviserParTaux(montantTTC, vatRatio);
    }

    private static void appliquerMontants(StlQuote quote, MontantsCalcules m) {
        quote.setArea(m.area());
        quote.setVatRatio(m.vatRatio());
        quote.setUnitCostHT(m.unitCostHT());
        quote.setUnitCost(m.unitCost());
        quote.setItemPriceHT(m.itemPriceHT());
        quote.setItemPrice(m.itemPrice());
        quote.setUnderItemsPriceHT(m.underItemsPriceHT());
        quote.setUnderItemsPrice(m.underItemsPrice());
        quote.setPriceBeforeDiscountHT(m.totalHT());
        quote.setPriceBeforeDiscount(m.totalTTC());
        quote.setDiscount(m.discountPercent());
        quote.setDiscountAmountHT(m.discountAmountHT());
        quote.setDiscountAmount(m.discountAmountTTC());
        quote.setUnitCostAfterDiscountHT(m.unitCostAfterDiscountHT());
        quote.setUnitCostAfterDiscount(m.unitCostAfterDiscountTTC());
        quote.setItemPriceAfterDiscountHT(m.itemPriceAfterDiscountHT());
        quote.setItemPriceAfterDiscount(m.itemPriceAfterDiscountTTC());
        quote.setUnderItemsPriceAfterDiscountHT(m.underItemsPriceAfterDiscountHT());
        quote.setUnderItemsPriceAfterDiscount(m.underItemsPriceAfterDiscountTTC());
        quote.setPriceAfterDiscountHT(m.priceAfterDiscountHT());
        quote.setPriceAfterDiscount(m.priceAfterDiscountTTC());
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

    private DevisDto toDto(StlQuote q) {
        StkItem article = q.getStkItem();
        CfgTranche tranche = q.getCfgTranche();
        ComProspection prospection = q.getComProspection();
        return new DevisDto(
                q.getId(),
                q.getCode(),
                q.getDocDate(),
                Champs.de(prospection, ComProspection::getId),
                Champs.de(prospection, ComProspection::getCode),
                Champs.de(tranche, CfgTranche::getId),
                Champs.de(tranche, CfgTranche::getCode),
                Champs.de(tranche, CfgTranche::getDescription),
                Champs.de(article, StkItem::getId),
                Champs.de(article, StkItem::getCode),
                Champs.de(article, StkItem::getDescription),
                Champs.de(article, StkItem::getArea),
                Champs.de(article, StkItem::getStkItemCategory, StkItemCategory::getDescription),
                Champs.de(article, StkItem::getStkItemType, StkItemType::getDescription),
                q.getCfgProspect() != null ? q.getCfgProspect().getId() : null,
                TierNomFormatter.nom(q.getCfgProspect()),
                q.getCfgCommercial() != null ? q.getCfgCommercial().getId() : null,
                TierNomFormatter.nom(q.getCfgCommercial()),
                q.getComPaymentMethod() != null ? q.getComPaymentMethod().getId() : null,
                q.getComPaymentMethod() != null ? q.getComPaymentMethod().getDescription() : null,
                toMontantsDto(q),
                calculerSchemaFinancement(q),
                q.getNotes()
        );
    }

    /**
     * Même calcul que le dossier ({@link SchemaFinancementCalculator}), sur le TTC après remise.
     * Sans vocation d'article, {@code exclureDuPrix} retombe sur la convention de la base :
     * seule FRAIS SYNDIC est exclue.
     */
    List<LigneSchemaFinancementDto> calculerSchemaFinancement(StlQuote q) {
        BigDecimal prix = q.getPriceAfterDiscount();
        if (prix == null) {
            return List.of();
        }
        StkItem article = q.getStkItem();
        UUID vocationId = article != null && article.getStkVocation() != null
                ? article.getStkVocation().getId() : null;

        return SchemaFinancementCalculator.calculer(q.getCfgTranche(), prix).stream()
                .map(ligne -> {
                    Boolean exclureDuPrix = vocationId != null
                            ? settlementCategoryRepository.findByCodeAndStkVocationId(ligne.categorieCode(), vocationId)
                                    .map(categorie -> categorie.getExcludeFromPrice())
                                    .orElse("FRAIS SYNDIC".equals(ligne.categorieCode()))
                            : "FRAIS SYNDIC".equals(ligne.categorieCode());
                    return new LigneSchemaFinancementDto(ligne.categorieCode(), ligne.montant(), exclureDuPrix);
                })
                .toList();
    }
}
