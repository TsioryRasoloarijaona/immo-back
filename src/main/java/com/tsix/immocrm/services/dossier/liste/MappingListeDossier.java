package com.tsix.immocrm.services.dossier.liste;

import com.tsix.immocrm.domain.ColonneDossier;
import com.tsix.immocrm.domain.ConventionRemise;
import com.tsix.immocrm.domain.PrixArticle;
import com.tsix.immocrm.entities.cfg.CfgCompany;
import com.tsix.immocrm.entities.cfg.CfgProject;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComAction;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.com.ComPaymentMethod;
import com.tsix.immocrm.entities.com.ComProspection;
import com.tsix.immocrm.entities.com.ComProspectionKind;
import com.tsix.immocrm.entities.com.ComProspectionOrigin;
import com.tsix.immocrm.entities.com.ComProspectionOriginSupport;
import com.tsix.immocrm.entities.stk.StkBuilding;
import com.tsix.immocrm.entities.stk.StkFloor;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.entities.stk.StkItemCategory;
import com.tsix.immocrm.entities.stk.StkItemType;
import com.tsix.immocrm.entities.stk.StkPresentationType;
import com.tsix.immocrm.entities.stk.StkVocation;
import com.tsix.immocrm.entities.str.StrStatus;
import com.tsix.immocrm.utils.Champs;
import com.tsix.immocrm.utils.TierNomFormatter;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.tsix.immocrm.domain.ColonneDossier.*;

/**
 * Lignes de la liste des dossiers (dossier ou lot libre). Les associations coûteuses ne sont traversées que si
 * une de leurs colonnes est demandée. Formules des prix au m² et délais : celles de {@code ComFolderView}.
 */
public final class MappingListeDossier {

    private static final int ECHELLE = 6;

    /** Statut affiché d'un lot libre, comme le « Libre » d'OMRAN ; ce n'est pas un code StrStatus. */
    public static final String STATUT_LIBRE = "LIBRE";

    private MappingListeDossier() {
    }

    public static LigneDossier depuisDossier(ComFolder f, Set<ColonneDossier> colonnes, ValeursCalculees calcul) {
        LigneDossier l = new LigneDossier()
                .met(ID, f.getId())
                .met(LIBRE, false)
                .met(CODE, f.getCode())
                .met(DATE_DOCUMENT, f.getDocDate())
                .met(STATUT, Champs.de(f.getStrStatus(), StrStatus::getCode))
                .met(PRIX, f.getPrice())
                .met(TOTAL_A_PAYER, f.getTotalToPay())
                .met(DATE_CONCRETISATION, f.getAdvanceDate())
                .met(PROSPECTION_ID, Champs.de(f.getComProspection(), ComProspection::getId))
                .met(PROSPECTION_CODE, Champs.de(f.getComProspection(), ComProspection::getCode));
        client(l, f);
        acteurs(l, f, colonnes);
        financier(l, f);
        prix(l, f, calcul);
        jalons(l, f);
        delais(l, f);
        audit(l, f);
        if (ColonneDossier.exigeLePatrimoine(colonnes) || exigeLaTranche(colonnes)) {
            patrimoine(l, f.getStkItem(), f.getStkVocation());
        }
        if (ColonneDossier.exigeLaProspection(colonnes)) {
            prospection(l, f.getComProspection());
        }
        if (ColonneDossier.exigeLArticle(colonnes)) {
            article(l, f.getStkItem());
        }
        if (calcul != null) {
            calcules(l, calcul);
        }
        return l;
    }

    /**
     * Ligne « Libre » : l'article sans dossier ; {@code id} est celui de l'article. Les colonnes par défaut d'un dossier
     * reprennent l'article pour rester lisibles : code = code article, statut LIBRE, prix = prix de vente TTC du lot.
     */
    public static LigneDossier depuisLotLibre(StkItem article) {
        LigneDossier l = new LigneDossier()
                .met(ID, article.getId())
                .met(LIBRE, true)
                .met(CODE, article.getCode())
                .met(STATUT, STATUT_LIBRE)
                .met(PRIX, PrixArticle.prixDeReference(article).map(PrixArticle.PrixDeReference::ttc).orElse(null))
                .met(SUPERFICIE, article.getArea());
        patrimoine(l, article, article.getStkVocation());
        article(l, article);
        return l;
    }

