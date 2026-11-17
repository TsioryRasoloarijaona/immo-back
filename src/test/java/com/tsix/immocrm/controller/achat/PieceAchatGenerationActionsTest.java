package com.tsix.immocrm.controller.achat;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.fixtures.FixtureAchat;
import com.tsix.immocrm.fixtures.FixtureAchat.Cadre;
import com.tsix.immocrm.fixtures.FixtureAchat.Saisie;
import com.tsix.immocrm.fixtures.FixtureGenerationAchat;
import com.tsix.immocrm.fixtures.FixtureGenerationAchat.LigneSource;
import org.assertj.core.api.SoftAssertions;
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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.tsix.immocrm.controller.achat.AppelsEcrituresAchat.ROLE_JWT;
import static com.tsix.immocrm.controller.achat.AppelsEcrituresAchat.appeler;
import static com.tsix.immocrm.controller.achat.AppelsEcrituresAchat.corps;
import static com.tsix.immocrm.controller.achat.AppelsEcrituresAchat.fiche;
import static com.tsix.immocrm.controller.achat.AppelsEcrituresAchat.statut;
import static com.tsix.immocrm.controller.achat.AppelsGenerationAchat.*;
import static com.tsix.immocrm.controller.achat.AppelsSaisieAchat.NOUVEAU_BC;
import static com.tsix.immocrm.controller.achat.AppelsSaisieAchat.NOUVEAU_BL;
import static com.tsix.immocrm.controller.achat.AppelsSaisieAchat.NOUVEAU_DEVIS;
import static com.tsix.immocrm.controller.achat.AppelsSaisieAchat.NOUVEAU_FACTURE;
import static com.tsix.immocrm.fixtures.FixtureAchat.BON_COMMANDE;
import static com.tsix.immocrm.fixtures.FixtureAchat.BON_LIVRAISON;
import static com.tsix.immocrm.fixtures.FixtureAchat.DEVIS;
import static com.tsix.immocrm.fixtures.FixtureAchat.FACTURE;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

