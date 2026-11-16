package com.tsix.immocrm.services.achat.ecriture;

import com.tsix.immocrm.domain.achat.MontantsPieceAchat;
import com.tsix.immocrm.domain.achat.NatureDocumentAchat;
import com.tsix.immocrm.domain.achat.NetAPayerAchat;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.buy.BuyDocument;
import com.tsix.immocrm.entities.buy.BuyDocumentLine;
import com.tsix.immocrm.entities.buy.BuyDocumentStatusHistory;
import com.tsix.immocrm.entities.buy.BuyVentilationCategory;
import com.tsix.immocrm.entities.buy.BuyVentilationExpense;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.prj.PrjMarket;
import com.tsix.immocrm.security.UtilisateurAffiche;
import com.tsix.immocrm.services.achat.ecriture.ConfigurationPieceAchat.Configuration;
import com.tsix.immocrm.services.achat.ecriture.PieceAchatACreer.EnTete;
import com.tsix.immocrm.services.achat.ecriture.PieceAchatACreer.Ligne;
import com.tsix.immocrm.services.achat.ecriture.PieceAchatACreer.Part;
import com.tsix.immocrm.services.projet.PrevisionsPaiementLot;
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
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Seul écrivain d'une pièce d'achat (FEATURE-achat-regle-commune.md §3, FEATURE-achat-fiche-creation.md E-C7) :
 * configuration et contrôles lus avant la séquence, puis pièce, lignes, ventilations, historique, dans la transaction
 * de l'appelant.
 */
@Component
public class EcriturePieceAchat {

    private static final String PIECE = "buyDocumentId";
    private static final String RESTE_AVANCE = """
            SELECT (SELECT ISNULL(SUM(AmountTTC), 0) FROM BuyDocument WHERE ReferenceType = 'MARKET_ADVANCE' AND PrjMarketId = :lot),
                   (SELECT ISNULL(SUM(AdvanceAmount), 0) FROM PrjWorkingOut WHERE PrjMarketId = :lot)""";
    /** Σ des avoirs non financiers d'une facture (OMRAN {@code Invoice.cs} AmountRefunded) ; suivi de l'id de la facture. */
    public static final String AVOIRS_NON_FINANCIERS = "SELECT SUM(r.AmountToPay) FROM BuyDocument r "
            + "WHERE r.DocType = 'REFUND' AND r.IsFinancialRefund = 0 AND r.InvoiceSourceId = ";
    private static final String AVOIRS_DE_LA_FACTURE = "SELECT ISNULL((" + AVOIRS_NON_FINANCIERS + ":facture), 0)";

    private final ConfigurationPieceAchat configuration;
    private final GenerateurCodeSequence sequences;
    private final UtilisateurAffiche utilisateur;
    private final EntityManager em;
    private final Clock clock;
    private final PrevisionsPaiementLot previsions;

