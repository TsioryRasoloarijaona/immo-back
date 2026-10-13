package com.tsix.immocrm.services.dossier.fiche;

import com.tsix.immocrm.domain.StatutDossier;
import com.tsix.immocrm.dto.dossier.reajustement.LigneReajustementDto;
import com.tsix.immocrm.dto.dossier.reajustement.LigneReajustementDto.LigneCible;
import com.tsix.immocrm.entities.cfg.CfgProject;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComDocument;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.com.ComFolderItem;
import com.tsix.immocrm.entities.com.ComSettlementCategory;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.entities.stk.StkVocation;
import com.tsix.immocrm.exceptions.ConflictException;
import com.tsix.immocrm.exceptions.InvalidRequestException;
import com.tsix.immocrm.exceptions.ResourceNotFoundException;
import com.tsix.immocrm.repositories.cfg.TrancheVisibleRepository;
import com.tsix.immocrm.repositories.com.CategorieAutofinancementRepository;
import com.tsix.immocrm.repositories.com.ComDocumentRepository;
import com.tsix.immocrm.repositories.com.ComFolderItemRepository;
import com.tsix.immocrm.repositories.com.ComFolderRepository;
import com.tsix.immocrm.services.perimetre.PerimetreCommercialService;
import com.tsix.immocrm.utils.Champs;
import com.tsix.immocrm.utils.TierNomFormatter;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * « Réajuster les superficies » d'une tranche ({@code ReinitialiseFolders.cs}) : seule la superficie déclenche
 * un réajustement ; l'écart de prix va sur la dernière ligne d'autofinancement, sinon de crédit, sinon sur une
 * nouvelle ligne d'autofinancement. Échéancier écrit ici directement (pas par EcheancierService). FEATURE §B6.
 */
@Service
public class ReajustementSuperficiesService {

    private static final int DECIMALES = 2;
    private static final int LONGUEUR_NOTES = 1000;
    static final String MOTIF_SANS_PRIX_M2 = "Prix du m² absent sur le dossier : réajustement impossible";
    private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final EcritureDossier ecriture;
    private final TrancheVisibleRepository trancheRepository;
    private final ComFolderRepository folderRepository;
    private final ComFolderItemRepository folderItemRepository;
    private final ComDocumentRepository documentRepository;
    private final CategorieAutofinancementRepository categorieRepository;
    private final PerimetreCommercialService perimetreService;

    @SuppressWarnings("java:S107") // un dépôt par table lue ou écrite
    public ReajustementSuperficiesService(EcritureDossier ecriture, TrancheVisibleRepository trancheRepository,
                                          ComFolderRepository folderRepository,
                                          ComFolderItemRepository folderItemRepository,
                                          ComDocumentRepository documentRepository,
                                          CategorieAutofinancementRepository categorieRepository,
                                          PerimetreCommercialService perimetreService) {
        this.ecriture = ecriture;
        this.trancheRepository = trancheRepository;
        this.folderRepository = folderRepository;
        this.folderItemRepository = folderItemRepository;
        this.documentRepository = documentRepository;
        this.categorieRepository = categorieRepository;
        this.perimetreService = perimetreService;
    }

    @Transactional(readOnly = true)
    public List<LigneReajustementDto> verifier(UUID trancheId) {
        return dossiersDeLaTranche(trancheVisible(trancheId)).stream()
                .map(this::planifier)
                .flatMap(Optional::stream)
                .map(Plan::ligne)
                .toList();
    }

    /** Tout ou rien : chaque dossier coché est vérifié avant la première écriture. */
    @Transactional
    public List<LigneReajustementDto> validerReajustement(UUID trancheId, List<UUID> dossierIds) {
        if (dossierIds == null || dossierIds.isEmpty()) {
            throw new InvalidRequestException("dossierIds est requis et ne peut pas être vide");
        }
        Map<UUID, ComFolder> dossiers = dossiersDeLaTranche(trancheVisible(trancheId)).stream()
                .collect(Collectors.toMap(ComFolder::getId, Function.identity()));
        List<Plan> plans = new ArrayList<>();
        for (UUID id : dossierIds.stream().distinct().toList()) {
            ComFolder dossier = dossiers.get(id);
            if (dossier == null) {
                throw new ResourceNotFoundException("Dossier introuvable dans cette tranche: " + id);
            }
            Plan plan = planifier(dossier).orElseThrow(() -> new ConflictException(
                    "Le dossier " + dossier.getCode() + " n'a aucun écart de superficie à réajuster."));
            if (plan.ligne().motifExclusion() != null) {
                throw new ConflictException("Le dossier " + dossier.getCode() + " : " + plan.ligne().motifExclusion());
            }
            plans.add(plan);
        }
        String auteur = ecriture.auteur().nom();
        LocalDateTime maintenant = ecriture.maintenant();
        return plans.stream().map(p -> appliquer(p, auteur, maintenant)).toList();
    }

