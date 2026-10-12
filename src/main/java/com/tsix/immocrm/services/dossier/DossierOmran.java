package com.tsix.immocrm.services.dossier;

import com.tsix.immocrm.domain.FormatOmran;
import com.tsix.immocrm.domain.PrixArticle;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.cfg.CfgTierType;
import com.tsix.immocrm.entities.com.ComDocument;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.com.ComFolderItem;
import com.tsix.immocrm.entities.com.ComSettlementCategory;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.entities.str.StrStatus;
import com.tsix.immocrm.services.referentiel.ReferencesOmran;
import com.tsix.immocrm.utils.Champs;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;

import static java.util.Objects.requireNonNullElse;

/**
 * Colonnes de {@code ComFolder} qu'OMRAN écrit toujours à l'insertion et que ses vues ou ses contrôles
 * exigent. Voir {@code FEATURE-interop-omran-ecritures.md} §3 (valeurs et mesures).
 */
@Component
public class DossierOmran {

    static final String ENTETE_SCHEMA = "Schèma de financement :";
    /** {@code Environment.NewLine} du poste Windows d'OMRAN. */
    static final String FIN_DE_LIGNE = "\r\n";
    private static final int LARGEUR_MONTANT = 20;
    private static final String CODE_TYPE_CLIENT = "CLI";

    private final ReferencesOmran references;

    public DossierOmran(ReferencesOmran references) {
        this.references = references;
    }

    /** Valeurs d'insertion de {@code Sale.cs} (constantes, surfaces, statut, signature GEN0016). */
    public void completerALaCreation(ComFolder folder, StkItem principal, List<StkItem> sousArticles,
                                     StrStatus statutInitial, String auteur, LocalDateTime maintenant) {
        LocalDateTime docDate = LigneEcheancierOmran.minuit(maintenant);
        folder.setDocDate(docDate);
        folder.setDescription(folder.getCode() + " du " + FormatOmran.dateCourte(docDate));
        folder.setStrEntityId(references.entiteDossier());
        folder.setStrStatus(statutInitial);
        folder.setStatusValidityDate(dateDeValidite(statutInitial, docDate));
        folder.setIsOldCustomer(estDejaClient(folder.getCfgCustomer()));
        folder.setPartPercent(BigDecimal.ONE);

        BigDecimal surface = requireNonNullElse(principal.getArea(), BigDecimal.ZERO);
        BigDecimal surfaceAnnexes = sousArticles.stream()
                .map(article -> requireNonNullElse(article.getArea(), BigDecimal.ZERO))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal tva = requireNonNullElse(principal.getVat(), BigDecimal.ZERO);
        BigDecimal coutUnitaireHT = requireNonNullElse(principal.getComSaleUnitCostHT(), BigDecimal.ZERO);
        folder.setArea(surface);
        folder.setGlobalArea(surface.add(surfaceAnnexes));
        folder.setUnitCostHT(coutUnitaireHT);
        folder.setUnitCost(coutUnitaireHT.multiply(BigDecimal.ONE.add(tva)).setScale(6, RoundingMode.HALF_UP));

        // Sale.cs:127-135, 265 : constantes d'insertion.
        folder.setDeliveryRatio(new BigDecimal("0.001"));
        folder.setDeliveryMaxNumber(180);
        folder.setProbabilityOfFinalisation(BigDecimal.ONE);
        folder.setModificationsAmount(BigDecimal.ZERO);
        folder.setModificationsAmountHT(BigDecimal.ZERO);
        folder.setDeliveryNumber(0);
        folder.setDeliveryAmount(BigDecimal.ZERO);
        folder.setRomInterest(BigDecimal.ZERO);
        folder.setCommissionRate(BigDecimal.ZERO);
        folder.setIsAccounted(Boolean.FALSE);
        folder.setIsPenaltyDisabled(Boolean.FALSE);
        folder.setPriceWithModification(Boolean.FALSE);
        folder.setIsConfirmedAgreementDate(Boolean.FALSE);
        folder.setIsWithGiftChoiceLater(Boolean.FALSE);
        folder.setNotes(requireNonNullElse(folder.getNotes(), ""));
        folder.setCancelDescription(requireNonNullElse(folder.getCancelDescription(), ""));
        folder.setPenalityDescription(requireNonNullElse(folder.getPenalityDescription(), ""));
        folder.setCreditAgency(requireNonNullElse(folder.getCreditAgency(), ""));
        folder.setFinalTitleNumber(requireNonNullElse(folder.getFinalTitleNumber(), ""));
        folder.setComDocumentDetail(detailSchema(List.of()));

        folder.setUserCreate(auteur);
        folder.setUserCreateDateTime(maintenant);
        folder.setUserUpdate(auteur);
        folder.setUserUpdateDateTime(maintenant);
    }

