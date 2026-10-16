package com.tsix.immocrm.services.dossier.fiche;

import com.tsix.immocrm.domain.ConventionRemise;
import com.tsix.immocrm.dto.dossier.reinitialisation.EtatPrixDto;
import com.tsix.immocrm.dto.dossier.reinitialisation.ReinitialisationDto;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.com.ComFolderItem;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.services.dossier.DossierSorti;
import com.tsix.immocrm.exceptions.ConflictException;
import com.tsix.immocrm.repositories.com.ComFolderItemRepository;
import com.tsix.immocrm.repositories.stk.StkItemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * « Réinitialiser » ({@code Sale.cs:Reinitialise}) : ajoute les sous-articles du stock absents, recharge l'article et
 * les sous-articles depuis le stock, puis recalcule la remise (le montant reste, le taux change). Le schéma de
 * financement n'est pas touché. Voir FEATURE-vente-fiche.md §B5.
 */
@Service
public class ReinitialisationService {

    private final EcritureDossier ecriture;
    private final ComFolderItemRepository folderItemRepository;
    private final StkItemRepository stkItemRepository;

    public ReinitialisationService(EcritureDossier ecriture, ComFolderItemRepository folderItemRepository,
                                   StkItemRepository stkItemRepository) {
        this.ecriture = ecriture;
        this.folderItemRepository = folderItemRepository;
        this.stkItemRepository = stkItemRepository;
    }

    @Transactional(readOnly = true)
    public ReinitialisationDto apercu(UUID dossierId) {
        ComFolder dossier = ecriture.charger(dossierId);
        Plan plan = planifier(dossier);
        return new ReinitialisationDto(etat(dossier), etat(plan.resultat()), plan.modifie(), plan.codesAjoutes());
    }

    @Transactional
    public ReinitialisationDto appliquerReinitialisation(UUID dossierId) {
        ComFolder dossier = ecriture.charger(dossierId);
        Plan plan = planifier(dossier);
        EtatPrixDto avant = etat(dossier);
        recopierDescriptifs(plan.sousArticles());
        RecalculPrixDossier.appliquer(plan.resultat(), dossier, plan.sousArticles().stream()
                .collect(Collectors.toMap(ComFolderItem::getId, Function.identity())));
        folderItemRepository.saveAll(plan.sousArticles());
        for (StkItem stock : plan.stocksAjoutes()) {
            stock.setComFolderId(dossier.getId());
        }
        stkItemRepository.saveAll(plan.stocksAjoutes());
        ecriture.signer(dossier);
        return new ReinitialisationDto(avant, etat(plan.resultat()), plan.modifie(), plan.codesAjoutes());
    }

    private record Plan(RecalculPrixDossier.Resultat resultat, List<ComFolderItem> sousArticles,
                        List<StkItem> stocksAjoutes, boolean modifie) {
        List<String> codesAjoutes() {
            return stocksAjoutes.stream().map(StkItem::getCode).toList();
        }
    }

    private Plan planifier(ComFolder dossier) {
        exigerDossierActif(dossier);
        StkItem article = dossier.getStkItem();
        if (article == null) {
            throw new ConflictException("Ce dossier n'a pas d'article principal : rien à recharger du stock.");
        }
        exigerPrixDeStock(article);
        List<ComFolderItem> lignes = folderItemRepository.findByComFolderId(dossier.getId());
        List<ComFolderItem> existants = RecalculPrixDossier.sousArticlesDe(lignes);
        List<StkItem> manquants = sousArticlesManquants(dossier, article, lignes);
        List<ComFolderItem> sousArticles = new ArrayList<>(existants);
        manquants.forEach(stock -> sousArticles.add(nouvelleLigne(dossier, article, stock)));

        RecalculPrixDossier.Saisie actuelle = RecalculPrixDossier.depuisDossier(dossier, existants);
        RecalculPrixDossier.Saisie rechargee = new RecalculPrixDossier.Saisie(
                stockOuActuel(article.getArea(), dossier.getArea()),
                stockOuActuel(article.getVat(), dossier.getVATRatio()),
                stockOuActuel(article.getComSaleUnitCostHT(), dossier.getUnitCostHT()),
                actuelle.remiseMontantHT(), actuelle.avecModifications(), actuelle.modificationsHT(),
                actuelle.modificationsTTC(),
                sousArticles.stream().map(ReinitialisationService::sousArticleDuStock).toList());
        exigerPrixDesSousArticles(existants);
        RecalculPrixDossier.Resultat resultat = RecalculPrixDossier.calculer(rechargee);
        if (resultat.prixVenteTTC().signum() <= 0) {
            throw new ConflictException("Nouveau prix nul ou négatif : réinitialisation impossible.");
        }
        return new Plan(resultat, sousArticles, manquants, !manquants.isEmpty() || differe(actuelle, rechargee));
    }

