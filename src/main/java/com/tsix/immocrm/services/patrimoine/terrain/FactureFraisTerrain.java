package com.tsix.immocrm.services.patrimoine.terrain;

import com.tsix.immocrm.dto.patrimoine.FactureFraisTerrainDto;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.buy.BuyDocument;
import com.tsix.immocrm.entities.buy.BuyDocumentLine;
import com.tsix.immocrm.entities.buy.BuyDocumentStatusHistory;
import com.tsix.immocrm.entities.buy.BuyVentilationCategory;
import com.tsix.immocrm.entities.cfg.CfgCompany;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.stk.StkExpense;
import com.tsix.immocrm.entities.stk.StkExpenseType;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.entities.stl.StlCategory;
import com.tsix.immocrm.exceptions.ConflictException;
import com.tsix.immocrm.exceptions.InvalidRequestException;
import com.tsix.immocrm.security.UtilisateurAffiche;
import com.tsix.immocrm.services.patrimoine.terrain.ConfigurationFacture.Configuration;
import com.tsix.immocrm.services.sequence.GenerateurCodeSequence;
import com.tsix.immocrm.utils.Champs;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * « Valider pour règlement » (FEATURE-patrimoine-terrains.md §3) : seul écrivain de {@code BuyDocument} du CRM. Facture,
 * ligne, ventilation, historique, séquence et {@code IsValidated} dans une seule transaction — OMRAN
 * ({@code LandProject.cs:ValidateExpense}) les sépare. Tout est contrôlé et lu avant la première écriture.
 */
@Component
public class FactureFraisTerrain {

    static final String DEJA_VALIDE = "Vous ne pouvez pas ajouter une facture pour un frais qui est déjà validé !";
    private static final String SEQUENCE = "Facture fournisseur";

    private final TerrainsVisibles terrains;
    private final FraisFactures factures;
    private final ConfigurationFacture configuration;
    private final GenerateurCodeSequence sequences;
    private final UtilisateurAffiche utilisateur;
    private final EntityManager em;
    private final Clock clock;

    FactureFraisTerrain(TerrainsVisibles terrains, FraisFactures factures, ConfigurationFacture configuration,
                        GenerateurCodeSequence sequences, UtilisateurAffiche utilisateur, EntityManager em, Clock clock) {
        this.terrains = terrains;
        this.factures = factures;
        this.configuration = configuration;
        this.sequences = sequences;
        this.utilisateur = utilisateur;
        this.em = em;
        this.clock = clock;
    }

    @Transactional
    public FactureFraisTerrainDto valider(UUID terrainId, UUID fraisId) {
        StkItem terrain = terrains.exiger(terrainId);
        StkExpense frais = factures.verrouiller(fraisId, terrainId);
        if (FraisFactures.valide(frais, factures.parFrais(List.of(frais)))) {
            throw new ConflictException(DEJA_VALIDE);
        }
        CfgTier fournisseur = frais.getCfgSupplier() != null ? frais.getCfgSupplier() : terrain.getCfgSupplier();
        if (fournisseur == null) {
            throw new InvalidRequestException(FraisTerrainService.SANS_FOURNISSEUR);
        }
        StkExpenseType type = frais.getStkExpenseType();
        if (type == null || type.getStlCategory() == null || type.getPrjMarketType() == null) {
            throw new InvalidRequestException("Le type de frais %s n'a pas de catégorie comptable ou de type de marché."
                    .formatted(Champs.de(type, StkExpenseType::getDescription)));
        }
        Configuration conf = configuration.lire();
        BuyDocument facture = facture(terrain, frais, fournisseur, type, conf, sequences.suivantSelonLigne(SEQUENCE));
        ecrireLignes(facture, frais, type, conf.statut());
        frais.setIsValidated(true);
        frais.setUserUpdate(facture.getUserCreate());
        frais.setUserUpdateDateTime(facture.getUserCreateDateTime());
        return new FactureFraisTerrainDto(facture.getId(), facture.getCode(), facture.getAmountTTC());
    }

    /** Valeurs de la facture mesurée (§3.2) ; montants = montant du frais, sans arrondi ni TVA. */
    private BuyDocument facture(StkItem terrain, StkExpense frais, CfgTier fournisseur, StkExpenseType type,
                                Configuration conf, String code) {
        LocalDateTime maintenant = LocalDateTime.now(clock);
        LocalDateTime jour = LocalDate.now(clock).atStartOfDay();
        BigDecimal montant = frais.getAmount();
        BuyDocument f = new BuyDocument();
        f.setId(UUID.randomUUID());
        f.setCode(code);
        f.setDescription("Facture N° %s du %s".formatted(code, jour.format(SuppressionTerrain.JOUR)));
        f.setDocDate(jour);
        f.setPrevisionDeliveryDate(jour);
        Integer delai = fournisseur.getPaymentDelay();
        f.setMaturityDate(delai != null && delai > 0 ? jour.plusDays(delai) : jour);
        f.setDocType("INVOICE");
        f.setReferenceType("LAND_EXPENSE");
        f.setStkExpenseId(frais.getId());
        f.setCfgTierId(fournisseur.getId());
        f.setSupplierDescription(fournisseur.getDescription());
        f.setCfgCompanyId(Champs.de(terrain.getCfgCompany(), CfgCompany::getId));
        f.setPrjMarketTypeId(type.getPrjMarketType().getId());
        f.setNotes("Frais : %s sur terrain : %s".formatted(type.getDescription(), terrain.getDescription()));
        f.setAdmModuleId(conf.module());
        f.setStlCurrencyId(conf.devise());
        f.setStrEntityId(conf.entite());
        f.setBuyDocumentStatusId(conf.statut());
        f.setCfgTierTriggerId(employeDuCompte());
        f.setAmountHT(montant);
        f.setAmountTTC(montant);
        f.setAmountToPay(montant);
        zeros(f);
        f.setIsSelected(false);
        f.setIsClosed(false);
        f.setCanBePayed(true);
        f.setIsPropotionCharged(false);
        f.setIsAccounted(false);
        f.setLinesCount(1);
        f.setDocRef("");
        f.setPersonToContact("");
        f.setNotes2("");
        f.setOfficeOrderRef("");
        String auteur = utilisateur.courant().nom();
        f.setUserCreate(auteur);
        f.setUserCreateDateTime(maintenant);
        f.setUserUpdate(auteur);
        f.setUserUpdateDateTime(maintenant);
        em.persist(f);
        return f;
    }

