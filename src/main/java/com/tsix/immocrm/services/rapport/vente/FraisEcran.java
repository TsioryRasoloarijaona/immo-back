package com.tsix.immocrm.services.rapport.vente;

import com.tsix.immocrm.domain.rapport.ContexteRapport;
import com.tsix.immocrm.domain.rapport.ParametreRapport;
import com.tsix.immocrm.domain.rapport.TypeParametre;
import com.tsix.immocrm.services.rapport.vente.flux.MesureFlux;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static com.tsix.immocrm.services.rapport.vente.flux.MesureFlux.ECHEANCES_DUES;
import static com.tsix.immocrm.services.rapport.vente.flux.MesureFlux.ECHEANCES_DUES_FRAIS;
import static com.tsix.immocrm.services.rapport.vente.flux.MesureFlux.ENCAISSEMENTS;
import static com.tsix.immocrm.services.rapport.vente.flux.MesureFlux.ENCAISSEMENTS_FRAIS;

/** Filtre « Frais » de l'écran (FEATURE-tdb-vente-previsions.md §4.2) : mesures du réel et du schéma de financement. */
enum FraisEcran {

    SANS("Sans frais", List.of(ENCAISSEMENTS), List.of(ECHEANCES_DUES)),
    AVEC("Avec frais", List.of(ENCAISSEMENTS, ENCAISSEMENTS_FRAIS), List.of(ECHEANCES_DUES, ECHEANCES_DUES_FRAIS)),
    SEULS("Seulement les frais", List.of(ENCAISSEMENTS_FRAIS), List.of(ECHEANCES_DUES_FRAIS));

    static final String NOM = "frais";

    private final String libelle;
    private final List<MesureFlux> reel;
    private final List<MesureFlux> schema;

    FraisEcran(String libelle, List<MesureFlux> reel, List<MesureFlux> schema) {
        this.libelle = libelle;
        this.reel = reel;
        this.schema = schema;
    }

    List<MesureFlux> reel() {
        return reel;
    }

    List<MesureFlux> schema() {
        return schema;
    }

    static ParametreRapport parametre() {
        return new ParametreRapport(NOM, "Frais", TypeParametre.CHOIX, false, false, SANS.name(),
                Arrays.stream(values()).map(f -> new ParametreRapport.Option(f.name(), f.libelle)).toList(), null, null);
    }

    /** Toujours présent : le paramètre a un défaut. */
    static FraisEcran de(ContexteRapport contexte) {
        return valueOf(Objects.requireNonNull(contexte.texte(NOM), "Paramètre frais absent"));
    }
}
