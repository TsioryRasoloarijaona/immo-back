package com.tsix.immocrm.config;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Les tables écrites viennent de blocs par domaine : l'ordre suit les rangs, un doublon est refusé au chargement. */
class OrdreBlocsEcrituresTest {

    private static TablesEcrites bloc(int rang, String... tables) {
        return new TablesEcrites() {
            @Override
            public int rang() {
                return rang;
            }

            @Override
            public List<String> tables() {
                return List.of(tables);
            }
        };
    }

    @Test
    void blocsMisBoutABoutParRang() {
        assertThat(CollecteurLignesEcrites.ordre(List.of(bloc(20, "C"), bloc(10, "A", "B"))))
                .containsExactly("A", "B", "C");
    }

    @Test
    void rangPartageRefuse() {
        List<TablesEcrites> blocs = List.of(bloc(10, "A"), bloc(10, "B"));

        assertThatThrownBy(() -> CollecteurLignesEcrites.ordre(blocs)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void tableEnDoubleRefusee() {
        List<TablesEcrites> blocs = List.of(bloc(10, "A"), bloc(20, "A"));

        assertThatThrownBy(() -> CollecteurLignesEcrites.ordre(blocs)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void blocsDuDepotTrouves() {
        assertThat(CollecteurLignesEcrites.ORDRE_CLES_ETRANGERES)
                .startsWith("StkKind")
                .endsWith("AdmMessage", "AdmWebHistorics")
                .doesNotHaveDuplicates();
    }
}
