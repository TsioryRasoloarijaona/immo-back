package com.tsix.immocrm.services.dossier.fiche;

import com.tsix.immocrm.domain.ConventionRemise;
import com.tsix.immocrm.domain.StatutDossier;
import com.tsix.immocrm.dto.dossier.reinitialisation.EtatPrixDto;
import com.tsix.immocrm.dto.dossier.reinitialisation.ReinitialisationDto;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.com.ComFolderItem;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.entities.str.StrStatus;
import com.tsix.immocrm.exceptions.ConflictException;
import com.tsix.immocrm.repositories.com.ComFolderItemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * « Réinitialiser » ({@code Sale.cs:Reinitialise}) : recharge depuis le stock la superficie, la TVA et le prix du
 * m² HT de l'article, la superficie, la TVA et le prix HT des sous-articles, puis recalcule la remise (le montant
 * reste, le taux change). Le schéma de financement n'est pas touché. Voir FEATURE-vente-fiche.md §B5.
 */
@Service
public class ReinitialisationService {

    private final EcritureDossier ecriture;
    private final ComFolderItemRepository folderItemRepository;

    public ReinitialisationService(EcritureDossier ecriture, ComFolderItemRepository folderItemRepository) {
        this.ecriture = ecriture;
        this.folderItemRepository = folderItemRepository;
    }

    @Transactional(readOnly = true)
    public ReinitialisationDto apercu(UUID dossierId) {
        ComFolder dossier = ecriture.charger(dossierId);
        Plan plan = planifier(dossier);
        return new ReinitialisationDto(etat(dossier), etat(plan.resultat()), plan.modifie());
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
        ecriture.signer(dossier);
        return new ReinitialisationDto(avant, etat(plan.resultat()), plan.modifie());
    }

    private record Plan(RecalculPrixDossier.Resultat resultat, List<ComFolderItem> sousArticles, boolean modifie) {
    }

    private Plan planifier(ComFolder dossier) {
        exigerDossierActif(dossier);
        StkItem article = dossier.getStkItem();
        if (article == null) {
            throw new ConflictException("Ce dossier n'a pas d'article principal : rien à recharger du stock.");
        }
        List<ComFolderItem> sousArticles = RecalculPrixDossier.sousArticlesDe(
                folderItemRepository.findByComFolderId(dossier.getId()));
        RecalculPrixDossier.Saisie actuelle = RecalculPrixDossier.depuisDossier(dossier, sousArticles);
        RecalculPrixDossier.Saisie rechargee = new RecalculPrixDossier.Saisie(
                stockOuActuel(article.getArea(), dossier.getArea()),
                stockOuActuel(article.getVat(), dossier.getVATRatio()),
                stockOuActuel(article.getComSaleUnitCostHT(), dossier.getUnitCostHT()),
                actuelle.remiseMontantHT(), actuelle.avecModifications(), actuelle.modificationsHT(),
                actuelle.modificationsTTC(),
                sousArticles.stream().map(ReinitialisationService::sousArticleDuStock).toList());
        return new Plan(RecalculPrixDossier.calculer(rechargee), sousArticles, differe(actuelle, rechargee));
    }

    /** Un dossier sorti a libéré ses articles : le stock ne le décrit plus. */
    private static void exigerDossierActif(ComFolder dossier) {
        StrStatus statut = dossier.getStrStatus();
        if (statut != null && statut.getCode() != null && estSortie(statut.getCode())) {
            throw new ConflictException("Dossier annulé ou désisté : réinitialisation impossible.");
        }
    }

    private static boolean estSortie(String code) {
        try {
            return StatutDossier.fromCode(code).isSortie();
        } catch (IllegalArgumentException e) {
            return false;
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
            return !egal(avant.superficie(), s.superficie()) || !egal(avant.tva(), s.tva())
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