/**
 * A2b-2b G7 et D4 (FEATURE-achat-generation.md §3, §4, §8 « actions.generations ») : bloc {@code actions.generations} de la
 * fiche — libellés exacts, mêmes prédicats que G2, BC → facture masquée tant que GenerateInvoiceFromOrder est faux ; invariant
 * « action présente ⇔ {@code modele} de la cible avec cette source répond 200 », pour un compte qui a tous les droits de créer.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PieceAchatGenerationActionsTest {

    private static final List<String> NATURES = List.of(DEVIS, BON_COMMANDE, BON_LIVRAISON, FACTURE);

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureAchat fixture;
    @Autowired private FixtureGenerationAchat generation;

    private Cadre c;
    private Saisie s;
    private AdmUser redacteur;
    private int rang;

    @BeforeEach
    void setUp() {
        c = fixture.cadre("AGA");
        s = fixture.configurationSaisie(c.jeton());
        redacteur = fixture.compteHabilite(c, "AGA", ROLE_JWT, NOUVEAU_DEVIS, NOUVEAU_BC, NOUVEAU_BL, NOUVEAU_FACTURE);
        fixture.sansPersonnel(redacteur);
        generation.parametres(false, false, false);
        generation.tableBuyOrderInvoice();
    }

    private UUID piece(String nature, String gestion, UUID statut) {
        UUID id = generation.source(c, "-" + nature + (rang++), nature, s.gestion(gestion), statut);
        generation.ligne(id, LigneSource.simple(1, "ART-1", "10", s.categorie()));
        return id;
    }

    private UUID devis() {
        return piece(DEVIS, FixtureAchat.GESTION_DEVIS, null);
    }

    private UUID bc(UUID statut) {
        return piece(BON_COMMANDE, FixtureAchat.GESTION_BC, statut);
    }

    private UUID bl(UUID statut) {
        return piece(BON_LIVRAISON, FixtureAchat.GESTION_BL, statut);
    }

    private MvcResult par(MockHttpServletRequestBuilder requete) throws Exception {
        return appeler(mockMvc, requete, connecte(redacteur, ROLE_JWT));
    }

    private String ficheDe(UUID piece) throws Exception {
        MvcResult r = par(fiche(piece));
        assertThat(statut(r)).as(corps(r)).isEqualTo(200);
        return corps(r);
    }

    private List<Map<String, Object>> generations(UUID piece) throws Exception {
        return JsonPath.read(ficheDe(piece), GENERATIONS);
    }

    // ------------------------------------------------------------------ libellés et prédicats

    @Test
    void g7_devis_genererBonDeCommande() throws Exception {
        assertThat(generations(devis())).extracting(g -> g.get("natureCible"), g -> g.get("libelle"))
                .containsExactly(tuple(BON_COMMANDE, GENERER_BC));
    }

    /** G7 / D4 : BC validé, GenerateInvoiceFromOrder faux → seulement « Générer Bon de livraison ». */
    @Test
    void g7d4_bcValide_parametreFaux_seulementBonDeLivraison() throws Exception {
        assertThat(generations(bc(s.valideDg()))).extracting(g -> g.get("natureCible"), g -> g.get("libelle"))
                .containsExactly(tuple(BON_LIVRAISON, GENERER_BL));
    }

    /** G7 / D4 : ligne de paramètre absente = faux. */
    @Test
    void g7d4_bcValide_parametreAbsent_pasDeFacture() throws Exception {
        generation.parametre(FixtureGenerationAchat.FACTURE_DEPUIS_BC, null);

        assertThat(naturesGenerees(ficheDe(bc(s.valideDg())))).containsExactly(BON_LIVRAISON);
    }

    /** G7 / D4 : paramètre vrai → « Générer Bon de livraison » et « Générer Facture ». */
    @Test
    void g7d4_bcValide_parametreVrai_bonDeLivraisonEtFacture() throws Exception {
        generation.parametre(FixtureGenerationAchat.FACTURE_DEPUIS_BC, true);

        assertThat(generations(bc(s.valideDg()))).extracting(g -> g.get("natureCible"), g -> g.get("libelle"))
                .containsExactlyInAnyOrder(tuple(BON_LIVRAISON, GENERER_BL), tuple(FACTURE, GENERER_FACTURE));
    }

    /** Réponse 4 : BC validé entièrement livré → « Générer Bon de livraison » masqué. */
    @Test
    void r4_bcEntierementLivre_pasDeBonDeLivraison() throws Exception {
        UUID bc = bc(s.valideDg());
        UUID livre = bl(s.valideDg());
        fixture.poser(livre, "BuyOrderId", bc);

        assertThat(naturesGenerees(ficheDe(bc))).doesNotContain(BON_LIVRAISON);
    }

    /** Réponse 3 : {@code actions.generations} filtré par le droit de créer la cible (compte sans droit BL, avec droit facture). */
    @Test
    void r3_generationsFiltreesParLeDroitSurLaCible() throws Exception {
        generation.parametre(FixtureGenerationAchat.FACTURE_DEPUIS_BC, true);
        AdmUser sansDroitBl = fixture.compteHabilite(c, "AGB", ROLE_JWT, NOUVEAU_FACTURE);
        fixture.sansPersonnel(sansDroitBl);
        UUID bc = bc(s.valideDg());

        MvcResult r = appeler(mockMvc, fiche(bc), connecte(sansDroitBl, ROLE_JWT));

        assertThat(statut(r)).as(corps(r)).isEqualTo(200);
        assertThat(naturesGenerees(corps(r))).containsExactly(FACTURE);
    }

    /** G7 : BC non validé → aucune génération, même paramètre vrai. */
    @Test
    void g7_bcNonValide_aucuneGeneration() throws Exception {
        generation.parametre(FixtureGenerationAchat.FACTURE_DEPUIS_BC, true);

        assertThat(generations(bc(s.enCours()))).isNotNull().isEmpty();
    }

    @Test
    void g7_blValideNonFacture_genererFacture() throws Exception {
        assertThat(generations(bl(s.valideDg()))).extracting(g -> g.get("natureCible"), g -> g.get("libelle"))
                .containsExactly(tuple(FACTURE, GENERER_FACTURE));
    }

    @Test
    void g7_blDejaFacture_aucuneGeneration() throws Exception {
        UUID bl = bl(s.valideDg());
        fixture.poser(bl, "BuyInvoiceId", piece(FACTURE, FixtureAchat.GESTION_FACTURE, s.enCours()));

        assertThat(generations(bl)).isNotNull().isEmpty();
    }

    @Test
    void g7_blNonValide_aucuneGeneration() throws Exception {
        assertThat(generations(bl(s.enCours()))).isNotNull().isEmpty();
    }

    @Test
    void g7_facture_aucuneGeneration() throws Exception {
        assertThat(generations(piece(FACTURE, FixtureAchat.GESTION_FACTURE, s.enCours()))).isNotNull().isEmpty();
    }

    // ------------------------------------------------------------------ invariant G7

    /** G7 : pour chaque source et chaque nature cible, action présente ⇔ {@code modele(cible, source)} répond 200. */
    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void g7_invariant_actionPresente_siEtSeulementSiModele200(boolean factureDepuisBc) throws Exception {
        generation.parametre(FixtureGenerationAchat.FACTURE_DEPUIS_BC, factureDepuisBc);
        Map<String, UUID> sources = new LinkedHashMap<>();
        sources.put("devis", devis());
        sources.put("BC validé", bc(s.valideDg()));
        sources.put("BC non validé", bc(s.enCours()));
        sources.put("BL validé", bl(s.valideDg()));
        sources.put("BL non validé", bl(s.enCours()));
        UUID blFacture = bl(s.valideDg());
        UUID facture = piece(FACTURE, FixtureAchat.GESTION_FACTURE, s.enCours());
        fixture.poser(blFacture, "BuyInvoiceId", facture);
        sources.put("BL facturé", blFacture);
        sources.put("facture", facture);

        SoftAssertions verifs = new SoftAssertions();
        for (Map.Entry<String, UUID> source : sources.entrySet()) {
            List<String> generees = naturesGenerees(ficheDe(source.getValue()));
            for (String cible : NATURES) {
                boolean modele200 = statut(par(modele(cible, source.getValue()))) == 200;
                verifs.assertThat(generees.contains(cible)).as("%s → %s", source.getKey(), cible).isEqualTo(modele200);
            }
        }
        verifs.assertAll();
    }
}
