package com.tsix.immocrm.services.rapport.vente;

import com.tsix.immocrm.domain.InteractionKpi;
import com.tsix.immocrm.domain.rapport.ColonneRapport;
import com.tsix.immocrm.domain.rapport.ContexteRapport;
import com.tsix.immocrm.domain.rapport.FonctionTotal;
import com.tsix.immocrm.domain.rapport.ParametreRapport;
import com.tsix.immocrm.domain.rapport.ResultatRapport;
import com.tsix.immocrm.domain.rapport.SectionRapport;
import com.tsix.immocrm.domain.rapport.TypeColonne;
import com.tsix.immocrm.entities.com.ComProspection;
import com.tsix.immocrm.services.prospection.DiscriminantPreProspection;
import com.tsix.immocrm.services.rapport.LotsTranches;
import com.tsix.immocrm.services.rapport.stock.MouvementStock;
import com.tsix.immocrm.services.rapport.stock.MouvementsStockTranche;
import com.tsix.immocrm.services.rapport.vente.flux.DemandeFlux;
import com.tsix.immocrm.services.rapport.vente.flux.FluxMensuel;
import com.tsix.immocrm.services.rapport.vente.flux.FluxVenteMensuel;
import com.tsix.immocrm.services.rapport.vente.flux.LignesMensuelles;
import com.tsix.immocrm.services.rapport.vente.flux.MesureFlux;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Root;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.ToLongFunction;
import java.util.stream.Collectors;

/**
 * N° 4 SUIVI HEBDOMADAIRE DES PRÉVISIONS, modèle {@code ComTdbSuiviHebdoPrevision} (FEATURE-tdb-vente-previsions.md §6) :
 * une ligne par tranche en vente visible ; stock lu dans {@link MouvementsStockTranche}, versements dans
 * {@link FluxVenteMensuel#encaissements}. Semaine = [J ; J+6], mois = mois civil de J.
 */
@Component
public class SuiviHebdoPrevisionsRapport extends RapportFluxVente {

    static final String CODE = "ComTdbSuiviHebdoPrevision";
    private static final String DATE = "date";
    private static final String TAUX_SEMAINE = "tauxSemaine";
    private static final String TAUX_MOIS = "tauxMois";
    private static final String REALISATION = "Réalisation";
    private static final String ECART = "Écart";
    private static final String VISITES = "Nombre de visites";
    private static final DateTimeFormatter JJ_MM_AAAA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final BigDecimal SEMAINES_PAR_MOIS = BigDecimal.valueOf(4);

    private final MouvementsStockTranche stock;
    private final EntityManager em;

    public SuiviHebdoPrevisionsRapport(FluxVenteMensuel flux, Clock clock, MouvementsStockTranche stock,
                                       EntityManager em) {
        super(CODE, flux, clock);
        this.stock = stock;
        this.em = em;
    }

    @Override
    public List<ParametreRapport> parametres() {
        LocalDate lundi = LocalDate.now(clock).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        return List.of(ParametreRapport.date(DATE, "Semaine du", lundi), ParametreRapport.tranches());
    }

    /** Jour J, fin de semaine et bornes du mois de J, fins exclues. */
    private record Periodes(LocalDate jour, LocalDate finSemaine, LocalDate debutMois, LocalDate finMois) {

        static Periodes de(LocalDate jour) {
            LocalDate debutMois = jour.withDayOfMonth(1);
            return new Periodes(jour, jour.plusDays(7), debutMois, debutMois.plusMonths(1));
        }

        LocalDate finVisites() {
            return finSemaine.isAfter(finMois) ? finSemaine : finMois;
        }
    }

