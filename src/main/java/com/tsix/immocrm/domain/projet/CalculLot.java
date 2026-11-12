package com.tsix.immocrm.domain.projet;

import com.tsix.immocrm.domain.projet.EntreesLot.Somme;
import com.tsix.immocrm.dto.projet.BlocAvanceDto;
import com.tsix.immocrm.dto.projet.BlocRetenueDto;
import com.tsix.immocrm.dto.projet.BlocRetenueDto.PartRetenueDto;
import com.tsix.immocrm.dto.projet.ColonneCoutDto;
import com.tsix.immocrm.dto.projet.ColonneRetenueDto;
import com.tsix.immocrm.dto.projet.ColonneSuiviDto;
import com.tsix.immocrm.dto.projet.GeneralLotDto;
import com.tsix.immocrm.dto.projet.RetenueGarantieDto;
import com.tsix.immocrm.dto.projet.RetenuesLotDto;
import com.tsix.immocrm.dto.projet.SuiviLotDto;
import com.tsix.immocrm.dto.projet.TrioDto;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.Arrays;

import static com.tsix.immocrm.domain.projet.EntreesLot.Somme.*;
import static com.tsix.immocrm.domain.projet.MontantsLot.arrondi;
import static com.tsix.immocrm.domain.projet.MontantsLot.pourcentage;
import static java.math.BigDecimal.ZERO;

/**
 * Seul calcul des montants d'un lot, repris de {@code PrjMarketMonitoringView} : coût prévisionnel (liste et fiche), coût réel,
 * avancements, onglets Suivi et Retenues. Voir FEATURE-lot-fiche-suivi.md.
 */
public final class CalculLot {

    private final EntreesLot e;
    private final EntreesLot.Drapeaux d;
    private final EntreesLot.Taux t;
    private final Paire est;
    private final Paire marcheAjuste;
    private final Paire marche;
    private final Paire decomptes;
    private final Paire depassements;
    private final Paire depassementsFactures;
    private final Paire supplementaires;
    private final Paire supplementairesFactures;
    private final Paire avenants;
    private final Paire avenantsFactures;
    private final Paire factures;
    private final Paire avoirs;
    private final Paire stockDecomptes;
    private final Paire fluctuationsDecomptes;
    private final Paire stockFactures;
    private final Paire fluctuationsFactures;
    private final Paire stockAvoirs;
    private final Paire fluctuationsAvoirs;

    public CalculLot(EntreesLot e) {
        this.e = e;
        d = e.drapeaux();
        t = e.taux();
        Paire lignesEstimation = paire(ESTIM_HT, ESTIM_TTC);
        est = lignesEstimation.ht().signum() == 0 ? new Paire(t.forfaitHT(), t.forfaitTTC()) : lignesEstimation;
        Paire commandes = paire(CMD_HT, CMD_TTC);
        marcheAjuste = d.multi() ? commandes : paire(LIGNES_HT, LIGNES_TTC);
        marche = d.multi() ? commandes : paire(DEVIS_HT, DEVIS_TTC);
        // Multi-fournisseur : la vue prend le HT des factures sur commande pour le HT et le TTC des décomptes.
        decomptes = d.multi() ? new Paire(s(AVEC_CMD_HT), s(AVEC_CMD_HT)) : paire(DEC_HT, DEC_TTC);
        depassements = d.multi() ? paire(CMD_DEP_HT, CMD_DEP_TTC) : paire(DEP_HT, DEP_TTC);
        depassementsFactures = d.multi() ? depassements : paire(DEP_FACT_HT, DEP_FACT_TTC);
        supplementaires = d.multi() ? paire(SANS_CMD_HT, SANS_CMD_TTC) : paire(SUPP_HT, SUPP_TTC);
        supplementairesFactures = d.multi() ? supplementaires : paire(SUPP_FACT_HT, SUPP_FACT_TTC);
        avenants = paire(AV_HT, AV_TTC);
        avenantsFactures = paire(FAV_HT, FAV_TTC);
        factures = paire(FAC_HT, FAC_TTC);
        avoirs = paire(AVO_HT, AVO_TTC);
        stockDecomptes = paire(DEC_STOCK_HT, DEC_STOCK);
        fluctuationsDecomptes = paire(DEC_FLUCT_HT, DEC_FLUCT);
        stockFactures = paire(DEC_FACT_STOCK_HT, DEC_FACT_STOCK);
        fluctuationsFactures = paire(DEC_FACT_FLUCT_HT, DEC_FACT_FLUCT);
        stockAvoirs = new Paire(s(AVO_STOCK), s(AVO_STOCK));
        fluctuationsAvoirs = new Paire(s(AVO_FLUCT), s(AVO_FLUCT));
    }

