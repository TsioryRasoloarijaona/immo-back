package com.tsix.immocrm.services.dossier.edition;

import com.tsix.immocrm.dto.dossier.edition.ModifierDossierRequest;
import com.tsix.immocrm.entities.com.ComDocument;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.com.ComFolderItem;
import com.tsix.immocrm.entities.com.ComSettlementCategory;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.exceptions.ForbiddenException;
import com.tsix.immocrm.exceptions.InvalidRequestException;
import com.tsix.immocrm.repositories.com.ComDocumentRepository;
import com.tsix.immocrm.repositories.com.ComFolderItemRepository;
import com.tsix.immocrm.services.dossier.fiche.EcritureDossier;
import com.tsix.immocrm.services.dossier.fiche.RecalculPrixDossier;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Remise, TVA et « modifications intégrées au prix » : un seul recalcul ({@link RecalculPrixDossier}, montant de
 * remise conservé, taux recalculé), plafonds {@link PlafondRemise} et {@code DiscountChangeDate}
 * ({@code Sale.cs:HistorizeDiscount}). Le schéma de financement n'est pas rééquilibré : seul l'écart est rendu.
 */
@Component
class PrixEdition {

    private static final BigDecimal CENT = BigDecimal.valueOf(100);
    private static final int ECHELLE = 6;
    private static final int ECHELLE_TAUX = 16;

    private final ComFolderItemRepository folderItemRepository;
    private final ComDocumentRepository documentRepository;
    private final EcritureDossier ecriture;

    PrixEdition(ComFolderItemRepository folderItemRepository, ComDocumentRepository documentRepository,
                EcritureDossier ecriture) {
        this.folderItemRepository = folderItemRepository;
        this.documentRepository = documentRepository;
        this.ecriture = ecriture;
    }

    static boolean touchePrix(ModifierDossierRequest r) {
        return toucheRemise(r) || r.tva() != null;
    }

    /** Une des quatre saisies de la remise (montant, pourcentage, prix de vente HT ou TTC). */
    static boolean toucheRemise(ModifierDossierRequest r) {
        return Stream.of(r.remiseMontantHT(), r.remisePourcentage(), r.prixVenteHT(), r.prixVenteTTC())
                .anyMatch(Objects::nonNull);
    }

    /** 400 avant toute écriture. */
    static void valider(ModifierDossierRequest r) {
        long saisies = Stream.of(r.remiseMontantHT(), r.remisePourcentage(), r.prixVenteHT(), r.prixVenteTTC())
                .filter(Objects::nonNull).count();
        if (saisies > 1) {
            throw new InvalidRequestException(
                    "remiseMontantHT, remisePourcentage, prixVenteHT et prixVenteTTC sont exclusifs.");
        }
        if (r.remiseMontantHT() != null && r.remiseMontantHT().signum() < 0) {
            throw new InvalidRequestException("La remise ne peut pas être négative.");
        }
        if (Stream.of(r.prixVenteHT(), r.prixVenteTTC()).anyMatch(p -> p != null && p.signum() < 0)) {
            throw new InvalidRequestException("Le prix de vente ne peut pas être négatif.");
        }
        if (r.remisePourcentage() != null
                && (r.remisePourcentage().signum() < 0 || r.remisePourcentage().compareTo(CENT) > 0)) {
            throw new InvalidRequestException("remisePourcentage doit être compris entre 0 et 100.");
        }
        if (r.tva() != null && (r.tva().signum() < 0 || r.tva().compareTo(BigDecimal.ONE) >= 0)) {
            throw new InvalidRequestException("tva est un ratio compris entre 0 et 1 (0,2 = 20 %).");
        }
    }

    /**
     * Recalcule les prix du dossier et de ses sous-articles ; vrai si le montant de remise a changé.
     * {@code niveau} et {@code forcer} : contrôle des plafonds, seulement quand le montant change.
     */
    boolean recalculer(ComFolder f, ModifierDossierRequest r, PlafondRemise.Niveau niveau, boolean forcer) {
        List<ComFolderItem> sousArticles = sousArticles(f);
        RecalculPrixDossier.Saisie actuelle = depuisPrixStocke(f, sousArticles);
        BigDecimal tva = r.tva() != null ? r.tva() : actuelle.tva();
        BigDecimal ancienMontant = zero(f.getDiscountAmount());
        BigDecimal montant = montantRemise(r, actuelle, tva, ancienMontant);

        RecalculPrixDossier.Resultat resultat = RecalculPrixDossier.calculer(saisie(actuelle, tva, montant));
        if (montant.compareTo(resultat.totalHT()) > 0) {
            throw new InvalidRequestException("La remise dépasse le total HT avant remise.");
        }
        boolean remiseChangee = montant.compareTo(ancienMontant) != 0;
        if (remiseChangee && !forcer) {
            verifierPlafond(f.getStkItem(), resultat.remise(), niveau);
        }
        appliquer(resultat, f, sousArticles);
        if (remiseChangee) {
            f.setDiscountAmount(montant.setScale(ECHELLE, RoundingMode.HALF_UP));
            f.setDiscountChangeDate(ecriture.maintenant());
        }
        return remiseChangee;
    }

