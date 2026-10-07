package com.tsix.immocrm.mail;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
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

    /** Lu comme le fait le bean réel, pour vérifier que la propriété est bien câblée. */
    @Value("${app.arpent.url}")
    private String urlArpent;

    private static final MotDePasseTemporaireEmis EVENEMENT = new MotDePasseTemporaireEmis(
            "jean.rakoto@exemple.ma", "Jean Rakoto", "j.rakoto", "Ab3xK9mQp2Rt");

    @Test
    void urlArpent_valeurAttendue() {
        assertThat(urlArpent).isEqualTo("https://arpent.ayaddeveloppement.com");
    }

    @Test
    void construit_variablesDuTemplateEtSujet() {
        EnvoiEmailMemoire memoire = new EnvoiEmailMemoire();
        MessageEmail message = new EmailMotDePasseListener(memoire, urlArpent).construire(EVENEMENT);

        assertThat(message.destinataire()).isEqualTo("jean.rakoto@exemple.ma");
        assertThat(message.sujet()).isEqualTo("Vos identifiants Arpent");
        assertThat(message.template()).isEqualTo("mail/mot-de-passe-temporaire");
        assertThat(message.variables())
                .containsEntry("nomComplet", "Jean Rakoto")
                .containsEntry("identifiant", "j.rakoto")
                .containsEntry("motDePasse", "Ab3xK9mQp2Rt")
                .containsEntry("urlArpent", "https://arpent.ayaddeveloppement.com")
                .doesNotContainKey("creation");
    }

    /**
     * Un client de messagerie qui n'affiche pas le HTML doit tout de même montrer le mot de
     * passe et le lien — sans quoi le message est inutile pour son unique destinataire.
     */
    @Test
    void construit_alternativeTexteContenantIdentifiantMotDePasseEtLien() {
        EnvoiEmailMemoire memoire = new EnvoiEmailMemoire();
        MessageEmail message = new EmailMotDePasseListener(memoire, urlArpent).construire(EVENEMENT);

        assertThat(message.corpsTexte())
                .contains("Jean Rakoto")
                .contains("j.rakoto")
                .contains("Ab3xK9mQp2Rt")
                .contains("https://arpent.ayaddeveloppement.com")
                .doesNotContain("provisoire");
    }

    @Test
    void surMotDePasseEmis_remetLeMessageAuPortDEnvoi() {
        EnvoiEmailMemoire memoire = new EnvoiEmailMemoire();
        new EmailMotDePasseListener(memoire, urlArpent).surMotDePasseEmis(EVENEMENT);

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
        assertThat(EVENEMENT.toString())
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
        EmailMotDePasseListener ecouteur = new EmailMotDePasseListener(memoire, urlArpent);

        assertThatCode(() -> ecouteur.surMotDePasseEmis(new MotDePasseTemporaireEmis(
                "x@y.z", "", "", "mdp"))).doesNotThrowAnyException();
    }
}
