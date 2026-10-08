package com.tsix.immocrm.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TypeActionRecenteTest {

    @Test
    void chaquePrefixeDonneSonCode() {
        assertThat(TypeActionRecente.depuisHistoriqueProspection("Pré-prospection créée : Karim"))
                .isEqualTo(TypeActionRecente.PRE_PROSPECTION_CREEE);
        assertThat(TypeActionRecente.depuisHistoriqueProspection("Prospect créé et lié : Jean Rakoto"))
                .isEqualTo(TypeActionRecente.PROSPECT_CREE);
        assertThat(TypeActionRecente.depuisHistoriqueProspection("Prospect existant rattaché : Jean"))
                .isEqualTo(TypeActionRecente.PROSPECT_RATTACHE);
        assertThat(TypeActionRecente.depuisHistoriqueProspection("Transformée en prospection : commercial assigné, X"))
                .isEqualTo(TypeActionRecente.TRANSFORMEE_EN_PROSPECTION);
        assertThat(TypeActionRecente.depuisHistoriqueProspection("Prospection créée : prospect X"))
                .isEqualTo(TypeActionRecente.PROSPECTION_CREEE);
        assertThat(TypeActionRecente.depuisHistoriqueProspection("Étape changée : — → 1 APPEL"))
                .isEqualTo(TypeActionRecente.ETAPE_CHANGEE);
        assertThat(TypeActionRecente.depuisHistoriqueProspection("Archivée : Ne répond plus"))
                .isEqualTo(TypeActionRecente.ARCHIVEE);
        assertThat(TypeActionRecente.depuisHistoriqueProspection("Désarchivée"))
                .isEqualTo(TypeActionRecente.DESARCHIVEE);
    }

    @Test
    void prefixeInconnuOuAbsent_donneAutre() {
        assertThat(TypeActionRecente.depuisHistoriqueProspection("Saisie OMRAN quelconque"))
                .isEqualTo(TypeActionRecente.AUTRE);
        assertThat(TypeActionRecente.depuisHistoriqueProspection(null)).isEqualTo(TypeActionRecente.AUTRE);
        assertThat(TypeActionRecente.depuisHistoriqueProspection("")).isEqualTo(TypeActionRecente.AUTRE);
    }

    @Test
    void lEcritureEtLaLectureUtilisentLeMemePrefixe() {
        for (TypeActionRecente type : TypeActionRecente.values()) {
            if (type.prefixe() != null) {
                assertThat(TypeActionRecente.depuisHistoriqueProspection(type.description("suite")))
                        .isEqualTo(type);
            }
        }
    }
}
