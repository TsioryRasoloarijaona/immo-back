package com.tsix.immocrm.controller.achat;

import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.fixtures.CorpsJson;
import com.tsix.immocrm.fixtures.FixtureAchat;
import com.tsix.immocrm.fixtures.FixtureAchat.Cadre;
import com.tsix.immocrm.fixtures.FixtureAchat.Saisie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static com.tsix.immocrm.controller.achat.AppelsEcrituresAchat.ADMINISTRATEUR;
import static com.tsix.immocrm.controller.achat.AppelsEcrituresAchat.INTROUVABLE;
import static com.tsix.immocrm.controller.achat.AppelsEcrituresAchat.ROLE_JWT;
import static com.tsix.immocrm.controller.achat.AppelsEcrituresAchat.appeler;
import static com.tsix.immocrm.controller.achat.AppelsEcrituresAchat.corps;
import static com.tsix.immocrm.controller.achat.AppelsEcrituresAchat.detail;
import static com.tsix.immocrm.controller.achat.AppelsEcrituresAchat.statut;
import static com.tsix.immocrm.controller.achat.AppelsSaisieAchat.*;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

/**
 * A2b-2a E-C1.4, E-C1.5 (FEATURE-achat-fiche-creation.md, Vérification n° 3) : la pièce écrite doit être relisible (société
 * habilitée sinon 404 « Société introuvable », projet habilité sinon 404 « Projet introuvable ») ; la pièce modifiée doit être
 * {@code VISIBLE} (+ « mes BC ») sinon 404 « Pièce introuvable », jamais 403 / 409 ; DOCUMENT_TO_PAY, REFUND, nature inconnue → 400.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PieceAchatSaisieVisibiliteTest {

    private static final String TRIGGER = "CfgTierTriggerId";
    private static final String TOUS_DROITS_BC = NOUVEAU_BC;

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureAchat fixture;

    private Cadre c;
    private Saisie s;
    private AdmUser redacteur;
    private int rang;

    @BeforeEach
    void setUp() {
        c = fixture.cadre("AVS");
        s = fixture.configurationSaisie(c.jeton());
        redacteur = fixture.compteHabilite(c, "AVS", ROLE_JWT, NOUVEAU_DEVIS, NOUVEAU_BC, NOUVEAU_BL, NOUVEAU_FACTURE,
                NOUVEL_AVOIR);
        fixture.sansPersonnel(redacteur);
    }

    private MvcResult par(AdmUser compte, MockHttpServletRequestBuilder requete) throws Exception {
        return appeler(mockMvc, requete, connecte(compte, ROLE_JWT));
    }

    private void assertRefus(MvcResult r, int code, String texte) throws Exception {
        assertThat(statut(r)).as(corps(r)).isEqualTo(code);
        assertThat(detail(r)).isEqualTo(texte);
    }

    private void assertRienCree() {
        fixture.oublier();
        assertThat(fixture.nombre("BuyDocument", "CfgTierId", c.fournisseur())).isZero();
    }

    private UUID bc(Cadre cadre) {
        UUID id = fixture.piece(cadre, "-BC" + (rang++), FixtureAchat.BON_COMMANDE);
        fixture.poser(id, "StrEntityId", s.gestion(FixtureAchat.GESTION_BC));
        fixture.poser(id, "BuyDocumentStatusId", s.enCours());
        return id;
    }

    // ------------------------------------------------------------------ E-C1.4 création

    @Test
    void ec14_creation_societeNonHabilitee_404_rienEcrit() throws Exception {
        UUID autreSociete = fixture.societe("SOC2-" + c.jeton());
        CorpsJson corps = corpsValide(c, s, FixtureAchat.BON_COMMANDE).avec("societeId", autreSociete)
                .avec("projetId", null).avec("ventilationProjets", List.of(part(c.projet(), "1")));

        assertRefus(par(redacteur, creer(corps)), 404, SOCIETE_INTROUVABLE);
        assertRienCree();
    }

    @Test
    void ec14_creation_projetNonHabilite_404_rienEcrit() throws Exception {
        UUID cache = fixture.projet("PX-" + c.jeton(), c.societe());

        assertRefus(par(redacteur, creer(corpsValide(c, s, FixtureAchat.BON_COMMANDE).avec("projetId", cache))), 404,
                PROJET_INTROUVABLE);
        assertRienCree();
    }

    /** Ordre au POST (réponse 1 du manager) : 404 → 403 → 400 ; société hors habilitation et compte sans droit → 404. */
    @Test
    void ordre_creation_societeNonHabilitee_sansDroit_404() throws Exception {
        UUID autreSociete = fixture.societe("SOC2-" + c.jeton());
        AdmUser sansDroit = fixture.compteHabilite(c, "AVS-SD", ROLE_JWT);
        fixture.sansPersonnel(sansDroit);
        CorpsJson corps = corpsValide(c, s, FixtureAchat.BON_COMMANDE).avec("societeId", autreSociete)
                .avec("projetId", null).avec("ventilationProjets", List.of(part(c.projet(), "1")));

        assertRefus(par(sansDroit, creer(corps)), 404, SOCIETE_INTROUVABLE);
        assertRienCree();
    }

    /** Ordre au POST : projet non habilité et compte sans droit → 404 (avant 403). */
    @Test
    void ordre_creation_projetNonHabilite_sansDroit_404() throws Exception {
        UUID cache = fixture.projet("PX-" + c.jeton(), c.societe());
        AdmUser sansDroit = fixture.compteHabilite(c, "AVS-SD", ROLE_JWT);
        fixture.sansPersonnel(sansDroit);

        assertRefus(par(sansDroit, creer(corpsValide(c, s, FixtureAchat.BON_COMMANDE).avec("projetId", cache))), 404,
                PROJET_INTROUVABLE);
        assertRienCree();
    }

    /** E-C1.4 : sans projet d'en-tête, une pièce de société habilitée est relisible → 201. */
    @Test
    void ec14_creation_sansProjet_societeHabilitee_201() throws Exception {
        CorpsJson corps = corpsValide(c, s, FixtureAchat.BON_COMMANDE).avec("projetId", null)
                .avec("ventilationProjets", List.of(part(c.projet(), "1")));

        MvcResult r = par(redacteur, creer(corps));

        assertThat(statut(r)).as(corps(r)).isEqualTo(201);
    }

    // ------------------------------------------------------------------ E-C1.5 natures non prises en charge

    @ParameterizedTest
    @ValueSource(strings = {FixtureAchat.DOCUMENT_A_REGLER, FixtureAchat.AVOIR, "INCONNUE"})
    void ec15_creation_natureNonPriseEnCharge_400_memeAdministrateur(String nature) throws Exception {
        AdmUser admin = fixture.compteHabilite(c, "AVS-ADM", ADMINISTRATEUR);
        fixture.sansPersonnel(admin);

        assertRefus(par(admin, creer(corpsValide(c, s, nature))), 400, NATURE_NON_PRISE);
        assertRienCree();
    }

    /** E-C1.5 : un avoir n'est pas créable, même avec le code {@code RefundList.toolNew} (Q-5). */
    @Test
    void ec15_creation_avoir_avecSonCode_400() throws Exception {
        assertRefus(par(redacteur, creer(corpsValide(c, s, FixtureAchat.AVOIR))), 400, NATURE_NON_PRISE);
    }

    // ------------------------------------------------------------------ E-C1.4 modification : 404

    @Test
    void ec14_modification_pieceInconnue_404() throws Exception {
        assertRefus(par(redacteur, modifier(UUID.randomUUID(), corpsValide(c, s, FixtureAchat.BON_COMMANDE))), 404,
                INTROUVABLE);
    }

    @Test
    void ec14_modification_pieceHorsSociete_404_jamais403() throws Exception {
        UUID autreSociete = fixture.societe("SOC2-" + c.jeton());
        Cadre ailleurs = new Cadre(c.jeton(), autreSociete, fixture.projet("P3-" + c.jeton(), autreSociete),
                c.fournisseur(), c.fournisseurNom());
        UUID piece = bc(ailleurs);
        AdmUser sansDroit = fixture.compteHabilite(c, "AVS-SD", ROLE_JWT);
        fixture.sansPersonnel(sansDroit);

        assertAll(
                () -> assertRefus(par(redacteur, modifier(piece, corpsValide(c, s, FixtureAchat.BON_COMMANDE))), 404,
                        INTROUVABLE),
                () -> assertRefus(par(sansDroit, modifier(piece, corpsValide(c, s, FixtureAchat.BON_COMMANDE))), 404,
                        INTROUVABLE));
    }

    @Test
    void ec14_modification_projetNonHabilite_404() throws Exception {
        Cadre cache = new Cadre(c.jeton(), c.societe(), fixture.projet("PX-" + c.jeton(), c.societe()), c.fournisseur(),
                c.fournisseurNom());
        UUID piece = bc(cache);

        assertRefus(par(redacteur, modifier(piece, corpsValide(c, s, FixtureAchat.BON_COMMANDE))), 404, INTROUVABLE);
    }

    /** E-C1.4 : pièce invisible et en lecture seule → 404, jamais 409. */
    @Test
    void ec14_modification_pieceInvisibleEnLectureSeule_404_jamais409() throws Exception {
        Cadre cache = new Cadre(c.jeton(), c.societe(), fixture.projet("PX-" + c.jeton(), c.societe()), c.fournisseur(),
                c.fournisseurNom());
        UUID piece = bc(cache);
        fixture.poser(piece, "BuyDocumentStatusId", s.valideDt());

        assertRefus(par(redacteur, modifier(piece, corpsValide(c, s, FixtureAchat.BON_COMMANDE))), 404, INTROUVABLE);
    }

    /** E-C1.4 : modifier une pièce visible pour l'écrire hors habilitation → 404 « Société introuvable ». */
    @Test
    void ec14_modification_versSocieteNonHabilitee_404() throws Exception {
        UUID piece = bc(c);
        UUID autreSociete = fixture.societe("SOC2-" + c.jeton());

        MvcResult r = par(redacteur, modifier(piece, corpsValide(c, s, FixtureAchat.BON_COMMANDE)
                .avec("societeId", autreSociete).avec("projetId", null)
                .avec("ventilationProjets", List.of(part(c.projet(), "1")))));

        assertRefus(r, 404, SOCIETE_INTROUVABLE);
        fixture.oublier();
        assertThat(fixture.lienDe(piece, "CfgCompanyId")).isEqualTo(AppelsEcrituresAchat.id(c.societe()));
    }

    /** E-C1.4 : BC d'un autre déclencheur pour un compte restreint (« mes BC ») → 404, son propre BC → 200. */
    @Test
    void ec14_modification_bcDUnAutreDeclencheur_compteRestreint_404_sonBc_200() throws Exception {
        AdmUser restreint = fixture.compteHabilite(c, "AVS-R", ROLE_JWT, TOUS_DROITS_BC);
        UUID autrePersonnel = fixture.compteHabilite(c, "AVS-B", ROLE_JWT).getCfgEmployee().getId();
        UUID bcAutre = bc(c);
        fixture.poser(bcAutre, TRIGGER, autrePersonnel);
        UUID bcSien = bc(c);
        fixture.poser(bcSien, TRIGGER, restreint.getCfgEmployee().getId());
        CorpsJson corps = corpsValide(c, s, FixtureAchat.BON_COMMANDE).avec("declencheurId", restreint.getCfgEmployee().getId());

        MvcResult autre = par(restreint, modifier(bcAutre, corps));
        MvcResult sien = par(restreint, modifier(bcSien, corps));

        assertAll(
                () -> assertRefus(autre, 404, INTROUVABLE),
                () -> assertThat(statut(sien)).as(corps(sien)).isEqualTo(200));
    }
}
