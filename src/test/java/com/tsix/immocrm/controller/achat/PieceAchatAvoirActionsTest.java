package com.tsix.immocrm.controller.achat;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.fixtures.FixtureAchat;
import com.tsix.immocrm.fixtures.FixtureGenerationAchat;
import com.tsix.immocrm.fixtures.FixtureGenerationAchat.LigneSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static com.tsix.immocrm.controller.achat.AppelsAvoirAchat.*;
import static com.tsix.immocrm.controller.achat.AppelsEcrituresAchat.ADMINISTRATEUR;
import static com.tsix.immocrm.controller.achat.AppelsEcrituresAchat.ROLE_JWT;
import static com.tsix.immocrm.controller.achat.AppelsEcrituresAchat.appeler;
import static com.tsix.immocrm.controller.achat.AppelsEcrituresAchat.corps;
import static com.tsix.immocrm.controller.achat.AppelsEcrituresAchat.fiche;
import static com.tsix.immocrm.controller.achat.AppelsEcrituresAchat.statut;
import static com.tsix.immocrm.controller.achat.AppelsGenerationAchat.naturesGenerees;
import static com.tsix.immocrm.controller.achat.AppelsSaisieAchat.NOUVEAU_BC;
import static com.tsix.immocrm.controller.achat.AppelsSaisieAchat.NOUVEAU_BL;
import static com.tsix.immocrm.controller.achat.AppelsSaisieAchat.NOUVEAU_FACTURE;
import static com.tsix.immocrm.controller.achat.AppelsSaisieAchat.NOUVEL_AVOIR;
import static com.tsix.immocrm.controller.achat.AppelsSaisieAchat.referentiels;
import static com.tsix.immocrm.fixtures.FixtureAchat.AVOIR;
import static com.tsix.immocrm.fixtures.FixtureAchat.BON_COMMANDE;
import static com.tsix.immocrm.fixtures.FixtureAchat.BON_LIVRAISON;
import static com.tsix.immocrm.fixtures.FixtureAchat.DEVIS;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Lot Avoirs (FEATURE-achat-avoirs.md) — « Générer Avoir » dans {@code actions.generations} uniquement sur une facture, y
 * compris de décompte (R4, D2) ; jamais REFUND dans {@code naturesCreables} (R8, D10).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PieceAchatAvoirActionsTest {

    private static final String LIBELLE_REFUND = "$.actions.generations[?(@.natureCible == 'REFUND')].libelle";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureAchat fixture;
    @Autowired private FixtureGenerationAchat generation;

    private Contexte x;
    private AdmUser toutesNatures;

    @BeforeEach
    void setUp() {
        x = preparer(fixture, generation, "AAA");
        toutesNatures = fixture.compteHabilite(x.c(), "AAA-T", ROLE_JWT, NOUVEL_AVOIR, NOUVEAU_BC, NOUVEAU_BL,
                NOUVEAU_FACTURE);
        fixture.sansPersonnel(toutesNatures);
    }

    private String ficheDe(AdmUser compte, UUID piece) throws Exception {
        MvcResult r = appeler(mockMvc, fiche(piece), connecte(compte, ROLE_JWT));
        assertThat(statut(r)).as(corps(r)).isEqualTo(200);
        return corps(r);
    }

    /** R4 : facture PrjInvoiceDebit → une génération REFUND libellée « Générer Avoir ». */
    @Test
    void r4_facture_genererAvoir() throws Exception {
        UUID facture = facture(fixture, x, "-FA");

        List<String> libelles = JsonPath.read(ficheDe(x.redacteur(), facture), LIBELLE_REFUND);

        assertThat(libelles).containsExactly(GENERER_AVOIR);
    }

    /** R4 / D2 : facture de décompte « gérée ailleurs » → « Générer Avoir » quand même. */
    @Test
    void r4_factureDeDecompte_genererAvoir() throws Exception {
        UUID facture = facture(fixture, x, "-FD");
        fixture.decompte(facture);

        List<String> libelles = JsonPath.read(ficheDe(x.redacteur(), facture), LIBELLE_REFUND);

        assertThat(libelles).containsExactly(GENERER_AVOIR);
    }

    /** R4 / D10 : sans le droit de créer un avoir, pas de « Générer Avoir » sur la facture. */
    @Test
    void r4_facture_sansDroitAvoir_absent() throws Exception {
        AdmUser sansDroit = fixture.compteHabilite(x.c(), "AAA-S", ROLE_JWT, NOUVEAU_FACTURE);
        fixture.sansPersonnel(sansDroit);
        UUID facture = facture(fixture, x, "-FS");

        assertThat(naturesGenerees(ficheDe(sansDroit, facture))).doesNotContain(AVOIR);
    }

    /** R4 : jamais sur un avoir. */
    @Test
    void r4_avoir_absent() throws Exception {
        UUID avoir = avoirExistant(fixture, x, facture(fixture, x, "-FAV"), true, "100", "120");

        assertThat(naturesGenerees(ficheDe(toutesNatures, avoir))).doesNotContain(AVOIR);
    }

    /** R4 : jamais sur un BL validé (qui propose pourtant « Générer Facture »). */
    @Test
    void r4_blValide_absent() throws Exception {
        UUID bl = generation.source(x.c(), "-BL", BON_LIVRAISON, x.s().gestion(FixtureAchat.GESTION_BL), x.s().valideDg());
        generation.ligne(bl, LigneSource.simple(1, "A", "1", x.s().categorie()));

        assertThat(naturesGenerees(ficheDe(toutesNatures, bl))).doesNotContain(AVOIR);
    }

    /** R4 : jamais sur un BC validé. */
    @Test
    void r4_bcValide_absent() throws Exception {
        UUID bc = generation.source(x.c(), "-BC", BON_COMMANDE, x.s().gestion(FixtureAchat.GESTION_BC), x.s().valideDg());
        generation.ligne(bc, LigneSource.simple(1, "A", "1", x.s().categorie()));

        assertThat(naturesGenerees(ficheDe(toutesNatures, bc))).doesNotContain(AVOIR);
    }

    /** R4 : jamais sur un devis. */
    @Test
    void r4_devis_absent() throws Exception {
        UUID devis = generation.source(x.c(), "-DV", DEVIS, x.s().gestion(FixtureAchat.GESTION_DEVIS), null);
        generation.ligne(devis, LigneSource.simple(1, "A", "1", x.s().categorie()));

        assertThat(naturesGenerees(ficheDe(toutesNatures, devis))).doesNotContain(AVOIR);
    }

    /** R8 / D10 : REFUND (désormais avec gestion et séquence) jamais dans naturesCreables, même pour un administrateur. */
    @Test
    void r8_naturesCreables_administrateur_sansRefund() throws Exception {
        AdmUser admin = fixture.compteHabilite(x.c(), "AAA-ADM", ADMINISTRATEUR);
        fixture.sansPersonnel(admin);

        MvcResult r = appeler(mockMvc, referentiels(), connecte(admin, ROLE_JWT));

        List<String> natures = JsonPath.read(corps(r), "$.naturesCreables");
        assertThat(natures).isNotEmpty().doesNotContain(AVOIR);
    }
}