    private CfgTranche trancheVisible(UUID trancheId) {
        CfgTranche tranche = trancheRepository.trancheVisible(trancheId)
                .orElseThrow(() -> new ResourceNotFoundException("Tranche introuvable: " + trancheId));
        UUID projetId = Champs.de(tranche.getCfgProject(), CfgProject::getId);
        boolean horsPerimetre = perimetreService.perimetreCourant()
                .map(ids -> projetId != null && !ids.contains(projetId))
                .orElse(false);
        if (horsPerimetre) {
            throw new ResourceNotFoundException("Tranche introuvable: " + trancheId);
        }
        return tranche;
    }

    /** Dossiers actifs dont l'article principal est dans la tranche, au périmètre de l'appelant. */
    private List<ComFolder> dossiersDeLaTranche(CfgTranche tranche) {
        Specification<ComFolder> dansLaTranche = (root, query, cb) ->
                cb.equal(root.get("stkItem").get("stkHierarchy").get("id"), tranche.getId());
        return folderRepository.findAll(dansLaTranche.and(perimetreService.filtreDossiers())).stream()
                .filter(Predicate.not(ReajustementSuperficiesService::estSorti))
                .sorted(Comparator.comparing(f -> Objects.requireNonNullElse(f.getCode(), "")))
                .toList();
    }

