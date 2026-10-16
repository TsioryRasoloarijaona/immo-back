package com.tsix.immocrm.web.export;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

class LignesExportTest {

    /** Simule la lecture paginée d'une liste de {@code total} lignes et note les pages demandées. */
    private static final class Liste {
        private final List<Integer> lignes;
        private final List<Pageable> demandes = new ArrayList<>();

        private Liste(int total) {
            lignes = IntStream.range(0, total).boxed().toList();
        }

        private Page<Integer> lire(Pageable page) {
            demandes.add(page);
            int debut = (int) Math.min(page.getOffset(), lignes.size());
            int fin = Math.min(debut + page.getPageSize(), lignes.size());
            return new PageImpl<>(lignes.subList(debut, fin), page, lignes.size());
        }
    }

    @Test
    void toutes_litPageApresPage_sousLaLimiteDeParametres() {
        Liste liste = new Liste(4_500);

        List<Integer> lues = LignesExport.toutes(Sort.by(Sort.Direction.DESC, "docDate"), liste::lire);

        assertThat(lues).hasSize(4_500).startsWith(0, 1).endsWith(4_499);
        assertThat(liste.demandes).hasSize(3)
                .allSatisfy(p -> assertThat(p.getPageSize()).isEqualTo(LignesExport.TAILLE_PAGE));
    }

    @Test
    void toutes_ajouteIdCommeDepartage_saufSIlEstDejaTrie() {
        Liste liste = new Liste(1);
        LignesExport.toutes(Sort.by(Sort.Direction.DESC, "docDate"), liste::lire);
        LignesExport.toutes(Sort.by(Sort.Direction.DESC, "id"), liste::lire);

        assertThat(liste.demandes.get(0).getSort())
                .isEqualTo(Sort.by(Sort.Order.desc("docDate"), Sort.Order.asc("id")));
        assertThat(liste.demandes.get(1).getSort()).isEqualTo(Sort.by(Sort.Order.desc("id")));
    }

    @Test
    void toutes_plafonneeALaLimiteDExport() {
        Liste liste = new Liste(LignesExport.LIGNES_EXPORT_MAX + 3_000);

        List<Integer> lues = LignesExport.toutes(Sort.unsorted(), liste::lire);

        assertThat(lues).hasSize(LignesExport.LIGNES_EXPORT_MAX);
    }
}