    /** Un prix du m² absent ou nul au stock mettrait le prix du dossier à 0. Voir FEATURE-vente-fiche.md §B5. */
    private static void exigerPrixDeStock(StkItem article) {
        BigDecimal prix = article.getComSaleUnitCostHT();
        if (prix == null || prix.signum() <= 0) {
            throw new ConflictException("Prix du m² absent dans le stock pour l'article " + article.getCode()
                    + " : réinitialisation impossible.");
        }
    }

    /** Un sous-article déjà payé ne doit pas retomber à 0 parce que son prix manque au stock. */
    private static void exigerPrixDesSousArticles(List<ComFolderItem> existants) {
        List<String> sansPrix = existants.stream()
                .filter(l -> l.getStkItem() != null && positif(l.getPriceBeforeDiscountHT())
                        && !positif(l.getStkItem().getSalePriceHT()))
                .map(l -> Objects.requireNonNullElse(l.getCode(), l.getStkItem().getCode()))
                .toList();
        if (!sansPrix.isEmpty()) {
            throw new ConflictException("Prix de stock absent pour le sous-article " + String.join(", ", sansPrix)
                    + " : réinitialisation impossible.");
        }
    }

    private static boolean positif(BigDecimal v) {
        return v != null && v.signum() > 0;
    }

