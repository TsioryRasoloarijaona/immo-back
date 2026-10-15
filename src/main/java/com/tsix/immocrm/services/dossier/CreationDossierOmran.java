package com.tsix.immocrm.services.dossier;

import com.tsix.immocrm.domain.StatutDossier;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.com.ComDocument;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.com.ComFolderItem;
import com.tsix.immocrm.entities.com.ComSettlementCategory;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.entities.stl.StlQuoteItem;
import com.tsix.immocrm.entities.str.StrStatus;
import com.tsix.immocrm.exceptions.ConfigurationManquanteException;
import com.tsix.immocrm.repositories.com.ComFolderItemRepository;
import com.tsix.immocrm.repositories.stk.StkItemRepository;
import com.tsix.immocrm.repositories.str.StrStatusRepository;
import com.tsix.immocrm.security.UtilisateurAffiche;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import static java.util.Objects.requireNonNullElse;

/**
 * Ce qu'OMRAN écrit à la création d'un dossier de vente et que la saisie ne fournit pas : statut initial,
 * constantes, superficies, prix contractuel, schéma texte, sous-articles ({@code ComFolderItem}).
 * Voir {@code FEATURE-interop-omran-ecritures.md} §3.
 */
@Component
public class CreationDossierOmran {

    static final BigDecimal TAUX_LIVRAISON = new BigDecimal("0.001");
    static final int NOMBRE_MAX_LIVRAISONS = 180;
    static final String CODE_TYPE_CLIENT = "CLI";
    static final String ENTETE_SCHEMA = "Schèma de financement :";
    private static final String FIN_DE_LIGNE = "\r\n";
    private static final DateTimeFormatter JJ_MM_AAAA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final StrStatusRepository strStatusRepository;
    private final ComFolderItemRepository folderItemRepository;
    private final StkItemRepository stkItemRepository;
    private final HistoriqueDossier historique;
    private final ReferencesOmran references;
    private final UtilisateurAffiche utilisateurAffiche;
    private final Clock clock;

    public CreationDossierOmran(StrStatusRepository strStatusRepository, ComFolderItemRepository folderItemRepository,
                                StkItemRepository stkItemRepository, HistoriqueDossier historique,
                                ReferencesOmran references, UtilisateurAffiche utilisateurAffiche, Clock clock) {
        this.strStatusRepository = strStatusRepository;
        this.folderItemRepository = folderItemRepository;
        this.stkItemRepository = stkItemRepository;
        this.historique = historique;
        this.references = references;
        this.utilisateurAffiche = utilisateurAffiche;
        this.clock = clock;
    }

    /** Statut RESERVATION de l'entité dossier ; OMRAN refuse un dossier sans statut, nous aussi (500). */
    public StrStatus statutInitial() {
        StrStatus statut = strStatusRepository.findByStrConfigEntityIdAndCode(
                StatutDossier.CONFIG_ENTITY_ID, StatutDossier.RESERVATION.getCode());
        if (statut == null) {
            throw new ConfigurationManquanteException("Statut RESERVATION introuvable pour les dossiers de vente "
                    + "(StrStatus de l'entité ComFolder) : configuration OMRAN à compléter, aucun dossier créé");
        }
        return statut;
    }

    /**
     * Colonnes calculées avant l'enregistrement ; {@code Code}, {@code DocDate} (minuit), client, montants
     * et statut sont déjà posés.
     */
    public void completer(ComFolder folder, StkItem principal, List<StkItem> annexes, CfgTier client) {
        String auteur = utilisateurAffiche.courant().nom();
        LocalDateTime maintenant = LocalDateTime.now(clock);
        folder.setUserCreate(auteur);
        folder.setUserCreateDateTime(maintenant);
        folder.setUserUpdate(auteur);
        folder.setUserUpdateDateTime(maintenant);
        folder.setStrEntityId(references.entiteDossier());
        folder.setDescription(folder.getCode() + " du " + folder.getDocDate().format(JJ_MM_AAAA));
        folder.setStatusValidityDate(dateDeValidite(folder.getDocDate(), folder.getStrStatus()));
        folder.setPartPercent(BigDecimal.ONE);
        folder.setIsOldCustomer(estDejaClient(client));

        BigDecimal surface = requireNonNullElse(principal.getArea(), BigDecimal.ZERO);
        BigDecimal surfaceAnnexes = annexes.stream()
                .map(annexe -> requireNonNullElse(annexe.getArea(), BigDecimal.ZERO))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        folder.setArea(surface);
        folder.setGlobalArea(surface.add(surfaceAnnexes));
        BigDecimal prixM2HT = requireNonNullElse(principal.getComSaleUnitCostHT(), BigDecimal.ZERO);
        BigDecimal tva = requireNonNullElse(folder.getVATRatio(), BigDecimal.ZERO);
        folder.setUnitCostHT(prixM2HT);
        folder.setUnitCost(prixM2HT.multiply(BigDecimal.ONE.add(tva)).setScale(6, RoundingMode.HALF_UP));

        poserConstantes(folder);
    }

