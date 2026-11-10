package com.tsix.immocrm.services.achat.ecriture;

import com.tsix.immocrm.domain.achat.NetAPayerAchat;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.buy.BuyDocument;
import com.tsix.immocrm.entities.buy.BuyDocumentLine;
import com.tsix.immocrm.entities.buy.BuyDocumentStatusHistory;
import com.tsix.immocrm.entities.buy.BuyVentilationCategory;
import com.tsix.immocrm.entities.buy.BuyVentilationExpense;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.security.UtilisateurAffiche;
import com.tsix.immocrm.services.achat.ecriture.ConfigurationPieceAchat.Configuration;
import com.tsix.immocrm.services.achat.ecriture.PieceAchatACreer.Ligne;
import com.tsix.immocrm.services.achat.ecriture.PieceAchatACreer.Part;
import com.tsix.immocrm.services.sequence.GenerateurCodeSequence;
import com.tsix.immocrm.utils.Champs;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Seul écrivain d'une pièce d'achat (FEATURE-achat-regle-commune.md §3) : configuration et contrôles lus avant la séquence,
 * puis pièce, lignes, ventilation catégorie, ventilation projet, historique, dans la transaction de l'appelant.
 */
@Component
public class EcriturePieceAchat {

    private static final String SEQUENCE = "Facture fournisseur";

    private final ConfigurationPieceAchat configuration;
    private final GenerateurCodeSequence sequences;
    private final UtilisateurAffiche utilisateur;
    private final EntityManager em;
    private final Clock clock;

