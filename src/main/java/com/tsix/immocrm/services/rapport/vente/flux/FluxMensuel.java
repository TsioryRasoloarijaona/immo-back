package com.tsix.immocrm.services.rapport.vente.flux;

import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * Sortie immuable de {@link FluxVenteMensuel} : grain tranche × année × mois, creux (une cellule existe ssi au moins une
 * ligne source l'alimente) ; toute cellule absente vaut {@code ZERO}.
 */
public final class FluxMensuel {

    private final Map<MesureFlux, Map<CleFlux, BigDecimal>> valeurs;
    private final AnomaliesFlux anomalies;

    private FluxMensuel(Map<MesureFlux, Map<CleFlux, BigDecimal>> valeurs, AnomaliesFlux anomalies) {
        Map<MesureFlux, Map<CleFlux, BigDecimal>> copie = new EnumMap<>(MesureFlux.class);
        valeurs.forEach((m, cellules) -> copie.put(m, Map.copyOf(cellules)));
        this.valeurs = copie;
        this.anomalies = anomalies;
    }

    public static FluxMensuel vide(AnomaliesFlux anomalies) {
        return new FluxMensuel(Map.of(), anomalies);
    }

    public BigDecimal valeur(MesureFlux m, UUID tranche, int annee, int mois) {
        return cellules(m).getOrDefault(new CleFlux(tranche, annee, mois), BigDecimal.ZERO);
    }

    /** Somme des tranches. */
    public BigDecimal mois(MesureFlux m, int annee, int mois) {
        return somme(m, c -> c.annee() == annee && c.mois() == mois);
    }

    public BigDecimal annee(MesureFlux m, int annee) {
        return somme(m, c -> c.annee() == annee);
    }

    public BigDecimal tranche(MesureFlux m, UUID tranche, int annee) {
        return somme(m, c -> c.annee() == annee && c.trancheId().equals(tranche));
    }

    /** Toutes les années demandées. */
    public BigDecimal tranche(MesureFlux m, UUID tranche) {
        return somme(m, c -> c.trancheId().equals(tranche));
    }

    public BigDecimal total(MesureFlux m) {
        return somme(m, c -> true);
    }

    /** Cellules ayant au moins une ligne source, toutes mesures confondues. */
    public Set<CleFlux> cles() {
        Set<CleFlux> cles = new HashSet<>();
        valeurs.values().forEach(cellules -> cles.addAll(cellules.keySet()));
        return Set.copyOf(cles);
    }

    /** Cellules ayant au moins une ligne source pour la mesure {@code m}. */
    public Set<CleFlux> cles(MesureFlux m) {
        return cellules(m).keySet();
    }

    public AnomaliesFlux anomalies() {
        return anomalies;
    }

    private Map<CleFlux, BigDecimal> cellules(MesureFlux m) {
        return valeurs.getOrDefault(m, Map.of());
    }

    private BigDecimal somme(MesureFlux m, Predicate<CleFlux> retenue) {
        return cellules(m).entrySet().stream().filter(e -> retenue.test(e.getKey())).map(Map.Entry::getValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** Accumulateur d'un calcul (aussi pour une {@link SourcePrevu}) : une ligne source crée sa cellule, même à 0. */
    public static final class Cumul {

        private final Map<MesureFlux, Map<CleFlux, BigDecimal>> valeurs = new EnumMap<>(MesureFlux.class);

        public void ajouter(MesureFlux m, CleFlux cle, BigDecimal valeur) {
            valeurs.computeIfAbsent(m, k -> new HashMap<>())
                    .merge(cle, valeur == null ? BigDecimal.ZERO : valeur, BigDecimal::add);
        }

        /** Reprend les cellules de {@code autre} pour les mesures {@code mesures}. */
        public void ajouter(FluxMensuel autre, Set<MesureFlux> mesures) {
            mesures.forEach(m -> autre.cellules(m).forEach((cle, v) -> ajouter(m, cle, v)));
        }

        public FluxMensuel flux(AnomaliesFlux anomalies) {
            return new FluxMensuel(valeurs, anomalies);
        }
    }
}