    /** Valeurs fixes d'OMRAN à l'insertion ({@code Sale.cs:127-135, 265}) : jamais NULL. */
    private static void poserConstantes(ComFolder folder) {
        folder.setDeliveryRatio(TAUX_LIVRAISON);
        folder.setDeliveryMaxNumber(NOMBRE_MAX_LIVRAISONS);
        folder.setProbabilityOfFinalisation(BigDecimal.ONE);
        folder.setModificationsAmount(BigDecimal.ZERO);
        folder.setModificationsAmountHT(BigDecimal.ZERO);
        folder.setDeliveryNumber(0);
        folder.setDeliveryAmount(BigDecimal.ZERO);
        folder.setRomInterest(BigDecimal.ZERO);
        folder.setCommissionRate(BigDecimal.ZERO);
        folder.setIsAccounted(false);
        folder.setIsPenaltyDisabled(false);
        folder.setPriceWithModification(false);
        folder.setIsConfirmedAgreementDate(false);
        folder.setIsWithGiftChoiceLater(false);
        folder.setNotes(requireNonNullElse(folder.getNotes(), ""));
        folder.setCancelDescription(requireNonNullElse(folder.getCancelDescription(), ""));
        folder.setPenalityDescription(requireNonNullElse(folder.getPenalityDescription(), ""));
        folder.setCreditAgency(requireNonNullElse(folder.getCreditAgency(), ""));
        folder.setFinalTitleNumber(requireNonNullElse(folder.getFinalTitleNumber(), ""));
    }

    /**
     * Sous-articles du dossier, après son enregistrement : une ligne {@code ComFolderItem} chacun
     * ({@code UnderItem(selectedItem, sale)}), l'article rattaché au lot et une ligne « AFFECTATION ».
     */
    public void rattacherAnnexes(ComFolder folder, StkItem principal, List<StkItem> annexes, PrixSousArticles prix) {
        for (StkItem annexe : annexes) {
            ComFolderItem ligne = ligneSousArticle(folder, principal, annexe);
            prix.ligne(annexe).ifPresent(ligneOffre -> prixDeLOffre(ligne, ligneOffre));
            folderItemRepository.save(ligne);
            annexe.setComFolderId(folder.getId());
            annexe.setParent(principal);
            annexe.setIsUnderItem(true);
            stkItemRepository.save(annexe);
            historique.ligneStatut(folder, folder.getStrStatus(), annexe, HistoriqueDossier.AFFECTATION);
        }
    }

    private ComFolderItem ligneSousArticle(ComFolder folder, StkItem principal, StkItem annexe) {
        BigDecimal prix = requireNonNullElse(annexe.getComSalePrice(), BigDecimal.ZERO);
        BigDecimal prixHT = requireNonNullElse(annexe.getSalePriceHT(), BigDecimal.ZERO);
        ComFolderItem ligne = new ComFolderItem();
        ligne.setId(UUID.randomUUID());
        ligne.setComFolder(folder);
        ligne.setStkItem(annexe);
        ligne.setStkItemType(annexe.getStkItemType());
        ligne.setStkItemCategory(annexe.getStkItemCategory());
        ligne.setParentId(principal.getId());
        ligne.setCode(annexe.getCode());
        ligne.setDescription(annexe.getDescription());
        ligne.setAddress(annexe.getAddress());
        ligne.setNotes(requireNonNullElse(annexe.getNotes(), ""));
        ligne.setContenance(annexe.getContenance());
        ligne.setAreaFloor(annexe.getAreaFloor());
        ligne.setArea(requireNonNullElse(annexe.getArea(), BigDecimal.ZERO));
        ligne.setVATRatio(requireNonNullElse(annexe.getVat(), BigDecimal.ZERO));
        ligne.setPriceBeforeDiscountHT(prixHT);
        ligne.setPriceHT(prixHT);
        ligne.setPriceBeforeDiscount(prix);
        ligne.setDiscount(BigDecimal.ZERO);
        ligne.setPrice(prix);
        ligne.setAlwaysAffected(Boolean.TRUE.equals(annexe.getAlwaysAffected()));
        ligne.setIsUnderItem(true);
        ligne.setIsUnderItemOrigin(Boolean.TRUE.equals(annexe.getIsUnderItemOrigin()));
        ligne.setIsTitled(Boolean.TRUE.equals(annexe.getIsTitled()));
        ligne.setUserCreate(folder.getUserCreate());
        ligne.setUserCreateDateTime(folder.getUserCreateDateTime());
        ligne.setUserUpdate(folder.getUserCreate());
        ligne.setUserUpdateDateTime(folder.getUserCreateDateTime());
        return ligne;
    }

