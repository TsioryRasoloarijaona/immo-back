package com.tsix.immocrm.services.projet;

import com.tsix.immocrm.domain.achat.MontantsPieceAchat;
import com.tsix.immocrm.dto.projet.FactureGenereeDto;
import com.tsix.immocrm.entities.buy.BuyDocument;
import com.tsix.immocrm.entities.buy.BuyDocumentLine;
import com.tsix.immocrm.entities.cfg.CfgCompany;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.prj.PrjMarket;
import com.tsix.immocrm.entities.prj.PrjMarketType;
import com.tsix.immocrm.entities.prj.PrjWorkingOut;
import com.tsix.immocrm.entities.prj.PrjWorkingOutLine;
import com.tsix.immocrm.entities.prj.PrjWorkingOutLineExtra;
import com.tsix.immocrm.entities.stl.StlCategory;
import com.tsix.immocrm.exceptions.ConflictException;
import com.tsix.immocrm.exceptions.InvalidRequestException;
import com.tsix.immocrm.services.achat.ecriture.ConfigurationPieceAchat;
import com.tsix.immocrm.services.achat.ecriture.EcriturePieceAchat;
import com.tsix.immocrm.services.achat.ecriture.GenerationPieceAchat;
import com.tsix.immocrm.services.achat.ecriture.PieceAchatACreer;
import com.tsix.immocrm.services.achat.ecriture.PieceAchatACreer.Avoir;
import com.tsix.immocrm.services.achat.ecriture.PieceAchatACreer.EnTete;
import com.tsix.immocrm.services.achat.ecriture.PieceAchatACreer.Generation;
import com.tsix.immocrm.services.achat.ecriture.PieceAchatACreer.Ligne;
import com.tsix.immocrm.services.achat.ecriture.PieceAchatACreer.Part;
import com.tsix.immocrm.services.dossier.document.MontantFormatter;
import com.tsix.immocrm.utils.Champs;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.tsix.immocrm.domain.achat.NatureDocumentAchat.INVOICE;
import static com.tsix.immocrm.domain.achat.NatureDocumentAchat.REFUND;

/** Facture et avoir fournisseur d'un décompte (FEATURE-lc8-facture-decompte.md, OMRAN {@code Invoice.cs:689-832}). */
@Service
@Transactional
public class FacturationDecompteService {

    private static final String DROIT_FACTURE = "Yes.Stl.Front.Debit.InvoiceList.toolNew";
    private static final String DROIT_AVOIR = "Yes.Prj.Front.Market.RefundList.toolNew";
    private static final String GESTION_DECOMPTE = "PrjInvoiceDebitWorkingOut";
    private static final String SANS_DEPART = "La gestion %s n'a aucun statut de départ « Nouveau ».";
    private static final String NUMERO = "%s N° %s";
    private static final String LIGNES = "select l from PrjWorkingOutLine l where l.prjWorkingOutId = :d"
            + " order by l.sorting asc";
    private static final String EXTRAS = "select x from PrjWorkingOutLineExtra x where x.prjWorkingOutId = :d"
            + " order by x.sorting asc";
    // Colonnes lues par OMRAN (Invoice.cs l.783-784) mais non mappées sur l'entité PrjWorkingOutLineExtra.
    private static final String EXTRA_CATEGORIE_NOTES = "SELECT CAST(StlCategoryId AS char(36)), Notes"
            + " FROM PrjWorkingOutLineExtra WHERE PKey = :x";
    private static final String RETENUES = "select sum(r.amount) from PrjWorkingOutRetainedLine r"
            + " where r.prjWorkingOutId = :d";
    private static final String LIGNES_FACTURE = "select l from BuyDocumentLine l where l.buyDocumentId = :f"
            + " order by l.sorting asc";
    private static final String CATEGORIES_FACTURE = "select v.stlCategoryId, v.ventilationRate"
            + " from BuyVentilationCategory v where v.buyDocumentId = :f";
    private static final String PROJETS_FACTURE = "select v.prjProjectId, v.ventilationRate"
            + " from BuyVentilationExpense v where v.buyDocumentId = :f";