    private static boolean exigeLaTranche(Set<ColonneDossier> colonnes) {
        return colonnes.contains(DATE_PERMIS_HABITER) || colonnes.contains(DATE_REMISE_CLES_PREVUE);
    }

    private static void client(LigneDossier l, ComFolder f) {
        CfgTier c = f.getCfgCustomer();
        l.met(CLIENT_ID, Champs.de(c, CfgTier::getId))
                .met(CLIENT_NOM, TierNomFormatter.nom(c))
                .met(ANCIEN_CLIENT, f.getIsOldCustomer())
                .met(CLIENT_PRENOM, Champs.de(c, CfgTier::getFirstName))
                .met(CLIENT_NOM_FAMILLE, Champs.de(c, CfgTier::getLastName))
                .met(CLIENT_MOBILE, Champs.de(c, CfgTier::getMobile))
                .met(CLIENT_MOBILE_INDICATIF, Champs.de(c, CfgTier::getMobileIndicatif))
                .met(CLIENT_CIN, Champs.de(c, CfgTier::getIDNumber))
                .met(CLIENT_EMAIL, Champs.de(c, CfgTier::getEMail));
        if (c != null) {
            l.met(CLIENT_MOBILE_COMPLET, numero(c.getMobileIndicatif(), c.getMobile()))
                    .met(CLIENT_TELEPHONE, numero(c.getPhoneIndicatif(), c.getPhone()))
                    .met(CLIENT_ADRESSE, adresse(c));
        }
    }

    /** Lignes d'adresse remplies, jointes par « , » ; aucune ⇒ {@code null}. */
    static String adresse(CfgTier c) {
        String jointe = Stream.of(c.getAddress(), c.getAddress2(), c.getAddress3())
                .filter(StringUtils::hasText).map(String::trim).collect(Collectors.joining(", "));
        return jointe.isEmpty() ? null : jointe;
    }

    /** « (indicatif) numéro » comme la vue ; sans numéro, rien. */
    static String numero(String indicatif, String numero) {
        if (!StringUtils.hasText(numero)) {
            return null;
        }
        String prefixe = StringUtils.hasText(indicatif) ? "(" + indicatif + ")" : "";
        return (prefixe + " " + numero).trim();
    }

    private static void acteurs(LigneDossier l, ComFolder f, Set<ColonneDossier> colonnes) {
        l.met(COMMERCIAL_ID, Champs.de(f.getCfgCommercial(), CfgTier::getId))
                .met(COMMERCIAL_NOM, TierNomFormatter.nom(f.getCfgCommercial()))
                .met(AVOCAT_ID, siDemande(colonnes, AVOCAT_ID, f.getCfgLawyer(), CfgTier::getId))
                .met(AVOCAT_NOM, siDemande(colonnes, AVOCAT_NOM, f.getCfgLawyer(), TierNomFormatter::nom))
                .met(MODE_PAIE, siDemande(colonnes, MODE_PAIE, f.getComPaymentMethod(), ComPaymentMethod::getCode))
                .met(RESPONSABLE_VENTE_ID, siDemande(colonnes, RESPONSABLE_VENTE_ID, f.getCfgSaleManager(),
                        CfgTier::getId))
                .met(RESPONSABLE_VENTE_NOM, siDemande(colonnes, RESPONSABLE_VENTE_NOM, f.getCfgSaleManager(),
                        TierNomFormatter::nom))
                .met(BANQUE_CREDIT, siDemande(colonnes, BANQUE_CREDIT, f.getCreditBank(), TierNomFormatter::nom));
    }

    private static void financier(LigneDossier l, ComFolder f) {
        l.met(PRIX_AVANT_REMISE, f.getPriceBeforeDiscount())
                .met(REMISE, ConventionRemise.pourcentage(f.getDiscount()))
                .met(REMISE_MONTANT, f.getDiscountAmount())
                .met(PRIX_APRES_REMISE, f.getPriceAfterDiscount())
                // Le « montant d'avance » vient d'AgreementAmount : ComFolder n'a pas d'AdvanceAmount.
                .met(MONTANT_AVANCE, f.getAgreementAmount())
                .met(PROBABILITE_FINALISATION, f.getProbabilityOfFinalisation())
                .met(PRIX_CONTRACTUEL, f.getAgreementPrice());
    }