    /** Chiffres d'une tranche ; réalisation = stock au début de la période − stock au lendemain de sa fin. */
    private record LigneSuivi(TrancheEnVente tranche, long stock, long cumulLancement, BigDecimal prevuMois,
                              long realisationSemaine, long realisationMois, long visitesSemaine, long visitesMois,
                              BigDecimal versementsSemaine, BigDecimal versementsMois) {

        Map<String, Object> versLigne() {
            BigDecimal prevuSemaine = prevuMois.divide(SEMAINES_PAR_MOIS, 2, RoundingMode.HALF_UP);
            return ligne(PROGRAMME, tranche.libelleProjet(), TRANCHE, tranche.libelle(), "stock", stock,
                    "cumulLancement", cumulLancement, "prevuSemaine", prevuSemaine,
                    "realisationSemaine", realisationSemaine,
                    "ecartSemaine", BigDecimal.valueOf(realisationSemaine).subtract(prevuSemaine),
                    "visitesSemaine", visitesSemaine, "versementsSemaine", versementsSemaine, "prevuMois", prevuMois,
                    "realisationMois", realisationMois,
                    "ecartMois", BigDecimal.valueOf(realisationMois).subtract(prevuMois), "visitesMois", visitesMois,
                    "versementsMois", versementsMois, TAUX_SEMAINE, taux(realisationSemaine, visitesSemaine),
                    TAUX_MOIS, taux(realisationMois, visitesMois));
        }
    }

    @Override
    public ResultatRapport executer(ContexteRapport contexte) {
        Periodes p = Periodes.de(Objects.requireNonNull(contexte.date(DATE), "Paramètre date absent"));
        List<TrancheEnVente> tranches = tranchesEnVente(em, contexte.tranchesVisibles());
        List<UUID> ids = tranches.stream().map(TrancheEnVente::id).toList();
        int annee = p.jour().getYear();
        FluxMensuel prevus = flux.calculer(new DemandeFlux(ids, annee, annee,
                Set.of(MesureFlux.PREVU_CONCRETISATION_QUANTITE)));
        List<MouvementStock> mouvements = stock.lire(ids);
        Map<UUID, BigDecimal> versementsSemaine = flux.encaissements(ids, p.jour(), p.finSemaine().minusDays(1));
        Map<UUID, BigDecimal> versementsMois = flux.encaissements(ids, p.debutMois(), p.finMois().minusDays(1));
        Map<UUID, List<LocalDate>> visites = lireVisites(ids, p.debutMois(), p.finVisites());
        List<LigneSuivi> lignes = tranches.stream().map(t -> {
            UUID id = t.id();
            long stockJ = stock.stockAvant(mouvements, id, p.jour());
            List<LocalDate> jours = visites.getOrDefault(id, List.of());
            return new LigneSuivi(t, stockJ, lancesAvant(mouvements, id, p.jour()) - stockJ,
                    prevus.valeur(MesureFlux.PREVU_CONCRETISATION_QUANTITE, id, annee, p.jour().getMonthValue()),
                    stockJ - stock.stockAvant(mouvements, id, p.finSemaine()),
                    stock.stockAvant(mouvements, id, p.debutMois()) - stock.stockAvant(mouvements, id, p.finMois()),
                    compter(jours, p.jour(), p.finSemaine()), compter(jours, p.debutMois(), p.finMois()),
                    versementsSemaine.getOrDefault(id, BigDecimal.ZERO), versementsMois.getOrDefault(id, BigDecimal.ZERO));
        }).toList();
        List<String> avertissements = new ArrayList<>(stock.avertissements());
        avertissements.addAll(LignesMensuelles.avertissements(prevus));
        long sansLancement = tranches.stream().filter(t -> !t.lancee()).count();
        if (sansLancement > 0) {
            avertissements.add("%d tranche(s) sans date de commercialisation : stock négatif".formatted(sansLancement));
        }
        return new ResultatRapport(List.of(section(p, lignes)), avertissements);
    }