    /** Prix figés de l'offre d'origine, colonne pour colonne comme {@code GenerateComFolderFromQuote} ({@code Quote.cs}). */
    private static void prixDeLOffre(ComFolderItem ligne, StlQuoteItem ligneOffre) {
        ligne.setVATRatio(requireNonNullElse(ligneOffre.getVatRatio(), ligne.getVATRatio()));
        ligne.setPriceBeforeDiscountHT(requireNonNullElse(ligneOffre.getPriceBeforeDiscountHT(), ligne.getPriceBeforeDiscountHT()));
        ligne.setPriceBeforeDiscount(requireNonNullElse(ligneOffre.getPriceBeforeDiscount(), ligne.getPriceBeforeDiscount()));
        ligne.setDiscount(requireNonNullElse(ligneOffre.getDiscount(), BigDecimal.ZERO));
        ligne.setPriceHT(requireNonNullElse(ligneOffre.getPriceAfterDiscountHT(), ligne.getPriceBeforeDiscountHT()));
        ligne.setPrice(requireNonNullElse(ligneOffre.getPriceAfterDiscount(), ligne.getPriceBeforeDiscount()));
    }

    /** Ligne de création au lot principal, puis {@code StrHistory} « Création » ({@code Sale.cs:3419}, {@code AfterInsert}). */
    public void historiserCreation(ComFolder folder, StkItem principal) {
        historique.ligneStatut(folder, folder.getStrStatus(), principal, "");
        historique.statutGenerique(folder, folder.getStrStatus(), HistoriqueDossier.CREATION);
    }

    /**
     * Prix contractuel, montant d'accord et schéma texte, depuis les échéances du dossier :
     * {@code Sale.cs:778, 782, 631} et {@code GenrateComDocumentDetail}. Rien n'est encore réglé.
     */
    public void completerAvecEcheancier(ComFolder folder, List<ComDocument> lignes) {
        BigDecimal prixContractuel = BigDecimal.ZERO;
        BigDecimal avance = BigDecimal.ZERO;
        for (ComDocument ligne : lignes) {
            ComSettlementCategory categorie = ligne.getComSettlementCategory();
            BigDecimal montant = requireNonNullElse(ligne.getAmount(), BigDecimal.ZERO);
            boolean credit = categorie != null && Boolean.TRUE.equals(categorie.getIsCredit());
            if (credit && !Boolean.TRUE.equals(ligne.getExcludeFromPrice())) {
                prixContractuel = prixContractuel.add(montant);
            }
            if (categorie != null && Boolean.TRUE.equals(categorie.getIsSaleAdvance())) {
                avance = avance.add(credit ? montant : montant.negate());
            }
        }
        folder.setAgreementPrice(prixContractuel);
        folder.setAgreementAmount(requireNonNullElse(folder.getPrice(), BigDecimal.ZERO).subtract(avance));
        folder.setComDocumentDetail(detailEcheancier(lignes));
    }

    /** Format exact de {@code Sale.cs:GenrateComDocumentDetail}, orthographe comprise (« Schèma »). */
    static String detailEcheancier(List<ComDocument> lignes) {
        StringBuilder texte = new StringBuilder(ENTETE_SCHEMA).append(FIN_DE_LIGNE);
        lignes.stream()
                .sorted(Comparator.comparing(ComDocument::getDocDate, Comparator.nullsLast(Comparator.naturalOrder())))
                .forEach(ligne -> {
                    BigDecimal montant = requireNonNullElse(ligne.getAmount(), BigDecimal.ZERO);
                    String date = ligne.getDocDate() != null ? ligne.getDocDate().format(JJ_MM_AAAA) : "";
                    String categorie = ligne.getComSettlementCategory() != null
                            ? ligne.getComSettlementCategory().getDescription() : "";
                    texte.append("Date :").append(date)
                            .append("   Montant :").append(monnaie(montant))
                            .append("   Réglé :").append(monnaie(BigDecimal.ZERO))
                            .append("   Reste :").append(monnaie(montant))
                            .append("   Exclure du prix contractuel:")
                            .append(Boolean.TRUE.equals(ligne.getExcludeFromPrice()) ? "Oui" : "Non")
                            .append("   Catégorie : ").append(categorie)
                            .append(FIN_DE_LIGNE);
                });
        return texte.toString();
    }

    /** {@code decimal.ToString("c").PadLeft(20)} tel que l'écrit OMRAN : « 1 075 200.00 DH » sur 20 caractères. */
    static String monnaie(BigDecimal montant) {
        DecimalFormatSymbols symboles = new DecimalFormatSymbols(Locale.ROOT);
        symboles.setGroupingSeparator(' ');
        symboles.setDecimalSeparator('.');
        DecimalFormat format = new DecimalFormat("#,##0.00", symboles);
        String texte = format.format(montant.setScale(2, RoundingMode.HALF_UP)) + " DH";
        return " ".repeat(Math.max(0, 20 - texte.length())) + texte;
    }

    private static LocalDateTime dateDeValidite(LocalDateTime dateDossier, StrStatus statut) {
        Integer delai = statut != null ? statut.getDelai() : null;
        return delai != null && delai > 0 ? dateDossier.plusDays(delai) : null;
    }

    private static boolean estDejaClient(CfgTier client) {
        return client.getCfgTierType() != null && client.getCfgTierType().getCode() != null
                && CODE_TYPE_CLIENT.equalsIgnoreCase(client.getCfgTierType().getCode().trim());
    }
}
