package com.tsix.immocrm.domain;

import org.junit.jupiter.api.Test;

import static com.tsix.immocrm.domain.StatutReglement.CANCELED;
import static com.tsix.immocrm.domain.StatutReglement.IN_WORK;
import static com.tsix.immocrm.domain.StatutReglement.POINTED;
import static org.assertj.core.api.Assertions.assertThat;

class StatutReglementTest {

    @Test
    void depuisInWork_onPeutPointerOuAnnuler() {
        assertThat(StatutReglement.transitionsAutorisees(IN_WORK)).containsExactlyInAnyOrder(POINTED, CANCELED);
    }

    @Test
    void pointeEtAnnule_sontTerminaux() {
        assertThat(StatutReglement.transitionsAutorisees(POINTED)).isEmpty();
        assertThat(StatutReglement.transitionsAutorisees(CANCELED)).isEmpty();
        assertThat(StatutReglement.estValide(POINTED, CANCELED)).isFalse();
        assertThat(StatutReglement.estValide(CANCELED, POINTED)).isFalse();
        // Pas de re-pointage : c'est ce qui rend le 409 déterministe côté API.
        assertThat(StatutReglement.estValide(POINTED, POINTED)).isFalse();
    }

    @Test
    void statutInitial_estInWork() {
        // Les 3 218 règlements legacy saisis portent IN_WORK ; l'API doit s'aligner dessus.
        assertThat(StatutReglement.INITIAL).isEqualTo(IN_WORK);
    }

    /**
     * {@code StlSettlement.Status} est du texte libre alimenté par l'application desktop, qui
     * continue d'écrire dans cette base. Une valeur inattendue ne doit jamais faire échouer une
     * lecture — d'où la variante tolérante.
     */
    @Test
    void lectureTolerante_rendNullSurValeurInconnueOuAbsente() {
        assertThat(StatutReglement.depuisCodeOuNull(null)).isNull();
        assertThat(StatutReglement.depuisCodeOuNull("")).isNull();
        assertThat(StatutReglement.depuisCodeOuNull("   ")).isNull();
        // UNPAID/LOST existent dans les filtres legacy documentés mais ne portent aucune ligne
        // réelle : délibérément absents de l'enum, donc inconnus ici.
        assertThat(StatutReglement.depuisCodeOuNull("UNPAID")).isNull();
        assertThat(StatutReglement.depuisCodeOuNull("n'importe quoi")).isNull();
    }

    @Test
    void lectureTolerante_accepteCasseEtEspaces() {
        assertThat(StatutReglement.depuisCodeOuNull("pointed")).isEqualTo(POINTED);
        assertThat(StatutReglement.depuisCodeOuNull(" IN_WORK ")).isEqualTo(IN_WORK);
    }

    /** Côté requête client, en revanche, une valeur inconnue est une erreur — pas une donnée à tolérer. */
    @Test
    void conversionStricte_rejetteUneValeurInconnue() {
        assertThat(StatutReglement.depuisCode("CANCELED")).isEqualTo(CANCELED);
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> StatutReglement.depuisCode("ENCAISSE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ENCAISSE");
    }
}
