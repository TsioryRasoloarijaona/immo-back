package com.tsix.immocrm.services.rapport.vente.flux;

import com.tsix.immocrm.domain.ColonneDossier;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.services.dossier.liste.MontantsParNatureDossier;
import com.tsix.immocrm.services.rapport.LotsTranches;
import com.tsix.immocrm.services.rapport.vente.flux.LecturesFlux.Dossier;
import com.tsix.immocrm.services.rapport.vente.flux.LecturesFlux.Ligne;
import com.tsix.immocrm.services.rapport.vente.flux.LecturesFlux.Origine;
import com.tsix.immocrm.services.rapport.vente.flux.MesureFlux.Famille;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.tsix.immocrm.services.rapport.vente.flux.MesureFlux.*;

/**
 * Seul calcul des flux mensuels de vente (FEATURE-tdb-vente-rapports-flux.md §3) : concrétisations, chiffre d'affaires,
 * livraisons, sorties, encaissements et frais par tranche × année × mois, recalculés sur les tables (jamais la vue
 * {@code ComSalePrevision}). Ne lit que les tranches de la demande ; le prévu vient de {@link SourcePrevu}.
 */
@Service
@Transactional(readOnly = true)
public class FluxVenteMensuel {

    static final String FRAIS_ANNULATION = "DefaultCancellationDocumentType";
    private static final String FRAIS_SYNDIC = "DefaultSyndicFeeDocumentType";
    private static final Map<MesureFlux, List<String>> CATEGORIES_FRAIS = Map.of(
            ENCAISSEMENTS_FRAIS_DOSSIER, List.of("DefaultFeeDocumentType", "DefaultOtherFeeDocumentType"),
            ENCAISSEMENTS_FRAIS_TRANSFERT, List.of("DefaultCustomerTransfertDocumentType",
                    "DefaultProductTransfertDocumentType"));

    private final LecturesFlux lectures;
    private final SeuilsStatutsVente seuils;
    private final MontantsParNatureDossier montants;
    private final SourcePrevu sourcePrevu;
    private final EntityManager em;

    public FluxVenteMensuel(LecturesFlux lectures, SeuilsStatutsVente seuils, MontantsParNatureDossier montants,
                            SourcePrevu sourcePrevu, EntityManager em) {
        this.lectures = lectures;
        this.seuils = seuils;
        this.montants = montants;
        this.sourcePrevu = sourcePrevu;
        this.em = em;
    }

    /** Tranches vides ⇒ flux vide sans aucune requête. Agrège {@link #mouvements} au mois, plus le prévu. */
    public FluxMensuel calculer(DemandeFlux demande) {
        if (demande.tranches().isEmpty()) {
            return FluxMensuel.vide(AnomaliesFlux.AUCUNE);
        }
        MouvementsFlux mouvements = lignes(demande);
        FluxMensuel.Cumul cumul = new FluxMensuel.Cumul();
        mouvements.lignes().forEach(l -> cumul.ajouter(l.mesure(),
                new CleFlux(l.trancheId(), l.date().getYear(), l.date().getMonthValue()), l.valeur()));
        boolean prevuNonSaisi = false;
        Set<MesureFlux> prevues = demande.mesures().stream().filter(m -> m.famille() == Famille.PREVU)
                .collect(Collectors.toSet());
        if (!prevues.isEmpty()) {
            FluxMensuel prevu = sourcePrevu.prevu(demande);
            cumul.ajouter(prevu, prevues);
            prevuNonSaisi = prevu.anomalies().prevuNonSaisi();
        }
        AnomaliesFlux a = mouvements.anomalies();
        return cumul.flux(new AnomaliesFlux(a.dossiersSansStatut(), a.reglementsSansDate(), a.parametresAbsents(),
                prevuNonSaisi));
    }

    /**
     * Les lignes sources de {@link #calculer}, datées au jour, une par fait retenu (même population, même règle) ; une
     * mesure {@code PREVU} est refusée (le prévu est mensuel).
     */
    public MouvementsFlux mouvements(DemandeFlux demande) {
        if (demande.mesures().stream().anyMatch(m -> m.famille() == Famille.PREVU)) {
            throw new IllegalArgumentException("Mesure prévue : lue par calculer seulement.");
        }
        return lignes(demande);
    }

    private MouvementsFlux lignes(DemandeFlux demande) {
        if (demande.tranches().isEmpty()) {
            return new MouvementsFlux(List.of(), AnomaliesFlux.AUCUNE);
        }
        Calcul calcul = new Calcul(demande, lireSeuils(demande.mesures()));
        if (calcul.demande(Famille.DOSSIER, Famille.REGLEMENT, Famille.ECHEANCE)) {
            dossiers(calcul);
        }
        if (calcul.demande(Famille.REGLEMENT)) {
            reglements(calcul);
        }
        if (demande.mesures().contains(FRAIS_DESISTEMENT_DUS)) {
            fraisDesistement(calcul);
        }
        return new MouvementsFlux(calcul.lignes, new AnomaliesFlux(calcul.sansStatut,
                new AnomaliesFlux.ReglementsSansDate(calcul.sansDate, calcul.montantSansDate),
                calcul.parametresAbsents, false));
    }