    private static SectionRapport section(Periodes p, List<LigneSuivi> lignes) {
        String semaine = "Analyse de la semaine du %s au %s".formatted(p.jour().format(JJ_MM_AAAA),
                p.finSemaine().minusDays(1).format(JJ_MM_AAAA));
        String mois = "Analyse mois %s %d".formatted(LignesMensuelles.libelleMois(p.jour().getMonthValue()),
                p.jour().getYear());
        String groupeTaux = "% réalisation";
        List<ColonneRapport> colonnes = new ArrayList<>(colonnesTranche());
        colonnes.addAll(List.of(ColonneRapport.effectif("stock", "Stock", "Stock au " + p.jour().format(JJ_MM_AAAA)),
                ColonneRapport.effectif("cumulLancement", "Cumul (lancement projet)", semaine),
                decimal("prevuSemaine", "Prévision de la semaine", semaine),
                ColonneRapport.effectif("realisationSemaine", REALISATION, semaine),
                decimal("ecartSemaine", ECART, semaine), ColonneRapport.effectif("visitesSemaine", VISITES, semaine),
                ColonneRapport.montant("versementsSemaine", "Total versements de la semaine", semaine),
                decimal("prevuMois", "Prévision du mois", mois),
                ColonneRapport.effectif("realisationMois", REALISATION, mois),
                decimal("ecartMois", ECART, mois), ColonneRapport.effectif("visitesMois", VISITES, mois),
                ColonneRapport.montant("versementsMois", "Total versements du mois", mois),
                ColonneRapport.pourcentage(TAUX_SEMAINE, "Semaine", groupeTaux),
                ColonneRapport.pourcentage(TAUX_MOIS, "Mois", groupeTaux)));
        // Taux du total imposés (Σ réalisations ÷ Σ visites) ; le socle ne calcule aucun sous-total de taux par programme.
        Map<String, Object> totaux = ligne(TAUX_SEMAINE, taux(somme(lignes, LigneSuivi::realisationSemaine),
                somme(lignes, LigneSuivi::visitesSemaine)), TAUX_MOIS, taux(somme(lignes, LigneSuivi::realisationMois),
                somme(lignes, LigneSuivi::visitesMois)));
        return new SectionRapport("suivi", "Suivi hebdomadaire des prévisions", colonnes,
                lignes.stream().map(LigneSuivi::versLigne).toList(), PROGRAMME, totaux, null);
    }

    private static ColonneRapport decimal(String nom, String libelle, String groupe) {
        return new ColonneRapport(nom, libelle, TypeColonne.NOMBRE, groupe, FonctionTotal.SOMME, true, 2);
    }

    private static BigDecimal taux(long realisation, long visites) {
        return TableauPrevisionsVente.taux(BigDecimal.valueOf(realisation), BigDecimal.valueOf(visites));
    }

    private static long somme(List<LigneSuivi> lignes, ToLongFunction<LigneSuivi> valeur) {
        return lignes.stream().mapToLong(valeur).sum();
    }

    /** Articles lancés avant J : le cumul « lancement projet » = lancés − stock = sorties nettes avant J. */
    private static long lancesAvant(List<MouvementStock> mouvements, UUID tranche, LocalDate jour) {
        return mouvements.stream().filter(m -> m.lancement() && tranche.equals(m.tranche()) && m.date().isBefore(jour))
                .mapToLong(MouvementStock::quantite).sum();
    }

    private static long compter(List<LocalDate> jours, LocalDate debut, LocalDate finExclue) {
        return jours.stream().filter(j -> !j.isBefore(debut) && j.isBefore(finExclue)).count();
    }

    /** Jours des visites (type par code, pré-prospections exclues) des tranches {@code ids} dans [debut ; fin[. */
    private Map<UUID, List<LocalDate>> lireVisites(List<UUID> ids, LocalDate debut, LocalDate fin) {
        return LotsTranches.lire(ids, lot -> {
            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> q = cb.createQuery(Object[].class);
            Root<ComProspection> p = q.from(ComProspection.class);
            Path<UUID> tranche = p.get("stkHierarchy").get("id");
            Path<LocalDateTime> date = p.get("docDate");
            q.multiselect(tranche, date).where(tranche.in(lot),
                    cb.equal(p.join("comActionMessageType").get("code"), InteractionKpi.TYPE_VISITE),
                    cb.greaterThanOrEqualTo(date, debut.atStartOfDay()), cb.lessThan(date, fin.atStartOfDay()),
                    DiscriminantPreProspection.nEstPasPreProspection(p, q, cb));
            return em.createQuery(q).getResultList();
        }).stream().collect(Collectors.groupingBy(r -> (UUID) r[0],
                Collectors.mapping(r -> ((LocalDateTime) r[1]).toLocalDate(), Collectors.toList())));
    }
}