    /** Recalcul à remise et TVA inchangées, après un changement de « modifications intégrées au prix ». */
    void recalculerSansRemise(ComFolder f) {
        List<ComFolderItem> sousArticles = sousArticles(f);
        appliquer(RecalculPrixDossier.calculer(depuisPrixStocke(f, sousArticles)), f, sousArticles);
    }

    private List<ComFolderItem> sousArticles(ComFolder f) {
        return RecalculPrixDossier.sousArticlesDe(folderItemRepository.findByComFolderId(f.getId()));
    }

    /**
     * Comme {@code Sale.cs:RecalcDiscount} et le setter {@code VATRatio}, la base est le prix article HT stocké, pas
     * superficie × prix du m² (nuls sur des dossiers réels au prix renseigné) : on le passe comme « 1 × prix ».
     * Prix jamais calculé (null) : superficie × prix du m², comme à la création.
     */
    private static RecalculPrixDossier.Saisie depuisPrixStocke(ComFolder f, List<ComFolderItem> sousArticles) {
        RecalculPrixDossier.Saisie a = RecalculPrixDossier.depuisDossier(f, sousArticles);
        BigDecimal base = f.getItemPriceHT() != null ? f.getItemPriceHT()
                : zero(a.superficie()).multiply(zero(a.prixM2HT()));
        return new RecalculPrixDossier.Saisie(BigDecimal.ONE, a.tva(), base, a.remiseMontantHT(),
                a.avecModifications(), a.modificationsHT(), a.modificationsTTC(), a.sousArticles());
    }

    /** {@link RecalculPrixDossier#appliquer} écrit aussi superficie et prix du m² : on garde ceux du dossier. */
    private void appliquer(RecalculPrixDossier.Resultat resultat, ComFolder f, List<ComFolderItem> sousArticles) {
        BigDecimal superficie = f.getArea();
        BigDecimal superficieGlobale = f.getGlobalArea();
        BigDecimal prixM2HT = f.getUnitCostHT();
        BigDecimal prixM2TTC = f.getUnitCost();
        RecalculPrixDossier.appliquer(resultat, f, sousArticles.stream()
                .collect(Collectors.toMap(ComFolderItem::getId, Function.identity())));
        f.setArea(superficie);
        f.setGlobalArea(superficieGlobale);
        f.setUnitCostHT(prixM2HT);
        f.setUnitCost(prixM2HT == null ? prixM2TTC
                : prixM2HT.multiply(BigDecimal.ONE.add(resultat.tva())).setScale(ECHELLE, RoundingMode.HALF_UP));
        folderItemRepository.saveAll(sousArticles);
    }

    private static void verifierPlafond(StkItem article, BigDecimal taux, PlafondRemise.Niveau niveau) {
        BigDecimal max1 = article != null ? article.getDiscountMax() : null;
        BigDecimal max2 = article != null ? article.getDiscountMax2() : null;
        Optional<String> depassement = PlafondRemise.depassement(taux, max1, max2, niveau);
        if (depassement.isPresent()) {
            throw new InvalidRequestException(depassement.get());
        }
    }

    /**
     * B5 (FEATURE-dossier-modification.md) : article sans premier plafond ({@code Sale.cs:Sale_Load}), une nouvelle
     * remise non nulle exige le deuxième niveau ; 403 avant toute écriture.
     */
    void verifierVerrou(ComFolder f, ModifierDossierRequest r, PlafondRemise.Niveau niveau) {
        if (!toucheRemise(r) || !PlafondRemise.verrouillee(VerrousEdition.premierPlafond(f), niveau)) {
            return;
        }
        StkItem article = f.getStkItem();
        BigDecimal max2 = article != null ? article.getDiscountMax2() : null;
        RecalculPrixDossier.Saisie actuelle = depuisPrixStocke(f, sousArticles(f));
        BigDecimal tva = r.tva() != null ? r.tva() : actuelle.tva();
        BigDecimal ancienMontant = zero(f.getDiscountAmount());
        BigDecimal montant = montantRemise(r, actuelle, tva, ancienMontant);
        if (montant.signum() > 0 && montant.compareTo(ancienMontant) != 0) {
            throw new ForbiddenException(PlafondRemise.messageSansRemise(max2));
        }
    }