    /** Colonnes calculées depuis l'échéancier, à appeler une fois le schéma initial écrit. */
    public void completerDepuisEcheancier(ComFolder folder, List<ComDocument> lignes) {
        folder.setAgreementPrice(prixContractuel(lignes));
        folder.setAgreementAmount(montantContrat(folder.getPrice(), lignes));
        folder.setComDocumentDetail(detailSchema(lignes));
    }

    /** Une ligne {@code ComFolderItem} copiée du sous-article, comme {@code UnderItem(Item, Sale)} d'OMRAN. */
    public static ComFolderItem ligneSousArticle(ComFolder folder, StkItem sousArticle, String auteur,
                                                 LocalDateTime maintenant) {
        BigDecimal tva = requireNonNullElse(sousArticle.getVat(), BigDecimal.ZERO);
        BigDecimal prix = PrixArticle.ttcOuZero(sousArticle);
        BigDecimal prixHT = PrixArticle.htOuZero(sousArticle, tva);
        ComFolderItem ligne = new ComFolderItem();
        ligne.setId(UUID.randomUUID());
        ligne.setComFolder(folder);
        ligne.setStkItem(sousArticle);
        ligne.setStkItemType(sousArticle.getStkItemType());
        ligne.setStkItemCategory(sousArticle.getStkItemCategory());
        ligne.setParentId(Champs.de(sousArticle.getParent(), StkItem::getId));
        ligne.setCode(requireNonNullElse(sousArticle.getCode(), ""));
        ligne.setDescription(requireNonNullElse(sousArticle.getDescription(), ""));
        ligne.setAddress(requireNonNullElse(sousArticle.getAddress(), ""));
        ligne.setNotes(requireNonNullElse(sousArticle.getNotes(), ""));
        ligne.setContenance(requireNonNullElse(sousArticle.getContenance(), ""));
        BigDecimal surface = requireNonNullElse(sousArticle.getArea(), BigDecimal.ZERO);
        ligne.setStkBuilding(sousArticle.getStkBuilding());
        ligne.setStkFloor(sousArticle.getStkFloor());
        ligne.setAreaFloor(requireNonNullElse(sousArticle.getAreaFloor(), BigDecimal.ZERO));
        ligne.setArea(surface);
        // OMRAN recopie StkItem.Area dans AreaNet : StkItem n'a pas de colonne AreaNet (seulement AreaNetOld).
        ligne.setAreaNet(surface);
        ligne.setVATRatio(tva);
        ligne.setDiscount(BigDecimal.ZERO);
        ligne.setPriceBeforeDiscountHT(prixHT);
        ligne.setPriceHT(prixHT);
        ligne.setPriceBeforeDiscount(prix);
        ligne.setPrice(prix);
        ligne.setAlwaysAffected(Boolean.TRUE.equals(sousArticle.getAlwaysAffected()));
        ligne.setIsUnderItem(Boolean.TRUE);
        ligne.setIsUnderItemOrigin(Boolean.TRUE.equals(sousArticle.getIsUnderItemOrigin()));
        ligne.setIsTitled(Boolean.TRUE.equals(sousArticle.getIsTitled()));
        ligne.setUserCreate(auteur);
        ligne.setUserCreateDateTime(maintenant);
        ligne.setUserUpdate(auteur);
        ligne.setUserUpdateDateTime(maintenant);
        return ligne;
    }