    private static void prix(LigneDossier l, ComFolder f, ValeursCalculees calcul) {
        l.met(SUPERFICIE, f.getArea())
                .met(SUPERFICIE_GLOBALE, f.getGlobalArea())
                .met(PRIX_M2_HT_AVANT_REMISE, f.getUnitCostHT())
                .met(PRIX_ARTICLE_HT_AVANT_REMISE, f.getItemPriceHT())
                .met(PRIX_SOUS_ARTICLES_HT_AVANT_REMISE, f.getUnderItemsPriceHT())
                .met(PRIX_AVANT_REMISE_HT, f.getPriceBeforeDiscountHT())
                .met(PRIX_M2_AVANT_REMISE, f.getUnitCost())
                .met(PRIX_ARTICLE_AVANT_REMISE, f.getItemPrice())
                .met(PRIX_SOUS_ARTICLES_AVANT_REMISE, f.getUnderItemsPrice())
                .met(TVA, f.getVATRatio())
                .met(PRIX_M2_HT, f.getUnitCostHT())
                .met(PRIX_ARTICLE_HT, f.getItemPriceHT())
                .met(PRIX_SOUS_ARTICLES_HT, f.getUnderItemsPriceHT())
                .met(PRIX_HT, f.getPriceHT())
                .met(PRIX_M2_TTC, PrixAuMetreCarre.article(f.getItemPriceAfter(), f.getArea()))
                .met(PRIX_ARTICLE_TTC, f.getItemPriceAfter())
                .met(PRIX_SOUS_ARTICLES_TTC, f.getUnderItemsPriceAfter());
        if (calcul != null) {
            prixVenteAuM2(f, calcul).forEach(l::met);
        }
    }

    /** Prix de vente au m² TTC et HT, annexes comprises ; seul calcul, repris par l'état des ventes. */
    public static Map<ColonneDossier, BigDecimal> prixVenteAuM2(ComFolder f, ValeursCalculees calcul) {
        Map<ColonneDossier, BigDecimal> prix = new EnumMap<>(ColonneDossier.class);
        prix.put(PRIX_VENTE_M2_TTC, PrixAuMetreCarre.venteTtc(f.getPriceAfterDiscount(), f.getArea(),
                calcul.superficieAnnexes()));
        prix.put(PRIX_VENTE_M2_HT, PrixAuMetreCarre.venteHt(f.getPriceAfterDiscount(), f.getArea(),
                calcul.superficieAnnexes(), f.getVATRatio()));
        return prix;
    }

    private static void jalons(LigneDossier l, ComFolder f) {
        l.met(DATE_COMPROMIS, f.getCreditApprovalDate())
                .met(DATE_ENVOI_NOTAIRE, f.getAgreementDate())
                .met(DATE_LIVRAISON_REELLE, f.getDeliveryDateReal())
                .met(DATE_FICHE_BANCAIRE, f.getBankRecordDate())
                .met(DATE_MINUTE, f.getAgreementSavingDate())
                .met(DATE_ENREGISTREMENT_CONTRAT, f.getAgreementRegistryDate())
                .met(DATE_VALIDITE_STATUT, f.getStatusValidityDate())
                .met(DATE_MODIFICATION_REMISE, f.getDiscountChangeDate())
                .met(DATE_ENVOI_NOTAIRE_PREVUE, f.getAgreementDatePrevisionel())
                .met(ENVOI_NOTAIRE_CONFIRME, f.getIsConfirmedAgreementDate())
                .met(DATE_DECISION_MEILLEUR_TAUX, f.getLoanCompanyDecisionDate())
                .met(DATE_IMPRESSION_CONTRAT, f.getContractPrintDate())
                .met(CADEAU_PLUS_TARD, f.getIsWithGiftChoiceLater());
    }