    // ------------------------------------------------------------------ onglet Général

    public GeneralLotDto general() {
        BigDecimal ratio = ratioAnnulation();
        Paire facture = factures.moins(avoirs);
        return new GeneralLotDto((d.annule() ? facture : marche).trio(),
                new TrioDto(arrondi(t.forfaitHT().multiply(ratio)), arrondi(t.forfaitTVA().multiply(ratio)),
                        arrondi(t.forfaitTTC().multiply(ratio))),
                est.fois(ratio).trio(), marcheAjuste.moins(marche).trio(), (d.annule() ? facture : marcheAjuste).trio(),
                coutPrevisionnel(), coutReel());
    }

    /** {@code CostHT/TTC}, {@code CostToPay}… : ceux de la liste des lots. */
    public ColonneCoutDto coutPrevisionnel() {
        if (d.termine()) {
            return colonne(new Lignes(facturesLignes(), stockFactures, depassementsFactures, supplementairesFactures,
                            fluctuationsFactures, avenantsFactures, factureCout().moins(avoirsTotal())),
                    null, facturesNRR(), s(FAC_REMB_PRORATA), facturesREG().subtract(avoirsREG()), s(REGLE));
        }
        boolean sansMarche = marcheAjuste.ht().signum() == 0;
        BigDecimal nrr = sansMarche ? nonRemboursables(est.ttc()) : previsionNRR();
        return colonne(new Lignes(sansMarche ? est : marcheAjuste, stockDecomptes, depassements, supplementaires,
                        fluctuationsDecomptes, avenants, totalEnCours(sansMarche)),
                null, nrr, s(PREV_REMB_PRORATA), s(REGUL), s(REGLE));
    }

    /** {@code Position*} : factures − avoirs. */
    public ColonneCoutDto coutReel() {
        return colonne(new Lignes(facturesLignes().moins(avoirs), stockFactures.moins(stockAvoirs), depassementsFactures,
                        supplementairesFactures, fluctuationsFactures.moins(fluctuationsAvoirs), avenantsFactures,
                        factures.moins(avoirs)),
                factureCout().moins(avoirsTotal()).ttc(), facturesNRR().subtract(avoirsNRR()),
                s(FAC_REMB_PRORATA).subtract(s(AVO_REMB_PRORATA)), facturesREG().subtract(avoirsREG()),
                s(REGLE).subtract(s(REGLE_AVOIRS)));
    }

    /** {@code Achievement} : facturé (hors dépassements et extras) / marché ajusté HT. */
    public BigDecimal avancementFacture() {
        return avancement(factures.ht().subtract(depassementsFactures.ht()).subtract(supplementairesFactures.ht()));
    }

    /** {@code AchievementPrev} : décompté (hors dépassements) / marché ajusté HT. */
    public BigDecimal avancementDecompte() {
        return avancement(decomptes.ht().subtract(depassements.ht()));
    }

    // ------------------------------------------------------------------ onglets Suivi et Retenues

    public SuiviLotDto suivi() {
        return new SuiviLotDto(new SuiviLotDto.ColonnesSuiviDto(colonneDecomptes(), colonneFactures(), colonneAvoirs()),
                bloc(new BigDecimal[]{pourcentage(t.avance()), sur(t.avance(), t.forfaitTTC()), sur(t.avance(), est.ttc()),
                        arrondi(d.multi() ? s(CMD_AVANCE) : t.avance().multiply(marche.ttc()))},
                        AVANCE_VALIDEE, REGLE_AVANCE, FAC_AVANCE),
                bloc(new BigDecimal[4], AVANCE_DEC_VALIDEE, REGLE_AVANCE_DEC, FAC_AVANCE_DECOMPTE),
                retenueGarantie(), retenues());
    }

