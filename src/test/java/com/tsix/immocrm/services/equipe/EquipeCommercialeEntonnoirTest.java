package com.tsix.immocrm.services.equipe;

import com.tsix.immocrm.dto.equipe.EtapeEntonnoirDto;
import com.tsix.immocrm.repositories.com.ComProspectionEquipeRepository.LeadCohorte;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** Niveaux de l'entonnoir de cohorte, sans base (FEATURE-equipe-commerciale.md §4.2). */
class EquipeCommercialeEntonnoirTest {

    private static final LocalDateTime ARRIVEE = LocalDateTime.of(2026, 9, 1, 10, 0);

    private record Lead(Integer transformee, String etapeCode, Integer visite, Integer offre,
                        LocalDateTime premierDossier) implements LeadCohorte {
        @Override
        public UUID getId() {
            return UUID.randomUUID();
        }

        @Override
        public LocalDateTime getDateArrivee() {
            return ARRIVEE;
        }

        @Override
        public Integer getTransformee() {
            return transformee;
        }

        @Override
        public String getEtapeCode() {
            return etapeCode;
        }

        @Override
        public Integer getVisite() {
            return visite;
        }

        @Override
        public Integer getOffre() {
            return offre;
        }

        @Override
        public LocalDateTime getPremierDossier() {
            return premierDossier;
        }
    }

    @Test
    void niveau_etapeLateraleResteAPrisEnCharge() {
        assertThat(EntonnoirCohorte.niveau(new Lead(1, "A RAPPELER PAR LE COMMERCIAL", 0, 0, null)))
                .isEqualTo(EntonnoirCohorte.Etape.PRIS_EN_CHARGE);
    }

    @Test
    void niveau_etapeQuatreSansActionVisiteCompteEnVisite() {
        assertThat(EntonnoirCohorte.niveau(new Lead(1, " 4 APRES VISITES", 0, 0, null)))
                .isEqualTo(EntonnoirCohorte.Etape.VISITES);
    }

    @Test
    void niveau_dossierSansOffreCompteDansToutesLesEtapes() {
        assertThat(EntonnoirCohorte.niveau(new Lead(0, null, 0, 0, ARRIVEE.plusDays(3))))
                .isEqualTo(EntonnoirCohorte.Etape.DOSSIERS);
    }

    @Test
    void calculer_entonnoirDecroissantTauxEtMedianeBasse() {
        List<LeadCohorte> leads = List.of(
                new Lead(0, "1 A TRAITER", 0, 0, null),
                new Lead(1, "3 QUALIFIÉ", 0, 0, null),
                new Lead(1, "2 EN COURS DE TRAITEMENT", 1, 1, null),
                new Lead(1, "5 AVANT CONCRETISATION", 0, 1, ARRIVEE.plusDays(10)),
                new Lead(1, "4 APRES VISITES", 1, 0, ARRIVEE.minusDays(2)));

        EntonnoirCohorte.Resultat resultat = EntonnoirCohorte.calculer(leads);

        assertThat(resultat.etapes()).extracting(EtapeEntonnoirDto::nombre).containsExactly(5L, 4L, 4L, 3L, 3L, 2L);
        assertThat(resultat.etapes()).extracting(EtapeEntonnoirDto::tauxPassage)
                .containsExactly(null, new BigDecimal("0.800"), new BigDecimal("1.000"), new BigDecimal("0.750"),
                        new BigDecimal("1.000"), new BigDecimal("0.667"));
        // Délais 10 et 0 (négatif ramené à 0) : médiane basse = 0.
        assertThat(resultat.delaiMedianJours()).isZero();
    }

    @Test
    void calculer_cohorteVide_tauxEtMedianeNuls() {
        EntonnoirCohorte.Resultat resultat = EntonnoirCohorte.calculer(List.of());

        assertThat(resultat.etapes()).hasSize(6).extracting(EtapeEntonnoirDto::tauxPassage).containsOnlyNulls();
        assertThat(resultat.delaiMedianJours()).isNull();
    }
}