    public EcriturePieceAchat(ConfigurationPieceAchat configuration, GenerateurCodeSequence sequences,
                              UtilisateurAffiche utilisateur, EntityManager em, Clock clock, PrevisionsPaiementLot previsions) {
        this.configuration = configuration;
        this.sequences = sequences;
        this.utilisateur = utilisateur;
        this.em = em;
        this.clock = clock;
        this.previsions = previsions;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public BuyDocument creer(PieceAchatACreer piece) {
        EnTete e = piece.entete();
        if (piece.frais() == null && piece.projets().isEmpty() && (e == null || e.projet() == null)) {
            throw new IllegalStateException("Pièce sans projet ni frais : une ventilation par projet est obligatoire.");
        }
        Configuration conf = configuration.lire(piece);
        UUID declencheur = e == null ? employeDuCompte() : e.declencheur();
        String code = sequences.suivantSelonLigne(NatureDocumentAchat.valueOf(piece.nature()).sequence());
        LocalDateTime maintenant = LocalDateTime.now(clock);
        String auteur = utilisateur.courant().nom();
        BuyDocument d = new BuyDocument();
        d.setId(UUID.randomUUID());
        d.setCode(code);
        d.setDocType(piece.nature());
        d.setReferenceType(piece.referenceType());
        d.setStkExpenseId(piece.frais());
        d.setStlCurrencyId(conf.devise());
        d.setStrEntityId(conf.entite());
        d.setBuyDocumentStatusId(conf.statut());
        zeros(d);
        d.setIsSelected(false);
        d.setIsClosed(false);
        d.setCanBePayed(true);
        d.setIsPropotionCharged(false);
        d.setIsAccounted(false);
        d.setDocRef("");
        d.setPersonToContact("");
        d.setNotes2("");
        d.setOfficeOrderRef("");
        d.setUserCreate(auteur);
        d.setUserCreateDateTime(maintenant);
        entete(d, piece, conf.module(), declencheur);
        d.setDescription(piece.description().apply(code));
        if (e != null && e.lot() != null && NatureDocumentAchat.INVOICE.name().equals(piece.nature())) {
            retenuesDuLot(d);
        }
        em.persist(d);
        ecrireContenu(d, piece, Map.of(), BigDecimal.ZERO);
        if (d.getBuyDocumentStatusId() != null && !NatureDocumentAchat.DELIVRY.name().equals(piece.nature())) {
            historique(d);
        }
        previsions.apresPiece(d);
        return d;
    }

    /** En-tête, lignes (synchronisées par id) et ventilations ; nature, numéro, gestion, statut, liens et retenues intacts. */
    @Transactional(propagation = Propagation.MANDATORY)
    public BuyDocument modifier(BuyDocument d, PieceAchatACreer piece) {
        EnTete e = Objects.requireNonNull(piece.entete());
        BigDecimal avoirs = NatureDocumentAchat.INVOICE.name().equals(d.getDocType())
                ? new BigDecimal(em.createNativeQuery(AVOIRS_DE_LA_FACTURE).setParameter("facture", d.getId())
                        .getSingleResult().toString())
                : BigDecimal.ZERO;
        UUID ancienLot = d.getPrjMarketId();
        entete(d, piece, e.module(), e.declencheur());
        List.of(BuyVentilationCategory.class, BuyVentilationExpense.class).forEach(v -> lire(v, d.getId()).forEach(em::remove));
        Map<UUID, BuyDocumentLine> existantes = lire(BuyDocumentLine.class, d.getId()).stream()
                .collect(Collectors.toMap(BuyDocumentLine::getId, Function.identity()));
        ecrireContenu(d, piece, existantes, avoirs);
        previsions.apresPiece(d);
        if (ancienLot != null && !ancienLot.equals(d.getPrjMarketId())) {
            previsions.recalculer(ancienLot);
        }
        return d;
    }

    /** Colonnes d'en-tête communes à la création et à la modification ; auteur et date de modification. */
    private void entete(BuyDocument d, PieceAchatACreer piece, UUID module, UUID declencheur) {
        CfgTier tiers = piece.tiers();
        EnTete e = piece.entete();
        d.setCfgTierId(tiers.getId());
        d.setSupplierDescription(tiers.getDescription());
        d.setCfgCompanyId(piece.societe());
        d.setPrjMarketTypeId(piece.typeMarche());
        d.setNotes(piece.notes());
        d.setAdmModuleId(module);
        d.setCfgTierTriggerId(declencheur);
        if (e == null) {
            LocalDate jour = LocalDate.now(clock);
            dates(d, jour, null, jour, tiers, null);
        } else {
            d.setPrjProjectId(e.projet());
            d.setPrjMarketId(e.lot());
            d.setDocRef(e.reference());
            d.setPersonToContact(e.personne());
            d.setNotes2(e.notes2());
            d.setOfficeOrderRef(e.referenceBureauOrdre());
            d.setAmountRegul(e.regul());
            d.setFiscalStamp(e.timbre());
            dates(d, e.date(), e.echeance(), Objects.requireNonNullElse(e.livraison(), e.date()), tiers, e.lot());
        }
        d.setUserUpdate(utilisateur.courant().nom());
        d.setUserUpdateDateTime(LocalDateTime.now(clock));
    }

    /** Échéance absente : date + délai du lot s'il est > 0, sinon du fournisseur (OMRAN {@code CalculateMaturityDate}). */
    private void dates(BuyDocument d, LocalDate date, LocalDate echeance, LocalDate livraison, CfgTier tiers, UUID lot) {
        d.setDocDate(date.atStartOfDay());
        d.setPrevisionDeliveryDate(livraison.atStartOfDay());
        LocalDate fin = echeance;
        if (fin == null) {
            Integer delaiLot = lot == null ? null : Champs.de(em.find(PrjMarket.class, lot), PrjMarket::getPaymentDelay);
            Integer delai = delaiLot != null && delaiLot > 0 ? delaiLot : tiers.getPaymentDelay();
            fin = delai != null && delai > 0 ? date.plusDays(delai) : date;
        }
        d.setMaturityDate(fin.atStartOfDay());
    }

    /** Facture à lot : taux de retenue du lot et reste d'avance (OMRAN {@code GetMarketInfoForInvoice}, Q-6). */
    private void retenuesDuLot(BuyDocument d) {
        PrjMarket lot = Objects.requireNonNull(em.find(PrjMarket.class, d.getPrjMarketId()));
        d.setAdvanceRatio(zero(lot.getAdvanceRatio()));
        d.setWarrantyRatio(zero(lot.getWarrantyRatio()));
        d.setProportionRatio(zero(lot.getProportionRatio()));
        d.setInsuranceDECRatio(zero(lot.getInsuranceDECRatio()));
        d.setInsuranceTRCRatio(zero(lot.getInsuranceTRCRatio()));
        Object[] sommes = (Object[]) em.createNativeQuery(RESTE_AVANCE).setParameter("lot", lot.getId()).getSingleResult();
        BigDecimal avances = new BigDecimal(sommes[0].toString());
        d.setAdvanceRest(avances.signum() == 0 ? BigDecimal.ZERO : avances.subtract(new BigDecimal(sommes[1].toString())));
    }

    /** Lignes, montants et ventilations ; {@code existantes} : lignes de la pièce par id, celles absentes du corps supprimées. */
    private void ecrireContenu(BuyDocument d, PieceAchatACreer piece, Map<UUID, BuyDocumentLine> existantes,
                               BigDecimal avoirs) {
        List<Ligne> lignes = piece.lignes();
        MontantsPieceAchat.Totaux totaux = MontantsPieceAchat.totaux(lignes.stream().map(Ligne::montants).toList());
        d.setAmountHT(totaux.ht());
        d.setAmountVat(totaux.tva());
        d.setAmountTTC(totaux.ttc());
        d.setAmountToPay(NetAPayerAchat.calculer(totaux.ttc(), composantes(d, avoirs)));
        d.setLinesCount(lignes.size());
        List<UUID> gardees = lignes.stream().map(Ligne::id).filter(Objects::nonNull).toList();
        existantes.values().stream().filter(l -> !gardees.contains(l.getId())).forEach(em::remove);
        int rang = 1;
        for (Ligne ligne : lignes) {
            if (ligne.id() == null) {
                // remplie avant persist : Hibernate fige l'INSERT au persist, le reste partirait en UPDATE
                em.persist(remplir(nouvelleLigne(d), ligne, rang++, d));
            } else {
                remplir(existantes.get(ligne.id()), ligne, rang++, d);
            }
        }
        BigDecimal baseProjets = piece.entete() == null ? d.getAmountTTC() : null;
        ventiler(piece.categories(), d.getAmountTTC(), (cle, taux) -> {
            BuyVentilationCategory c = new BuyVentilationCategory();
            c.setId(UUID.randomUUID());
            c.setBuyDocumentId(d.getId());
            c.setStlCategoryId(cle);
            c.setVentilationRate(taux);
            em.persist(c);
        });
        ventiler(piece.projets(), baseProjets, (cle, taux) -> {
            BuyVentilationExpense v = new BuyVentilationExpense();
            v.setId(UUID.randomUUID());
            v.setBuyDocumentId(d.getId());
            v.setPrjProjectId(cle);
            v.setVentilationRate(taux);
            em.persist(v);
        });
    }

    private static BuyDocumentLine nouvelleLigne(BuyDocument d) {
        BuyDocumentLine l = new BuyDocumentLine();
        l.setId(UUID.randomUUID());
        l.setBuyDocumentId(d.getId());
        l.setCoefficient(BigDecimal.ONE);
        l.setQuantityOld(BigDecimal.ZERO);
        l.setQuantityOrigin(BigDecimal.ZERO);
        l.setUserCreate(d.getUserUpdate());
        l.setUserCreateDateTime(d.getUserUpdateDateTime());
        return l;
    }

    static BuyDocumentLine remplir(BuyDocumentLine l, Ligne ligne, int rang, BuyDocument d) {
        l.setCode(ligne.code());
        l.setDescription(ligne.description());
        l.setUnity(ligne.unite());
        l.setQuantity(ligne.quantite());
        l.setSorting(rang);
        l.setUnitPriceHT(ligne.prix());
        l.setUnitPriceTTC(ligne.montants().prixTTC());
        l.setVatRatio(zero(ligne.tva()));
        l.setDiscountRatio(zero(ligne.remise()));
        l.setFodecRatio(zero(ligne.fodec()));
        l.setNotes(ligne.notes());
        l.setStlCategoryId(ligne.categorie());
        l.setUserUpdate(d.getUserUpdate());
        l.setUserUpdateDateTime(d.getUserUpdateDateTime());
        return l;
    }

    private static NetAPayerAchat.Composantes composantes(BuyDocument d, BigDecimal avoirs) {
        return new NetAPayerAchat.Composantes(zero(d.getAmountRegul()), zero(d.getFiscalStamp()),
                zero(d.getStockAmountToPay()), zero(d.getProrataRefundAmount()), zero(d.getFluctuationsToPay()),
                zero(d.getProportionAmount()), zero(d.getWarrantyAmount()), zero(d.getAdvanceAmount()),
                zero(d.getInsuranceTRCAmount()), zero(d.getInsuranceDECAmount()), zero(d.getWorkingOutAdvanceAmount()),
                zero(d.getRetainedAmount()), avoirs);
    }

    private static BigDecimal zero(BigDecimal v) {
        return Objects.requireNonNullElse(v, BigDecimal.ZERO);
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

    /** Taux = part / base à 6 décimales, le dernier complète Σ = 1 ; base null : parts déjà en taux, écrites telles quelles. */
    private static void ventiler(List<Part> parts, BigDecimal base, BiConsumer<UUID, BigDecimal> ecrire) {
        if (base == null) {
            parts.forEach(p -> ecrire.accept(p.cle(), p.montant()));
            return;
        }
        BigDecimal reste = BigDecimal.ONE;
        int n = 0;
        for (Part p : parts) {
            BigDecimal taux = ++n == parts.size() ? reste : p.montant().divide(base, 6, RoundingMode.HALF_UP);
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

    private <T> List<T> lire(Class<T> entite, UUID piece) {
        return em.createQuery("SELECT x FROM " + entite.getSimpleName() + " x WHERE x." + PIECE + " = :piece", entite)
                .setParameter("piece", piece).getResultList();
    }

    /** {@code AdmUser.CfgEmployeeId} lu en base : le compte de la session peut être chargé depuis plus longtemps. */
    UUID employeDuCompte() {
        UUID compte = Champs.de(utilisateur.courant().compte(), AdmUser::getId);
        if (compte == null) {
            return null;
        }
        return em.createQuery("SELECT e.id FROM AdmUser u LEFT JOIN u.cfgEmployee e WHERE u.id = :id", UUID.class)
                .setParameter("id", compte).getResultStream().filter(Objects::nonNull).findFirst().orElse(null);
    }
}
