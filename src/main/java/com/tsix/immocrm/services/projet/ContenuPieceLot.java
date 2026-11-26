package com.tsix.immocrm.services.projet;

import com.tsix.immocrm.domain.projet.NaturePieceLot;
import com.tsix.immocrm.dto.projet.BlocAvanceDto;
import com.tsix.immocrm.dto.projet.PieceLotModeleDto;
import com.tsix.immocrm.dto.projet.PieceLotSaisieDto;
import com.tsix.immocrm.dto.projet.RetenueGarantieDto;
import com.tsix.immocrm.dto.projet.SuiviLotDto;
import com.tsix.immocrm.entities.buy.BuyDocument;
import com.tsix.immocrm.entities.cfg.CfgCompany;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.prj.PrjMarket;
import com.tsix.immocrm.entities.prj.PrjMarketType;
import com.tsix.immocrm.entities.prj.PrjWorkingOut;
import com.tsix.immocrm.services.achat.ecriture.ConfigurationPieceAchat;
import com.tsix.immocrm.services.achat.ecriture.EcriturePieceAchat;
import com.tsix.immocrm.services.achat.ecriture.PieceAchatACreer;
import com.tsix.immocrm.services.achat.ecriture.PieceAchatACreer.EnTete;
import com.tsix.immocrm.services.achat.ecriture.PieceAchatACreer.Ligne;
import com.tsix.immocrm.services.achat.ecriture.PieceAchatACreer.Part;
import com.tsix.immocrm.utils.Champs;
import com.tsix.immocrm.utils.TierNomFormatter;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** Contenu d'une pièce à payer du lot (FEATURE-lot-pieces-a-payer.md R1, R5-R8 ter) écrit par {@link EcriturePieceAchat}. */
@Component
class ContenuPieceLot {

    private static final String DOCUMENT_TO_PAY = "DOCUMENT_TO_PAY";
    private static final String MODULE = "PRJ";
    private static final String DU_LOT = "SELECT COUNT(*) FROM BuyDocument WHERE PrjMarketId = :lot AND ";
    private static final String DE_LA_NATURE = DU_LOT + "ReferenceType = :type";
    // Gestions OMRAN de la restitution de RG : lot (B602) et bon de commande multi-fournisseur (A71B4F91).
    private static final String RESTITUTIONS_RG = DU_LOT
            + "StrEntityId IN ('E7999F70-7507-40D9-AA78-A2825C42B602', 'A71B4F91-AEFC-4C70-87F7-82A6C7365CB5')";
    static final String VENTILATION ="select v.stlCategoryId, v.ventilationRate from PrjMarketVentilationCategory v"
            + " where v.prjMarketId = :lot order by v.ventilationRate asc";
    private static final String DERNIER_DECOMPTE = "select w from PrjWorkingOut w where w.prjMarket.id = :lot"
            + " order by w.sorting desc";

    private final MontantsListeLots montants;
    private final ConfigurationPieceAchat configuration;
    private final EcriturePieceAchat ecriture;
    private final DonneesDecompte decomptes;
    private final EntityManager em;
    private final Clock clock;

    ContenuPieceLot(MontantsListeLots montants, ConfigurationPieceAchat configuration, EcriturePieceAchat ecriture,
                    DonneesDecompte decomptes, EntityManager em, Clock clock) {
        this.montants = montants;
        this.configuration = configuration;
        this.ecriture = ecriture;
        this.decomptes = decomptes;
        this.em = em;
        this.clock = clock;
    }

    PieceLotModeleDto modele(PrjMarket lot, NaturePieceLot nature) {
        LocalDate jour = LocalDate.now(clock);
        BigDecimal defaut = montantParDefaut(lot, nature);
        boolean plafonne = nature == NaturePieceLot.MARKET_ADVANCE || nature == NaturePieceLot.MARKET_WARRANTY;
        return new PieceLotModeleDto(nature, jour, ecriture.echeance(jour, lot.getCfgSupplier(), lot.getId()),
                reference(lot, nature), defaut, notes(lot, nature), TierNomFormatter.nom(lot.getCfgSupplier()),
                plafonne ? defaut : null);
    }

