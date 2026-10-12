package com.tsix.immocrm.services.equipe;

import com.tsix.immocrm.dto.equipe.EtapeEntonnoirDto;
import com.tsix.immocrm.repositories.com.ComProspectionEquipeRepository.LeadCohorte;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Niveau atteint par chaque lead et comptes cumulés : règles de FEATURE-equipe-commerciale.md §4. */
final class EntonnoirCohorte {

    enum Etape {
        ARRIVES("Pré-prospects arrivés"),
        PRIS_EN_CHARGE("Pris en charge"),
        QUALIFIES("Qualifiés"),
        VISITES("Visites"),
        OFFRES("Offres"),
        DOSSIERS("Dossiers");

        private final String libelle;

        Etape(String libelle) {
            this.libelle = libelle;
        }
    }

    record Resultat(List<EtapeEntonnoirDto> etapes, Long delaiMedianJours) {
    }

    private EntonnoirCohorte() {
    }

    static Resultat calculer(List<LeadCohorte> leads) {
        long[] auMoins = new long[Etape.values().length];
        List<Long> delais = new ArrayList<>();
        for (LeadCohorte lead : leads) {
            Etape niveau = niveau(lead);
            for (int i = 0; i <= niveau.ordinal(); i++) {
                auMoins[i]++;
            }
            if (niveau == Etape.DOSSIERS) {
                delais.add(Math.max(0L, ChronoUnit.DAYS.between(
                        lead.getDateArrivee().toLocalDate(), lead.getPremierDossier().toLocalDate())));
            }
        }
        List<EtapeEntonnoirDto> etapes = new ArrayList<>();
        for (Etape etape : Etape.values()) {
            BigDecimal taux = etape.ordinal() == 0 ? null : ratio(auMoins[etape.ordinal()], auMoins[etape.ordinal() - 1]);
            etapes.add(new EtapeEntonnoirDto(etape.name(), etape.libelle, auMoins[etape.ordinal()], taux));
        }
        return new Resultat(etapes, mediane(delais));
    }

    /** L'étape la plus avancée atteinte : chaque test part de la fin, l'entonnoir est décroissant par construction. */
    static Etape niveau(LeadCohorte lead) {
        if (lead.getPremierDossier() != null) {
            return Etape.DOSSIERS;
        }
        if (vrai(lead.getOffre())) {
            return Etape.OFFRES;
        }
        char etape = premierCaractere(lead.getEtapeCode());
        if (vrai(lead.getVisite()) || etape == '4' || etape == '5') {
            return Etape.VISITES;
        }
        if (etape == '3') {
            return Etape.QUALIFIES;
        }
        return vrai(lead.getTransformee()) ? Etape.PRIS_EN_CHARGE : Etape.ARRIVES;
    }

    private static boolean vrai(Integer drapeau) {
        return drapeau != null && drapeau == 1;
    }

    // « 3 QUALIFIÉ », « 4 APRES VISITES »… : le rang est le chiffre de tête ; les étapes latérales n'en ont pas.
    private static char premierCaractere(String code) {
        String nettoye = code == null ? "" : code.strip();
        return nettoye.isEmpty() ? ' ' : nettoye.charAt(0);
    }

    private static BigDecimal ratio(long numerateur, long denominateur) {
        if (denominateur <= 0) {
            return null;
        }
        return BigDecimal.valueOf(numerateur).divide(BigDecimal.valueOf(denominateur), 3, RoundingMode.HALF_UP);
    }

    /** Valeur centrale basse, comme PerformanceCommercialService. */
    private static Long mediane(List<Long> valeurs) {
        if (valeurs.isEmpty()) {
            return null;
        }
        List<Long> triees = new ArrayList<>(valeurs);
        Collections.sort(triees);
        return triees.get((triees.size() - 1) / 2);
    }
}