    /** {@code AdmUser.CfgEmployeeId} lu en base : le compte de la session peut être chargé depuis plus longtemps. */
    private UUID employeDuCompte() {
        UUID compte = Champs.de(utilisateur.courant().compte(), AdmUser::getId);
        if (compte == null) {
            return null;
        }
        return em.createQuery("SELECT e.id FROM AdmUser u LEFT JOIN u.cfgEmployee e WHERE u.id = :id", UUID.class)
                .setParameter("id", compte).getResultStream().filter(Objects::nonNull).findFirst().orElse(null);
    }

    /** Montants nuls de la facture mesurée, dont les 13 colonnes à {@code DEFAULT(0)} absent de yesRemsTest. */
    private static void zeros(BuyDocument f) {
        BigDecimal z = BigDecimal.ZERO;
        f.setAmountVat(z);
        f.setFiscalStamp(z);
        f.setAmountRegul(z);
        f.setStockAmountToPay(z);
        f.setStockAmountToPayHT(z);
        f.setWorkingOutAdvanceAmount(z);
        f.setRetainedAmount(z);
        f.setProrataRefundAmount(z);
        f.setFluctuationsToPay(z);
        f.setFluctuationsToPayHT(z);
        f.setAdvanceRatio(z);
        f.setAdvanceAmount(z);
        f.setAdvanceRest(z);
        f.setWarrantyRatio(z);
        f.setWarrantyAmount(z);
        f.setProportionRatio(z);
        f.setProportionAmount(z);
        f.setInsuranceDECRatio(z);
        f.setInsuranceDECAmount(z);
        f.setInsuranceDECRest(z);
        f.setInsuranceTRCRatio(z);
        f.setInsuranceTRCAmount(z);
        f.setInsuranceTRCRest(z);
    }

    /** Une ligne ({@code Invoice.cs:940-970}), une ventilation à 100 % ({@code DocumentBase.Verif}), un historique. */
    private void ecrireLignes(BuyDocument facture, StkExpense frais, StkExpenseType type, UUID statut) {
        UUID categorie = Champs.de(type.getStlCategory(), StlCategory::getId);
        BuyDocumentLine l = new BuyDocumentLine();
        l.setId(UUID.randomUUID());
        l.setBuyDocumentId(facture.getId());
        l.setCode(type.getCode());
        l.setDescription("Frais : " + type.getDescription());
        l.setUnity("U");
        l.setQuantity(BigDecimal.ONE);
        l.setSorting(1);
        l.setUnitPriceHT(frais.getAmount());
        l.setUnitPriceTTC(frais.getAmount());
        l.setVatRatio(BigDecimal.ZERO);
        l.setDiscountRatio(BigDecimal.ZERO);
        l.setFodecRatio(BigDecimal.ZERO);
        l.setCoefficient(BigDecimal.ONE);
        l.setQuantityOld(BigDecimal.ZERO);
        l.setQuantityOrigin(BigDecimal.ZERO);
        l.setNotes("");
        l.setStlCategoryId(categorie);
        l.setUserCreate(facture.getUserCreate());
        l.setUserCreateDateTime(facture.getUserCreateDateTime());
        l.setUserUpdate(facture.getUserCreate());
        l.setUserUpdateDateTime(facture.getUserCreateDateTime());
        em.persist(l);

        BuyVentilationCategory v = new BuyVentilationCategory();
        v.setId(UUID.randomUUID());
        v.setBuyDocumentId(facture.getId());
        v.setStlCategoryId(categorie);
        v.setVentilationRate(BigDecimal.ONE);
        em.persist(v);

        BuyDocumentStatusHistory h = new BuyDocumentStatusHistory();
        h.setId(UUID.randomUUID());
        h.setBuyDocumentId(facture.getId());
        h.setBuyDocumentStatusId(statut);
        h.setDocDate(facture.getUserCreateDateTime());
        h.setAdmUserId(Champs.de(utilisateur.courant().compte(), AdmUser::getId));
        h.setNotes("");
        em.persist(h);
    }
}
