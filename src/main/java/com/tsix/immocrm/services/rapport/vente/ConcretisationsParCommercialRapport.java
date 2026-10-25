package com.tsix.immocrm.services.rapport.vente;

import com.tsix.immocrm.domain.rapport.ColonneRapport;
import com.tsix.immocrm.domain.rapport.ContexteRapport;
import com.tsix.immocrm.domain.rapport.GraphiqueRapport;
import com.tsix.immocrm.domain.rapport.ResultatRapport;
import com.tsix.immocrm.domain.rapport.SectionRapport;
import com.tsix.immocrm.domain.rapport.TypeGraphique;
import com.tsix.immocrm.services.rapport.vente.flux.DemandeFlux;
import com.tsix.immocrm.services.rapport.vente.flux.FluxVenteMensuel;
import com.tsix.immocrm.services.rapport.vente.flux.LignesMensuelles;
import com.tsix.immocrm.services.rapport.vente.flux.MesureFlux;
import com.tsix.immocrm.services.rapport.vente.flux.MouvementFlux;
import com.tsix.immocrm.services.rapport.vente.flux.MouvementsFlux;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalField;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;
import java.util.stream.IntStream;

import static com.tsix.immocrm.services.rapport.vente.flux.LignesMensuelles.ANNEE;
import static com.tsix.immocrm.services.rapport.vente.flux.LignesMensuelles.MOIS;

/**
 * N° 21 CONCRÉTISATIONS PAR PÉRIODES (FEATURE-tdb-vente-rapports-rythme.md §4.4), modèle {@code ComTdbConcretPerCommercial} :
 * concrétisations nettes par commercial visible, par semaine et par mois de {@code annee}, puis sur cinq ans.
 */
@Component
public class ConcretisationsParCommercialRapport extends RapportFluxVente {

    static final String CODE = "ComTdbConcretPerCommercial";
    private static final String TOTAL = "total";
    private static final String NON_AFFECTE = "nonAffecte";
    private static final String TITRE = "Concrétisations par commercial — suivi ";
    private static final int RECUL = 4;
    // Semaine de SQL Server sous DATEFIRST 1 : la semaine du 1er janvier est S01, coupée chaque lundi.
    private static final TemporalField SEMAINE = WeekFields.of(DayOfWeek.MONDAY, 1).weekOfYear();

    /** Une section : lignes {@code valeurs}, rang de la ligne d'une date (−1 = hors section). */
    private record Periode(String id, String titre, String colonne, String libelle, List<String> valeurs,
                           ToIntFunction<LocalDate> rang) {
    }

    private final PorteeCommerciauxRapport portee;
    private final TransformationCommerciale transformation;

    public ConcretisationsParCommercialRapport(FluxVenteMensuel flux, Clock clock, PorteeCommerciauxRapport portee,
                                               TransformationCommerciale transformation) {
        super(CODE, flux, clock);
        this.portee = portee;
        this.transformation = transformation;
    }

    @Override
    public ResultatRapport executer(ContexteRapport contexte) {
        int annee = annee(contexte);
        Predicate<UUID> retenu = portee.retenus(contexte);
        MouvementsFlux mouvements = flux.mouvements(new DemandeFlux(contexte.tranchesVisibles(), annee - RECUL, annee,
                Set.of(MesureFlux.CONCRETISATIONS_NETTES)));
        List<MouvementFlux> lignes = mouvements.lignes().stream().filter(l -> retenu.test(l.commercialId())).toList();
        Map<UUID, String> noms = transformation.commerciaux(lignes.stream().map(MouvementFlux::commercialId)
                .filter(Objects::nonNull).distinct().toList());
        return new ResultatRapport(periodes(annee).stream().map(p -> section(p, lignes, noms)).toList(),
                LignesMensuelles.avertissements(mouvements.anomalies()));
    }

    private static List<Periode> periodes(int annee) {
        int semaines = LocalDate.of(annee, 12, 31).get(SEMAINE);
        return List.of(
                new Periode("par-semaine", TITRE + "par semaine " + annee, "semaine", "Semaine",
                        IntStream.rangeClosed(1, semaines).mapToObj("S%02d"::formatted).toList(),
                        d -> d.getYear() == annee ? d.get(SEMAINE) - 1 : -1),
                new Periode("par-mois", TITRE + "par mois " + annee, MOIS, "Mois",
                        IntStream.rangeClosed(1, 12).mapToObj(LignesMensuelles::libelleMois).toList(),
                        d -> d.getYear() == annee ? d.getMonthValue() - 1 : -1),
                new Periode("par-annee", TITRE + "sur cinq ans", ANNEE, "Année",
                        IntStream.rangeClosed(annee - RECUL, annee).mapToObj(String::valueOf).toList(),
                        d -> d.getYear() - (annee - RECUL)));
    }

    private static SectionRapport section(Periode p, List<MouvementFlux> mouvements, Map<UUID, String> noms) {
        List<MouvementFlux> dans = mouvements.stream().filter(l -> p.rang().applyAsInt(l.date()) >= 0).toList();
        List<UUID> commerciaux = dans.stream().map(MouvementFlux::commercialId).filter(Objects::nonNull).distinct()
                .sorted(Comparator.comparing((UUID id) -> noms.getOrDefault(id, "")).thenComparing(UUID::toString))
                .toList();
        Map<UUID, String> colonneDe = new HashMap<>();
        List<ColonneRapport> colonnes = new ArrayList<>(List.of(ColonneRapport.texte(p.colonne(), p.libelle())));
        for (UUID id : commerciaux) {
            String nom = "commercial%02d".formatted(colonneDe.size() + 1);
            colonneDe.put(id, nom);
            colonnes.add(ColonneRapport.effectif(nom, noms.getOrDefault(id, id.toString()), null));
        }
        boolean nonAffecte = dans.stream().anyMatch(l -> l.commercialId() == null);
        if (nonAffecte) {
            colonnes.add(ColonneRapport.effectif(NON_AFFECTE, TransformationCommerciale.NON_AFFECTE, null));
        }
        colonnes.add(ColonneRapport.effectif(TOTAL, "Total", null));
        List<Map<String, Object>> lignes = p.valeurs().stream().map(v -> {
            Map<String, Object> ligne = new LinkedHashMap<>();
            colonnes.forEach(c -> ligne.put(c.nom(), BigDecimal.ZERO));
            ligne.put(p.colonne(), v);
            return ligne;
        }).toList();
        for (MouvementFlux l : dans) {
            Map<String, Object> ligne = lignes.get(p.rang().applyAsInt(l.date()));
            ligne.merge(l.commercialId() == null ? NON_AFFECTE : colonneDe.get(l.commercialId()), l.valeur(),
                    ConcretisationsParCommercialRapport::plus);
            ligne.merge(TOTAL, l.valeur(), ConcretisationsParCommercialRapport::plus);
        }
        return new SectionRapport(p.id(), p.titre(), colonnes, lignes, null, null,
                List.of(new GraphiqueRapport(TypeGraphique.COLONNES, p.titre(), p.colonne(), List.of(TOTAL))));
    }

    private static Object plus(Object cumul, Object valeur) {
        return ((BigDecimal) cumul).add((BigDecimal) valeur);
    }
}