    private static BigDecimal montantRemise(ModifierDossierRequest r, RecalculPrixDossier.Saisie actuelle,
                                            BigDecimal tva, BigDecimal ancienMontant) {
        if (r.remiseMontantHT() != null) {
            return r.remiseMontantHT();
        }
        if (r.remisePourcentage() != null) {
            BigDecimal totalHT = RecalculPrixDossier.calculer(actuelle).totalHT();
            return totalHT.multiply(r.remisePourcentage()).divide(CENT, ECHELLE, RoundingMode.HALF_UP);
        }
        if (r.prixVenteHT() != null) {
            BigDecimal totalHT = RecalculPrixDossier.calculer(actuelle).totalHT();
            depassePrixAvantRemise(r.prixVenteHT(), totalHT, "HT");
            return totalHT.subtract(r.prixVenteHT());
        }
        if (r.prixVenteTTC() != null) {
            return remiseDepuisPrixTTC(r.prixVenteTTC(),
                    RecalculPrixDossier.calculer(saisie(actuelle, tva, BigDecimal.ZERO)));
        }
        return ancienMontant;
    }

    /** {@code Sale.cs}, setter {@code PriceAfterDiscount} : taux = 1 − saisie / TTC avant remise, appliqué au HT. */
    private static BigDecimal remiseDepuisPrixTTC(BigDecimal prixTTC, RecalculPrixDossier.Resultat avantRemise) {
        BigDecimal totalTTC = avantRemise.totalTTC();
        depassePrixAvantRemise(prixTTC, totalTTC, "TTC");
        if (totalTTC.signum() == 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal taux = BigDecimal.ONE.subtract(prixTTC.divide(totalTTC, ECHELLE_TAUX, RoundingMode.HALF_UP));
        return avantRemise.totalHT().multiply(taux).setScale(ECHELLE, RoundingMode.HALF_UP);
    }

    private static void depassePrixAvantRemise(BigDecimal prix, BigDecimal avantRemise, String base) {
        if (prix.compareTo(avantRemise) > 0) {
            throw new InvalidRequestException("Le prix de vente %s dépasse le prix %s avant remise.".formatted(base, base));
        }
    }

    private static RecalculPrixDossier.Saisie saisie(RecalculPrixDossier.Saisie a, BigDecimal tva, BigDecimal montant) {
        return new RecalculPrixDossier.Saisie(a.superficie(), tva, a.prixM2HT(), montant, a.avecModifications(),
                a.modificationsHT(), a.modificationsTTC(), a.sousArticles());
    }

    /**
     * {@code Sale.cs:VerifSchema} sans la levée : prix après remise TTC − (avance + crédit + autofinancement),
     * chaque part nette de ses remboursements ({@code IsCredit = 0}) ; null si l'écart est nul.
     */
    BigDecimal ecartSchema(ComFolder f) {
        List<ComDocument> echeances = documentRepository.findByComFolderIdOrderByDocDateAsc(f.getId());
        BigDecimal finance = part(echeances, c -> Boolean.TRUE.equals(c.getIsSaleAdvance()))
                .add(part(echeances, c -> Boolean.TRUE.equals(c.getIsSaleCredit())))
                .add(part(echeances, c -> Boolean.TRUE.equals(c.getIsSaleAutoFinancment())));
        BigDecimal ecart = zero(f.getPriceAfterDiscount()).subtract(finance).setScale(2, RoundingMode.HALF_UP);
        return ecart.signum() == 0 ? null : ecart;
    }

    private static BigDecimal part(List<ComDocument> echeances, Predicate<ComSettlementCategory> drapeau) {
        Predicate<ComDocument> concerne = d -> d.getComSettlementCategory() != null
                && drapeau.test(d.getComSettlementCategory());
        return echeances.stream().filter(concerne)
                .map(d -> Boolean.FALSE.equals(d.getComSettlementCategory().getIsCredit())
                        ? zero(d.getAmount()).negate() : zero(d.getAmount()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static BigDecimal zero(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }
}
