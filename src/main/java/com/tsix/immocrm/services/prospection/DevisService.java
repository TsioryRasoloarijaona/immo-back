package com.tsix.immocrm.services.prospection;

import com.tsix.immocrm.domain.ConventionRemise;
import com.tsix.immocrm.domain.DisponibiliteArticle;
import com.tsix.immocrm.domain.PrixArticle;
import com.tsix.immocrm.domain.SchemaFinancementCalculator;
import com.tsix.immocrm.domain.SchemaFinancementSaisi;
import com.tsix.immocrm.dto.dossier.LigneSchemaFinancementDto;
import com.tsix.immocrm.dto.prospection.CreerDevisRequest;
import com.tsix.immocrm.dto.prospection.DevisDto;
import com.tsix.immocrm.dto.prospection.DossierLieDto;
import com.tsix.immocrm.dto.prospection.FicheDevisDto;
import com.tsix.immocrm.dto.prospection.ModifierDevisRequest;
import com.tsix.immocrm.dto.prospection.MontantsOffreDto;
import com.tsix.immocrm.dto.prospection.SousArticleOffreDto;
import com.tsix.immocrm.entities.adm.AdmSequence;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComPaymentMethod;
import com.tsix.immocrm.entities.com.ComProspection;
import com.tsix.immocrm.entities.com.ComSettlementCategory;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.entities.stk.StkItemCategory;
import com.tsix.immocrm.entities.stk.StkItemType;
import com.tsix.immocrm.entities.stl.StlQuote;
import com.tsix.immocrm.entities.stl.StlQuoteDocument;
import com.tsix.immocrm.entities.stl.StlQuoteItem;
import com.tsix.immocrm.exceptions.ConflictException;
import com.tsix.immocrm.exceptions.InvalidRequestException;
import com.tsix.immocrm.exceptions.ResourceNotFoundException;
import com.tsix.immocrm.repositories.adm.AdmSequenceRepository;
import com.tsix.immocrm.repositories.cfg.CfgTierRepository;
import com.tsix.immocrm.repositories.com.ComFolderRepository;
import com.tsix.immocrm.repositories.com.ComPaymentMethodRepository;
import com.tsix.immocrm.repositories.com.ComProspectionRepository;
import com.tsix.immocrm.repositories.com.ComSettlementCategoryRepository;
import com.tsix.immocrm.repositories.stl.StlQuoteDocumentRepository;
import com.tsix.immocrm.repositories.stl.StlQuoteItemRepository;
import com.tsix.immocrm.repositories.stl.StlQuoteRepository;
import com.tsix.immocrm.security.UtilisateurAffiche;
import com.tsix.immocrm.services.dossier.ReferencesOmran;
import com.tsix.immocrm.services.perimetre.ObjetsVisibles;
import com.tsix.immocrm.services.perimetre.PerimetreCommercialService;
import com.tsix.immocrm.utils.Champs;
import com.tsix.immocrm.utils.Requis;
import com.tsix.immocrm.utils.TierNomFormatter;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
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
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/** Offres d'une prospection ({@code StlQuote}) : le formulaire Offre legacy, entête et onglet « Montants ». */
@Service
@Transactional(readOnly = true)
public class DevisService {

    private static final String CODE = "code";
    private static final String DESCRIPTION = "description";
    private static final String OFFRE_INTROUVABLE = "Offre introuvable: ";
    private static final String FRAIS_SYNDIC = "FRAIS SYNDIC";

    private final ComProspectionRepository prospectionRepository;
    private final StlQuoteRepository quoteRepository;
    private final StlQuoteItemRepository quoteItemRepository;
    private final ObjetsVisibles objetsVisibles;
    private final CfgTierRepository tierRepository;
    private final ComPaymentMethodRepository paymentMethodRepository;
    private final AdmSequenceRepository sequenceRepository;
    private final ComSettlementCategoryRepository settlementCategoryRepository;
    private final StlQuoteDocumentRepository schemaRepository;
    private final ComFolderRepository folderRepository;
    private final PerimetreCommercialService perimetreService;
    private final ReferencesOmran referencesOmran;
    private final UtilisateurAffiche utilisateurAffiche;
    private final Clock clock;