    private final DecomptesProjetService lecture;
    private final VerrouDecompte verrou;
    private final EcriturePieceAchat ecriture;
    private final ConfigurationPieceAchat configuration;
    private final GenerationPieceAchat generation;
    private final PrevisionsPaiementLot previsions;
    private final EntityManager em;
    private final Clock clock;

    public FacturationDecompteService(DecomptesProjetService lecture, VerrouDecompte verrou, EcriturePieceAchat ecriture,
                                      ConfigurationPieceAchat configuration, GenerationPieceAchat generation,
                                      PrevisionsPaiementLot previsions, EntityManager em, Clock clock) {
        this.lecture = lecture;
        this.verrou = verrou;
        this.ecriture = ecriture;
        this.configuration = configuration;
        this.generation = generation;
        this.previsions = previsions;
        this.em = em;
        this.clock = clock;
    }

    /** R1-R4 : 404 → 403 → 409 (déjà facturé) → 400 (lot sans ventilation par catégorie). */
    public FactureGenereeDto facturer(UUID id) {
        PrjWorkingOut w = lecture.visible(id);
        verrou.exiger(DROIT_FACTURE);
        if (w.getBuyDocument() != null) {
            throw new ConflictException("Une facture est déjà générée sur ce décompte");
        }
        PrjMarket lot = w.getPrjMarket();
        List<Object[]> ventilation = em.createQuery(ContenuPieceLot.VENTILATION, Object[].class)
                .setParameter("lot", lot.getId()).getResultList();
        if (ventilation.isEmpty()) {
            throw new InvalidRequestException("Le lot du décompte n'a aucune ventilation par catégorie.");
        }
        UUID categorieLot = Champs.de(lot.getStlCategory(), StlCategory::getId);
        List<Ligne> lignes = new ArrayList<>(lignes(id, categorieLot));
        lignes.addAll(extras(id, categorieLot));
        BigDecimal ttc = MontantsPieceAchat.totaux(lignes.stream().map(Ligne::montants).toList()).ttc();
        List<Part> categories = parts(ventilation, ttc);
        LocalDate date = Objects.requireNonNullElseGet(w.getDocDate(), () -> LocalDateTime.now(clock)).toLocalDate();
        EnTete e = new EnTete(configuration.module("PRJ"), lot.getPrjProject().getId(), lot.getId(),
                ecriture.employeDuCompte(), date, null, null, "", "", "", "", z(w.getAmountRegul()), BigDecimal.ZERO);
        CfgTier tiers = w.getCfgTier() != null ? w.getCfgTier() : lot.getCfgSupplier();
        BuyDocument d = ecriture.creer(new PieceAchatACreer(null, GESTION_DECOMPTE, SANS_DEPART.formatted(GESTION_DECOMPTE),
                INVOICE.name(), null, tiers, Champs.de(lot.getPrjProject().getCfgCompany(), CfgCompany::getId),
                Champs.de(lot.getPrjMarketType(), PrjMarketType::getId), null,
                code -> NUMERO.formatted(INVOICE.libelle(), code), notes(w, date), lignes, categories, List.of(), e, null,
                retenues(w)));
        if (lot.getStlCurrencyId() != null) {
            d.setStlCurrencyId(lot.getStlCurrencyId());
        }
        w.setBuyDocument(d);
        // apresPiece (écrivain) ignore les factures : R8 recalcule ici, une fois le décompte lié.
        previsions.recalculer(lot.getId());
        return dto(d);
    }

