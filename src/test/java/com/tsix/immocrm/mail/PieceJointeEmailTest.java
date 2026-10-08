package com.tsix.immocrm.mail;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** FIX-sonar-fiabilite.md, correction 8 : égalité sur le contenu, toString sans les octets. */
class PieceJointeEmailTest {

    @Test
    void memeContenu_egalesEtMemeHash() {
        PieceJointeEmail a = new PieceJointeEmail("offre.pdf", "application/pdf", new byte[]{1, 2, 3});
        PieceJointeEmail b = new PieceJointeEmail("offre.pdf", "application/pdf", new byte[]{1, 2, 3});
        assertThat(a).isEqualTo(b).hasSameHashCodeAs(b);
        assertThat(new PieceJointeEmail(null, null, null)).isEqualTo(new PieceJointeEmail(null, null, null));
    }

    @Test
    void contenuOuNomDifferent_differentes() {
        PieceJointeEmail a = new PieceJointeEmail("offre.pdf", "application/pdf", new byte[]{1, 2, 3});
        assertThat(a).isNotEqualTo(new PieceJointeEmail("offre.pdf", "application/pdf", new byte[]{1, 2, 4}))
                .isNotEqualTo(new PieceJointeEmail("autre.pdf", "application/pdf", new byte[]{1, 2, 3}))
                .isNotEqualTo(new PieceJointeEmail("offre.pdf", "application/pdf", null));
    }

    @Test
    void toString_sansLesOctets() {
        assertThat(new PieceJointeEmail("offre.pdf", "application/pdf", new byte[]{1, 2, 3}))
                .hasToString("PieceJointeEmail[nom=offre.pdf, contentType=application/pdf, taille=3 octets]");
    }
}