    private ColonneSuiviDto colonneDecomptes() {
        Paire total = decomptesTotal();
        return new ColonneSuiviDto(arrondi(decomptes.ht()), arrondi(decomptes.tva()), arrondi(decomptes.ttc()),
                arrondi(stockDecomptes.ttc()), arrondi(depassements.ttc()), arrondi(supplementaires.ttc()),
                arrondi(fluctuationsDecomptes.ttc()), arrondi(total.ttc()), arrondi(avenants.ttc()),
                arrondi(total.plus(avenants).ttc()), arrondi(decomptesNRR()), arrondi(s(DEC_REMB_PRORATA)), null);
    }

    private ColonneSuiviDto colonneFactures() {
        return new ColonneSuiviDto(arrondi(factures.ht()), arrondi(factures.tva()), arrondi(factures.ttc()),
                arrondi(stockFactures.ttc()), arrondi(depassementsFactures.ttc()), arrondi(supplementairesFactures.ttc()),
                arrondi(fluctuationsFactures.ttc()), arrondi(facturesTotal().ttc()), arrondi(avenantsFactures.ttc()),
                arrondi(factureCout().ttc()), arrondi(facturesNRR()), arrondi(s(FAC_REMB_PRORATA)), arrondi(facturesREG()));
    }

    private ColonneSuiviDto colonneAvoirs() {
        return new ColonneSuiviDto(arrondi(avoirs.ht()), arrondi(avoirs.tva()), arrondi(avoirs.ttc()), null, null, null, null,
                null, null, null, null, null, arrondi(avoirsREG()));
    }

    /** {@code prevu} = taux, est. de base, confidentielle, marché (nuls pour l'avance sur décompte). */
    private BlocAvanceDto bloc(BigDecimal[] prevu, Somme validee, Somme reglee, Somme amortie) {
        BigDecimal r = arrondi(s(reglee));
        BigDecimal a = arrondi(s(amortie));
        return new BlocAvanceDto(prevu[0], prevu[1], prevu[2], prevu[3], arrondi(s(validee)), r, a, r.subtract(a));
    }

    private RetenueGarantieDto retenueGarantie() {
        BigDecimal taux = t.garantie();
        BigDecimal retenue = s(FAC_RG).add(s(FAV_RG));
        BigDecimal factureesPrevues = s(AV_RG).add(taux.multiply(marche.ttc())).add(taux.multiply(depassementsFactures.ttc()))
                .add(d.retenueSurSupplementaires() ? taux.multiply(supplementairesFactures.ttc()) : ZERO);
        BigDecimal reste = d.blocage() ? ZERO : retenue.subtract(s(RG_PENALITE)).subtract(s(RG_RESTITUEE));
        return new RetenueGarantieDto(pourcentage(taux), sur(taux, t.forfaitTTC()), sur(taux, est.ttc()),
                arrondi(taux.multiply(marcheAjuste.ttc())), arrondi(s(AV_RG)), arrondi(factureesPrevues), arrondi(retenue),
                arrondi(s(RG_VALIDEE)), arrondi(s(RG_RESTITUEE)), arrondi(reste), d.gestionnaire(),
                arrondi(s(FAC_REMB_PRORATA).add(s(PROPORTION))), arrondi(s(FAC_PRORATA_PROJET)));
    }

    private RetenuesLotDto retenues() {
        return new RetenuesLotDto(
                blocRetenue(t.prorata(), null, new Somme[]{AV_PRORATA, DEC_PRORATA, FAV_PRORATA, FAC_PRORATA}),
                blocRetenue(t.decRatio(), t.decMontant(), new Somme[]{AV_DEC, DEC_DEC, FAV_DEC, FAC_DEC}),
                blocRetenue(t.trcRatio(), t.trcMontant(), new Somme[]{AV_TRC, DEC_TRC, FAV_TRC, FAC_TRC}),
                new ColonneRetenueDto(arrondi(s(RETENUES_FORFAIT)), null, arrondi(s(RG_PENALITE)), arrondi(blocageDecomptes()),
                        arrondi(penaliteAvance(DEC_AVANCE, DEC_AVANCE_DECOMPTE)), arrondi(decomptesNRR())),
                // pénalité de compte prorata (factures imputées au compte prorata, ActExpenseView) non reprise : 0
                new ColonneRetenueDto(arrondi(s(RETENUES_FORFAIT)), arrondi(ZERO), arrondi(s(RG_PENALITE)),
                        arrondi(blocageFactures()), arrondi(penaliteAvance(FAC_AVANCE, FAC_AVANCE_DECOMPTE)),
                        arrondi(facturesNRR())));
    }