    /** R6 : avoir de la facture du décompte, en-têtes, lignes et ventilations recopiés (OMRAN {@code Refund.Create}). */
    public FactureGenereeDto avoir(UUID id) {
        PrjWorkingOut w = lecture.visible(id);
        verrou.exiger(DROIT_AVOIR);
        BuyDocument f = w.getBuyDocument();
        if (f == null) {
            throw new ConflictException("Impossible de générer un avoir sur un décompte sans facture");
        }
        List<Ligne> lignes = em.createQuery(LIGNES_FACTURE, BuyDocumentLine.class).setParameter("f", f.getId())
                .getResultStream().map(FacturationDecompteService::clone).toList();
        BigDecimal ttc = z(f.getAmountTTC());
        List<Part> categories = ventilations(CATEGORIES_FACTURE, f, taux -> ttc.multiply(taux));
        List<Part> projets = ventilations(PROJETS_FACTURE, f, Function.identity());
        EnTete e = new EnTete(f.getAdmModuleId(), f.getPrjProjectId(), f.getPrjMarketId(), ecriture.employeDuCompte(),
                LocalDate.now(clock), null, null, "", "", "", "", BigDecimal.ZERO, BigDecimal.ZERO,
                new Avoir(z(f.getAmountHT()), z(f.getAmountVat()), null));
        CfgTier tiers = f.getCfgTierId() == null ? null : em.find(CfgTier.class, f.getCfgTierId());
        Generation g = new Generation(REFUND.gestion(), f.getStlCurrencyId(), null, f.getBuyOrderId(), List.of(),
                List.of(), f.getId(), generation.avoirFinancier(REFUND), f.getBuySelectionId());
        BuyDocument a = ecriture.creer(new PieceAchatACreer(null, REFUND.gestion(),
                SANS_DEPART.formatted(REFUND.gestion()), REFUND.name(), null, tiers, f.getCfgCompanyId(),
                f.getPrjMarketTypeId(), null, code -> NUMERO.formatted(REFUND.libelle(), code),
                Objects.toString(f.getNotes(), ""), lignes, categories, projets, e, g));
        if (f.getPrjMarketId() != null) {
            previsions.recalculer(f.getPrjMarketId());
        }
        return dto(a);
    }

    /** R2 : lignes groupées par ligne de bordereau (sinon par description), quantités sommées, groupe à 0 ignoré. */
    private List<Ligne> lignes(UUID id, UUID categorieLot) {
        var groupes = em.createQuery(LIGNES, PrjWorkingOutLine.class).setParameter("d", id).getResultStream()
                .collect(Collectors.groupingBy(FacturationDecompteService::groupe, LinkedHashMap::new, Collectors.toList()));
        List<Ligne> lignes = new ArrayList<>();
        for (List<PrjWorkingOutLine> groupe : groupes.values()) {
            BigDecimal quantite = somme(groupe, PrjWorkingOutLine::getQuantity);
            if (quantite.signum() != 0) {
                PrjWorkingOutLine p = groupe.getFirst();
                BigDecimal n = BigDecimal.valueOf(groupe.size());
                lignes.add(new Ligne(null, p.getCode(), p.getDescription(), p.getUnity(), quantite,
                        somme(groupe, PrjWorkingOutLine::getUnitPriceHT).divide(n, 6, RoundingMode.HALF_UP),
                        BigDecimal.ZERO, z(p.getFodecRatio()),
                        somme(groupe, PrjWorkingOutLine::getVatRatio).divide(n, 6, RoundingMode.HALF_UP),
                        p.getStlCategoryId() != null ? p.getStlCategoryId() : categorieLot,
                        Objects.toString(p.getNotes(), ""), p.getPrjMarketLineId()));
            }
        }
        return lignes;
    }

    private static String groupe(PrjWorkingOutLine l) {
        return l.getPrjMarketLineId() != null ? l.getPrjMarketLineId().toString() : "#" + l.getDescription();
    }