    /** Les trois premiers sont des {@code @Formula} ; le dernier, le {@code DATEDIFF(d, …)} de la vue en Java. */
    private static void delais(LigneDossier l, ComFolder f) {
        Integer meilleurTaux = jours(f.getLoanCompanySendDate(), f.getLoanCompanyDecisionDate());
        l.met(DELAI_CONCRETISATION, f.getDelaiConcretisation())
                .met(DELAI_NOTAIRE, f.getDelaiNotaire())
                .met(DELAI_LIVRAISON, f.getDelaiLivraison())
                // ISNULL(…, 0) dans la vue : zéro, pas null, quand une des deux dates manque.
                .met(DELAI_MEILLEUR_TAUX, meilleurTaux != null ? meilleurTaux : 0);
    }

    /** {@code DATEDIFF(day, a, b)} compte les changements de date, pas des tranches de 24 h. */
    static Integer jours(LocalDateTime debut, LocalDateTime fin) {
        if (debut == null || fin == null) {
            return null;
        }
        return Math.toIntExact(ChronoUnit.DAYS.between(debut.toLocalDate(), fin.toLocalDate()));
    }

    private static void audit(LigneDossier l, ComFolder f) {
        l.met(CREE_PAR, f.getUserCreate())
                .met(CREE_LE, f.getUserCreateDateTime())
                .met(MODIFIE_PAR, f.getUserUpdate())
                .met(MODIFIE_LE, f.getUserUpdateDateTime());
    }

    /** Tranche de l'article principal, jamais {@code StkItem.CfgProjectId}, toujours nul en base réelle. */
    private static void patrimoine(LigneDossier l, StkItem article, StkVocation vocation) {
        CfgTranche tranche = Champs.de(article, StkItem::getStkHierarchy);
        CfgProject projet = Champs.de(tranche, CfgTranche::getCfgProject);
        CfgCompany societe = Champs.de(projet, CfgProject::getCfgCompany);
        l.met(PROJET_ID, Champs.de(projet, CfgProject::getId))
                .met(PROJET_CODE, Champs.de(projet, CfgProject::getCode))
                .met(PROJET_LIBELLE, Champs.de(projet, CfgProject::getDescription))
                .met(TRANCHE_ID, Champs.de(tranche, CfgTranche::getId))
                .met(TRANCHE_CODE, Champs.de(tranche, CfgTranche::getCode))
                .met(TRANCHE_LIBELLE, Champs.de(tranche, CfgTranche::getDescription))
                .met(SOCIETE_ID, Champs.de(societe, CfgCompany::getId))
                .met(SOCIETE_CODE, Champs.de(societe, CfgCompany::getCode))
                .met(SOCIETE_LIBELLE, Champs.de(societe, CfgCompany::getDescription))
                .met(VOCATION_CODE, Champs.de(vocation, StkVocation::getCode))
                .met(VOCATION_LIBELLE, Champs.de(vocation, StkVocation::getDescription))
                .met(DATE_PERMIS_HABITER, Champs.de(tranche, CfgTranche::getOccupancyPermitsDate))
                .met(DATE_REMISE_CLES_PREVUE, Champs.de(tranche, CfgTranche::getDeliveryDate));
    }

    private static void prospection(LigneDossier l, ComProspection p) {
        l.met(ETAPE_PROSPECTION, Champs.de(p, ComProspection::getComProspectionKind,
                        ComProspectionKind::getDescription))
                .met(ORIGINE_PROSPECTION, Champs.de(p, ComProspection::getComProspectionOrigin,
                        ComProspectionOrigin::getDescription))
                .met(DATE_PROSPECTION, Champs.de(p, ComProspection::getDocDate))
                .met(SUPPORT_SOURCE, Champs.de(p, ComProspection::getComProspectionOriginSupport,
                        ComProspectionOriginSupport::getDescription))
                .met(ACTION_COMMERCIALE, Champs.de(p, ComProspection::getComAction, ComAction::getDescription));
    }

