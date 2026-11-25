package com.tsix.immocrm.services.projet;

import com.tsix.immocrm.dto.projet.BordereauSaisieDto;
import com.tsix.immocrm.dto.projet.LigneBordereauDto;
import com.tsix.immocrm.dto.projet.LigneSaisieDto;
import com.tsix.immocrm.services.patrimoine.importation.LectureClasseur.LigneLue;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** Revue LC2 (2e tour) : appariement R2 quand la cellule Code est numérique, conversion R4. */
class FusionBordereauRevueTest {

    private static final UUID CATEGORIE = UUID.fromString("00000000-0000-0000-0000-0000000000c1");
    private static final UUID LIGNE = UUID.fromString("00000000-0000-0000-0000-0000000000a1");

    private static LigneBordereauDto existante(String code) {
        return new LigneBordereauDto(LIGNE, 1, null, code, "Béton", "m3", BigDecimal.ONE, BigDecimal.TEN,
                new BigDecimal("20"), BigDecimal.TEN, new BigDecimal("12"), null, null, BigDecimal.ZERO, CATEGORIE, null, false);
    }

    // LectureClasseur rend une cellule numérique « 12 » en BigDecimal.valueOf(12.0) ; 3 900 des 4 132 codes de ligne en base sont entiers.
    @Test
    void codeSaisiEnNombreApparieLaLigneExistante() {
        LigneLue lue = new LigneLue(2, Map.of("Code", BigDecimal.valueOf(12.0), "Quantité", BigDecimal.valueOf(3.0),
                "P.U. HT", BigDecimal.valueOf(10.0)));

        BordereauSaisieDto corps = new FusionBordereau(List.of(), List.of(existante("12")), CATEGORIE).fusionner(List.of(lue));

        assertThat(corps.lignes()).extracting(LigneSaisieDto::id).containsExactly(LIGNE);
        assertThat(corps.groupes()).isEmpty();
    }

    @Test
    void puTtcConvertiEnHtAvecTvaEtFodecEnPourcentage() {
        LigneLue lue = new LigneLue(2, Map.of("Code", "12", "Quantité", BigDecimal.ONE, "P.U. TTC", new BigDecimal("121.2"),
                "TVA", new BigDecimal("20"), "FODEC", BigDecimal.ONE));

        BordereauSaisieDto corps = new FusionBordereau(List.of(), List.of(existante("12")), CATEGORIE).fusionner(List.of(lue));

        assertThat(corps.lignes()).singleElement().extracting(LigneSaisieDto::prixUnitaireHT)
                .isEqualTo(new BigDecimal("100.000000"));
    }
}
