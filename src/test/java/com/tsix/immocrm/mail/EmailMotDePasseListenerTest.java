package com.tsix.immocrm.mail;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Ce que l'écouteur fabrique à partir de l'événement.
 *
 * <p><b>L'écouteur est instancié à la main, pas injecté.</b> Le bean du contexte est enveloppé
 * dans un proxy {@code @Async} : l'appeler rendrait la main immédiatement et exécuterait le
 * corps sur le pool {@code mail-}, ce qui donnerait des assertions à l'issue incertaine. Une
 * instance directe exécute le même code de façon synchrone et déterministe.
 */
@SpringBootTest
class EmailMotDePasseListenerTest {

    @Autowired
    private EmailProperties properties;

    private static final MotDePasseTemporaireEmis CREATION = new MotDePasseTemporaireEmis(
            "jean.rakoto@exemple.ma", "Jean Rakoto", "j.rakoto", "Ab3xK9mQp2Rt", true);

    private static final MotDePasseTemporaireEmis REINITIALISATION = new MotDePasseTemporaireEmis(
            "jean.rakoto@exemple.ma", "Jean Rakoto", "j.rakoto", "Ab3xK9mQp2Rt", false);

    @Test
    void construit_variablesDuTemplateEtSujetDeCreation() {
        EnvoiEmailMemoire memoire = new EnvoiEmailMemoire();
        MessageEmail message = new EmailMotDePasseListener(memoire, properties).construire(CREATION);

        assertThat(message.destinataire()).isEqualTo("jean.rakoto@exemple.ma");
        assertThat(message.sujet()).isEqualTo("Vos identifiants de connexion");
        assertThat(message.template()).isEqualTo("mail/mot-de-passe-temporaire");
        assertThat(message.variables())
                .containsEntry("nomComplet", "Jean Rakoto")
                .containsEntry("identifiant", "j.rakoto")
                .containsEntry("motDePasse", "Ab3xK9mQp2Rt")
                .containsEntry("creation", true)
                .containsEntry("urlApplication", properties.getUrlApplication());
    }

    @Test
    void construit_sujetDistinctPourUneReinitialisation() {
        EnvoiEmailMemoire memoire = new EnvoiEmailMemoire();
        MessageEmail message = new EmailMotDePasseListener(memoire, properties).construire(REINITIALISATION);

        assertThat(message.sujet()).isEqualTo("Réinitialisation de votre mot de passe");
        assertThat(message.variables()).containsEntry("creation", false);
    }

    /**
     * Un client de messagerie qui n'affiche pas le HTML doit tout de même montrer le mot de
     * passe — sans quoi le message est inutile pour son unique destinataire.
     */
    @Test
    void construit_alternativeTexteContenantIdentifiantEtMotDePasse() {
        EnvoiEmailMemoire memoire = new EnvoiEmailMemoire();
        MessageEmail message = new EmailMotDePasseListener(memoire, properties).construire(CREATION);

        assertThat(message.corpsTexte())
                .contains("Jean Rakoto")
                .contains("j.rakoto")
                .contains("Ab3xK9mQp2Rt");
    }

    @Test
    void surMotDePasseEmis_remetLeMessageAuPortDEnvoi() {
        EnvoiEmailMemoire memoire = new EnvoiEmailMemoire();
        new EmailMotDePasseListener(memoire, properties).surMotDePasseEmis(CREATION);

        assertThat(memoire.nombreMessages()).isEqualTo(1);
        assertThat(memoire.dernier().destinataire()).isEqualTo("jean.rakoto@exemple.ma");
    }

    /**
     * L'événement transporte un secret en clair. Son {@code toString()} est redéfini pour le
     * masquer : un {@code record} logué par accident recracherait sinon le mot de passe dans le
     * journal, où il survivrait bien plus longtemps que sa validité.
     */
    @Test
    void toString_neDivulguePasLeMotDePasse() {
        assertThat(CREATION.toString())
                .doesNotContain("Ab3xK9mQp2Rt")
                .doesNotContain("jean.rakoto@exemple.ma")
                .contains("j***@exemple.ma")
                .contains("j.rakoto");
    }

    @Test
    void adresseMasquee_conserveLeDomaineEtResisteAuxEntreesAberrantes() {
        assertThat(AdresseMasquee.de("jean.rakoto@exemple.ma")).isEqualTo("j***@exemple.ma");
        assertThat(AdresseMasquee.de("pas-une-adresse")).isEqualTo("***");
        assertThat(AdresseMasquee.de("@exemple.ma")).isEqualTo("***");
        assertThat(AdresseMasquee.de("  ")).isNull();
        assertThat(AdresseMasquee.de(null)).isNull();
    }

    @Test
    void surMotDePasseEmis_neLevePasQuandLeDestinataireEstAtypique() {
        EnvoiEmailMemoire memoire = new EnvoiEmailMemoire();
        EmailMotDePasseListener ecouteur = new EmailMotDePasseListener(memoire, properties);

        assertThatCode(() -> ecouteur.surMotDePasseEmis(new MotDePasseTemporaireEmis(
                "x@y.z", "", "", "mdp", true))).doesNotThrowAnyException();
    }
}