    /**
     * Σ {@code ComFolder.DiscountAmount} des dossiers de vente ni annulés ni désistés sur article racine, toutes dates
     * (n° 5, section « remises »).
     */
    public BigDecimal remises(List<UUID> tranches) {
        return lectures.remises(tranches);
    }

    /** Paramètre de seuil d'une mesure « statut », {@code null} si la mesure n'en dépend pas. */
    private static String seuilDe(MesureFlux m) {
        return switch (m) {
            case CONCRETISATIONS, CONCRETISATIONS_NETTES, CONCRETISATIONS_MONTANT, AVANCES_ENCAISSEES ->
                    SeuilsStatutsVente.CONCRETISATION;
            case CA_QUANTITE, CA_TTC, CA_HT -> SeuilsStatutsVente.FINALISATION;
            case LIVRAISONS -> SeuilsStatutsVente.LIVRAISON;
            default -> null;
        };
    }

    private Map<String, Integer> lireSeuils(Set<MesureFlux> mesures) {
        Map<String, Integer> lus = new HashMap<>();
        mesures.stream().map(FluxVenteMensuel::seuilDe).filter(Objects::nonNull).distinct()
                .forEach(p -> seuils.sorting(p).ifPresent(s -> lus.put(p, s)));
        return lus;
    }

    // ------------------------------------------------------------------ dossiers

    private void dossiers(Calcul calcul) {
        List<Dossier> dossiers = lectures.dossiers(calcul.demande.tranches());
        calcul.sansStatut = dossiers.stream().filter(d -> !d.etat().avecStatut()).count();
        Set<MesureFlux> mesures = calcul.mesures(Famille.DOSSIER);
        Map<UUID, BigDecimal> avances = mesures.contains(CONCRETISATIONS_MONTANT)
                ? avancesDues(calcul, dossiers) : Map.of();
        for (Dossier d : dossiers) {
            for (MesureFlux m : mesures) {
                LocalDateTime date = date(m, d);
                if (calcul.retient(date) && population(m, d.etat(), calcul.seuils)) {
                    calcul.ajouter(m, d.tranche(), date, valeur(m, d, avances), d.origine(), null);
                }
            }
        }
    }

    /** Montant dû d'avance par dossier concrétisé : {@code AVANCE} de {@link MontantsParNatureDossier}, pas refait. */
    private Map<UUID, BigDecimal> avancesDues(Calcul calcul, List<Dossier> dossiers) {
        List<UUID> ids = dossiers.stream().filter(d -> calcul.retient(d.avance())
                && population(CONCRETISATIONS_MONTANT, d.etat(), calcul.seuils)).map(Dossier::id).toList();
        List<ComFolder> entites = LotsTranches.lire(ids, lot -> em.createQuery(
                "SELECT f FROM ComFolder f WHERE f.id IN :ids", ComFolder.class).setParameter("ids", lot).getResultList());
        Map<UUID, BigDecimal> avances = new HashMap<>();
        montants.pour(entites).forEach((id, m) -> avances.put(id, m.get(ColonneDossier.AVANCE)));
        return avances;
    }

    private static LocalDateTime date(MesureFlux m, Dossier d) {
        return switch (m) {
            case CONCRETISATIONS, CONCRETISATIONS_NETTES, CONCRETISATIONS_MONTANT -> d.avance();
            case CA_QUANTITE, CA_TTC, CA_HT -> d.acte();
            case LIVRAISONS -> d.livraison();
            case RESERVATIONS -> d.saisie();
            default -> d.sortie();
        };
    }

    // Concrétisations, livraisons et réservations gardent annulés et désistés (la vue, le n° 8) ; nettes et CA les excluent.
    private static boolean population(MesureFlux m, LecturesFlux.Etat etat, Map<String, Integer> seuils) {
        return switch (m) {
            case CONCRETISATIONS, CONCRETISATIONS_MONTANT, LIVRAISONS -> etat.atteint(seuils.get(seuilDe(m)));
            case CONCRETISATIONS_NETTES, CA_QUANTITE, CA_TTC, CA_HT ->
                    etat.atteint(seuils.get(seuilDe(m))) && etat.vivant();
            case DESISTEMENTS, DESISTEMENTS_MONTANT -> Boolean.TRUE.equals(etat.desiste());
            case ANNULATIONS, ANNULATIONS_MONTANT -> Boolean.TRUE.equals(etat.annule());
            case RESERVATIONS -> etat.avecStatut();
            default -> false;
        };
    }

    private static BigDecimal valeur(MesureFlux m, Dossier d, Map<UUID, BigDecimal> avances) {
        return switch (m) {
            case CA_TTC -> d.prix();
            case CA_HT -> d.prixHt();
            case DESISTEMENTS_MONTANT, ANNULATIONS_MONTANT -> d.duResiliation();
            case CONCRETISATIONS_MONTANT -> avances.get(d.id());
            default -> BigDecimal.ONE;
        };
    }

    // ------------------------------------------------------------------ règlements