    public DevisService(ComProspectionRepository prospectionRepository, DepotsDevis depots,
                         PerimetreCommercialService perimetreService, ObjetsVisibles objetsVisibles,
                         ReferencesOmran referencesOmran, UtilisateurAffiche utilisateurAffiche, Clock clock) {
        this.clock = clock;
        this.objetsVisibles = objetsVisibles;
        this.perimetreService = perimetreService;
        this.referencesOmran = referencesOmran;
        this.utilisateurAffiche = utilisateurAffiche;
        this.prospectionRepository = prospectionRepository;
        this.quoteRepository = depots.offres();
        this.quoteItemRepository = depots.lignes();
        this.tierRepository = depots.tiers();
        this.paymentMethodRepository = depots.modesPaiement();
        this.sequenceRepository = depots.sequences();
        this.settlementCategoryRepository = depots.categories();
        this.schemaRepository = depots.schemas();
        this.folderRepository = depots.dossiers();
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
                predicates.add(recherche(root, cb, search));
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

    /**
     * Code de l'offre, code et désignation de l'article, nom du prospect (mêmes colonnes que
     * {@link TierNomFormatter}), code de la prospection. Jointures externes : l'offre sans article reste trouvable.
     */
    private static Predicate recherche(Root<StlQuote> root, CriteriaBuilder cb, String search) {
        String motif = "%" + search.trim().toLowerCase(Locale.ROOT) + "%";
        Join<StlQuote, StkItem> article = root.join("stkItem", JoinType.LEFT);
        Join<StlQuote, CfgTier> prospect = root.join("cfgProspect", JoinType.LEFT);
        Join<StlQuote, ComProspection> prospection = root.join("comProspection", JoinType.LEFT);
        Expression<String> prenomNom = cb.concat(cb.concat(cb.coalesce(prospect.<String>get("firstName"), ""), " "),
                cb.coalesce(prospect.<String>get("lastName"), ""));
        List<Expression<String>> champs = List.of(
                root.get(CODE),
                article.get(CODE),
                article.get(DESCRIPTION),
                prospect.get("socialReason"),
                prospect.get(DESCRIPTION),
                prenomNom,
                prospection.get(CODE));
        return cb.or(champs.stream().map(champ -> cb.like(cb.lower(champ), motif)).toArray(Predicate[]::new));
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
        CfgTranche tranche = objetsVisibles.tranche(request.trancheId());
        perimetreService.verifierEcritureSurTranche(tranche, "Création d'offre sur la tranche " + tranche.getCode());

        StkItem article = articleValide(request.articleId(), tranche);
        if (article == null && request.prix() == null) {
            throw new InvalidRequestException("prix est requis quand articleId n'est pas renseigné");
        }
        List<StkItem> sousArticles = sousArticlesValides(article, request.sousArticleIds());

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
        ComPaymentMethod modePaiement = modePaiement(request.modePaiementId());

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
        completerCommeOmran(quote, article);
        appliquerMontants(quote, montants);

        quote = quoteRepository.save(quote);
        persisterLignesSousArticles(quote, sousArticles, remisePercent);

        return toDto(quote);
    }

    private ComPaymentMethod modePaiement(UUID modePaiementId) {
        if (modePaiementId == null) {
            return null;
        }
        return paymentMethodRepository.findById(modePaiementId)
                .orElseThrow(() -> new InvalidRequestException("ComPaymentMethod introuvable: " + modePaiementId));
    }

    // ---------------------------------------------------------------- fiche, modification, suppression

    /** Seule lecture par id des routes {@code /api/devis/{id}} : inconnue ou hors périmètre (règle de la liste) → 404. */
    private StlQuote offreVisible(UUID id) {
        Specification<StlQuote> parId = (root, query, cb) -> cb.equal(root.get("id"), id);
        return quoteRepository.findOne(parId.and(perimetreService.filtreDevis()))
                .orElseThrow(() -> new ResourceNotFoundException(OFFRE_INTROUVABLE + id));
    }

    public FicheDevisDto findFiche(UUID id) {
        return toFiche(offreVisible(id));
    }

    /**
     * Modification partielle : null = inchangé. Les montants ne sont recalculés (et les sous-articles réécrits)
     * que si l'article, les sous-articles, le prix ou la remise sont fournis. Tout se vérifie avant l'écriture.
     */
    @Transactional
    public FicheDevisDto modifierDevis(UUID id, ModifierDevisRequest request) {
        StlQuote quote = offreVisible(id);
        CfgTranche tranche = quote.getCfgTranche();
        if (request.trancheId() != null) {
            tranche = objetsVisibles.tranche(request.trancheId());
            perimetreService.verifierEcritureSurTranche(tranche, "Déplacement de l'offre vers la tranche " + tranche.getCode());
        }
        boolean changeArticle = changeDArticle(quote, request.articleId());
        StkItem article = changeArticle ? nouvelArticle(quote, request.articleId(), tranche) : quote.getStkItem();
        if (!changeArticle && article != null && tranche != null) {
            ObjetsVisibles.verifierTranche(article, tranche);
        }
        ComPaymentMethod modePaiement = request.modePaiementId() != null
                ? modePaiement(request.modePaiementId()) : quote.getComPaymentMethod();
        Recalcul recalcul = recalculer(quote, article, changeArticle, request);

        quote.setCfgTranche(tranche);
        quote.setComPaymentMethod(modePaiement);
        if (request.notes() != null) {
            quote.setNotes(StringUtils.hasText(request.notes()) ? request.notes() : null);
        }
        if (changeArticle) {
            quote.setStkItem(article);
            quote.setStkVocation(article.getStkVocation());
        }
        if (recalcul != null) {
            appliquerMontants(quote, recalcul.montants());
        }
        quote.setUserUpdate(utilisateurAffiche.courant().nom());
        quote.setUserUpdateDateTime(LocalDateTime.now(clock));
        quote = quoteRepository.save(quote);
        if (recalcul != null) {
            ecrireLignes(quote, recalcul);
        }
        return toFiche(quote);
    }

    /** {@code lignesFigees} non null : les sous-articles restent, seule leur remise est réappliquée. */
    private record Recalcul(MontantsCalcules montants, List<StkItem> sousArticles, BigDecimal remisePercent,
                            List<StlQuoteItem> lignesFigees) {
    }

    private void ecrireLignes(StlQuote quote, Recalcul recalcul) {
        if (recalcul.lignesFigees() != null) {
            for (StlQuoteItem ligne : recalcul.lignesFigees()) {
                appliquerRemiseLigne(ligne, recalcul.remisePercent());
                quoteItemRepository.save(ligne);
            }
            return;
        }
        quoteItemRepository.deleteAll(quoteItemRepository.findByStlQuoteIdOrderByCodeAsc(quote.getId()));
        persisterLignesSousArticles(quote, recalcul.sousArticles(), recalcul.remisePercent());
    }

    private static boolean changeDArticle(StlQuote quote, UUID articleId) {
        return articleId != null && (quote.getStkItem() == null || !quote.getStkItem().getId().equals(articleId));
    }

    /** Montants recalculés et schéma saisi vérifié ; null si rien de ce qui fait le prix ne change (schéma vérifié aussi). */
    private Recalcul recalculer(StlQuote quote, StkItem article, boolean changeArticle, ModifierDevisRequest request) {
        boolean recalcul = changeArticle || request.sousArticleIds() != null
                || request.prix() != null || request.remisePercent() != null;
        if (!recalcul) {
            verifierSchemaSaisi(quote.getId(), quote.getPriceAfterDiscount());
            return null;
        }
        BigDecimal remisePercent = remiseModifiee(quote, request.remisePercent());
        if (article != null && !changeArticle && request.sousArticleIds() == null) {
            // Ni l'article ni les sous-articles ne changent : leurs prix figés dans l'offre restent la base.
            List<StlQuoteItem> lignes = quoteItemRepository.findByStlQuoteIdOrderByCodeAsc(quote.getId());
            MontantsCalcules montants = appliquerRemise(baseFigee(quote, article, lignes, request.prix()), remisePercent);
            verifierSchemaSaisi(quote.getId(), montants.priceAfterDiscountTTC());
            return new Recalcul(montants, List.of(), remisePercent, lignes);
        }
        List<StkItem> sousArticles = sousArticlesModifies(quote, article, changeArticle, request.sousArticleIds());
        BigDecimal prix = request.prix();
        if (prix == null && !changeArticle) {
            prix = prixActuel(quote);
        }
        if (article == null && prix == null) {
            throw new InvalidRequestException("prix est requis quand l'offre n'a pas d'article");
        }
        MontantsCalcules montants = calculerMontants(article, sousArticles, prix, remisePercent);
        verifierSchemaSaisi(quote.getId(), montants.priceAfterDiscountTTC());
        return new Recalcul(montants, sousArticles, remisePercent, null);
    }

    /** Base de prix déjà retenue par l'offre ; {@code prix} fourni remplace seulement le prix TTC de l'article. */
    private static BasePrix baseFigee(StlQuote quote, StkItem article, List<StlQuoteItem> lignes, BigDecimal prix) {
        BigDecimal vatRatio = quote.getVatRatio() != null ? quote.getVatRatio()
                : Objects.requireNonNullElse(article.getVat(), BigDecimal.ZERO);
        BigDecimal itemPrice = prix != null ? prix : quote.getItemPrice();
        BigDecimal itemPriceHT = prix != null ? null : quote.getItemPriceHT();
        if (itemPrice == null) {
            itemPrice = prixTtcOuZero(article);
            itemPriceHT = prixHtOuDeduit(article, vatRatio);
        } else if (itemPriceHT == null) {
            itemPriceHT = diviserParTaux(itemPrice, vatRatio);
        }
        BigDecimal underItemsPrice = lignes.stream().map(StlQuoteItem::getPriceBeforeDiscount)
                .filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal underItemsPriceHT = lignes.stream().map(StlQuoteItem::getPriceBeforeDiscountHT)
                .filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal area = quote.getArea() != null ? quote.getArea() : article.getArea();
        return new BasePrix(area, vatRatio, itemPriceHT, itemPrice, underItemsPriceHT, underItemsPrice);
    }

    /**
     * Comme la création, sauf que l'offre ne peut plus changer d'article une fois un dossier créé depuis elle :
     * l'article est occupé par ce dossier (FEATURE-encaissements-offres-lot2.md A2).
     */
    private StkItem nouvelArticle(StlQuote quote, UUID articleId, CfgTranche tranche) {
        dossierLie(quote.getId()).ifPresent(dossier -> {
            throw new InvalidRequestException("L'offre a donné le dossier " + dossier.code()
                    + " : son article ne peut plus changer");
        });
        Requis.champ(tranche, "trancheId est requis pour choisir un article");
        return articleValide(articleId, tranche);
    }

    private List<StkItem> sousArticlesModifies(StlQuote quote, StkItem article, boolean changeArticle,
                                               List<UUID> sousArticleIds) {
        if (sousArticleIds != null) {
            return sousArticlesValides(article, sousArticleIds);
        }
        if (changeArticle || article == null) {
            return List.of();
        }
        return quoteItemRepository.findByStlQuoteIdOrderByCodeAsc(quote.getId()).stream()
                .map(StlQuoteItem::getStkItem)
                .filter(Objects::nonNull)
                .toList();
    }

    private static BigDecimal remiseModifiee(StlQuote quote, BigDecimal demandee) {
        if (demandee != null) {
            return demandee;
        }
        BigDecimal actuelle = ConventionRemise.pourcentage(quote.getDiscount());
        return actuelle != null ? actuelle : BigDecimal.ZERO;
    }

    /** Prix TTC de base déjà retenu par l'offre : prix article (avec article), sinon prix de vente avant remise. */
    private static BigDecimal prixActuel(StlQuote quote) {
        if (quote.getStkItem() != null || quote.getItemPrice() != null) {
            return quote.getItemPrice();
        }
        return quote.getPriceBeforeDiscount();
    }

    /**
     * {@code VerifSchema} d'OMRAN ({@code Yes.Stl.Credit/Quote.cs}) : un schéma saisi sur l'offre doit égaler le prix
     * après remise, sur ses seules catégories d'encaissement AVANCE, CRÉDIT et AUTO-FINANCEMENT.
     */
    private void verifierSchemaSaisi(UUID offreId, BigDecimal prixApresRemise) {
        List<StlQuoteDocument> lignes = schemaRepository.findByStlQuoteIdOrderByCodeAsc(offreId);
        if (lignes.isEmpty()) {
            return;
        }
        BigDecimal ecart = SchemaFinancementSaisi.ecart(prixApresRemise, lignes);
        if (ecart.signum() > 0) {
            throw new InvalidRequestException("Votre schéma de financement ne correspond pas au montant contractuel : "
                    + "vous devez ajouter dans le schéma de financement un montant de " + ecart.toPlainString() + " DH");
        }
        if (ecart.signum() < 0) {
            throw new InvalidRequestException("Votre schéma de financement ne correspond pas au montant contractuel : "
                    + "vous devez soustraire du schéma de financement un montant de " + ecart.negate().toPlainString()
                    + " DH");
        }
    }

    /**
     * Comme OMRAN, une offre qui a donné un dossier (vente ou location) ne se supprime pas : les deux FK sont sans
     * cascade. Sous-articles et schéma partent avec l'offre ; le compteur {@code AdmSequence} n'est pas rendu.
     */
    @Transactional
    public void supprimerDevis(UUID id) {
        StlQuote quote = offreVisible(id);
        dossierLie(id).ifPresent(dossier -> {
            throw new ConflictException("Cette offre a donné le dossier " + dossier.code()
                    + " : elle ne peut pas être supprimée");
        });
        schemaRepository.deleteAll(schemaRepository.findByStlQuoteIdOrderByCodeAsc(id));
        quoteItemRepository.deleteAll(quoteItemRepository.findByStlQuoteIdOrderByCodeAsc(id));
        quoteRepository.delete(quote);
    }

    /** Dossier de vente d'abord, sinon de location ; vide si l'offre n'a donné aucun dossier. */
    private Optional<DossierLieDto> dossierLie(UUID offreId) {
        List<Object[]> ventes = folderRepository.dossiersDeLOffre(offreId);
        if (!ventes.isEmpty()) {
            return Optional.of(new DossierLieDto((UUID) ventes.get(0)[0], (String) ventes.get(0)[1], false));
        }
        return folderRepository.locationsDeLOffre(offreId).stream().findFirst()
                .map(code -> new DossierLieDto(null, code, true));
    }

    private FicheDevisDto toFiche(StlQuote quote) {
        List<SousArticleOffreDto> sousArticles = quoteItemRepository.findByStlQuoteIdOrderByCodeAsc(quote.getId())
                .stream()
                .map(DevisService::toSousArticle)
                .toList();
        boolean schemaSaisi = !schemaRepository.findByStlQuoteIdOrderByCodeAsc(quote.getId()).isEmpty();
        return new FicheDevisDto(toDto(quote), sousArticles, dossierLie(quote.getId()).orElse(null), schemaSaisi);
    }

    private static SousArticleOffreDto toSousArticle(StlQuoteItem ligne) {
        return new SousArticleOffreDto(
                ligne.getId(),
                Champs.de(ligne.getStkItem(), StkItem::getId),
                ligne.getCode(),
                ligne.getDescription(),
                Champs.de(ligne.getStkItemCategory(), StkItemCategory::getDescription),
                ligne.getArea(),
                ligne.getPriceBeforeDiscount(),
                ligne.getDiscountAmount(),
                ligne.getPriceAfterDiscountHT(),
                ligne.getPriceAfterDiscount());
    }

    /** Colonnes qu'OMRAN écrit sur toute offre : entité « ComQuote », vocation de l'article, auteur. */
    private void completerCommeOmran(StlQuote quote, StkItem article) {
        quote.setStrEntityId(referencesOmran.entiteOffreVente());
        quote.setStkVocation(article != null ? article.getStkVocation() : null);
        String auteur = utilisateurAffiche.courant().nom();
        LocalDateTime maintenant = LocalDateTime.now(clock);
        quote.setUserCreate(auteur);
        quote.setUserCreateDateTime(maintenant);
        quote.setUserUpdate(auteur);
        quote.setUserUpdateDateTime(maintenant);
    }

    /** Article de la tranche choisie et encore disponible ; null pour une offre à prix saisi. */
    private StkItem articleValide(UUID articleId, CfgTranche tranche) {
        if (articleId == null) {
            return null;
        }
        StkItem article = objetsVisibles.articleDeLaTranche(articleId, tranche);
        if (DisponibiliteArticle.estOccupe(article)) {
            throw new InvalidRequestException("L'article " + article.getCode() + " est déjà vendu (lié à un dossier)");
        }
        return article;
    }

    /** Sous-articles demandés, tous enfants de l'article ({@code StkItem.ParentId}). */
    private List<StkItem> sousArticlesValides(StkItem article, List<UUID> sousArticleIds) {
        if (article == null || sousArticleIds == null || sousArticleIds.isEmpty()) {
            return List.of();
        }
        List<StkItem> sousArticles = objetsVisibles.articles(sousArticleIds.stream().distinct().toList(),
                "sousArticleIds");
        for (StkItem sousArticle : sousArticles) {
            if (sousArticle.getParent() == null || !article.getId().equals(sousArticle.getParent().getId())) {
                throw new InvalidRequestException("Le sous-article " + sousArticle.getCode()
                        + " n'est pas un sous-article de " + article.getCode());
            }
        }
        return sousArticles;
    }

    /**
     * Lignes du tableau « SOUS ARTICLES » de la fiche Offre ; comme le legacy, la remise en % de
     * l'offre s'applique telle quelle à chaque sous-article.
     */
    private void persisterLignesSousArticles(StlQuote quote, List<StkItem> sousArticles, BigDecimal remisePercent) {
        for (StkItem sousArticle : sousArticles) {
            BigDecimal vatRatio = tauxTvaSousArticle(sousArticle, quote);
            StlQuoteItem item = new StlQuoteItem();
            item.setId(UUID.randomUUID());
            item.setStlQuote(quote);
            item.setStkItem(sousArticle);
            item.setStkItemType(sousArticle.getStkItemType());
            item.setStkItemCategory(sousArticle.getStkItemCategory());
            item.setCode(sousArticle.getCode());
            item.setDescription(sousArticle.getDescription());
            item.setArea(sousArticle.getArea());
            item.setVatRatio(vatRatio);
            item.setPriceBeforeDiscount(prixTtcOuZero(sousArticle));
            item.setPriceBeforeDiscountHT(prixHtOuDeduit(sousArticle, vatRatio));
            item.setAlwaysAffected(sousArticle.getAlwaysAffected());
            item.setIsUnderItem(Boolean.TRUE);
            appliquerRemiseLigne(item, remisePercent);
            quoteItemRepository.save(item);
        }
    }

    /** Remise de l'offre sur une ligne, à partir de ses prix avant remise (figés dans la ligne). */
    private static void appliquerRemiseLigne(StlQuoteItem item, BigDecimal remisePercent) {
        BigDecimal priceBeforeDiscount = Objects.requireNonNullElse(item.getPriceBeforeDiscount(), BigDecimal.ZERO);
        BigDecimal priceBeforeDiscountHT = Objects.requireNonNullElse(item.getPriceBeforeDiscountHT(), BigDecimal.ZERO);
        BigDecimal discountAmount = pourcentage(priceBeforeDiscount, remisePercent);
        BigDecimal discountAmountHT = pourcentage(priceBeforeDiscountHT, remisePercent);
        BigDecimal priceAfterDiscount = priceBeforeDiscount.subtract(discountAmount);
        BigDecimal priceAfterDiscountHT = priceBeforeDiscountHT.subtract(discountAmountHT);
        BigDecimal area = item.getArea();
        item.setUnitCost(parSurface(priceBeforeDiscount, area));
        item.setUnitCostHT(parSurface(priceBeforeDiscountHT, area));
        item.setDiscount(ConventionRemise.ratioDepuisPourcentage(remisePercent));
        item.setDiscountAmount(discountAmount);
        item.setDiscountAmountHT(discountAmountHT);
        item.setUnitCostAfterDiscount(parSurface(priceAfterDiscount, area));
        item.setUnitCostAfterDiscountHT(parSurface(priceAfterDiscountHT, area));
        item.setPriceAfterDiscount(priceAfterDiscount);
        item.setPriceAfterDiscountHT(priceAfterDiscountHT);
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
        BigDecimal itemPrice;
        BigDecimal itemPriceHT;
        if (prixOverride != null) {
            itemPrice = prixOverride;
            itemPriceHT = diviserParTaux(itemPrice, vatRatio);
        } else {
            itemPrice = prixTtcOuZero(article);
            itemPriceHT = prixHtOuDeduit(article, vatRatio);
        }

        BigDecimal underItemsPriceHT = BigDecimal.ZERO;
        BigDecimal underItemsPrice = BigDecimal.ZERO;
        for (StkItem sousArticle : sousArticles) {
            underItemsPrice = underItemsPrice.add(prixTtcOuZero(sousArticle));
            underItemsPriceHT = underItemsPriceHT.add(prixHtOuDeduit(sousArticle, vatRatio));
        }
        return appliquerRemise(new BasePrix(article.getArea(), vatRatio, itemPriceHT, itemPrice,
                underItemsPriceHT, underItemsPrice), remisePercent);
    }

    /** Prix avant remise d'une offre avec article : article et sous-articles, HT et TTC. */
    private record BasePrix(BigDecimal area, BigDecimal vatRatio, BigDecimal itemPriceHT, BigDecimal itemPrice,
                            BigDecimal underItemsPriceHT, BigDecimal underItemsPrice) {
    }

    /** Seul calcul de la remise d'une offre avec article : création et modification passent ici. */
    private static MontantsCalcules appliquerRemise(BasePrix base, BigDecimal remisePercent) {
        BigDecimal area = base.area();
        BigDecimal vatRatio = base.vatRatio();
        BigDecimal itemPrice = base.itemPrice();
        BigDecimal itemPriceHT = base.itemPriceHT();
        BigDecimal underItemsPrice = base.underItemsPrice();
        BigDecimal underItemsPriceHT = base.underItemsPriceHT();
        BigDecimal unitCostHT = parSurface(itemPriceHT, area);
        BigDecimal unitCost = parSurface(itemPrice, area);

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
        quote.setDiscount(ConventionRemise.ratioDepuisPourcentage(m.discountPercent()));
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

    /** Aussi lu par le PDF de l'offre ({@code OffreDevisDocumentService}) : une seule lecture de la remise. */
    static MontantsOffreDto toMontantsDto(StlQuote q) {
        return new MontantsOffreDto(
                q.getVatRatio(),
                q.getUnitCostHT(), q.getUnitCost(),
                q.getItemPriceHT(), q.getItemPrice(),
                q.getUnderItemsPriceHT(), q.getUnderItemsPrice(),
                q.getPriceBeforeDiscountHT(), q.getPriceBeforeDiscount(),
                ConventionRemise.pourcentage(q.getDiscount()), q.getDiscountAmountHT(), q.getDiscountAmount(),
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
                schemaFinancement(q),
                q.getNotes(),
                q.getUserCreate(),
                q.getUserCreateDateTime(),
                q.getUserUpdate(),
                q.getUserUpdateDateTime()
        );
    }

    /** Le schéma saisi dans OMRAN ({@code StlQuoteDocument}) s'il existe, sinon le schéma calculé depuis la tranche. */
    List<LigneSchemaFinancementDto> schemaFinancement(StlQuote q) {
        List<StlQuoteDocument> saisi = schemaRepository.findByStlQuoteIdOrderByCodeAsc(q.getId());
        if (saisi.isEmpty()) {
            return calculerSchemaFinancement(q);
        }
        return saisi.stream()
                .map(ligne -> {
                    ComSettlementCategory categorie = ligne.getComSettlementCategory();
                    return new LigneSchemaFinancementDto(
                            Champs.de(categorie, ComSettlementCategory::getCode),
                            ligne.getAmount(),
                            categorie != null && Boolean.TRUE.equals(categorie.getExcludeFromPrice()));
                })
                .toList();
    }

    /**
     * Même calcul que le dossier ({@link SchemaFinancementCalculator}), sur le TTC après remise.
     * Sans vocation d'article, {@code exclureDuPrix} retombe sur la convention de la base :
     * seule FRAIS SYNDIC est exclue.
     */
    private List<LigneSchemaFinancementDto> calculerSchemaFinancement(StlQuote q) {
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
                                    .orElse(FRAIS_SYNDIC.equals(ligne.categorieCode()))
                            : FRAIS_SYNDIC.equals(ligne.categorieCode());
                    return new LigneSchemaFinancementDto(ligne.categorieCode(), ligne.montant(), exclureDuPrix);
                })
                .toList();
    }
}
