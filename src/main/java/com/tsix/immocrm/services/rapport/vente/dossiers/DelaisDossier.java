package com.tsix.immocrm.services.rapport.vente.dossiers;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;

/**
 * Délais du n° 29 (FEATURE-tdb-vente-rapports-dossiers.md §5) : chacun est un couple « date de début → date de fin »
 * d'un dossier, défini une seule fois ; une date absente ou un délai négatif est ignoré (0 est gardé).
 */
public enum DelaisDossier {

    TITRE_ENVOI_NOTAIRE("titreEnvoiNotaire", "Délai entre date Titres Fonciers et date Envoi Notaire",
            Dates::titre, Dates::envoiNotaire),
    MINUTE_REMISE_CLES("minuteRemiseCles", "Délai Minute / Remise de Clés", Dates::minute, Dates::livraison),
    TITRE_REMISE_CLES("titreRemiseCles", "Délai Titre Foncier / Remise de clés", Dates::titre, Dates::livraison),
    ENVOI_MINUTE("envoiMinute", "Délai Envoi Notaire / Minute", Dates::envoiNotaire, Dates::minute),
    MINUTE_FORMALISE("minuteFormalise", "Délai Minute / Formalisé (expédition certificat de propriété)",
            Dates::minute, Dates::formalise),
    ENVOI_CREDIT_EN_COURS("envoiCreditEnCours", "Envoi Notaire / Crédit En cours", Dates::envoiNotaire,
            Dates::creditEnCours),
    CREDIT_EN_COURS_DEBLOQUE("creditEnCoursDebloque", "Crédit En cours / Crédit Débloqué", Dates::creditEnCours,
            Dates::creditDebloque),
    DEBLOQUE_MINUTE("debloqueMinute", "Crédit débloqué / Minute", Dates::creditDebloque, Dates::minute),
    CREDIT_EN_COURS_MINUTE("creditEnCoursMinute", "Crédit En Cours / Minute", Dates::creditEnCours, Dates::minute);

    /** Dates d'un dossier ({@code ComFolder}, titre foncier de l'article {@code StkItem.TitleDate}). */
    public record Dates(UUID notaire, boolean aCredit, LocalDate titre, LocalDate envoiNotaire, LocalDate minute,
                        LocalDate livraison, LocalDate formalise, LocalDate creditEnCours, LocalDate creditDebloque) {
    }

    private final String nom;
    private final String libelle;
    private final Function<Dates, LocalDate> debut;
    private final Function<Dates, LocalDate> fin;

    DelaisDossier(String nom, String libelle, Function<Dates, LocalDate> debut, Function<Dates, LocalDate> fin) {
        this.nom = nom;
        this.libelle = libelle;
        this.debut = debut;
        this.fin = fin;
    }

    public String nom() {
        return nom;
    }

    public String libelle() {
        return libelle;
    }

    /** {@code DATEDIFF(day, début, fin)} ; {@code null} si une date manque ou si le délai est négatif. */
    public Long jours(Dates d) {
        LocalDate a = debut.apply(d);
        LocalDate b = fin.apply(d);
        if (a == null || b == null) {
            return null;
        }
        long jours = ChronoUnit.DAYS.between(a, b);
        return jours < 0 ? null : jours;
    }

    /** Moyenne des délais renseignés ; {@code null} si aucun dossier n'en fournit. */
    public BigDecimal moyenne(Collection<Dates> dossiers) {
        return moyenneDe(dossiers.stream().map(this::jours).filter(Objects::nonNull).toList());
    }

    /** Moyenne à une décimale ; {@code null} pour une liste vide. */
    public static BigDecimal moyenneDe(Collection<? extends Number> valeurs) {
        if (valeurs.isEmpty()) {
            return null;
        }
        BigDecimal somme = valeurs.stream().map(v -> new BigDecimal(v.toString())).reduce(BigDecimal.ZERO,
                BigDecimal::add);
        return somme.divide(BigDecimal.valueOf(valeurs.size()), 1, RoundingMode.HALF_UP);
    }
}
