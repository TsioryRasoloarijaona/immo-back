package com.tsix.immocrm.services.prospection;

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
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * GAP-07 — Devis/offres proposées sur une Prospection ({@code StlQuote}), reproduisant le
 * formulaire Offre legacy (entête + onglet "Montants" HT/TTC).
 */
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
    private final com.tsix.immocrm.services.perimetre.PerimetreCommercialService perimetreService;

    public DevisService(ComProspectionRepository prospectionRepository, StlQuoteRepository quoteRepository,
                         StlQuoteItemRepository quoteItemRepository, CfgTrancheRepository trancheRepository,
                         StkItemRepository stkItemRepository, CfgTierRepository tierRepository,
                         ComPaymentMethodRepository paymentMethodRepository, AdmSequenceRepository sequenceRepository,
                         ComSettlementCategoryRepository settlementCategoryRepository,
                         com.tsix.immocrm.services.perimetre.PerimetreCommercialService perimetreService) {
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
     * Numérote une nouvelle offre comme le fait l'app legacy — confirmé sur données réelles:
     * {@code AdmSequence} (ligne {@code Code = "Offre"}) porte le compteur réel ({@code SeqValue}
     * = 313, qui correspond exactement au plus grand {@code StlQuote.Code} existant, "26/00313").
     * Compteur global, jamais réinitialisé par année (codes vus de "22/00001" à "26/00313" sans
     * repartir à 0) ; seule l'année affichée change ({@code BasedOnSystemYear = true} → année
     * système courante). Format: {@code "{année sur 2 chiffres}/{numéro sur 5 chiffres}"} (pas de
     * préfixe/suffixe pour ce type de document, contrairement à "Dossier de vente").
     */
    private String genererCodeOffre() {
        AdmSequence sequence = sequenceRepository.findByCode("Offre").orElseGet(() -> {
            // Auto-répare si la ligne de seed venait à manquer (ex. base de test fraîche) plutôt
            // que de bloquer toute création d'offre.
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
        int annee = LocalDateTime.now().getYear() % (int) Math.pow(10, largeurAnnee);
        String anneeFormatee = String.format("%0" + largeurAnnee + "d", annee);
        String numero = String.format("%0" + largeurNumero + "d", prochain);
        String prefixe = sequence.getSeqPrefix() != null ? sequence.getSeqPrefix() : "";
        String suffixe = sequence.getSeqSuffix() != null ? sequence.getSeqSuffix() : "";
        return prefixe + anneeFormatee + separateur + numero + suffixe;
    }

    /**
     * {@code GET /api/devis} — liste globale des offres tous prospections confondues, symétrique
     * de {@code GET /api/dossiers} (vs {@code GET /api/clients/{id}/dossiers} déjà existant pour
     * une seule prospection: {@link #findByProspection}). Mêmes conventions de filtre que
     * {@code DossierService.findAll}: {@code trancheId} répétable (OR), {@code search} en
     * LIKE insensible à la casse sur le code de l'offre.
     *
     * @param search      recherche libre sur {@code StlQuote.code}
     * @param trancheIds  {@code CfgTranche.id} — répétable, matche si l'offre porte une de ces
     *                    tranches
     * @param commercial  {@code CfgTier.id} (cfgCommercial) — filtre exact, une seule valeur
     * @param dateDebut   borne inférieure (incluse) sur {@code docDate}
     * @param dateFin     borne supérieure (incluse) sur {@code docDate}
     */
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
        // Périmètre commercial (§9bis.2) : cette liste globale porte tranche, commercial, montants
        // et pointe vers la prospection d'origine — elle exposait donc le pipeline de tous les
        // projets sous un préfixe d'URL que PerimetreInterceptor ne couvre pas. Prédicat neutre si
        // l'appelant n'est pas restreint.
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

        if (request.trancheId() == null) {
            throw new InvalidRequestException("trancheId est requis");
        }
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
        quote.setDocDate(LocalDateTime.now());
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
     * Persiste une {@link StlQuoteItem} par sous-article inclus dans l'offre, pour alimenter le
     * tableau "SOUS ARTICLES" de la Fiche Offre vente ({@code OffreDevisPdfRenderer}). La remise
     * est appliquée au même taux que la remise globale de l'offre sur chaque ligne (cohérent avec
     * le legacy: une seule remise en % saisie au niveau de l'offre, répercutée telle quelle sur
     * chaque sous-article).
     */
    private void persisterLignesSousArticles(StlQuote quote, List<StkItem> sousArticles, BigDecimal remisePercent) {
        for (StkItem sousArticle : sousArticles) {
            BigDecimal vatRatio = sousArticle.getVat() != null ? sousArticle.getVat()
                    : quote.getVatRatio() != null ? quote.getVatRatio() : BigDecimal.ZERO;
            BigDecimal priceBeforeDiscount = sousArticle.getComSalePrice() != null
                    ? sousArticle.getComSalePrice() : BigDecimal.ZERO;
            BigDecimal priceBeforeDiscountHT = sousArticle.getComSalePriceHT() != null
                    ? sousArticle.getComSalePriceHT() : diviserParTaux(priceBeforeDiscount, vatRatio);
            BigDecimal discountAmount = priceBeforeDiscount.multiply(remisePercent)
                    .divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP);
            BigDecimal discountAmountHT = priceBeforeDiscountHT.multiply(remisePercent)
                    .divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP);
            BigDecimal priceAfterDiscount = priceBeforeDiscount.subtract(discountAmount);
            BigDecimal priceAfterDiscountHT = priceBeforeDiscountHT.subtract(discountAmountHT);
            BigDecimal area = sousArticle.getArea();
            BigDecimal unitCost = area != null && area.signum() != 0
                    ? priceBeforeDiscount.divide(area, 6, RoundingMode.HALF_UP) : null;
            BigDecimal unitCostHT = area != null && area.signum() != 0
                    ? priceBeforeDiscountHT.divide(area, 6, RoundingMode.HALF_UP) : null;
            BigDecimal unitCostAfterDiscount = area != null && area.signum() != 0
                    ? priceAfterDiscount.divide(area, 6, RoundingMode.HALF_UP) : null;
            BigDecimal unitCostAfterDiscountHT = area != null && area.signum() != 0
                    ? priceAfterDiscountHT.divide(area, 6, RoundingMode.HALF_UP) : null;

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

    /** Résultat pur du calcul de l'onglet "Montants" — avant toute persistance. */
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
     * Calcule l'intégralité de l'onglet "Montants" du formulaire Offre legacy, à partir du prix
     * commercial de l'article ({@code ComSaleUnitCost(HT)}/{@code ComSalePrice(HT)}/{@code Vat} —
     * confirmé sur données réelles: {@code ComSaleUnitCostHT × Area == ComSalePriceHT} et
     * {@code ComSalePriceHT × (1+Vat) == ComSalePrice}) et de la somme des sous-articles inclus.
     * Si {@code prixOverride} est fourni, il remplace le prix commercial TTC de l'article comme
     * point de départ (HT recalculé via le taux de TVA de l'article). Sans article (offre libre),
     * {@code prixOverride} devient directement le prix de vente TTC, sans ventilation HT/m²/TVA.
     * La remise est répartie au prorata entre article et sous-articles pour les lignes "après
     * remise" — partagé par {@link #creerDevis} pour qu'aperçu et persistance ne divergent jamais.
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

        BigDecimal itemPrice = prixOverride != null ? prixOverride
                : article.getComSalePrice() != null ? article.getComSalePrice() : BigDecimal.ZERO;
        BigDecimal itemPriceHT = prixOverride != null
                ? diviserParTaux(itemPrice, vatRatio)
                : article.getComSalePriceHT() != null ? article.getComSalePriceHT() : diviserParTaux(itemPrice, vatRatio);

        BigDecimal unitCostHT = area != null && area.signum() != 0
                ? itemPriceHT.divide(area, 6, RoundingMode.HALF_UP) : null;
        BigDecimal unitCost = area != null && area.signum() != 0
                ? itemPrice.divide(area, 6, RoundingMode.HALF_UP) : null;

        BigDecimal underItemsPriceHT = BigDecimal.ZERO;
        BigDecimal underItemsPrice = BigDecimal.ZERO;
        for (StkItem sousArticle : sousArticles) {
            BigDecimal prixSousArticle = sousArticle.getComSalePrice() != null ? sousArticle.getComSalePrice() : BigDecimal.ZERO;
            BigDecimal prixSousArticleHT = sousArticle.getComSalePriceHT() != null
                    ? sousArticle.getComSalePriceHT() : diviserParTaux(prixSousArticle, vatRatio);
            underItemsPrice = underItemsPrice.add(prixSousArticle);
            underItemsPriceHT = underItemsPriceHT.add(prixSousArticleHT);
        }

        BigDecimal totalHT = itemPriceHT.add(underItemsPriceHT);
        BigDecimal totalTTC = itemPrice.add(underItemsPrice);

        BigDecimal discountAmountHT = totalHT.multiply(remisePercent)
                .divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP);
        BigDecimal discountAmountTTC = totalTTC.multiply(remisePercent)
                .divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP);

        // Répartit la remise au prorata entre article et sous-articles pour la ventilation
        // "après remise" (même principe que la répartition de remise du legacy).
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

        BigDecimal unitCostAfterDiscountHT = area != null && area.signum() != 0
                ? itemPriceAfterDiscountHT.divide(area, 6, RoundingMode.HALF_UP) : null;
        BigDecimal unitCostAfterDiscountTTC = area != null && area.signum() != 0
                ? itemPriceAfterDiscountTTC.divide(area, 6, RoundingMode.HALF_UP) : null;

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

    /** HT = TTC / (1 + taux). Renvoie TTC tel quel si le taux est nul/absent. */
    private static BigDecimal diviserParTaux(BigDecimal montantTTC, BigDecimal vatRatio) {
        if (vatRatio == null || vatRatio.signum() == 0) {
            return montantTTC;
        }
        return montantTTC.divide(BigDecimal.ONE.add(vatRatio), 6, RoundingMode.HALF_UP);
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
                prospection != null ? prospection.getId() : null,
                prospection != null ? prospection.getCode() : null,
                tranche != null ? tranche.getId() : null,
                tranche != null ? tranche.getCode() : null,
                tranche != null ? tranche.getDescription() : null,
                article != null ? article.getId() : null,
                article != null ? article.getCode() : null,
                article != null ? article.getDescription() : null,
                article != null ? article.getArea() : null,
                article != null && article.getStkItemCategory() != null ? article.getStkItemCategory().getDescription() : null,
                article != null && article.getStkItemType() != null ? article.getStkItemType().getDescription() : null,
                q.getCfgProspect() != null ? q.getCfgProspect().getId() : null,
                nomTier(q.getCfgProspect()),
                q.getCfgCommercial() != null ? q.getCfgCommercial().getId() : null,
                nomTier(q.getCfgCommercial()),
                q.getComPaymentMethod() != null ? q.getComPaymentMethod().getId() : null,
                q.getComPaymentMethod() != null ? q.getComPaymentMethod().getDescription() : null,
                toMontantsDto(q),
                calculerSchemaFinancement(q),
                q.getNotes()
        );
    }

    /**
     * Plan de financement (AUTO-FINANCEMENT / AVANCE / FRAIS SYNDIC) de l'offre, calculé sur son
     * prix de vente TTC après remise — même règle métier que {@code
     * DossierService#simulerSchemaFinancement} ({@link SchemaFinancementCalculator}, partagé pour
     * ne jamais afficher des montants différents entre l'offre et le futur Dossier). Vide si
     * l'offre n'a pas de prix. La catégorie ({@code ComSettlementCategory}) est résolue via la
     * vocation de l'article principal quand il y en a un; sans article (offre libre), on ne peut
     * pas la résoudre et {@code exclureDuPrix} retombe sur la convention observée en base
     * (seule FRAIS SYNDIC est exclue du TTC soumis à la règle de prix).
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

    private static String nomTier(CfgTier tier) {
        return com.tsix.immocrm.utils.TierNomFormatter.nom(tier);
    }
}