    /** R6, plancher 0 : restes du suivi du lot ({@link com.tsix.immocrm.domain.projet.CalculLot#suivi()}). */
    BigDecimal montantParDefaut(PrjMarket lot, NaturePieceLot nature) {
        if (nature == NaturePieceLot.WORKINGOUT_ADVANCE) {
            return BigDecimal.ZERO;
        }
        SuiviLotDto suivi = Objects.requireNonNull(montants.calculer(List.of(lot)).get(lot.getId())).suivi();
        BlocAvanceDto avance = suivi.avanceMarche();
        RetenueGarantieDto rg = suivi.retenueGarantie();
        BigDecimal reste = switch (nature) {
            case MARKET_ADVANCE -> z(avance.marche()).subtract(z(avance.reglee()))
                    .min(z(avance.marche()).subtract(z(avance.validee())));
            case MARKET_WARRANTY -> Boolean.TRUE.equals(lot.getBlokWarrantyRestitution()) ? BigDecimal.ZERO
                    : z(rg.retenue()).subtract(z(rg.validee()));
            default -> z(rg.remboursement()).subtract(z(rg.aRembourser()));
        };
        return reste.max(BigDecimal.ZERO);
    }

    /** R5 : préfixe + (pièces de la nature du lot, tout statut, + 1) ; RCP sans numéro. */
    String reference(PrjMarket lot, NaturePieceLot nature) {
        if (nature == NaturePieceLot.MARKET_PROPORTION) {
            return nature.prefixe();
        }
        var requete = em.createNativeQuery(nature == NaturePieceLot.MARKET_WARRANTY ? RESTITUTIONS_RG : DE_LA_NATURE)
                .setParameter("lot", lot.getId());
        if (nature != NaturePieceLot.MARKET_WARRANTY) {
            boolean commande = nature == NaturePieceLot.MARKET_ADVANCE && Boolean.TRUE.equals(lot.getIsMultiSupplier());
            requete.setParameter("type", commande ? "ORDER_ADVANCE" : nature.name());
        }
        return nature.prefixe() + (((Number) requete.getSingleResult()).intValue() + 1);
    }

    private static String notes(PrjMarket lot, NaturePieceLot nature) {
        return "%s %s Projet : %s".formatted(nature.prefixeNotes(), lot.getDescription(), lot.getPrjProject().getDescription());
    }

    BuyDocument creer(PrjMarket lot, NaturePieceLot nature, PieceLotSaisieDto corps) {
        BigDecimal montant = corps.montantTTC() != null ? corps.montantTTC() : montantParDefaut(lot, nature);
        EnTete e = new EnTete(configuration.module(MODULE), lot.getPrjProject().getId(), lot.getId(),
                ecriture.employeDuCompte(), corps.date(), corps.echeance(), null, reference(lot, nature), "", "", "",
                regul(nature, corps), BigDecimal.ZERO);
        BuyDocument d = ecriture.creer(piece(lot, nature, corps, montant, e,
                new Origine(lot.getCfgSupplier(), Champs.de(lot.getPrjProject().getCfgCompany(), CfgCompany::getId),
                        Champs.de(lot.getPrjMarketType(), PrjMarketType::getId))));
        if (nature.avance() && lot.getStlCurrencyId() != null) {
            d.setStlCurrencyId(lot.getStlCurrencyId());
        }
        if (nature == NaturePieceLot.WORKINGOUT_ADVANCE) {
            d.setPrjWorkingOutId(decompteOuvert(lot.getId()));
            recalculerReste(d.getPrjWorkingOutId(), lot.getId());
        }
        return d;
    }

    /** Date, échéance, montant (lignes recalculées), notes, pénalité ; le reste de l'en-tête est repris de la pièce. */
    void modifier(BuyDocument d, PrjMarket lot, NaturePieceLot nature, PieceLotSaisieDto corps) {
        BigDecimal montant = Objects.requireNonNullElse(corps.montantTTC(), z(d.getAmountTTC()));
        EnTete e = new EnTete(d.getAdmModuleId(), d.getPrjProjectId(), lot.getId(), d.getCfgTierTriggerId(), corps.date(),
                corps.echeance(), null, d.getDocRef(), d.getPersonToContact(), d.getNotes2(), d.getOfficeOrderRef(),
                regul(nature, corps), z(d.getFiscalStamp()));
        CfgTier tiers = d.getCfgTierId() == null ? null : em.find(CfgTier.class, d.getCfgTierId());
        ecriture.modifier(d, piece(lot, nature, corps, montant, e,
                new Origine(tiers, d.getCfgCompanyId(), d.getPrjMarketTypeId())));
        recalculerReste(d.getPrjWorkingOutId(), lot.getId());
    }