    /** {@code montant} nul = compte prorata (sans quote-part) ; {@code sommes} = avenants, décomptes, factures d'avenant, factures. */
    private BlocRetenueDto blocRetenue(BigDecimal ratio, BigDecimal montant, Somme[] sommes) {
        BigDecimal saisi = montant == null ? ZERO : montant;
        return new BlocRetenueDto(pourcentage(ratio), montant == null ? null : arrondi(montant),
                arrondi(assurance(saisi, ratio, t.forfaitTTC())), arrondi(assurance(saisi, ratio, est.ttc())),
                arrondi(assurance(saisi, ratio, marcheAjuste.ttc())),
                new PartRetenueDto(arrondi(s(sommes[0])), arrondi(s(sommes[1]))),
                new PartRetenueDto(arrondi(s(sommes[2])), arrondi(s(sommes[3]))));
    }

    // ------------------------------------------------------------------ calculs intermédiaires de la vue

    private Paire totalEnCours(boolean sansMarche) {
        Paire realise = marcheAjuste.plus(depassements).plus(supplementaires);
        if (d.multi()) {
            return realise.ht().compareTo(est.ht()) >= 0 ? realise : est;
        }
        return sansMarche ? est : realise.plus(fluctuationsDecomptes).plus(avenants);
    }

    /** {@code OfferedNRR + ExtraNRR + OverNRR + ClausesNRR} + retenues forfaitaires, pénalité et blocage de RG. */
    private BigDecimal previsionNRR() {
        BigDecimal ratios = t.prorata().add(t.decRatio()).add(t.trcRatio());
        BigDecimal extras = d.retenueSurSupplementaires() ? ratios.multiply(supplementaires.ttc()) : ZERO;
        return nonRemboursables(marcheAjuste.ttc()).add(extras).add(ratios.multiply(depassements.ttc()))
                .add(somme(AV_PRORATA, AV_DEC, AV_TRC, RETENUES_FORFAIT, RG_PENALITE)).add(blocageDecomptes());
    }

    private BigDecimal nonRemboursables(BigDecimal ttc) {
        if (ttc.signum() == 0) {
            return ZERO;
        }
        return t.prorata().multiply(ttc).add(assurance(t.decMontant(), t.decRatio(), ttc))
                .add(assurance(t.trcMontant(), t.trcRatio(), ttc));
    }

    private static BigDecimal assurance(BigDecimal montant, BigDecimal ratio, BigDecimal base) {
        return montant.signum() != 0 ? montant : ratio.multiply(base);
    }

    private BigDecimal decomptesNRR() {
        return somme(DEC_PRORATA, DEC_DEC, DEC_TRC, RETENUES_FORFAIT, AV_PRORATA, AV_DEC, AV_TRC, RG_PENALITE)
                .add(blocageDecomptes()).add(penaliteAvance(DEC_AVANCE, DEC_AVANCE_DECOMPTE));
    }

    private BigDecimal facturesNRR() {
        return somme(FAC_PRORATA, FAC_DEC, FAC_TRC, FAC_RETENUE, FAV_PRORATA, FAV_DEC, FAV_TRC, RG_PENALITE)
                .add(blocageFactures()).add(penaliteAvance(FAC_AVANCE, FAC_AVANCE_DECOMPTE));
    }

    private BigDecimal avoirsNRR() {
        return somme(AVO_PRORATA, AVO_DEC, AVO_TRC, AVO_RETENUE);
    }

    private BigDecimal facturesREG() {
        return somme(FAC_REGUL, FAC_TIMBRE, FAV_REGUL, FAV_TIMBRE);
    }