    private void reglements(Calcul calcul) {
        Set<MesureFlux> mesures = calcul.mesures(Famille.REGLEMENT);
        Map<MesureFlux, Set<UUID>> categories = categoriesFrais(mesures);
        for (Ligne l : lectures.reglements(calcul.demande.tranches())) {
            List<MesureFlux> retenues = mesures.stream().filter(m -> retenue(m, l, calcul.seuils, categories)).toList();
            if (l.mouvement() == null) {
                compterSansDate(calcul, l, retenues);
            } else if (calcul.retient(l.mouvement())) {
                retenues.forEach(m -> calcul.ajouter(m, l.tranche(), l.mouvement(), l.montant(), l.origine(), l.id()));
            }
        }
    }

    /** Ligne retenue par au moins une mesure mais sans aucune date : hors grille, comptée une fois en anomalie. */
    private static void compterSansDate(Calcul calcul, Ligne l, List<MesureFlux> retenues) {
        if (!retenues.isEmpty()) {
            calcul.sansDate++;
            calcul.montantSansDate = calcul.montantSansDate.add(l.montant() == null ? BigDecimal.ZERO : l.montant());
        }
    }

    private static boolean retenue(MesureFlux m, Ligne l, Map<String, Integer> seuils,
                                   Map<MesureFlux, Set<UUID>> categories) {
        return switch (m) {
            case ENCAISSEMENTS -> l.encaissement();
            case ENCAISSEMENTS_VIVANTS -> l.encaissement() && l.etat().vivant();
            case AVANCES_ENCAISSEES -> l.encaissement() && l.avanceAccord() && l.avance() != null
                    && l.etat().atteint(seuils.get(seuilDe(m))) && l.etat().vivant();
            case ENCAISSEMENTS_APRES_ACTE -> l.encaissement() && l.etat().vivant() && l.acte() != null
                    && l.mouvement() != null && !l.mouvement().toLocalDate().isBefore(l.acte().toLocalDate());
            case ENCAISSEMENTS_FRAIS_DOSSIER, ENCAISSEMENTS_FRAIS_TRANSFERT ->
                    l.frais() && categories.getOrDefault(m, Set.of()).contains(l.categorie());
            default -> false;
        };
    }

    /** Catégories des paramètres de frais (valeurs NULL ignorées), sans la catégorie syndic. */
    private Map<MesureFlux, Set<UUID>> categoriesFrais(Set<MesureFlux> mesures) {
        Map<MesureFlux, Set<UUID>> categories = new EnumMap<>(MesureFlux.class);
        Optional<UUID> syndic = seuils.valeur(FRAIS_SYNDIC);
        CATEGORIES_FRAIS.forEach((m, parametres) -> {
            if (mesures.contains(m)) {
                Set<UUID> ids = parametres.stream().map(seuils::valeur).flatMap(Optional::stream)
                        .collect(Collectors.toCollection(HashSet::new));
                syndic.ifPresent(ids::remove);
                categories.put(m, ids);
            }
        });
        return categories;
    }

    // ------------------------------------------------------------------ échéances de frais d'annulation

    private void fraisDesistement(Calcul calcul) {
        seuils.valeur(FRAIS_ANNULATION).ifPresent(categorie -> lectures.echeances(calcul.demande.tranches(), categorie)
                .stream().filter(e -> calcul.retient(e.date()))
                .forEach(e -> calcul.ajouter(FRAIS_DESISTEMENT_DUS, e.tranche(), e.date(), e.montant(), e.origine(),
                        e.id())));
    }

    /** État d'un calcul : demande, seuils lus, lignes retenues et anomalies. */
    private static final class Calcul {

        private final DemandeFlux demande;
        private final Map<String, Integer> seuils;
        private final List<String> parametresAbsents = new ArrayList<>();
        private final List<MouvementFlux> lignes = new ArrayList<>();
        private long sansStatut;
        private long sansDate;
        private BigDecimal montantSansDate = BigDecimal.ZERO;

        Calcul(DemandeFlux demande, Map<String, Integer> seuils) {
            this.demande = demande;
            this.seuils = seuils;
            demande.mesures().stream().map(FluxVenteMensuel::seuilDe).filter(Objects::nonNull).distinct().sorted()
                    .filter(p -> !seuils.containsKey(p)).forEach(parametresAbsents::add);
        }

        Set<MesureFlux> mesures(Famille famille) {
            return demande.mesures().stream().filter(m -> m.famille() == famille).collect(Collectors.toSet());
        }

        boolean demande(Famille... familles) {
            return Stream.of(familles).anyMatch(f -> !mesures(f).isEmpty());
        }

        boolean retient(LocalDateTime date) {
            return date != null && demande.retient(date.getYear());
        }

        void ajouter(MesureFlux m, UUID tranche, LocalDateTime date, BigDecimal valeur, Origine o, UUID ligne) {
            lignes.add(new MouvementFlux(m, tranche, date.toLocalDate(), valeur == null ? BigDecimal.ZERO : valeur,
                    o.dossier(), o.commercial(), o.categorieArticle(), o.sousArticle(), ligne));
        }
    }
}