    public EcriturePieceAchat(ConfigurationPieceAchat configuration, GenerateurCodeSequence sequences,
                              UtilisateurAffiche utilisateur, EntityManager em, Clock clock) {
        this.configuration = configuration;
        this.sequences = sequences;
        this.utilisateur = utilisateur;
        this.em = em;
        this.clock = clock;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public BuyDocument creer(PieceAchatACreer piece) {
        if (piece.frais() == null && piece.projets().isEmpty()) {
            throw new IllegalStateException("Pièce sans projet ni frais : une ventilation par projet est obligatoire.");
        }
        Configuration conf = configuration.lire(piece);
        UUID declencheur = employeDuCompte();
        BuyDocument d = piece(piece, conf, declencheur, sequences.suivantSelonLigne(SEQUENCE));
        ecrireLignes(d, piece.lignes());
        ventiler(d, piece.categories(), (v, taux) -> {
            BuyVentilationCategory c = new BuyVentilationCategory();
            c.setId(UUID.randomUUID());
            c.setBuyDocumentId(d.getId());
            c.setStlCategoryId(v);
            c.setVentilationRate(taux);
            em.persist(c);
        });
        ventiler(d, piece.projets(), (v, taux) -> {
            BuyVentilationExpense e = new BuyVentilationExpense();
            e.setId(UUID.randomUUID());
            e.setBuyDocumentId(d.getId());
            e.setPrjProjectId(v);
            e.setVentilationRate(taux);
            em.persist(e);
        });
        historique(d);
        return d;
    }

    private BuyDocument piece(PieceAchatACreer piece, Configuration conf, UUID declencheur, String code) {
        LocalDateTime maintenant = LocalDateTime.now(clock);
        LocalDateTime jour = LocalDate.now(clock).atStartOfDay();
        BigDecimal ttc = piece.lignes().stream().map(l -> Objects.requireNonNullElse(l.montant(), BigDecimal.ZERO))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        CfgTier tiers = piece.tiers();
        BuyDocument d = new BuyDocument();
        d.setId(UUID.randomUUID());
        d.setCode(code);
        d.setDescription(piece.description().apply(code));
        d.setDocDate(jour);
        d.setPrevisionDeliveryDate(jour);
        Integer delai = tiers.getPaymentDelay();
        d.setMaturityDate(delai != null && delai > 0 ? jour.plusDays(delai) : jour);
        d.setDocType(piece.nature());
        d.setReferenceType(piece.referenceType());
        d.setStkExpenseId(piece.frais());
        d.setCfgTierId(tiers.getId());
        d.setSupplierDescription(tiers.getDescription());
        d.setCfgCompanyId(piece.societe());
        d.setPrjMarketTypeId(piece.typeMarche());
        d.setNotes(piece.notes());
        d.setAdmModuleId(conf.module());
        d.setStlCurrencyId(conf.devise());
        d.setStrEntityId(conf.entite());
        d.setBuyDocumentStatusId(conf.statut());
        d.setCfgTierTriggerId(declencheur);
        zeros(d);
        d.setAmountHT(ttc);
        d.setAmountTTC(ttc);
        d.setAmountToPay(NetAPayerAchat.calculer(ttc, NetAPayerAchat.Composantes.AUCUNE));
        d.setIsSelected(false);
        d.setIsClosed(false);
        d.setCanBePayed(true);
        d.setIsPropotionCharged(false);
        d.setIsAccounted(false);
        d.setLinesCount(piece.lignes().size());
        d.setDocRef("");
        d.setPersonToContact("");
        d.setNotes2("");
        d.setOfficeOrderRef("");
        String auteur = utilisateur.courant().nom();
        d.setUserCreate(auteur);
        d.setUserCreateDateTime(maintenant);
        d.setUserUpdate(auteur);
        d.setUserUpdateDateTime(maintenant);
        em.persist(d);
        return d;
    }

    /** Montants nuls, dont les 13 colonnes à {@code DEFAULT(0)} absent de yesRemsTest. */
    private static void zeros(BuyDocument d) {
        BigDecimal z = BigDecimal.ZERO;
        List.<Consumer<BigDecimal>>of(d::setAmountVat, d::setFiscalStamp, d::setAmountRegul,
                d::setStockAmountToPay, d::setStockAmountToPayHT, d::setWorkingOutAdvanceAmount, d::setRetainedAmount,
                d::setProrataRefundAmount, d::setFluctuationsToPay, d::setFluctuationsToPayHT, d::setAdvanceRatio,
                d::setAdvanceAmount, d::setAdvanceRest, d::setWarrantyRatio, d::setWarrantyAmount, d::setProportionRatio,
                d::setProportionAmount, d::setInsuranceDECRatio, d::setInsuranceDECAmount, d::setInsuranceDECRest,
                d::setInsuranceTRCRatio, d::setInsuranceTRCAmount, d::setInsuranceTRCRest).forEach(s -> s.accept(z));
    }

    private void ecrireLignes(BuyDocument d, List<Ligne> lignes) {
        int rang = 1;
        for (Ligne ligne : lignes) {
            BuyDocumentLine l = new BuyDocumentLine();
            l.setId(UUID.randomUUID());
            l.setBuyDocumentId(d.getId());
            l.setCode(ligne.code());
            l.setDescription(ligne.description());
            l.setUnity("U");
            l.setQuantity(BigDecimal.ONE);
            l.setSorting(rang++);
            l.setUnitPriceHT(ligne.montant());
            l.setUnitPriceTTC(ligne.montant());
            l.setVatRatio(BigDecimal.ZERO);
            l.setDiscountRatio(BigDecimal.ZERO);
            l.setFodecRatio(BigDecimal.ZERO);
            l.setCoefficient(BigDecimal.ONE);
            l.setQuantityOld(BigDecimal.ZERO);
            l.setQuantityOrigin(BigDecimal.ZERO);
            l.setNotes(ligne.notes());
            l.setStlCategoryId(ligne.categorie());
            l.setUserCreate(d.getUserCreate());
            l.setUserCreateDateTime(d.getUserCreateDateTime());
            l.setUserUpdate(d.getUserCreate());
            l.setUserUpdateDateTime(d.getUserCreateDateTime());
            em.persist(l);
        }
    }

    /** Taux = part / TTC à 6 décimales ; le dernier complète Σ = 1. */
    private static void ventiler(BuyDocument d, List<Part> parts, BiConsumer<UUID, BigDecimal> ecrire) {
        BigDecimal reste = BigDecimal.ONE;
        int n = 0;
        for (Part p : parts) {
            BigDecimal taux = ++n == parts.size() ? reste : p.montant().divide(d.getAmountTTC(), 6, RoundingMode.HALF_UP);
            reste = reste.subtract(taux);
            ecrire.accept(p.cle(), taux);
        }
    }

    private void historique(BuyDocument d) {
        BuyDocumentStatusHistory h = new BuyDocumentStatusHistory();
        h.setId(UUID.randomUUID());
        h.setBuyDocumentId(d.getId());
        h.setBuyDocumentStatusId(d.getBuyDocumentStatusId());
        h.setDocDate(d.getUserCreateDateTime());
        h.setAdmUserId(Champs.de(utilisateur.courant().compte(), AdmUser::getId));
        h.setNotes("");
        em.persist(h);
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
}