    private BigDecimal avoirsREG() {
        return somme(AVO_REGUL, AVO_TIMBRE);
    }

    /** Lot terminé seulement : avances réglées − avances amorties. */
    private BigDecimal penaliteAvance(Somme amortie, Somme amortieDecompte) {
        return d.termine() ? somme(REGLE_AVANCE, REGLE_AVANCE_DEC).subtract(somme(amortie, amortieDecompte)) : ZERO;
    }

    private BigDecimal blocageDecomptes() {
        return d.blocage() ? somme(DEC_RG, AV_RG).subtract(s(RG_VALIDEE)) : ZERO;
    }

    private BigDecimal blocageFactures() {
        return d.blocage() ? somme(FAC_RG, FAV_RG).subtract(s(RG_VALIDEE)) : ZERO;
    }

    private Paire decomptesTotal() {
        return decomptes.plus(stockDecomptes).plus(supplementaires).plus(fluctuationsDecomptes);
    }

    private Paire facturesTotal() {
        return factures.plus(stockFactures).plus(fluctuationsFactures);
    }

    private Paire factureCout() {
        return facturesTotal().plus(avenantsFactures);
    }

    private Paire facturesLignes() {
        return factures.moins(depassementsFactures).moins(supplementairesFactures);
    }

    private Paire avoirsTotal() {
        return avoirs.plus(stockAvoirs).plus(fluctuationsAvoirs);
    }

    /** {@code CancelRatio} : lot annulé ou résilié = avancement décompté, sinon 1. */
    private BigDecimal ratioAnnulation() {
        return d.annule() ? avancementDecompte() : BigDecimal.ONE;
    }

    private BigDecimal avancement(BigDecimal realise) {
        if (marcheAjuste.ht().signum() == 0) {
            return ZERO;
        }
        BigDecimal r = d.multi() ? realise.subtract(avoirs.ht()) : realise;
        return r.divide(marcheAjuste.ht(), MathContext.DECIMAL64).setScale(6, RoundingMode.HALF_UP);
    }

    private BigDecimal sur(BigDecimal taux, BigDecimal base) {
        return arrondi(taux.multiply(base).multiply(ratioAnnulation()));
    }

    /** {@code base} nul = TTC du total (prévisionnel). */
    private static ColonneCoutDto colonne(Lignes l, BigDecimal base, BigDecimal nrr, BigDecimal remboursement,
                                          BigDecimal regulation, BigDecimal regle) {
        BigDecimal ttc = base == null ? l.total().ttc() : base;
        BigDecimal net = arrondi(ttc.add(remboursement).add(regulation).subtract(nrr));
        BigDecimal r = arrondi(regle);
        return new ColonneCoutDto(l.bordereau().trio(), l.appro().trio(), l.depassements().trio(), l.supplementaires().trio(),
                l.fluctuations().trio(), l.avenants().trio(), l.total().trio(), arrondi(nrr), arrondi(remboursement),
                arrondi(regulation), net, r, net.subtract(r));
    }

    private BigDecimal somme(Somme... sommes) {
        return Arrays.stream(sommes).map(this::s).reduce(ZERO, BigDecimal::add);
    }

    private BigDecimal s(Somme somme) {
        return e.de(somme);
    }

    private Paire paire(Somme ht, Somme ttc) {
        return new Paire(s(ht), s(ttc));
    }

    private record Lignes(Paire bordereau, Paire appro, Paire depassements, Paire supplementaires, Paire fluctuations,
                          Paire avenants, Paire total) {
    }

    private record Paire(BigDecimal ht, BigDecimal ttc) {
        Paire plus(Paire p) {
            return new Paire(ht.add(p.ht), ttc.add(p.ttc));
        }

        Paire moins(Paire p) {
            return new Paire(ht.subtract(p.ht), ttc.subtract(p.ttc));
        }

        Paire fois(BigDecimal r) {
            return new Paire(ht.multiply(r), ttc.multiply(r));
        }

        BigDecimal tva() {
            return ttc.subtract(ht);
        }

        TrioDto trio() {
            return new TrioDto(arrondi(ht), arrondi(tva()), arrondi(ttc));
        }
    }
}