    /** {@code Sale.cs:3061-3074} : date du dossier + {@code Delay} jours, NULL sans délai positif. */
    static LocalDateTime dateDeValidite(StrStatus statut, LocalDateTime docDate) {
        Integer delai = statut.getDelay();
        return delai != null && delai > 0 ? docDate.plusDays(delai) : null;
    }

    /** {@code Sale.cs:3341} : le tiers est déjà de type client au moment de la vente. */
    static boolean estDejaClient(CfgTier client) {
        String code = Champs.de(client, CfgTier::getCfgTierType, CfgTierType::getCode);
        return code != null && CODE_TYPE_CLIENT.equalsIgnoreCase(code.trim());
    }

    /** {@code Sale.cs:778} : échéances au crédit et non exclues du prix. */
    static BigDecimal prixContractuel(List<ComDocument> lignes) {
        return somme(lignes, ligne -> !Boolean.TRUE.equals(ligne.getExcludeFromPrice())
                && Boolean.TRUE.equals(Champs.de(ligne.getComSettlementCategory(), ComSettlementCategory::getIsCredit)));
    }

    /** {@code Sale.cs:631-638, 782} : prix − avance (crédit − débit), arrondi au centime comme {@code Math.Round}. */
    static BigDecimal montantContrat(BigDecimal prix, List<ComDocument> lignes) {
        Predicate<ComDocument> avance = ligne ->
                Boolean.TRUE.equals(Champs.de(ligne.getComSettlementCategory(), ComSettlementCategory::getIsSaleAdvance));
        Predicate<ComDocument> credit = ligne ->
                Boolean.TRUE.equals(Champs.de(ligne.getComSettlementCategory(), ComSettlementCategory::getIsCredit));
        BigDecimal montantAvance = somme(lignes, avance.and(credit)).subtract(somme(lignes, avance.and(credit.negate())));
        return requireNonNullElse(prix, BigDecimal.ZERO).subtract(montantAvance).setScale(2, RoundingMode.HALF_EVEN);
    }

    /** {@code Sale.cs:4040-4065 GenrateComDocumentDetail}, orthographe et alignement compris ; rien n'est encore réglé. */
    static String detailSchema(List<ComDocument> lignes) {
        StringBuilder texte = new StringBuilder(ENTETE_SCHEMA).append(FIN_DE_LIGNE);
        for (ComDocument ligne : lignes) {
            BigDecimal montant = requireNonNullElse(ligne.getAmount(), BigDecimal.ZERO);
            texte.append("Date :").append(FormatOmran.dateCourte(ligne.getDocDate()))
                    .append("   Montant :").append(aDroite(FormatOmran.montantDevise(montant)))
                    .append("   Réglé :").append(aDroite(FormatOmran.montantDevise(BigDecimal.ZERO)))
                    .append("   Reste :").append(aDroite(FormatOmran.montantDevise(montant)))
                    .append("   Exclure du prix contractuel:")
                    .append(Boolean.TRUE.equals(ligne.getExcludeFromPrice()) ? "Oui" : "Non")
                    .append("   Catégorie : ")
                    .append(requireNonNullElse(
                            Champs.de(ligne.getComSettlementCategory(), ComSettlementCategory::getDescription), ""))
                    .append(FIN_DE_LIGNE);
        }
        return texte.toString();
    }

    private static String aDroite(String texte) {
        return " ".repeat(Math.max(0, LARGEUR_MONTANT - texte.length())) + texte;
    }

    private static BigDecimal somme(List<ComDocument> lignes, Predicate<ComDocument> filtre) {
        return lignes.stream()
                .filter(filtre)
                .map(ligne -> requireNonNullElse(ligne.getAmount(), BigDecimal.ZERO))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