    private record Origine(CfgTier tiers, UUID societe, UUID typeMarche) { }

    private PieceAchatACreer piece(PrjMarket lot, NaturePieceLot nature, PieceLotSaisieDto corps, BigDecimal montant,
                                   EnTete e, Origine origine) {
        List<Ligne> lignes = new ArrayList<>();
        List<Part> categories = new ArrayList<>();
        BigDecimal tva = z(lot.getDefaultVat());
        List<Object[]> ventilation = em.createQuery(VENTILATION, Object[].class).setParameter("lot", lot.getId())
                .getResultList();
        if (ventilation.isEmpty()) {
            lignes.add(ligne(nature, montant, tva, null));
        }
        BigDecimal reste = montant;
        for (int i = 0; i < ventilation.size(); i++) {
            UUID categorie = (UUID) ventilation.get(i)[0];
            BigDecimal ttc = i == ventilation.size() - 1 ? reste
                    : montant.multiply(z((BigDecimal) ventilation.get(i)[1])).setScale(6, RoundingMode.HALF_UP);
            reste = reste.subtract(ttc);
            lignes.add(ligne(nature, ttc, tva, categorie));
            if (categorie != null) {
                categories.add(new Part(categorie, ttc));
            }
        }
        return new PieceAchatACreer(null, nature.gestion(),
                "La gestion %s n'a aucun statut de départ « Nouveau ».".formatted(nature.gestion()), DOCUMENT_TO_PAY,
                nature.name(), origine.tiers(), origine.societe(), origine.typeMarche(), null, code -> "%s N° %s".formatted(nature.libelleLigne(), code),
                Objects.requireNonNullElse(corps.notes(), ""), lignes, categories, List.of(), e);
    }

    /** R7 : PU HT = TTC ÷ (1 + TVA) à 6 décimales, quantité 1, unité U. */
    private static Ligne ligne(NaturePieceLot nature, BigDecimal ttc, BigDecimal tva, UUID categorie) {
        return new Ligne(null, nature.prefixe(), nature.libelleLigne(), "U", BigDecimal.ONE,
                ttc.divide(BigDecimal.ONE.add(tva), 6, RoundingMode.HALF_UP), BigDecimal.ZERO, BigDecimal.ZERO, tva, categorie,
                "");
    }

    /** R8 ter : {@code AmountRegul} = − pénalité, restitution de RG seulement. */
    private static BigDecimal regul(NaturePieceLot nature, PieceLotSaisieDto corps) {
        return nature == NaturePieceLot.MARKET_WARRANTY && corps.penaliteRg() != null ? corps.penaliteRg().negate()
                : BigDecimal.ZERO;
    }

    void apresSuppressionAvoir(UUID factureSource) {
        ecriture.recalculerNetAPayer(factureSource);
    }

    /** R8 bis : dernier décompte du lot s'il n'est ni final ni facturé, sinon null. */
    private UUID decompteOuvert(UUID lot) {
        return em.createQuery(DERNIER_DECOMPTE, PrjWorkingOut.class).setParameter("lot", lot).setMaxResults(1)
                .getResultStream().filter(w -> !Boolean.TRUE.equals(w.getFinalWorkingOut()) && w.getBuyDocument() == null)
                .map(PrjWorkingOut::getId).findFirst().orElse(null);
    }

    /** R8 bis : reste d'avance sur décompte du décompte rattaché, par le calcul de sa création. */
    void recalculerReste(UUID decompte, UUID lot) {
        PrjWorkingOut w = decompte == null ? null : em.find(PrjWorkingOut.class, decompte);
        if (w != null) {
            w.setWorkingOutAdvanceAmountRestAtCreation(decomptes.resteAvance(lot, decompte, DonneesDecompte.AVANCE_DECOMPTE,
                    DonneesDecompte.Colonne.AVANCE_DECOMPTE));
        }
    }

    private static BigDecimal z(BigDecimal v) {
        return Objects.requireNonNullElse(v, BigDecimal.ZERO);
    }
}
