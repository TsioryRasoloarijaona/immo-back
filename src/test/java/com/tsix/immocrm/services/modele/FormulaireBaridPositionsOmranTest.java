package com.tsix.immocrm.services.modele;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Revue BAM : origine des blocs = coin de la page (réponse 6 du 27/10). Le modèle OMRAN place ses zones dans la bande
 * DataCustomers ({@code ClientRectangle 0,0.4,…}) d'une page aux marges 0,5 cm : position sur la feuille = zone + (0,5 ; 0,9).
 */
class FormulaireBaridPositionsOmranTest {

    private static final BigDecimal DECALAGE_X = new BigDecimal("0.5");
    private static final BigDecimal DECALAGE_Y = new BigDecimal("0.9");

    @Test
    void blocsSemes_tombentAuxPositionsAbsoluesDuFormulaireOmran() {
        List<Element> blocs = Jsoup.parseBodyFragment(
                        InitialiseurModeles.contenu(InitialiseurModeles.RESSOURCE_FORMULAIRE_BARID))
                .select("div[data-bloc]");
        // Zones du §2 (relatives à la bande), dans l'ordre du modèle semé.
        String[][] bande = {{"0.2", "10.8"}, {"0.2", "13.4"}, {"9", "10.8"}, {"8.6", "13.4"}, {"5.2", "21.8"}};

        assertThat(blocs).hasSize(bande.length);
        for (int i = 0; i < bande.length; i++) {
            BigDecimal x = new BigDecimal(blocs.get(i).attr("data-x"));
            BigDecimal y = new BigDecimal(blocs.get(i).attr("data-y"));
            assertThat(x).as("data-x du bloc %d", i)
                    .isEqualByComparingTo(new BigDecimal(bande[i][0]).add(DECALAGE_X));
            assertThat(y).as("data-y du bloc %d", i)
                    .isEqualByComparingTo(new BigDecimal(bande[i][1]).add(DECALAGE_Y));
        }
    }
}
