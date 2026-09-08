package com.tsix.immocrm.whatsapp;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Normalisation du numéro WhatsApp. Test unitaire pur — aucun contexte Spring, aucune base :
 * c'est de l'arithmétique de chaîne, et la faire tourner en {@code @SpringBootTest} ne
 * prouverait rien de plus pour trente secondes de plus.
 *
 * <p>Chaque cas ci-dessous correspond à une forme <b>réellement présente</b> dans
 * {@code yesRemsProd}, avec son effectif. Les inventer aurait été plus rapide et aurait vérifié
 * mon imagination plutôt que les données.
 */
class NumeroWhatsAppTest {

    /** La forme dominante : 4 847 des 5 070 prospections avec rendez-vous et un mobile. */
    @Test
    void formeDominante_indicatif212_mobileEspace() {
        assertThat(NumeroWhatsApp.international("212", "6 61 22 48 57"))
                .isEqualTo("+212661224857");
    }

    /** 126 lignes commencent par 7 plutôt que 6 — c'est un mobile marocain valide. */
    @Test
    void mobileCommencantPar7() {
        assertThat(NumeroWhatsApp.international("212", "7 00 76 39 40"))
                .isEqualTo("+212700763940");
    }

    /**
     * 225 tiers portent l'indicatif « 33 ». La règle ne présume donc pas le Maroc — et c'est
     * précisément pourquoi le repli sur {@code TempTel}, qui n'a pas d'indicatif, est refusé.
     */
    @Test
    void indicatifEtranger_estRespecte() {
        assertThat(NumeroWhatsApp.international("33", "6 81 06 56 57"))
                .isEqualTo("+33681065657");
    }

    /** 19 lignes réelles sont des fixes (« 5 22 27 34 96 ») : WhatsApp n'a rien à y envoyer. */
    @Test
    void numeroFixe_estRefuse() {
        assertThat(NumeroWhatsApp.international("212", "5 22 27 34 96")).isNull();
    }

    /** 24 lignes ont une longueur anormale — saisies tronquées, « 000000 ». */
    @Test
    void longueurAnormale_estRefusee() {
        assertThat(NumeroWhatsApp.international("212", "000000")).isNull();
        assertThat(NumeroWhatsApp.international("212", "6 61 22 48 57 99")).isNull();
    }

    /** Une ligne réelle n'a pas d'indicatif : impossible de composer sans supposer un pays. */
    @Test
    void sansIndicatif_estRefuse() {
        assertThat(NumeroWhatsApp.international(null, "6 61 22 48 57")).isNull();
        assertThat(NumeroWhatsApp.international("   ", "6 61 22 48 57")).isNull();
    }

    @Test
    void sansNumero_estRefuse() {
        assertThat(NumeroWhatsApp.international("212", null)).isNull();
        assertThat(NumeroWhatsApp.international("212", "")).isNull();
    }

    /**
     * Absente des données mesurées, mais rien n'empêche une saisie future en forme nationale.
     * Le zéro initial doit disparaître, sinon le numéro composé serait {@code +2120661…}.
     */
    @Test
    void formeNationaleAvecZeroInitial_estRamenee() {
        assertThat(NumeroWhatsApp.international("212", "06 61 22 48 57"))
                .isEqualTo("+212661224857");
    }

    /** Un indicatif déjà préfixé d'un + ne doit pas produire « ++212 ». */
    @Test
    void indicatifDejaPrefixe_neDoublePasLePlus() {
        assertThat(NumeroWhatsApp.international("+212", "6 61 22 48 57"))
                .isEqualTo("+212661224857");
    }

    /** Un indicatif absurdement long est refusé plutôt que composé. */
    @Test
    void indicatifTropLong_estRefuse() {
        assertThat(NumeroWhatsApp.international("212212", "6 61 22 48 57")).isNull();
    }
}
