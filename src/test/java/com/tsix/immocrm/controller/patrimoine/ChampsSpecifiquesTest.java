package com.tsix.immocrm.controller.patrimoine;

import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.cfg.CfgCompany;
import com.tsix.immocrm.entities.cfg.CfgProject;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.fixtures.ApiFaisabilite;
import com.tsix.immocrm.fixtures.FixtureChampsSpecifiques;
import com.tsix.immocrm.fixtures.FixtureChampsSpecifiques.Definition;
import com.tsix.immocrm.fixtures.FixtureEntites;
import com.tsix.immocrm.fixtures.FixtureExportPatrimoine;
import com.tsix.immocrm.fixtures.FixtureFaisabilite;
import com.tsix.immocrm.fixtures.FixturePatrimoine;
import com.tsix.immocrm.fixtures.FixtureProspectionsTerrain;
import com.tsix.immocrm.fixtures.FixtureTerrains;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Lot P9d — champs spécifiques (GEN0007) : FEATURE-patrimoine-import-export.md §4.2, §5 (C1, C2) et
 * FEATURE-patrimoine-relances-champs.md (décision 25/10 : 7 porteurs, C3 ; décision 3 : étude lue par Administrateur / Direction
 * Générale ; correction 25/10 : terrain = {@code Yes.Stk.Land.LandProject}). Porteurs du fil : {@code projet}, {@code tranche},
 * {@code entite}, {@code article} (sous-articles compris, même classe OMRAN), {@code terrain}, {@code prospection-terrain},
 * {@code etude}. Les 403 sont dans {@link ChampsSpecifiquesSecuriteTest}.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ChampsSpecifiquesTest {

    private static final String CHAMPS = "/api/champs-specifiques/{porteur}/{id}";
    private static final String DEFINITIONS = "/api/champs-specifiques/{porteur}/definitions";
    private static final String DEFINITION = DEFINITIONS + "/{champId}";
    private static final String PROJET = "projet";
    private static final String TRANCHE = "tranche";
    private static final String ENTITE = "entite";
    private static final String ARTICLE = "article";
    private static final String SOUS_ARTICLE = "sous-article";
    private static final String TERRAIN = "terrain";
    private static final String PROSPECTION_TERRAIN = "prospection-terrain";
    private static final String ETUDE = "etude";
    private static final String DETAIL = "$.detail";

    @Autowired private MockMvc mockMvc;
    @Autowired private EntityManager em;
    @Autowired private FixturePatrimoine patrimoine;
    @Autowired private FixtureChampsSpecifiques champs;
    @Autowired private FixtureExportPatrimoine articles;
    @Autowired private FixtureTerrains terrains;
    @Autowired private FixtureProspectionsTerrain prospections;
    @Autowired private FixtureFaisabilite faisabilite;
    @Autowired private FixtureEntites entites;

    private AdmUser admin;
    private String jeton;
    private CfgCompany entite;
    private CfgProject projet;
    private CfgTranche tranche;

    /** Un objet porteur : valeur du fil, id, code d'entité OMRAN, entité juridique dont dépend sa visibilité (R4). */
    private record Porteur(String fil, UUID id, String codeEntite, UUID entiteJuridique) {
    }

    @BeforeEach
    void setUp() {
        champs.schema();
        admin = patrimoine.administrateur();
        jeton = FixtureExportPatrimoine.jeton().replace("P9C", "P9D");
        entite = patrimoine.entite("ENT-CS-" + jeton, true);
        projet = patrimoine.projet("PRJ-CS-" + jeton, entite);
        tranche = patrimoine.tranche("TR-CS-" + jeton, projet);
    }

    private Porteur porteur(String cas) {
        Porteur p = switch (cas) {
            case PROJET -> new Porteur(PROJET, projet.getId(), FixtureChampsSpecifiques.PROJET, entite.getId());
            case TRANCHE -> new Porteur(TRANCHE, tranche.getId(), FixtureChampsSpecifiques.TRANCHE, entite.getId());
            case ENTITE -> new Porteur(ENTITE, entite.getId(), FixtureChampsSpecifiques.ENTITE, entite.getId());
            case ARTICLE -> new Porteur(ARTICLE, article().getId(), FixtureChampsSpecifiques.ARTICLE, entite.getId());
            case SOUS_ARTICLE -> new Porteur(ARTICLE, sousArticle().getId(), FixtureChampsSpecifiques.ARTICLE,
                    entite.getId());
            case TERRAIN -> terrain();
            case PROSPECTION_TERRAIN -> new Porteur(PROSPECTION_TERRAIN,
                    prospections.prospection(prospections.contexte("P9D"), "PT-CS-" + jeton),
                    FixtureChampsSpecifiques.PROSPECTION_TERRAIN, null);
            default -> throw new IllegalArgumentException(cas);
        };
        relire();
        return p;
    }

    private StkItem article() {
        return articles.article(articles.referentiel(jeton), "ART-CS-" + jeton, tranche, null);
    }

    private StkItem sousArticle() {
        FixtureExportPatrimoine.Referentiel r = articles.referentiel(jeton);
        StkItem parent = articles.article(r, "ARTP-CS-" + jeton, tranche, null);
        return articles.sousArticle(r, "SA-CS-" + jeton, tranche, parent);
    }

    private Porteur terrain() {
        FixtureTerrains.Contexte c = terrains.contexte("P9D");
        return new Porteur(TERRAIN, terrains.terrain(c, "TER-CS-" + jeton), FixtureChampsSpecifiques.TERRAIN,
                c.entite().getId());
    }

    private UUID etude() throws Exception {
        faisabilite.parametrage();
        return new ApiFaisabilite(mockMvc, admin).etude(FixtureFaisabilite.fiche("EF-CS-" + jeton, entite.getId()));
    }

    private void relire() {
        em.flush();
        em.clear();
    }

    private static String corps(Map<UUID, String> valeurs) {
        StringBuilder json = new StringBuilder("{\"valeurs\":[");
        valeurs.forEach((champ, valeur) -> json.append(json.charAt(json.length() - 1) == '[' ? "" : ",")
                .append("{\"champId\":\"").append(champ).append("\",\"valeur\":")
                .append(valeur == null ? "null" : "\"" + valeur + "\"").append('}'));
        return json.append("]}").toString();
    }

    private ResultActions ecrire(Porteur p, UUID champ, String valeur) throws Exception {
        return ecrire(p.fil(), p.id(), corps(Collections.singletonMap(champ, valeur)), connecte(admin));
    }

    private ResultActions ecrire(String porteur, UUID id, String corps, RequestPostProcessor qui) throws Exception {
        return mockMvc.perform(put(CHAMPS, porteur, id).with(qui)
                .contentType(MediaType.APPLICATION_JSON).content(corps));
    }

    private String lire(String porteur, UUID id) throws Exception {
        return mockMvc.perform(get(CHAMPS, porteur, id).with(connecte(admin)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    // ------------------------------------------------------------------ C3 : les 7 porteurs, lecture et écriture

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {PROJET, TRANCHE, ENTITE, ARTICLE, SOUS_ARTICLE, TERRAIN, PROSPECTION_TERRAIN})
    void c3_putPuisGet_200_valeurEcriteDansValueEtRelue(String cas) throws Exception {
        Porteur p = porteur(cas);
        UUID champ = champs.champ(p.codeEntite(), Definition.simple("Libelle " + cas + " " + jeton, FixtureChampsSpecifiques.TEXTE));
        String valeur = "VAL-" + cas + "-" + jeton;

        ecrire(p, champ, valeur).andExpect(status().isOk());

        assertThat(champs.valeurs(champ, p.id())).singleElement()
                .satisfies(ligne -> assertThat(ligne).containsEntry("Value", valeur));
        relire();
        assertThat(lire(p.fil(), p.id())).contains("Libelle " + cas + " " + jeton).contains(valeur);
    }

    /** Code d'entité de l'étude non figé par le spec (Question 2) : lecture et écriture vide seulement. */
    @Test
    void c3_etude_administrateur_getEtPut_200() throws Exception {
        UUID etude = etude();
        relire();

        lire(ETUDE, etude);
        ecrire(ETUDE, etude, "{\"valeurs\":[]}", connecte(admin)).andExpect(status().isOk());
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {PROJET, TRANCHE, ARTICLE, TERRAIN})
    void c3_porteurDUneEntiteInactive_404_enLectureEtEnEcriture(String cas) throws Exception {
        Porteur p = porteur(cas);
        UUID champ = champs.champ(p.codeEntite(), Definition.simple("Invisible " + cas + " " + jeton,
                FixtureChampsSpecifiques.TEXTE));
        entites.actif(p.entiteJuridique(), false);
        relire();

        mockMvc.perform(get(CHAMPS, p.fil(), p.id()).with(connecte(admin))).andExpect(status().isNotFound());
        ecrire(p, champ, "X").andExpect(status().isNotFound());
        assertThat(champs.valeursDeLObjet(p.id())).isZero();
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {PROJET, TRANCHE, ENTITE, ARTICLE, TERRAIN, PROSPECTION_TERRAIN, ETUDE})
    void c3_idInconnu_404(String porteur) throws Exception {
        mockMvc.perform(get(CHAMPS, porteur, UUID.randomUUID()).with(connecte(admin))).andExpect(status().isNotFound());
    }

    /** Décision 3 (25/10) : les champs de l'étude suivent la lecture de son audit (Commercial refusé). */
    @Test
    void c3_etude_commercial_refuse() throws Exception {
        UUID etude = etude();
        relire();

        mockMvc.perform(get(CHAMPS, ETUDE, etude).with(FixtureExportPatrimoine.commercial(patrimoine.administrateur())))
                .andExpect(status().is(anyOf(is(403), is(404))));
    }

    @Test
    void lecture_ouverteAuCommercial_surUnProgrammeVisible() throws Exception {
        relire();

        mockMvc.perform(get(CHAMPS, PROJET, projet.getId())
                        .with(FixtureExportPatrimoine.commercial(patrimoine.administrateur())))
                .andExpect(status().isOk());
    }

    // ------------------------------------------------------------------ lecture : ordre et défaut (§4.2)

    @Test
    void lecture_triCategorySortingPuisCategoryPuisSorting() throws Exception {
        String code = FixtureChampsSpecifiques.PROJET;
        champs.champ(code, new Definition("A-" + jeton, "Troisieme " + jeton, FixtureChampsSpecifiques.TEXTE, null,
                false, 2, 1, null));
        champs.champ(code, new Definition("Z-" + jeton, "Deuxieme " + jeton, FixtureChampsSpecifiques.TEXTE, null,
                false, 1, 2, null));
        champs.champ(code, new Definition("Z-" + jeton, "Premier " + jeton, FixtureChampsSpecifiques.TEXTE, null,
                false, 1, 1, null));
        champs.champ(code, new Definition("B-" + jeton, "Quatrieme " + jeton, FixtureChampsSpecifiques.TEXTE, null,
                false, 2, 0, null));
        relire();

        String json = lire(PROJET, projet.getId());

        int premier = json.indexOf("Premier " + jeton);
        int deuxieme = json.indexOf("Deuxieme " + jeton);
        int troisieme = json.indexOf("Troisieme " + jeton);
        int quatrieme = json.indexOf("Quatrieme " + jeton);
        assertAll(
                () -> assertThat(premier).isNotNegative().isLessThan(deuxieme),
                () -> assertThat(deuxieme).isLessThan(troisieme),
                () -> assertThat(troisieme).as("Category A avant B à CategorySorting égal").isLessThan(quatrieme));
    }

    @Test
    void lecture_sansValeur_rendLaValeurParDefaut() throws Exception {
        String defaut = "DEF-" + jeton;
        champs.champ(FixtureChampsSpecifiques.PROJET, new Definition("G", "Avec defaut " + jeton,
                FixtureChampsSpecifiques.TEXTE, defaut, false, 1, 1, null));
        relire();

        assertThat(lire(PROJET, projet.getId())).contains(defaut);
    }

    @Test
    void ecriture_deuxFois_uneSeuleLigneParChampEtObjet_derniereValeur() throws Exception {
        Porteur p = porteur(PROJET);
        UUID champ = champs.champ(p.codeEntite(), Definition.simple("Deux fois " + jeton, FixtureChampsSpecifiques.TEXTE));

        ecrire(p, champ, "PREMIERE").andExpect(status().isOk());
        ecrire(p, champ, "SECONDE").andExpect(status().isOk());

        assertThat(champs.valeurs(champ, p.id())).singleElement()
                .satisfies(ligne -> assertThat(ligne).containsEntry("Value", "SECONDE"));
    }

    // ------------------------------------------------------------------ Question 11 : Value + colonne typée

    @Test
    void q11_decimal_ecritValueEtDecimalValue() throws Exception {
        Porteur p = porteur(TRANCHE);
        UUID champ = champs.champ(p.codeEntite(), Definition.simple("Decimal " + jeton, FixtureChampsSpecifiques.DECIMAL));

        ecrire(p, champ, "1234.5").andExpect(status().isOk());

        assertThat(champs.valeurs(champ, p.id())).singleElement().satisfies(ligne -> assertAll(
                () -> assertThat(ligne.get("Value")).isNotNull(),
                () -> assertThat((BigDecimal) ligne.get("DecimalValue")).isEqualByComparingTo("1234.5")));
    }

    @Test
    void q11_entier_ecritValueEtIntValue() throws Exception {
        Porteur p = porteur(TRANCHE);
        UUID champ = champs.champ(p.codeEntite(), Definition.simple("Entier " + jeton, FixtureChampsSpecifiques.ENTIER));

        ecrire(p, champ, "42").andExpect(status().isOk());

        assertThat(champs.valeurs(champ, p.id())).singleElement().satisfies(ligne -> assertAll(
                () -> assertThat(ligne.get("Value")).isNotNull(),
                () -> assertThat(((Number) ligne.get("IntValue")).intValue()).isEqualTo(42)));
    }

    // ------------------------------------------------------------------ C1 : 400 nommant le champ, rien écrit

    @Test
    void c1_decimalInvalide_400_nommeLeChamp_rienEcrit() throws Exception {
        Porteur p = porteur(PROJET);
        String libelle = "Surface utile " + jeton;
        UUID champ = champs.champ(p.codeEntite(), Definition.simple(libelle, FixtureChampsSpecifiques.DECIMAL));

        ecrire(p, champ, "abc").andExpect(status().isBadRequest())
                .andExpect(jsonPath(DETAIL, containsString(libelle)));

        assertThat(champs.valeursDeLObjet(p.id())).isZero();
    }

    @Test
    void c1_obligatoireSansValeur_400_messageGen0001() throws Exception {
        Porteur p = porteur(PROJET);
        String libelle = "Obligatoire " + jeton;
        UUID champ = champs.champ(p.codeEntite(), new Definition("G", libelle, FixtureChampsSpecifiques.TEXTE, null,
                true, 1, 1, null));

        ecrire(p, champ, "").andExpect(status().isBadRequest())
                .andExpect(jsonPath(DETAIL, containsString("La valeur du champ [" + libelle + "] est obligatoire !!")));

        assertThat(champs.valeursDeLObjet(p.id())).isZero();
    }

    @Test
    void c1_choixHorsListe_400_nommeLeChamp() throws Exception {
        Porteur p = porteur(PROJET);
        String libelle = "Couleur " + jeton;
        UUID champ = champs.champ(p.codeEntite(), new Definition("G", libelle, FixtureChampsSpecifiques.LISTE, null,
                false, 1, 1, "ROUGE"));

        ecrire(p, champ, "VERT-HORS-LISTE").andExpect(status().isBadRequest())
                .andExpect(jsonPath(DETAIL, containsString(libelle)));

        assertThat(champs.valeursDeLObjet(p.id())).isZero();
    }

    @Test
    void c1_choixDansLaListe_200_valueEcrite() throws Exception {
        Porteur p = porteur(PROJET);
        UUID champ = champs.champ(p.codeEntite(), new Definition("G", "Couleur ok " + jeton,
                FixtureChampsSpecifiques.LISTE, null, false, 1, 1, "ROUGE"));

        ecrire(p, champ, "ROUGE").andExpect(status().isOk());

        assertThat(champs.valeurs(champ, p.id())).singleElement()
                .satisfies(ligne -> assertThat(ligne).containsEntry("Value", "ROUGE"));
    }

    // ------------------------------------------------------------------ C2 : supprimer la définition

    @Test
    void c2_suppressionDeLaDefinition_supprimeLesValeursDeTousLesObjets() throws Exception {
        CfgProject autre = patrimoine.projet("PRJ-CS2-" + jeton, entite);
        UUID champ = champs.champ(FixtureChampsSpecifiques.PROJET, Definition.simple("A supprimer " + jeton,
                FixtureChampsSpecifiques.TEXTE));
        UUID garde = champs.champ(FixtureChampsSpecifiques.PROJET, Definition.simple("A garder " + jeton,
                FixtureChampsSpecifiques.TEXTE));
        champs.valeur(champ, projet.getId(), "V1");
        champs.valeur(champ, autre.getId(), "V2");
        champs.valeur(garde, projet.getId(), "V3");
        relire();

        int statut = mockMvc.perform(delete(DEFINITION, PROJET, champ).with(connecte(admin)))
                .andReturn().getResponse().getStatus();

        assertAll(
                () -> assertThat(statut).isBetween(200, 299),
                () -> assertThat(champs.champExiste(champ)).isFalse(),
                () -> assertThat(champs.valeursDuChamp(champ)).isZero(),
                () -> assertThat(champs.valeurs(garde, projet.getId())).as("autre champ intact").hasSize(1));
    }

    // ------------------------------------------------------------------ réponse 8 : formats des valeurs

    @Test
    void r8_date_iso_ecritDateValue() throws Exception {
        Porteur p = porteur(TRANCHE);
        UUID champ = champs.champ(p.codeEntite(), Definition.simple("Date " + jeton, FixtureChampsSpecifiques.DATE));

        ecrire(p, champ, "2026-05-12").andExpect(status().isOk());

        assertThat(champs.valeurs(champ, p.id())).singleElement().satisfies(ligne -> assertAll(
                () -> assertThat(ligne.get("Value")).isNotNull(),
                () -> assertThat(ligne.get("DateValue").toString()).startsWith("2026-05-12")));
    }

    @Test
    void r8_booleen_true_ecritBoolValue() throws Exception {
        Porteur p = porteur(TRANCHE);
        UUID champ = champs.champ(p.codeEntite(), Definition.simple("Booleen " + jeton, FixtureChampsSpecifiques.BOOLEEN));

        ecrire(p, champ, "true").andExpect(status().isOk());

        assertThat(champs.valeurs(champ, p.id())).singleElement()
                .satisfies(ligne -> assertThat(ligne).containsEntry("BoolValue", Boolean.TRUE));
    }

    @Test
    void r8_pourcentage_enFraction_ecritDecimalValue() throws Exception {
        Porteur p = porteur(TRANCHE);
        UUID champ = champs.champ(p.codeEntite(), Definition.simple("Taux " + jeton, FixtureChampsSpecifiques.POURCENTAGE));

        ecrire(p, champ, "0.2").andExpect(status().isOk());

        assertThat(champs.valeurs(champ, p.id())).singleElement()
                .satisfies(ligne -> assertThat((BigDecimal) ligne.get("DecimalValue")).isEqualByComparingTo("0.2"));
    }

    @Test
    void r8_dateInvalide_400_nommeLeChamp() throws Exception {
        Porteur p = porteur(TRANCHE);
        String libelle = "Date livraison " + jeton;
        UUID champ = champs.champ(p.codeEntite(), Definition.simple(libelle, FixtureChampsSpecifiques.DATE));

        ecrire(p, champ, "12/05/2026").andExpect(status().isBadRequest())
                .andExpect(jsonPath(DETAIL, containsString(libelle)));
    }

    /** Liste : choix séparés par « ; » dans {@code ComboValue} ; la valeur ne va que dans {@code Value}. */
    @Test
    void r8_liste_choixSeparesParPointVirgule_valueSeule() throws Exception {
        Porteur p = porteur(PROJET);
        String libelle = "Couleurs " + jeton;
        UUID champ = champs.champ(p.codeEntite(), new Definition("G", libelle, FixtureChampsSpecifiques.LISTE, null,
                false, 1, 1, "ROUGE;VERT"));

        ecrire(p, champ, "BLEU").andExpect(status().isBadRequest()).andExpect(jsonPath(DETAIL, containsString(libelle)));
        ecrire(p, champ, "VERT").andExpect(status().isOk());

        assertThat(champs.valeurs(champ, p.id())).singleElement().satisfies(ligne -> assertAll(
                () -> assertThat(ligne).containsEntry("Value", "VERT"),
                () -> assertThat(ligne.get("ComboValue")).isNull()));
    }

    // ------------------------------------------------------------------ réponse 9 : champ étranger, tout ou rien

    @Test
    void r9_champDUnAutrePorteur_400_rienEcrit() throws Exception {
        Porteur p = porteur(PROJET);
        UUID champTranche = champs.champ(FixtureChampsSpecifiques.TRANCHE, Definition.simple("Etranger " + jeton,
                FixtureChampsSpecifiques.TEXTE));

        ecrire(p, champTranche, "X").andExpect(status().isBadRequest());

        assertThat(champs.valeursDeLObjet(p.id())).isZero();
    }

    @Test
    void r9_unChampValideEtUnInvalide_400_toutOuRien() throws Exception {
        Porteur p = porteur(PROJET);
        UUID valide = champs.champ(p.codeEntite(), Definition.simple("Texte ok " + jeton, FixtureChampsSpecifiques.TEXTE));
        UUID invalide = champs.champ(p.codeEntite(), Definition.simple("Decimal ko " + jeton,
                FixtureChampsSpecifiques.DECIMAL));
        Map<UUID, String> valeurs = new LinkedHashMap<>();
        valeurs.put(valide, "BON");
        valeurs.put(invalide, "abc");

        ecrire(p.fil(), p.id(), corps(valeurs), connecte(admin)).andExpect(status().isBadRequest());

        assertThat(champs.valeursDeLObjet(p.id())).isZero();
    }

    // ------------------------------------------------------------------ C4 : définitions, FindOrCreate de l'entité

    private static String definition(String libelle, String type, String choix) {
        return """
                {"categorie":"GROUPE C4","ordreCategorie":2,"libelle":"%s","ordre":3,"typeDonnee":"%s",\
                "valeurDefaut":"DEF","obligatoire":true,"choix":%s}""".formatted(libelle, type, choix);
    }

    private int creerDefinition(String porteur, String corps) throws Exception {
        return mockMvc.perform(post(DEFINITIONS, porteur).with(connecte(admin))
                        .contentType(MediaType.APPLICATION_JSON).content(corps))
                .andReturn().getResponse().getStatus();
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {PROSPECTION_TERRAIN, TERRAIN})
    void c4_premiereDefinition_creeLaLigneDEntite_codeOmran(String cas) throws Exception {
        Porteur p = porteur(cas);
        String libelle = "C4 " + cas + " " + jeton;

        int statut = creerDefinition(p.fil(), definition(libelle, FixtureChampsSpecifiques.TEXTE, "[]"));

        assertAll(
                () -> assertThat(statut).isBetween(200, 299),
                () -> assertThat(champs.entitesDeCode(p.codeEntite())).isEqualTo(1),
                () -> assertThat(champs.definitions(libelle)).singleElement()
                        .satisfies(d -> assertThat(d).containsEntry("Entite", p.codeEntite())));
        relire();
        assertThat(lire(p.fil(), p.id())).contains(libelle);
    }

    /** Étude : code d'entité non figé (réponse 3) ; la définition est rattachée à une ligne d'entité créée et se relit. */
    @Test
    void c4_etude_premiereDefinition_rattacheeAUneEntiteEtRelue() throws Exception {
        UUID etude = etude();
        String libelle = "C4 etude " + jeton;
        relire();

        int statut = creerDefinition(ETUDE, definition(libelle, FixtureChampsSpecifiques.TEXTE, "[]"));

        assertAll(
                () -> assertThat(statut).isBetween(200, 299),
                () -> assertThat(champs.definitions(libelle)).hasSize(1));
        relire();
        assertThat(lire(ETUDE, etude)).contains(libelle);
    }

    @Test
    void c4_corpsDeLaDefinition_colonnesDeProd() throws Exception {
        String libelle = "C4 liste " + jeton;

        int statut = creerDefinition(PROJET, definition(libelle, FixtureChampsSpecifiques.LISTE, "[\"ROUGE\",\"VERT\"]"));

        assertThat(statut).isBetween(200, 299);
        assertThat(champs.definitions(libelle)).singleElement().satisfies(d -> assertAll(
                () -> assertThat(d).containsEntry("Entite", FixtureChampsSpecifiques.PROJET),
                () -> assertThat(d).containsEntry("Category", "GROUPE C4"),
                () -> assertThat(d).containsEntry("TypeDonnee", FixtureChampsSpecifiques.LISTE),
                () -> assertThat(d).containsEntry("DefaultValue", "DEF"),
                () -> assertThat(d).containsEntry("Required", Boolean.TRUE),
                () -> assertThat(((Number) d.get("CategorySorting")).intValue()).isEqualTo(2),
                () -> assertThat(((Number) d.get("Sorting")).intValue()).isEqualTo(3),
                () -> assertThat(d).containsEntry("ComboValue", "ROUGE;VERT")));
    }

    @Test
    void c4_patch_modifieLaDefinition() throws Exception {
        UUID champ = champs.champ(FixtureChampsSpecifiques.PROJET, Definition.simple("Avant " + jeton,
                FixtureChampsSpecifiques.TEXTE));
        String libelle = "Apres " + jeton;
        relire();

        int statut = mockMvc.perform(patch(DEFINITION, PROJET, champ).with(connecte(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(definition(libelle, FixtureChampsSpecifiques.TEXTE, "[]")))
                .andReturn().getResponse().getStatus();

        assertThat(statut).isBetween(200, 299);
        assertThat(champs.definitions(libelle)).singleElement()
                .satisfies(d -> assertThat(d).containsEntry("PKey", champ.toString().toUpperCase(Locale.ROOT)));
    }

    /** Les lignes du fil citent le champ par son id (sinon le PUT {@code champId} est inutilisable). */
    @Test
    void lecture_citeLIdDuChamp() throws Exception {
        UUID champ = champs.champ(FixtureChampsSpecifiques.PROJET, Definition.simple("Avec id " + jeton,
                FixtureChampsSpecifiques.TEXTE));
        relire();

        assertThat(lire(PROJET, projet.getId()).toLowerCase(Locale.ROOT))
                .contains(champ.toString().toLowerCase(Locale.ROOT));
    }
}
