package com.tsix.immocrm.controller.projet;

import com.tsix.immocrm.controller.projet.AppelsProjet.Reponse;
import com.tsix.immocrm.fixtures.CorpsJson;
import com.tsix.immocrm.fixtures.FixtureLotsParite;
import com.tsix.immocrm.fixtures.FixtureProjetLots;
import com.tsix.immocrm.fixtures.FixtureReferentielsPlanification;
import com.tsix.immocrm.fixtures.FixtureReunionsChantier;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.CorpsJson.corps;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static com.tsix.immocrm.fixtures.FixtureLotsParite.ENTREPRENEURS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * N9a, définition de nivellement d'un projet ({@code CfgTrancheLevelingDefinition}) : R4, D3, D5, D6, D7 et « Réponses du
 * manager » 3 (FEATURE-n9-planification.md). Route {@code /api/projet/config/projets/{id}/nivellement[/{noeudId}]}.
 * Niveaux du {@code StkLeveling} du projet : BLOC (rang 1), ETAGE (rang 2) → profondeur maximale 2.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class NivellementProjetTest {

    private static final String PARENT = "parentId";
    private static final String CODE = "code";
    private static final String DETAIL = "$.detail";
    private static final String NIVEAU_INFERIEUR = "Vous ne pouvez pas ajouter un niveau inférieur";
    private static final String AUCUN_NIVEAU = "Aucun niveau n'est défini pour ce projet";
    private static final String SUPPRESSION_IMPOSSIBLE = "Suppression impossible : ";
    private static final String NOEUD = "SELECT COUNT(*) FROM CfgTrancheLevelingDefinition WHERE PKey = ?";
    private static final String NOEUDS_DU_PROJET = "SELECT COUNT(*) FROM CfgTrancheLevelingDefinition WHERE CfgTrancheId = ?";
    private static final String ENFANTS = "SELECT COUNT(*) FROM CfgTrancheLevelingDefinition WHERE ParentId = ?";
    private static final String CODE_DU_NOEUD = "SELECT Code FROM CfgTrancheLevelingDefinition WHERE PKey = ?";
    private static final String ADMINISTRATEUR = "Administrateur";
    private static final String BLOC = "BLOC";
    private static final String ETAGE = "ETAGE";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureReferentielsPlanification fixture;
    @Autowired private FixtureLotsParite parite;
    @Autowired private FixtureReunionsChantier reunions;

    private String jeton;
    private UUID societe;
    private UUID projet;
    private RequestPostProcessor admin;

    @BeforeEach
    void setUp() {
        fixture.tables();
        jeton = FixtureProjetLots.jeton();
        societe = parite.entiteJuridique("NVE-" + jeton);
        projet = projetNivele("NVP-" + jeton);
        admin = connecte(parite.administrateur("n9a.niv." + jeton, projet), ADMINISTRATEUR);
    }

    // ------------------------------------------------------------------ outils

    private UUID projetNivele(String code) {
        UUID id = parite.projet(code, societe);
        fixture.nivellementDuProjet(id, fixture.nivellement("NIV-" + code, BLOC, ETAGE));
        return id;
    }

    private static String nivellement(UUID projet) {
        return "/api/projet/config/projets/" + projet + "/nivellement";
    }

    private static String noeud(UUID projet, UUID noeud) {
        return nivellement(projet) + "/" + noeud;
    }

    private Reponse appeler(MockHttpServletRequestBuilder requete, RequestPostProcessor qui) throws Exception {
        MockHttpServletResponse r = mockMvc.perform(requete.with(qui)).andReturn().getResponse();
        return new Reponse(r.getStatus(), r.getContentAsString(StandardCharsets.UTF_8));
    }

    private Reponse envoyer(MockHttpServletRequestBuilder requete, CorpsJson corps, RequestPostProcessor qui) throws Exception {
        return appeler(requete.contentType(MediaType.APPLICATION_JSON).content(corps.json()), qui);
    }

    private Reponse ajouter(UUID projet, UUID parent, RequestPostProcessor qui) throws Exception {
        return envoyer(post(nivellement(projet)), corps().avec(PARENT, parent), qui);
    }

    private List<Map<String, Object>> enfants(UUID projet, UUID parent) {
        if (parent == null) {
            return fixture.lignes("SELECT PKey, Code, Sorting FROM CfgTrancheLevelingDefinition "
                    + "WHERE CfgTrancheId = ? AND ParentId IS NULL ORDER BY Sorting ASC", projet);
        }
        return fixture.lignes("SELECT PKey, Code, Sorting FROM CfgTrancheLevelingDefinition "
                + "WHERE CfgTrancheId = ? AND ParentId = ? ORDER BY Sorting ASC", projet, parent);
    }

    // ------------------------------------------------------------------ R4 : création, code par défaut, rang

    @Test
    void r4_racines_codeParDefautNiveau1EtRangParmiLesFreres() throws Exception {
        Reponse premiere = ajouter(projet, null, admin);
        Reponse seconde = ajouter(projet, null, admin);

        List<Map<String, Object>> racines = enfants(projet, null);
        SoftAssertions.assertSoftly(s -> {
            s.assertThat(premiere.statut()).as(premiere.corps()).isIn(200, 201);
            s.assertThat(seconde.statut()).as(seconde.corps()).isIn(200, 201);
            s.assertThat(racines).extracting(l -> l.get("Code")).containsExactly("BLOC 1", "BLOC 2");
            s.assertThat(racines).extracting(l -> ((Number) l.get("Sorting")).intValue()).containsExactly(1, 2);
        });
    }

    @Test
    void r4_enfant_codeDuNiveauSuivant_rangApresLesFreresExistants() throws Exception {
        UUID racine = fixture.noeud(projet, null, "BLOC 1", 1);
        fixture.noeud(projet, racine, "ETAGE 1", 1);

        Reponse r = ajouter(projet, racine, admin);

        List<Map<String, Object>> fils = enfants(projet, racine);
        SoftAssertions.assertSoftly(s -> {
            s.assertThat(r.statut()).as(r.corps()).isIn(200, 201);
            s.assertThat(fils).extracting(l -> l.get("Code")).containsExactly("ETAGE 1", "ETAGE 2");
            s.assertThat(fils).extracting(l -> ((Number) l.get("Sorting")).intValue()).containsExactly(1, 2);
        });
    }

    // R4 : profondeur maximale = plus grand Sorting des lignes du StkLeveling (2 ici).
    @Test
    void r4_auDelaDeLaProfondeurDuNivellement_400() throws Exception {
        UUID racine = fixture.noeud(projet, null, "BLOC 1", 1);
        UUID etage = fixture.noeud(projet, racine, "ETAGE 1", 1);

        Reponse temoin = ajouter(projet, racine, admin);
        Reponse r = ajouter(projet, etage, admin);

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(temoin.statut()).as(temoin.corps()).isIn(200, 201);
            s.assertThat(r.statut()).as(r.corps()).isEqualTo(400);
            s.assertThat(r.corps()).contains(NIVEAU_INFERIEUR);
            s.assertThat(fixture.compter(ENFANTS, etage)).isZero();
        });
    }

    // Réponse du manager 3 : projet sans StkLevelingId → 400 propre (OMRAN plante).
    @Test
    void r4_projetSansNivellement_creation400() throws Exception {
        UUID sansNiveau = parite.projet("NVS-" + jeton, societe);
        RequestPostProcessor adminDesDeux = connecte(parite.administrateur("n9a.niv2." + jeton, projet, sansNiveau),
                ADMINISTRATEUR);

        Reponse temoin = ajouter(projet, null, adminDesDeux);
        Reponse r = ajouter(sansNiveau, null, adminDesDeux);

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(temoin.statut()).as(temoin.corps()).isIn(200, 201);
            s.assertThat(r.statut()).as(r.corps()).isEqualTo(400);
            s.assertThat(String.valueOf(r.valeur(DETAIL))).contains(AUCUN_NIVEAU);
            s.assertThat(fixture.compter(NOEUDS_DU_PROJET, sansNiveau)).isZero();
        });
    }

    // ------------------------------------------------------------------ R4 : lecture de l'arbre

    @Test
    void r4_lecture_noeudsDuProjetSeulement() throws Exception {
        UUID autre = projetNivele("NVA-" + jeton);
        RequestPostProcessor adminDesDeux = connecte(parite.administrateur("n9a.niv3." + jeton, projet, autre),
                ADMINISTRATEUR);
        UUID racine = fixture.noeud(projet, null, "BLOC A", 1);
        UUID fils = fixture.noeud(projet, racine, "ETAGE A", 1);
        UUID etranger = fixture.noeud(autre, null, "BLOC Z", 1);

        Reponse r = appeler(get(nivellement(projet)), adminDesDeux);

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(r.statut()).as(r.corps()).isEqualTo(200);
            s.assertThat(r.ids("$..id")).contains(racine.toString(), fils.toString()).doesNotContain(etranger.toString());
            s.assertThat(r.ids("$.." + CODE)).contains("BLOC A", "ETAGE A").doesNotContain("BLOC Z");
        });
    }

    // ------------------------------------------------------------------ R4 : PATCH = Code seulement

    @Test
    void r4_modification_codeSeulement_parentEtRangInchanges() throws Exception {
        UUID racine = fixture.noeud(projet, null, "BLOC 1", 1);
        UUID fils = fixture.noeud(projet, racine, "ETAGE 1", 3);

        Reponse r = envoyer(patch(noeud(projet, fils)), corps().avec(CODE, "BAT A"), admin);

        List<Map<String, Object>> enBase = fixture.lignes(
                "SELECT Code, Sorting, CAST(ParentId AS nvarchar(36)) AS Parent, CAST(CfgTrancheId AS nvarchar(36)) AS Projet "
                        + "FROM CfgTrancheLevelingDefinition WHERE PKey = ?", fils);
        SoftAssertions.assertSoftly(s -> {
            s.assertThat(r.statut()).as(r.corps()).isEqualTo(200);
            s.assertThat(enBase).hasSize(1);
            s.assertThat(enBase.get(0)).containsEntry("Code", "BAT A").containsEntry("Sorting", 3);
            s.assertThat(String.valueOf(enBase.get(0).get("Parent"))).isEqualToIgnoringCase(racine.toString());
            s.assertThat(String.valueOf(enBase.get(0).get("Projet"))).isEqualToIgnoringCase(projet.toString());
        });
    }

    // ------------------------------------------------------------------ R4 / D7 : suppression récursive, refus si cité

    @Test
    void r4_suppression_recursiveSurLesDescendants_autreRacineIntacte() throws Exception {
        UUID racine = fixture.noeud(projet, null, "BLOC 1", 1);
        UUID etage = fixture.noeud(projet, racine, "ETAGE 1", 1);
        UUID etage2 = fixture.noeud(projet, racine, "ETAGE 2", 2);
        UUID autreRacine = fixture.noeud(projet, null, "BLOC 2", 2);

        Reponse r = appeler(delete(noeud(projet, racine)), admin);

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(r.statut()).as(r.corps()).isIn(200, 204);
            s.assertThat(fixture.compter(NOEUD, racine) + fixture.compter(NOEUD, etage) + fixture.compter(NOEUD, etage2)).isZero();
            s.assertThat(fixture.compter(NOEUD, autreRacine)).isEqualTo(1);
        });
    }

    @ParameterizedTest(name = "descendant cité par {0}")
    @ValueSource(strings = {"PrjMarketHierarchy", "GrhWorkedDay", "BulMeetingPrjProjectProgressCriteriaLine"})
    void r4_d7_suppression_refusee409_siUnDescendantEstCite(String citante) throws Exception {
        UUID racine = fixture.noeud(projet, null, "BLOC 1", 1);
        UUID etage = fixture.noeud(projet, racine, "ETAGE 1", 1);
        citer(citante, etage);

        Reponse r = appeler(delete(noeud(projet, racine)), admin);

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(r.statut()).as(r.corps()).isEqualTo(409);
            s.assertThat(String.valueOf(r.valeur(DETAIL))).startsWith(SUPPRESSION_IMPOSSIBLE);
            s.assertThat(fixture.compter(NOEUD, racine) + fixture.compter(NOEUD, etage)).isEqualTo(2);
        });
    }

    private void citer(String citante, UUID niveau) {
        switch (citante) {
            case "PrjMarketHierarchy" -> {
                parite.genresDeLot();
                UUID statut = parite.statut(ENTREPRENEURS, "NVT-" + jeton);
                fixture.hierarchieDeLot(parite.lot(projet, "NVL-" + jeton, ENTREPRENEURS, statut), niveau);
            }
            case "GrhWorkedDay" -> fixture.journeeTravaillee(projet, niveau);
            default -> {
                reunions.tables();
                UUID reunion = reunions.reunion(projet, reunions.type("NVR" + jeton), 1, "NVR-" + jeton,
                        LocalDateTime.of(2026, 3, 2, 9, 0));
                reunions.avancement(reunion, reunions.critere(projet, "Gros œuvre"), niveau, new BigDecimal("0.5"));
            }
        }
    }

    // ------------------------------------------------------------------ D5 : droit d'écriture de la fiche projet

    @Test
    void d5_nonAdministrateurHabilite_ecritures403_noeudInchange() throws Exception {
        RequestPostProcessor commercial = connecte(parite.avecDroits("n9a.niv.c." + jeton, List.of(), projet), "Commercial");
        UUID racine = fixture.noeud(projet, null, "BLOC 1", 1);

        Reponse temoin = ajouter(projet, null, admin);
        Reponse creation = ajouter(projet, null, commercial);
        Reponse modification = envoyer(patch(noeud(projet, racine)), corps().avec(CODE, "PIRATE"), commercial);
        Reponse suppression = appeler(delete(noeud(projet, racine)), commercial);

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(temoin.statut()).as(temoin.corps()).isIn(200, 201);
            s.assertThat(creation.statut()).isEqualTo(403);
            s.assertThat(modification.statut()).isEqualTo(403);
            s.assertThat(suppression.statut()).isEqualTo(403);
            s.assertThat(fixture.lire(CODE_DU_NOEUD,racine)).isEqualTo("BLOC 1");
            s.assertThat(fixture.compter(NOEUDS_DU_PROJET, projet)).isEqualTo(2);
        });
    }

    // ------------------------------------------------------------------ D6 : périmètre projet relu depuis la ligne stockée

    @Test
    void d6_projetHorsPerimetre_lectureEtEcritures_refusees() throws Exception {
        UUID cache = projetNivele("NVH-" + jeton);
        UUID noeudCache = fixture.noeud(cache, null, "BLOC H", 1);

        Reponse temoin = appeler(get(nivellement(projet)), admin);
        Reponse lecture = appeler(get(nivellement(cache)), admin);
        Reponse creation = ajouter(cache, null, admin);
        Reponse modification = envoyer(patch(noeud(cache, noeudCache)), corps().avec(CODE, "PIRATE"), admin);
        Reponse suppression = appeler(delete(noeud(cache, noeudCache)), admin);

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(temoin.statut()).as(temoin.corps()).isEqualTo(200);
            s.assertThat(lecture.statut()).isIn(403, 404);
            s.assertThat(creation.statut()).isIn(403, 404);
            s.assertThat(modification.statut()).isIn(403, 404);
            s.assertThat(suppression.statut()).isIn(403, 404);
            s.assertThat(fixture.compter(NOEUDS_DU_PROJET, cache)).isEqualTo(1);
            s.assertThat(fixture.lire(CODE_DU_NOEUD,noeudCache)).isEqualTo("BLOC H");
        });
    }

    // D6 : le nœud d'un projet caché, adressé sous l'URL d'un projet visible, reste refusé (projet lu sur la ligne).
    @Test
    void d6_noeudDUnProjetCache_sousLURLDUnProjetVisible_refuse() throws Exception {
        UUID cache = projetNivele("NVK-" + jeton);
        UUID noeudCache = fixture.noeud(cache, null, "BLOC K", 1);
        UUID noeudVisible = fixture.noeud(projet, null, "BLOC V", 1);

        Reponse temoin = envoyer(patch(noeud(projet, noeudVisible)), corps().avec(CODE, "BLOC V2"), admin);
        Reponse modification = envoyer(patch(noeud(projet, noeudCache)), corps().avec(CODE, "PIRATE"), admin);
        Reponse ajoutSousCache = ajouter(projet, noeudCache, admin);
        Reponse suppression = appeler(delete(noeud(projet, noeudCache)), admin);

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(temoin.statut()).as(temoin.corps()).isEqualTo(200);
            s.assertThat(modification.statut()).isIn(400, 403, 404);
            s.assertThat(ajoutSousCache.statut()).isIn(400, 403, 404);
            s.assertThat(suppression.statut()).isIn(400, 403, 404);
            s.assertThat(fixture.compter(NOEUDS_DU_PROJET, cache)).isEqualTo(1);
            s.assertThat(fixture.compter(ENFANTS, noeudCache)).isZero();
            s.assertThat(fixture.lire(CODE_DU_NOEUD,noeudCache)).isEqualTo("BLOC K");
        });
    }
}
