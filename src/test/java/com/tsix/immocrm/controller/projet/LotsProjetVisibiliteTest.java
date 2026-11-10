package com.tsix.immocrm.controller.projet;

import com.tsix.immocrm.controller.projet.AppelsProjet.Reponse;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.fixtures.FixtureProjetLots;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

import static com.tsix.immocrm.controller.projet.AppelsProjet.DECOMPTES;
import static com.tsix.immocrm.controller.projet.AppelsProjet.LOTS;
import static com.tsix.immocrm.controller.projet.AppelsProjet.REFERENTIELS;
import static com.tsix.immocrm.controller.projet.AppelsProjet.TAILLE;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Visibilité des lots et décomptes (FEATURE-projet-lots-decomptes.md §2.1, §2.2, §3 règles 1 à 4).
 * <pre>
 *   projet A : lots L1, L2 ; décompte DA sur L1        projet B : lot LB ; décompte DB
 *   lot LS sans projet                                  « a » habilité sur A seulement (AdmUserHabilitation.PrjProjectId)
 * </pre>
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class LotsProjetVisibiliteTest {

    private static final String CONTENU_IDS = "$.content[*].id";
    private static final String TOTAL = "$.totalElements";
    private static final String DETAIL = "$.detail";
    private static final String LOT_INTROUVABLE = "Lot introuvable";
    private static final String DECOMPTE_INTROUVABLE = "Décompte introuvable";
    private static final String PROJET_IDS = "projetIds";
    private static final String SIZE = "size";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureProjetLots fixture;
    @Autowired private JdbcTemplate jdbc;

    private AppelsProjet appels;
    private String jeton;
    private UUID fournisseur;
    private UUID projetA;
    private UUID projetB;
    private UUID lotA1;
    private UUID lotA2;
    private UUID lotB;
    private UUID lotSansProjet;
    private UUID decompteA;
    private UUID decompteB;
    private RequestPostProcessor a;

    @BeforeEach
    void setUp() {
        appels = new AppelsProjet(mockMvc);
        jeton = FixtureProjetLots.jeton();
        fournisseur = fixture.fournisseur("FOU-" + jeton);
        projetA = fixture.projet("PA-" + jeton);
        projetB = fixture.projet("PB-" + jeton);
        lotA1 = fixture.lot(projetA, "L1-" + jeton, fournisseur);
        lotA2 = fixture.lot(projetA, "L2-" + jeton, fournisseur);
        lotB = fixture.lot(projetB, "LB-" + jeton, fournisseur);
        lotSansProjet = fixture.lot(null, "LS-" + jeton, fournisseur);
        decompteA = fixture.decompte(lotA1, "DA-" + jeton, 1, montants());
        decompteB = fixture.decompte(lotB, "DB-" + jeton, 1, montants());
        AdmUser compteA = fixture.compte("prj.a." + jeton);
        fixture.habiliter(compteA, projetA);
        a = connecte(compteA, "Commercial");
    }

    private static Map<String, BigDecimal> montants() {
        return Map.of(FixtureProjetLots.TTC, new BigDecimal("1200.00"), FixtureProjetLots.RG, new BigDecimal("120.00"),
                FixtureProjetLots.A_PAYER, new BigDecimal("1080.00"));
    }

    // ------------------------------------------------------------------ règle 1 : habilité sur A seulement

    @Test
    void r1_listeLots_neContientQueLesLotsDuProjetA() throws Exception {
        Reponse r = appels.lire(a, LOTS, SIZE, TAILLE);

        assertThat(r.statut()).isEqualTo(200);
        assertThat(r.ids(CONTENU_IDS)).containsExactlyInAnyOrder(lotA1.toString(), lotA2.toString());
    }

    @Test
    void lots_triParDefaut_codeDecroissant() throws Exception {
        Reponse r = appels.lire(a, LOTS, SIZE, TAILLE);

        assertThat(r.statut()).isEqualTo(200);
        assertThat(r.ids(CONTENU_IDS)).containsExactly(lotA2.toString(), lotA1.toString());
    }

    @Test
    void r1_ficheLot_horsPerimetre_404CommeIdInconnu() throws Exception {
        Reponse visible = appels.lire(a, LOTS + "/" + lotA1);
        Reponse horsPerimetre = appels.lire(a, LOTS + "/" + lotB);
        Reponse inconnu = appels.lire(a, LOTS + "/" + UUID.randomUUID());

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(visible.statut()).isEqualTo(200);
            s.assertThat(visible.valeur("$.id")).isEqualTo(lotA1.toString());
            s.assertThat(horsPerimetre.statut()).isEqualTo(404);
            s.assertThat((String) horsPerimetre.valeur(DETAIL)).contains(LOT_INTROUVABLE);
            s.assertThat(inconnu.statut()).isEqualTo(404);
            s.assertThat((String) inconnu.valeur(DETAIL)).contains(LOT_INTROUVABLE);
        });
    }

    @Test
    void r1_bordereau_horsPerimetre_404() throws Exception {
        Reponse visible = appels.lire(a, LOTS + "/" + lotA1 + "/bordereau");
        Reponse horsPerimetre = appels.lire(a, LOTS + "/" + lotB + "/bordereau");

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(visible.statut()).isEqualTo(200);
            s.assertThat(visible.valeur("$.lotId")).isEqualTo(lotA1.toString());
            s.assertThat(horsPerimetre.statut()).isEqualTo(404);
            s.assertThat((String) horsPerimetre.valeur(DETAIL)).contains(LOT_INTROUVABLE);
        });
    }

    @Test
    void r1_decomptesDuLot_horsPerimetre_404() throws Exception {
        Reponse visible = appels.lire(a, LOTS + "/" + lotA1 + "/decomptes");
        Reponse horsPerimetre = appels.lire(a, LOTS + "/" + lotB + "/decomptes");

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(visible.statut()).isEqualTo(200);
            s.assertThat(visible.ids("$[*].id")).containsExactly(decompteA.toString());
            s.assertThat(horsPerimetre.statut()).isEqualTo(404);
            s.assertThat((String) horsPerimetre.valeur(DETAIL)).contains(LOT_INTROUVABLE);
        });
    }

    @Test
    void r1_ficheDecompte_horsPerimetre_404CommeIdInconnu() throws Exception {
        Reponse visible = appels.lire(a, DECOMPTES + "/" + decompteA);
        Reponse horsPerimetre = appels.lire(a, DECOMPTES + "/" + decompteB);
        Reponse inconnu = appels.lire(a, DECOMPTES + "/" + UUID.randomUUID());

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(visible.statut()).isEqualTo(200);
            s.assertThat(visible.valeur("$.id")).isEqualTo(decompteA.toString());
            s.assertThat(horsPerimetre.statut()).isEqualTo(404);
            s.assertThat((String) horsPerimetre.valeur(DETAIL)).contains(DECOMPTE_INTROUVABLE);
            s.assertThat(inconnu.statut()).isEqualTo(404);
            s.assertThat((String) inconnu.valeur(DETAIL)).contains(DECOMPTE_INTROUVABLE);
        });
    }

    @Test
    void r1_listeDecomptes_neContientQueLesDecomptesDuProjetA() throws Exception {
        Reponse r = appels.lire(a, DECOMPTES, SIZE, TAILLE);

        assertThat(r.statut()).isEqualTo(200);
        assertThat(r.ids(CONTENU_IDS)).containsExactly(decompteA.toString());
    }

    @Test
    void r1_referentiels_projets_seulementA() throws Exception {
        Reponse r = appels.lire(a, REFERENTIELS);

        assertThat(r.statut()).isEqualTo(200);
        assertThat(r.ids("$.projets[*].id")).containsExactly(projetA.toString());
    }

    @Test
    void referentiels_projet_porteCodeEtNombreDeLots() throws Exception {
        Reponse r = appels.lire(a, REFERENTIELS);

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(r.statut()).isEqualTo(200);
            s.assertThat(r.valeur("$.projets[0].code")).isEqualTo("PA-" + jeton);
            s.assertThat(r.nombre("$.projets[0].nbLots")).isEqualByComparingTo("2");
        });
    }

    @Test
    void r1_referentiels_fournisseurs_seulementCeuxDesLotsVisibles() throws Exception {
        UUID fournisseurB = fixture.fournisseur("FOB-" + jeton);
        fixture.lot(projetB, "LB2-" + jeton, fournisseurB);

        Reponse r = appels.lire(a, REFERENTIELS);

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(r.statut()).isEqualTo(200);
            s.assertThat(r.ids("$.fournisseurs[*].id")).containsExactly(fournisseur.toString());
            s.assertThat(r.ids("$.fournisseurs[*].id")).doesNotContain(fournisseurB.toString());
        });
    }

    @Test
    void r1_referentiels_fournisseurs_tiersDeDecompte_seulementDecomptesVisibles() throws Exception {
        UUID tiersA = fixture.fournisseur("TDA-" + jeton);
        UUID tiersB = fixture.fournisseur("TDB-" + jeton);
        UUID tiersSansProjet = fixture.fournisseur("TDS-" + jeton);
        UUID decompteSansProjet = fixture.decompte(lotSansProjet, "DS-" + jeton, 2, montants());
        String maj = "UPDATE PrjWorkingOut SET CfgTierId = ? WHERE PKey = ?";
        jdbc.update(maj, tiersA.toString(), decompteA.toString());
        jdbc.update(maj, tiersB.toString(), decompteB.toString());
        jdbc.update(maj, tiersSansProjet.toString(), decompteSansProjet.toString());

        Reponse r = appels.lire(a, REFERENTIELS);
        Reponse filtre = appels.lire(a, DECOMPTES, "fournisseurId", tiersA.toString(), SIZE, TAILLE);

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(r.statut()).isEqualTo(200);
            s.assertThat(r.ids("$.fournisseurs[*].id"))
                    .containsExactlyInAnyOrder(fournisseur.toString(), tiersA.toString());
            s.assertThat(filtre.statut()).isEqualTo(200);
            s.assertThat(filtre.ids(CONTENU_IDS)).containsExactly(decompteA.toString());
        });
    }

    @Test
    void lotSansProjet_invisible_404() throws Exception {
        Reponse visible = appels.lire(a, LOTS + "/" + lotA1);
        Reponse sansProjet = appels.lire(a, LOTS + "/" + lotSansProjet);

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(visible.statut()).isEqualTo(200);
            s.assertThat(sansProjet.statut()).isEqualTo(404);
            s.assertThat((String) sansProjet.valeur(DETAIL)).contains(LOT_INTROUVABLE);
        });
    }

    // ------------------------------------------------------------------ règle 2 : Administrateur sans habilitation

    @Test
    void r2_administrateurSansHabilitation_listesVides() throws Exception {
        RequestPostProcessor admin = connecte(fixture.compte("prj.admin." + jeton), "Administrateur");

        Reponse lots = appels.lire(admin, LOTS, SIZE, TAILLE);
        Reponse decomptes = appels.lire(admin, DECOMPTES, SIZE, TAILLE);
        Reponse referentiels = appels.lire(admin, REFERENTIELS);

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(lots.statut()).isEqualTo(200);
            s.assertThat(lots.nombre(TOTAL)).isEqualByComparingTo("0");
            s.assertThat(decomptes.statut()).isEqualTo(200);
            s.assertThat(decomptes.nombre(TOTAL)).isEqualByComparingTo("0");
            s.assertThat(referentiels.statut()).isEqualTo(200);
            s.assertThat(referentiels.ids("$.projets[*].id")).isEmpty();
            s.assertThat(referentiels.ids("$.fournisseurs[*].id")).isEmpty();
        });
    }

    @Test
    void r2_administrateurSansHabilitation_404Partout() throws Exception {
        RequestPostProcessor admin = connecte(fixture.compte("prj.admin." + jeton), "Administrateur");
        Reponse temoin = appels.lire(a, LOTS + "/" + lotA1);

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(temoin.statut()).as("témoin habilité").isEqualTo(200);
            for (String url : new String[]{LOTS + "/" + lotA1, LOTS + "/" + lotA1 + "/bordereau",
                    LOTS + "/" + lotA1 + "/decomptes", DECOMPTES + "/" + decompteA}) {
                Reponse r = lireSansErreur(admin, url);
                s.assertThat(r.statut()).as(url).isEqualTo(404);
                s.assertThat(r.corps()).as(url).containsAnyOf(LOT_INTROUVABLE, DECOMPTE_INTROUVABLE);
            }
        });
    }

    // ------------------------------------------------------------------ règle 3 : filtre projetIds = intersection

    @Test
    void r3_projetIdsHorsPerimetre_pageVide() throws Exception {
        Reponse lots = appels.lire(a, LOTS, PROJET_IDS, projetB.toString(), SIZE, TAILLE);
        Reponse decomptes = appels.lire(a, DECOMPTES, PROJET_IDS, projetB.toString(), SIZE, TAILLE);

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(lots.statut()).isEqualTo(200);
            s.assertThat(lots.nombre(TOTAL)).isEqualByComparingTo("0");
            s.assertThat(decomptes.statut()).isEqualTo(200);
            s.assertThat(decomptes.nombre(TOTAL)).isEqualByComparingTo("0");
        });
    }

    @Test
    void r3_projetIdsAetB_seulementA() throws Exception {
        Reponse lots = appels.lire(a, LOTS, PROJET_IDS, projetA.toString(), PROJET_IDS, projetB.toString(),
                SIZE, TAILLE);
        Reponse decomptes = appels.lire(a, DECOMPTES, PROJET_IDS, projetA.toString(), PROJET_IDS, projetB.toString(),
                SIZE, TAILLE);

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(lots.statut()).isEqualTo(200);
            s.assertThat(lots.ids(CONTENU_IDS)).containsExactlyInAnyOrder(lotA1.toString(), lotA2.toString());
            s.assertThat(decomptes.statut()).isEqualTo(200);
            s.assertThat(decomptes.ids(CONTENU_IDS)).containsExactly(decompteA.toString());
        });
    }

    @Test
    void r3_projetIds_restreintAuProjetDemande() throws Exception {
        UUID projetC = fixture.projet("PC-" + jeton);
        UUID lotC = fixture.lot(projetC, "LC-" + jeton, fournisseur);
        AdmUser compteAC = fixture.compte("prj.ac." + jeton);
        fixture.habiliter(compteAC, projetA);
        fixture.habiliter(compteAC, projetC);

        Reponse r = appels.lire(connecte(compteAC, "Commercial"), LOTS, PROJET_IDS, projetC.toString(), SIZE, TAILLE);

        assertThat(r.statut()).isEqualTo(200);
        assertThat(r.ids(CONTENU_IDS)).containsExactly(lotC.toString());
    }

    // ------------------------------------------------------------------ règle 4 : appelant non identifiable

    @Test
    @WithMockUser
    void r4_appelantNonIdentifiable_listesVides() throws Exception {
        Reponse lots = appels.lire(null, LOTS, SIZE, TAILLE);
        Reponse decomptes = appels.lire(null, DECOMPTES, SIZE, TAILLE);

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(lots.statut()).isEqualTo(200);
            s.assertThat(lots.nombre(TOTAL)).isEqualByComparingTo("0");
            s.assertThat(decomptes.statut()).isEqualTo(200);
            s.assertThat(decomptes.nombre(TOTAL)).isEqualByComparingTo("0");
        });
    }

    private Reponse lireSansErreur(RequestPostProcessor qui, String url) {
        try {
            return appels.lire(qui, url);
        } catch (Exception e) {
            throw new IllegalStateException(url, e);
        }
    }
}