    private static void article(LigneDossier l, StkItem a) {
        l.met(ARTICLE_ID, Champs.de(a, StkItem::getId))
                .met(ARTICLE_CODE, Champs.de(a, StkItem::getCode))
                .met(ARTICLE_LIBELLE, Champs.de(a, StkItem::getDescription))
                .met(ARTICLE_TITRE_FONCIER, Champs.de(a, StkItem::getTitleNumber))
                .met(BLOC, Champs.de(a, StkItem::getStkBuilding, StkBuilding::getDescription))
                .met(ETAGE, Champs.de(a, StkItem::getStkFloor, StkFloor::getDescription))
                .met(CATEGORIE_ARTICLE, Champs.de(a, StkItem::getStkItemCategory, StkItemCategory::getCode))
                .met(TYPE_ARTICLE, Champs.de(a, StkItem::getStkItemType, StkItemType::getCode))
                .met(TYPE_PRESENTATION, Champs.de(a, StkItem::getStkPresentationType, StkPresentationType::getCode));
    }

    private static void calcules(LigneDossier l, ValeursCalculees c) {
        l.met(NOMBRE_ETATS_MODIFICATION, c.nombreEtatsModification())
                .met(NOMBRE_CADEAUX, c.nombreCadeaux())
                .met(ALERTE, c.alerte())
                .met(NOMBRE_ACTIONS, c.nombreActions())
                .met(DERNIERE_COMMUNICATION_DATE, c.derniereCommunicationDate())
                .met(DERNIERE_COMMUNICATION_RESULTAT, c.derniereCommunicationResultat())
                .met(DERNIERE_COMMUNICATION_MESSAGE, c.derniereCommunicationMessage())
                .met(INSTRUCTION_NON_LUE, c.instructionNonLue())
                .met(SOUS_ARTICLES_CODES, c.sousArticlesCodes())
                .met(SOUS_ARTICLES_LIBELLES, c.sousArticlesLibelles())
                .met(AUTRES_CLIENTS, c.autresClients())
                .met(CLIENT_VILLE, c.clientVille())
                .met(CLIENT_PAYS, c.clientPays());
    }

    /** Lu seulement si la colonne est demandée : une colonne absente vaut {@code null}. */
    private static <T, R> R siDemande(Set<ColonneDossier> colonnes, ColonneDossier colonne,
                                      T source, Function<T, R> lecteur) {
        return colonnes.contains(colonne) ? Champs.de(source, lecteur) : null;
    }

    /** Prix au m² de la grille OMRAN — un seul endroit pour ces divisions. */
    static final class PrixAuMetreCarre {

        private PrixAuMetreCarre() {
        }

        /** {@code prix / IIF(Area = 0, 1, Area)} : Area NULL rend NULL en SQL, seul 0 devient 1. */
        static BigDecimal article(BigDecimal prix, BigDecimal superficie) {
            if (prix == null || superficie == null) {
                return null;
            }
            BigDecimal diviseur = superficie.signum() == 0 ? BigDecimal.ONE : superficie;
            return prix.divide(diviseur, ECHELLE, RoundingMode.HALF_UP);
        }

        /** Prix après remise ÷ (superficie + annexes) ; sans superficie, le prix lui-même (vue OMRAN). */
        static BigDecimal venteTtc(BigDecimal prixApresRemise, BigDecimal superficie, BigDecimal annexes) {
            if (prixApresRemise == null) {
                return null;
            }
            BigDecimal total = totalSuperficie(superficie, annexes);
            return total.signum() == 0 ? prixApresRemise : prixApresRemise.divide(total, ECHELLE, RoundingMode.HALF_UP);
        }

        /** Idem ÷ (1 + TVA) ; TVA absente = NULL dans la vue, donc pas de prix. */
        static BigDecimal venteHt(BigDecimal prixApresRemise, BigDecimal superficie, BigDecimal annexes,
                                  BigDecimal tva) {
            if (prixApresRemise == null || tva == null) {
                return null;
            }
            BigDecimal total = totalSuperficie(superficie, annexes);
            if (total.signum() == 0) {
                return prixApresRemise;
            }
            return prixApresRemise.divide(total.multiply(BigDecimal.ONE.add(tva)), ECHELLE, RoundingMode.HALF_UP);
        }

        private static BigDecimal totalSuperficie(BigDecimal superficie, BigDecimal annexes) {
            BigDecimal s = superficie != null ? superficie : BigDecimal.ZERO;
            return annexes != null ? s.add(annexes) : s;
        }
    }
}