    private static BigDecimal somme(List<PrjWorkingOutLine> lignes, Function<PrjWorkingOutLine, BigDecimal> valeur) {
        return lignes.stream().map(valeur).map(FacturationDecompteService::z).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** R2 : chaque extra recopié ; catégorie absente → catégorie du lot. */
    private List<Ligne> extras(UUID id, UUID categorieLot) {
        return em.createQuery(EXTRAS, PrjWorkingOutLineExtra.class).setParameter("d", id).getResultList().stream()
                .map(x -> {
                    Object[] r = (Object[]) em.createNativeQuery(EXTRA_CATEGORIE_NOTES).setParameter("x", x.getId())
                            .getSingleResult();
                    UUID categorie = r[0] == null ? categorieLot : UUID.fromString(r[0].toString().trim());
                    return new Ligne(null, x.getCode(), x.getDescription(), x.getUnity(), z(x.getQuantity()),
                            z(x.getUnitPriceHT()), BigDecimal.ZERO, z(x.getFodecRatio()), z(x.getVatRatio()), categorie,
                            Objects.toString(r[1], ""));
                }).toList();
    }

    /** Parts de la ventilation du lot sur le TTC de la facture ; l'écrivain complète la dernière (Σ = 1). */
    private static List<Part> parts(List<Object[]> ventilation, BigDecimal ttc) {
        return ventilation.stream().filter(v -> v[0] != null)
                .map(v -> new Part((UUID) v[0], ttc.multiply(z((BigDecimal) v[1])))).toList();
    }

    private List<Part> ventilations(String requete, BuyDocument f, Function<BigDecimal, BigDecimal> montant) {
        return em.createQuery(requete, Object[].class).setParameter("f", f.getId()).getResultStream()
                .filter(v -> v[0] != null).map(v -> new Part((UUID) v[0], montant.apply(z((BigDecimal) v[1])))).toList();
    }

    /** R1 (Invoice.cs l.794-806) : numéro et date du décompte, puis stock et avance sur décompte s'ils sont non nuls. */
    private static String notes(PrjWorkingOut w, LocalDate date) {
        StringBuilder notes = new StringBuilder("Décompte N° ").append(Objects.toString(w.getDocRef(), "")).append(" du ")
                .append(MontantFormatter.date(date)).append('\n');
        if (z(w.getStockAmountToPay()).signum() != 0) {
            notes.append(" La Valeur du stock est de : ").append(MontantFormatter.montant(w.getStockAmountToPay()))
                    .append('\n');
        }
        if (z(w.getWorkingOutAdvanceAmount()).signum() != 0) {
            notes.append(" Avance sur décompte à déduire  : ")
                    .append(MontantFormatter.montant(w.getWorkingOutAdvanceAmount())).append('\n');
        }
        return notes.toString();
    }

    /** R3 : retenues et taux recopiés du décompte ; RetainedAmount = Σ des retenues forfaitaires du décompte. */
    private Consumer<BuyDocument> retenues(PrjWorkingOut w) {
        BigDecimal retenu = z(em.createQuery(RETENUES, BigDecimal.class).setParameter("d", w.getId()).getSingleResult());
        return d -> {
            d.setStockAmountToPay(z(w.getStockAmountToPay()));
            d.setStockAmountToPayHT(z(w.getStockAmountToPayHT()));
            d.setWorkingOutAdvanceAmount(z(w.getWorkingOutAdvanceAmount()));
            d.setRetainedAmount(retenu);
            d.setAdvanceAmount(z(w.getAdvanceAmount()));
            d.setWarrantyAmount(z(w.getWarrantyAmount()));
            d.setInsuranceDECAmount(z(w.getInsuranceDECAmount()));
            d.setInsuranceTRCAmount(z(w.getInsuranceTRCAmount()));
            d.setProportionAmount(z(w.getProportionAmount()));
            d.setProrataRefundAmount(z(w.getProrataRefundAmount()));
            d.setFluctuationsToPay(z(w.getFluctuationsToPay()));
            d.setFluctuationsToPayHT(z(w.getFluctuationsToPayHT()));
            d.setAdvanceRatio(z(w.getAdvanceRatio()));
            d.setWarrantyRatio(z(w.getWarrantyRatio()));
            d.setProportionRatio(z(w.getProportionRatio()));
            d.setInsuranceDECRatio(z(w.getInsuranceDECRatio()));
            d.setInsuranceTRCRatio(z(w.getInsuranceTRCRatio()));
        };
    }

    private static Ligne clone(BuyDocumentLine l) {
        return new Ligne(null, l.getCode(), l.getDescription(), l.getUnity(), z(l.getQuantity()), z(l.getUnitPriceHT()),
                z(l.getDiscountRatio()), z(l.getFodecRatio()), z(l.getVatRatio()), l.getStlCategoryId(),
                Objects.toString(l.getNotes(), ""), l.getPrjMarketLineId());
    }

    private static FactureGenereeDto dto(BuyDocument d) {
        return new FactureGenereeDto(d.getId(), d.getCode(), d.getAmountToPay());
    }

    private static BigDecimal z(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}