    /**
     * Sous-articles du stock ({@code ParentId} = article) absents du dossier, ajoutés comme OMRAN. Celui déjà
     * affecté à un autre dossier n'est pas pris : l'ajouter le retirerait à ce dossier. Voir FEATURE-vente-fiche.md §B5.
     */
    private List<StkItem> sousArticlesManquants(ComFolder dossier, StkItem article, List<ComFolderItem> lignes) {
        Set<UUID> presents = lignes.stream().map(ComFolderItem::getStkItem).filter(Objects::nonNull)
                .map(StkItem::getId).collect(Collectors.toSet());
        return stkItemRepository.findByParentId(article.getId()).stream()
                .filter(stock -> !presents.contains(stock.getId()))
                .filter(stock -> stock.getComFolderId() == null || stock.getComFolderId().equals(dossier.getId()))
                .sorted(Comparator.comparing(StkItem::getCode, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
    }

    /** Ligne créée comme {@code UnderItem(Yes.Stk.Building.UnderItem, Sale)} ; les prix viennent du recalcul. */
    private ComFolderItem nouvelleLigne(ComFolder dossier, StkItem article, StkItem stock) {
        ComFolderItem ligne = new ComFolderItem();
        ligne.setId(UUID.randomUUID());
        ligne.setComFolder(dossier);
        ligne.setStkItem(stock);
        ligne.setParentId(article.getId());
        ligne.setIsUnderItem(true);
        ligne.setArea(stock.getArea());
        ligne.setAreaNet(stock.getArea());
        ligne.setVATRatio(stock.getVat());
        ligne.setPriceBeforeDiscountHT(stock.getSalePriceHT());
        ligne.setUserCreate(ecriture.auteur().nom());
        ligne.setUserCreateDateTime(ecriture.maintenant());
        return ligne;
    }

    /** Un dossier sorti a libéré ses articles : le stock ne le décrit plus. */
    private static void exigerDossierActif(ComFolder dossier) {
        if (DossierSorti.estSorti(dossier)) {
            throw new ConflictException("Dossier annulé ou désisté : réinitialisation impossible.");
        }
    }

    /** Valeur absente du stock : la valeur du dossier est gardée plutôt que remise à zéro. */
    private static BigDecimal stockOuActuel(BigDecimal stock, BigDecimal actuel) {
        return stock != null ? stock : actuel;
    }

    private static RecalculPrixDossier.SousArticle sousArticleDuStock(ComFolderItem ligne) {
        StkItem stock = ligne.getStkItem();
        if (stock == null) {
            return new RecalculPrixDossier.SousArticle(ligne.getId(), ligne.getArea(), ligne.getVATRatio(),
                    ligne.getPriceBeforeDiscountHT());
        }
        return new RecalculPrixDossier.SousArticle(ligne.getId(),
                stockOuActuel(stock.getArea(), ligne.getArea()),
                stockOuActuel(stock.getVat(), ligne.getVATRatio()),
                stockOuActuel(stock.getSalePriceHT(), ligne.getPriceBeforeDiscountHT()));
    }

    /** Descriptifs rechargés avec les prix, comme la liste de l'aide OMRAN (code, catégorie, type…). */
    private static void recopierDescriptifs(List<ComFolderItem> sousArticles) {
        for (ComFolderItem ligne : sousArticles) {
            StkItem stock = ligne.getStkItem();
            if (stock == null) {
                continue;
            }
            ligne.setCode(stock.getCode());
            ligne.setDescription(stock.getDescription());
            ligne.setAddress(stock.getAddress());
            ligne.setContenance(stock.getContenance());
            ligne.setAlwaysAffected(stock.getAlwaysAffected());
            ligne.setIsUnderItemOrigin(stock.getIsUnderItemOrigin());
            ligne.setIsTitled(stock.getIsTitled());
            ligne.setStkItemCategory(stock.getStkItemCategory());
            ligne.setStkItemType(stock.getStkItemType());
            ligne.setNotes(stock.getNotes());
            ligne.setAreaFloor(stock.getAreaFloor());
            ligne.setStkBuildingId(stock.getStkBuilding() != null ? stock.getStkBuilding().getId() : null);
            ligne.setStkFloorId(stock.getStkFloor() != null ? stock.getStkFloor().getId() : null);
        }
    }

    private static boolean differe(RecalculPrixDossier.Saisie a, RecalculPrixDossier.Saisie b) {
        if (!egal(a.superficie(), b.superficie()) || !egal(a.tva(), b.tva()) || !egal(a.prixM2HT(), b.prixM2HT())) {
            return true;
        }
        Map<UUID, RecalculPrixDossier.SousArticle> parId = a.sousArticles().stream()
                .collect(Collectors.toMap(RecalculPrixDossier.SousArticle::id, Function.identity()));
        return b.sousArticles().stream().anyMatch(s -> {
            RecalculPrixDossier.SousArticle avant = parId.get(s.id());
            return avant == null || !egal(avant.superficie(), s.superficie()) || !egal(avant.tva(), s.tva())
                    || !egal(avant.prixAvantRemiseHT(), s.prixAvantRemiseHT());
        });
    }

    private static boolean egal(BigDecimal a, BigDecimal b) {
        BigDecimal x = a != null ? a : BigDecimal.ZERO;
        BigDecimal y = b != null ? b : BigDecimal.ZERO;
        return x.compareTo(y) == 0;
    }

    static EtatPrixDto etat(ComFolder f) {
        return new EtatPrixDto(f.getArea(), f.getGlobalArea(), f.getVATRatio(), f.getUnitCostHT(),
                f.getUnderItemsPriceHT(), f.getPriceBeforeDiscountHT(), f.getDiscountAmount(),
                ConventionRemise.pourcentage(f.getDiscount()), f.getPriceAfterDiscountHT(), f.getPriceAfterDiscount());
    }

    static EtatPrixDto etat(RecalculPrixDossier.Resultat r) {
        return new EtatPrixDto(r.superficie(), r.superficieGlobale(), r.tva(), r.prixM2HT(), r.prixSousArticlesHT(),
                r.totalHT(), r.remiseMontantHT(), ConventionRemise.pourcentage(r.remise()), r.prixVenteHT(),
                r.prixVenteTTC());
    }
}
