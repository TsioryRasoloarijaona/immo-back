package com.tsix.immocrm.utils;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Primitives de manipulation d'un numéro. Test unitaire pur — aucun contexte Spring, aucune
 * base : c'est de l'arithmétique de chaîne.
 *
 * <p>Chaque cas correspond à une forme réellement présente dans {@code yesRemsProd}, ou à une
 * saisie que le formulaire peut désormais produire.
 */
class NumeroTelephoneTest {

    @Test
    void formatDeStockage_neufChiffres_estEspaceCommeLaBase() {
        // La forme de 14 285 des 14 458 fiches (98,8 %).
        assertThat(NumeroTelephone.versFormatDeStockage("698298095")).isEqualTo("6 98 29 80 95");
        assertThat(NumeroTelephone.versFormatDeStockage("6 98 29 80 95")).isEqualTo("6 98 29 80 95");
    }

    @Test
    void formatDeStockage_retireLeZeroInitial() {
        assertThat(NumeroTelephone.versFormatDeStockage("0698298095")).isEqualTo("6 98 29 80 95");
        assertThat(NumeroTelephone.versFormatDeStockage("06 98 29 80 95")).isEqualTo("6 98 29 80 95");
    }

    /**
     * Un mobile nord-américain fait 10 chiffres et ne suit pas le découpage marocain — la base
     * les stocke déjà en chiffres nus, sur 33 fiches.
     */
    @Test
    void formatDeStockage_autreLongueur_resteEnChiffresNus() {
        assertThat(NumeroTelephone.versFormatDeStockage("5145829397")).isEqualTo("5145829397");
        assertThat(NumeroTelephone.versFormatDeStockage("+1 514 582 9397")).isEqualTo("15145829397");
    }

    /**
     * {@code null} et non chaîne vide : une colonne legacy doit porter {@code NULL}. Les deux se
     * confondent à la lecture mais pas dans les requêtes du légataire.
     */
    @Test
    void formatDeStockage_rienDExploitable_rendNull() {
        assertThat(NumeroTelephone.versFormatDeStockage(null)).isNull();
        assertThat(NumeroTelephone.versFormatDeStockage("   ")).isNull();
        assertThat(NumeroTelephone.versFormatDeStockage("abc")).isNull();
    }

    /** Le zéro initial n'est retiré que tant que le numéro reste plus long qu'un national. */
    @Test
    void chiffres_neMangePasLesZerosDUnNumeroCourt() {
        assertThat(NumeroTelephone.chiffres("012345")).isEqualTo("012345");
        assertThat(NumeroTelephone.chiffres("00212698298095")).isEqualTo("212698298095");
    }

    @Test
    void indicatif_estRamenAuxChiffres() {
        assertThat(NumeroTelephone.indicatifNormalise("+212")).isEqualTo("212");
        assertThat(NumeroTelephone.indicatifNormalise(" 212 ")).isEqualTo("212");
        assertThat(NumeroTelephone.indicatifNormalise("212")).isEqualTo("212");
    }

    @Test
    void indicatif_vide_rendNull() {
        assertThat(NumeroTelephone.indicatifNormalise(null)).isNull();
        assertThat(NumeroTelephone.indicatifNormalise("")).isNull();
        assertThat(NumeroTelephone.indicatifNormalise("+")).isNull();
    }
}