    private static boolean estSorti(ComFolder f) {
        if (f.getStrStatus() == null || f.getStrStatus().getCode() == null) {
            return false;
        }
        try {
            return StatutDossier.fromCode(f.getStrStatus().getCode()).isSortie();
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private record Plan(ComFolder dossier, RecalculPrixDossier.Resultat resultat, List<ComFolderItem> sousArticles,
                        BigDecimal difference, Optional<ComDocument> cible, LigneReajustementDto ligne) {
    }

    /**
     * Vide si la superficie n'a pas changé ou si le schéma n'a aucun écart (le légataire écarte alors la ligne).
     * Un dossier dont le nouveau prix ne peut pas être calculé revient avec son motif, sans écart ni ligne cible.
     */
    private Optional<Plan> planifier(ComFolder dossier) {
        StkItem article = dossier.getStkItem();
        if (article == null || dossier.getStrStatus() == null) {
            return Optional.empty();
        }
        BigDecimal ancienne = arrondi(dossier.getArea());
        BigDecimal nouvelle = article.getArea() != null ? arrondi(article.getArea()) : ancienne;
        List<String> notes = new ArrayList<>();
        if (ancienne.compareTo(nouvelle) != 0) {
            notes.add("Article %s : ancienne superficie %s => nouvelle superficie %s"
                    .formatted(article.getCode(), ancienne, nouvelle));
        }
        List<ComFolderItem> sousArticles = RecalculPrixDossier.sousArticlesDe(
                folderItemRepository.findByComFolderId(dossier.getId()));
        List<String> sansPrix = new ArrayList<>();
        List<RecalculPrixDossier.SousArticle> sousSaisis = sousArticles.stream()
                .map(i -> sousArticleReajuste(i, notes, sansPrix)).toList();
        if (notes.isEmpty()) {
            return Optional.empty();
        }
        RecalculPrixDossier.Saisie actuelle = RecalculPrixDossier.depuisDossier(dossier, sousArticles);
        RecalculPrixDossier.Resultat resultat = RecalculPrixDossier.calculer(new RecalculPrixDossier.Saisie(
                nouvelle, actuelle.tva(), actuelle.prixM2HT(), actuelle.remiseMontantHT(),
                actuelle.avecModifications(), actuelle.modificationsHT(), actuelle.modificationsTTC(), sousSaisis));
        String motif = motifExclusion(actuelle, sansPrix, resultat);
        if (motif != null) {
            LigneReajustementDto exclue = new LigneReajustementDto(dossier.getId(), dossier.getCode(),
                    article.getCode(), TierNomFormatter.nom(dossier.getCfgCustomer()), ancienne, nouvelle,
                    nouvelle.subtract(ancienne), dossier.getPrice(), null, null, null, null, null, null,
                    String.join("\n", notes), motif);
            return Optional.of(new Plan(dossier, resultat, sousArticles, BigDecimal.ZERO, Optional.empty(), exclue));
        }

        List<ComDocument> echeances = documentRepository.findByComFolderIdOrderByDocDateAsc(dossier.getId());
        BigDecimal difference = arrondi(resultat.prixVenteTTC().subtract(totalSchema(echeances)));
        if (difference.signum() == 0) {
            return Optional.empty();
        }
        Optional<ComDocument> cible = derniere(echeances, c -> Boolean.TRUE.equals(c.getIsSaleAutoFinancment()))
                .or(() -> derniere(echeances, c -> Boolean.TRUE.equals(c.getIsSaleCredit())));
        LigneCible type = cible.map(d -> Boolean.TRUE.equals(d.getComSettlementCategory().getIsSaleAutoFinancment())
                ? LigneCible.AUTOFINANCEMENT : LigneCible.CREDIT).orElse(LigneCible.NOUVELLE_LIGNE_AUTOFINANCEMENT);
        BigDecimal ancienPrix = Objects.requireNonNullElse(dossier.getPrice(), BigDecimal.ZERO);
        LigneReajustementDto ligne = new LigneReajustementDto(dossier.getId(), dossier.getCode(), article.getCode(),
                TierNomFormatter.nom(dossier.getCfgCustomer()), ancienne, nouvelle, nouvelle.subtract(ancienne),
                ancienPrix, resultat.prix(), resultat.prix().subtract(ancienPrix), difference, type,
                cible.map(ComDocument::getId).orElse(null), cible.map(ComDocument::getDocDate).orElse(null),
                String.join("\n", notes), null);
        return Optional.of(new Plan(dossier, resultat, sousArticles, difference, cible, ligne));
    }

    /**
     * Le nouveau prix vaut superficie × prix du m² HT du dossier : sans ce prix (dossier saisi au forfait) ou sans
     * prix de stock pour un sous-article, il tomberait à 0 et le schéma serait vidé. Voir FEATURE-vente-fiche.md §B6.
     */
    private static String motifExclusion(RecalculPrixDossier.Saisie actuelle, List<String> sansPrix,
                                         RecalculPrixDossier.Resultat resultat) {
        if (actuelle.prixM2HT() == null || actuelle.prixM2HT().signum() <= 0) {
            return MOTIF_SANS_PRIX_M2;
        }
        if (!sansPrix.isEmpty()) {
            return "Prix de stock absent pour le sous-article " + String.join(", ", sansPrix)
                    + " : réajustement impossible";
        }
        if (resultat.prixVenteTTC().signum() <= 0) {
            return "Nouveau prix nul ou négatif : réajustement impossible";
        }
        return null;
    }

    /** Comme le légataire : superficie du stock si elle diffère, et alors prix HT = prix TTC du stock / (1 + TVA). */
    private static RecalculPrixDossier.SousArticle sousArticleReajuste(ComFolderItem i, List<String> notes,
                                                                      List<String> sansPrix) {
        StkItem stock = i.getStkItem();
        BigDecimal ancienne = arrondi(i.getArea());
        if (stock == null || stock.getArea() == null || arrondi(stock.getArea()).compareTo(ancienne) == 0) {
            return new RecalculPrixDossier.SousArticle(i.getId(), i.getArea(), i.getVATRatio(),
                    i.getPriceBeforeDiscountHT());
        }
        BigDecimal nouvelle = arrondi(stock.getArea());
        notes.add("Sous article %s : ancienne superficie %s => nouvelle superficie %s"
                .formatted(i.getCode(), ancienne, nouvelle));
        if (stock.getSalePrice() == null || stock.getSalePrice().signum() <= 0) {
            sansPrix.add(Objects.requireNonNullElse(i.getCode(), stock.getCode()));
            return new RecalculPrixDossier.SousArticle(i.getId(), nouvelle, i.getVATRatio(),
                    i.getPriceBeforeDiscountHT());
        }
        BigDecimal prixHT = stock.getSalePrice().divide(BigDecimal.ONE.add(Objects.requireNonNullElse(i.getVATRatio(),
                BigDecimal.ZERO)), DECIMALES, RoundingMode.HALF_UP);
        return new RecalculPrixDossier.SousArticle(i.getId(), nouvelle, i.getVATRatio(), prixHT);
    }

    /** Avance + crédit + autofinancement du schéma, signés comme le légataire (une catégorie au débit soustrait). */
    static BigDecimal totalSchema(List<ComDocument> echeances) {
        return echeances.stream()
                .filter(d -> d.getComSettlementCategory() != null && entreDansLePrix(d.getComSettlementCategory()))
                .map(d -> {
                    BigDecimal montant = Objects.requireNonNullElse(d.getAmount(), BigDecimal.ZERO);
                    return Boolean.TRUE.equals(d.getComSettlementCategory().getIsCredit()) ? montant : montant.negate();
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static boolean entreDansLePrix(ComSettlementCategory c) {
        return Boolean.TRUE.equals(c.getIsSaleAdvance()) || Boolean.TRUE.equals(c.getIsSaleCredit())
                || Boolean.TRUE.equals(c.getIsSaleAutoFinancment());
    }

    /** Dernière échéance (par date) de la catégorie demandée, au crédit. */
    private static Optional<ComDocument> derniere(List<ComDocument> echeances,
                                                  Predicate<ComSettlementCategory> drapeau) {
        return echeances.stream()
                .filter(d -> d.getComSettlementCategory() != null
                        && drapeau.test(d.getComSettlementCategory())
                        && Boolean.TRUE.equals(d.getComSettlementCategory().getIsCredit()))
                .max(Comparator.comparing(ComDocument::getDocDate, Comparator.nullsFirst(Comparator.naturalOrder())));
    }

    private LigneReajustementDto appliquer(Plan p, String auteur, LocalDateTime maintenant) {
        ComFolder dossier = p.dossier();
        RecalculPrixDossier.appliquer(p.resultat(), dossier, p.sousArticles().stream()
                .collect(Collectors.toMap(ComFolderItem::getId, Function.identity(), (a, b) -> a, LinkedHashMap::new)));
        folderItemRepository.saveAll(p.sousArticles());
        String note = "Document modifié lors du réajustement des superficies par %s le %s"
                .formatted(Objects.requireNonNullElse(auteur, "système"), maintenant.format(FORMAT));
        ComDocument ligne = p.cible().orElseGet(() -> nouvelleLigne(dossier, auteur, maintenant));
        ligne.setAmount(Objects.requireNonNullElse(ligne.getAmount(), BigDecimal.ZERO).add(p.difference()));
        ligne.setNotes(ajouterNote(ligne.getNotes(), note));
        ligne.setUserUpdate(auteur);
        ligne.setUserUpdateDateTime(maintenant);
        documentRepository.save(ligne);
        ecriture.signer(dossier);
        return p.ligne();
    }

    /** Mêmes colonnes qu'une échéance ajoutée à la main ; montant posé ensuite par l'appelant. */
    private ComDocument nouvelleLigne(ComFolder dossier, String auteur, LocalDateTime maintenant) {
        UUID vocationId = Champs.de(dossier.getStkVocation(), StkVocation::getId);
        ComSettlementCategory categorie = categorieRepository
                .findFirstByStkVocationIdAndIsSaleAutoFinancmentTrueAndIsCreditTrueOrderByCodeAsc(vocationId)
                .orElseThrow(() -> new ConflictException("Aucune catégorie d'autofinancement pour la vocation du dossier "
                        + dossier.getCode() + " : impossible de créer la ligne du réajustement."));
        ComDocument d = new ComDocument();
        d.setId(UUID.randomUUID());
        d.setComFolder(dossier);
        d.setComSettlementCategory(categorie);
        d.setDocDate(maintenant);
        d.setAmount(BigDecimal.ZERO);
        d.setUserCreate(auteur);
        d.setUserCreateDateTime(maintenant);
        return d;
    }

    /** {@code ComDocument.Notes} est un nvarchar(1000) : on garde la fin, où vient d'arriver la note. */
    static String ajouterNote(String notes, String ajout) {
        String tout = notes == null || notes.isBlank() ? ajout : notes + "\n" + ajout;
        return tout.length() <= LONGUEUR_NOTES ? tout : tout.substring(tout.length() - LONGUEUR_NOTES);
    }

    private static BigDecimal arrondi(BigDecimal v) {
        return Objects.requireNonNullElse(v, BigDecimal.ZERO).setScale(DECIMALES, RoundingMode.HALF_UP);
    }
}
