package com.tsix.immocrm.services.rapport.vente.flux;

import com.tsix.immocrm.domain.rapport.ParametreRapport;
import com.tsix.immocrm.domain.rapport.TypeParametre;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.ObjIntConsumer;

/** Mise en forme commune des rapports de flux : paramètre année, grille de 12 mois, libellés, avertissements. */
public final class LignesMensuelles {

    public static final String ANNEE = "annee";
    public static final String MOIS = "mois";
    public static final String PREVU_NON_SAISI = "Prévu non saisi : les colonnes de prévision valent 0.";

    private LignesMensuelles() {
    }

    /** {@code annee} (ANNEE), défaut = année de l'horloge. */
    public static ParametreRapport annee(Clock clock) {
        return new ParametreRapport(ANNEE, "Année", TypeParametre.ANNEE, false, false,
                String.valueOf(LocalDate.now(clock).getYear()), List.of(), null, null);
    }

    /** « Janvier » … « Décembre ». */
    public static String libelleMois(int mois) {
        String nom = Month.of(mois).getDisplayName(TextStyle.FULL, Locale.FRENCH);
        return Character.toUpperCase(nom.charAt(0)) + nom.substring(1);
    }

    /** Toujours douze lignes, colonne {@link #MOIS} remplie ; {@code remplir} reçoit la ligne et le mois (1–12). */
    public static List<Map<String, Object>> douzeMois(ObjIntConsumer<Map<String, Object>> remplir) {
        List<Map<String, Object>> lignes = new ArrayList<>();
        for (int mois = 1; mois <= 12; mois++) {
            Map<String, Object> ligne = new LinkedHashMap<>();
            ligne.put(MOIS, libelleMois(mois));
            remplir.accept(ligne, mois);
            lignes.add(ligne);
        }
        return lignes;
    }

    /** Avertissements communs (spec §2 a–d) tirés des anomalies du flux. */
    public static List<String> avertissements(FluxMensuel flux) {
        AnomaliesFlux a = flux.anomalies();
        List<String> avertissements = new ArrayList<>();
        if (a.dossiersSansStatut() > 0) {
            avertissements.add("%d dossier(s) sans statut ne sont comptés dans aucune mesure."
                    .formatted(a.dossiersSansStatut()));
        }
        if (a.reglementsSansDate().nombre() > 0) {
            avertissements.add("%d règlement(s) sans date (%s MAD) hors du tableau.".formatted(
                    a.reglementsSansDate().nombre(),
                    a.reglementsSansDate().montant().setScale(2, RoundingMode.HALF_UP).toPlainString()));
        }
        if (a.prevuNonSaisi()) {
            avertissements.add(PREVU_NON_SAISI);
        }
        a.parametresAbsents().forEach(p -> avertissements.add(
                "Paramètre %s absent : les mesures qui en dépendent valent 0.".formatted(p)));
        return avertissements;
    }

    /** {@code part / base × 100}, 2 décimales ; 0 si la base est nulle. */
    public static BigDecimal taux(BigDecimal part, BigDecimal base) {
        return base.signum() == 0 ? BigDecimal.ZERO
                : part.multiply(BigDecimal.valueOf(100)).divide(base, 2, RoundingMode.HALF_UP);
    }
}
