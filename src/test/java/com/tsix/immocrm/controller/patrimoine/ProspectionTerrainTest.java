package com.tsix.immocrm.controller.patrimoine;

import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.fixtures.ApiProspectionsTerrain;
import com.tsix.immocrm.fixtures.FixturePatrimoine;
import com.tsix.immocrm.fixtures.FixtureProspectionsTerrain;
import com.tsix.immocrm.fixtures.FixtureProspectionsTerrain.Contexte;
import com.tsix.immocrm.fixtures.FixtureProspectionsTerrain.Statut;
import com.jayway.jsonpath.JsonPath;
import jakarta.persistence.Tuple;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.ApiProspectionsTerrain.corpsProspection;
import static com.tsix.immocrm.fixtures.ApiProspectionsTerrain.d;
import static com.tsix.immocrm.fixtures.ApiProspectionsTerrain.lireDecimal;
import static com.tsix.immocrm.fixtures.CorpsJson.corps;
import static com.tsix.immocrm.fixtures.FixturePatrimoine.decimal;
import static com.tsix.immocrm.fixtures.FixturePatrimoine.guid;
import static com.tsix.immocrm.fixtures.FixtureProspectionsTerrain.*;
import static com.tsix.immocrm.fixtures.FixtureTerrains.horodatage;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Prospection terrain — FEATURE-patrimoine-prospections-terrain.md §2, §3.1, §4, §5.1-§5.3 (ProspectionTerrainTest) :
 * création (colonnes une à une, défauts OMRAN, statut initial lu en base, StrHistory « Création »), lecture, liste,
 * modification (calculs, champsAVider, archivage BeforeUpdate), 404, 409 code déjà pris, configuration manquante → 500.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ProspectionTerrainTest {

    private static final String HISTORIQUE_SQL =
            "SELECT * FROM StrHistory WHERE ObjectId = :id ORDER BY DocDate ASC";
    private static final UUID INCONNU = UUID.fromString("0F1E2D3C-4B5A-6978-8796-A5B4C3D2E1F0");

    @Autowired private MockMvc mockMvc;
    @Autowired private FixturePatrimoine patrimoine;
    @Autowired private FixtureProspectionsTerrain fixture;
    @Autowired private Clock clock;

    private AdmUser admin;
    private ApiProspectionsTerrain api;
    private Contexte c;

    @BeforeEach
    void setUp() {
        admin = patrimoine.administrateur();
        api = new ApiProspectionsTerrain(mockMvc, admin);
        c = fixture.contexte("P7B");
        fixture.formule("MA");
    }

    private String code(String suffixe) {
        return suffixe + "-" + c.jeton();
    }

    private Tuple ligne(UUID id) {
        return fixture.ligne(id);
    }

    // ------------------------------------------------------------------ §2 création

    /** §2 et §5.3 : chaque champ du corps écrit dans sa colonne ; calculs §3.1 stockés ; StrEntityId = entité LndLand. */
    @Test
    void r2_creation_201_colonnesEcritesUneAUne() throws Exception {
        UUID id = api.creerOk(corpsProspection(c, code("CRE"))
                .avec(COURTIER_ID, c.courtier()).avec(NOTAIRE_ID, c.notaire()).avec(ORIENTATION_ID, c.orientation())
                .avec(LIEN_GOOGLE, "https://www.google.com/maps/@33.5,-7.6").avec(ADRESSE, "12 route cotiere")
                .avec(PROPRIETAIRES, "Proprietaire A").avec(NOTES, "Notes P7b").avec(ANOMALIES, "Voisinage")
                .avec(ARRETE_ALIGNEMENT, 4).avec(HAUTEUR_MAX, 30).avec(NUMERO_ENREGISTREMENT, "RG-1")
                .avec(LIEU_ENREGISTREMENT, "Casablanca").avec(CPF_IMMATRICULATION, "CPF-1")
                .avec(IDENTIFICATION_ZONE, "Zone A").avec(CONDITIONS_RACCORDEMENT, "Raccordement")
                .avec(ASSIETTE_FONCIERE, "Assiette").avec(LEVEE_TOPOGRAPHIQUE, true));

        Tuple t = ligne(id);
        assertAll(
                () -> assertThat(t.get("Code")).isEqualTo(code("CRE")),
                () -> assertThat(t.get("Description")).isEqualTo("Prospection " + code("CRE")),
                () -> assertThat(guid(t.get("LndCategoryId"))).isEqualTo(guid(c.categorie())),
                () -> assertThat(guid(t.get("LndTypeId"))).isEqualTo(guid(c.type())),
                () -> assertThat(guid(t.get("LndLevelId"))).isEqualTo(guid(c.niveau())),
                () -> assertThat(guid(t.get("LndVocationId"))).isEqualTo(guid(c.vocation())),
                () -> assertThat(guid(t.get("AdmCountryId"))).isEqualTo(guid(c.pays().getId())),
                () -> assertThat(guid(t.get("AdmCityId"))).isEqualTo(guid(c.ville())),
                () -> assertThat(guid(t.get("CfgMiddlemanId"))).isEqualTo(guid(c.courtier())),
                () -> assertThat(guid(t.get("CfgLawyerId"))).isEqualTo(guid(c.notaire())),
                () -> assertThat(guid(t.get("StkOrientationId"))).isEqualTo(guid(c.orientation())),
                () -> assertThat(t.get("GmapLink")).isEqualTo("https://www.google.com/maps/@33.5,-7.6"),
                () -> assertThat(t.get("Adress")).isEqualTo("12 route cotiere"),
                () -> assertThat(t.get("Owner")).isEqualTo("Proprietaire A"),
                () -> assertThat(t.get("Notes")).isEqualTo("Notes P7b"),
                () -> assertThat(t.get("Anomalies")).isEqualTo("Voisinage"),
                () -> assertThat(((Number) t.get("AlinmentStop")).intValue()).isEqualTo(4),
                () -> assertThat(((Number) t.get("MaxHeight")).intValue()).isEqualTo(30),
                () -> assertThat(t.get("RegistryNumber")).isEqualTo("RG-1"),
                () -> assertThat(t.get("RegistryPlace")).isEqualTo("Casablanca"),
                () -> assertThat(t.get("ImmatriculationCPF")).isEqualTo("CPF-1"),
                () -> assertThat(t.get("ZoneIdentification")).isEqualTo("Zone A"),
                () -> assertThat(t.get("DealerConnectionConditions")).isEqualTo("Raccordement"),
                () -> assertThat(t.get("LandBase")).isEqualTo("Assiette"),
                () -> assertThat(t.get("IsTopographicSurvey")).isEqualTo(true),
                () -> assertThat(t.get("IsFinished")).isEqualTo(true),
                () -> assertThat(t.get("IsInArchive")).isEqualTo(false),
                () -> assertThat(t.get("IsTransformedToLand")).isEqualTo(false),
                () -> assertThat(decimal(t.get("Area"))).isEqualByComparingTo("1200"),
                () -> assertThat(decimal(t.get("EquipementsArea"))).isEqualByComparingTo("100"),
                () -> assertThat(decimal(t.get("AreaNet"))).isEqualByComparingTo("1100"),
                () -> assertThat(decimal(t.get("EquipementsCoef"))).isEqualByComparingTo("0.090909"),
                () -> assertThat(decimal(t.get("Cos"))).isEqualByComparingTo("0.3"),
                () -> assertThat(decimal(t.get("AreaCos"))).isEqualByComparingTo("330"),
                () -> assertThat(decimal(t.get("Cuf"))).isEqualByComparingTo("2.5"),
                () -> assertThat(decimal(t.get("AreaCuf"))).isEqualByComparingTo("2750"),
                () -> assertThat(decimal(t.get("CoefSaleableArea"))).isEqualByComparingTo("1"),
                () -> assertThat(decimal(t.get("SalableArea"))).isEqualByComparingTo("2750"),
                () -> assertThat(decimal(t.get("HabitatArea"))).isEqualByComparingTo("2750"),
                () -> assertThat(decimal(t.get("ProspectionUnitCost"))).isEqualByComparingTo("4000"),
                () -> assertThat(decimal(t.get("ProspectionPrice"))).isEqualByComparingTo("4800000"),
                () -> assertThat(decimal(t.get("ProspectionPriceProposed"))).isEqualByComparingTo("4000000"),
                () -> assertThat(guid(t.get("StrEntityId"))).isEqualTo(guid(fixture.entiteLndLand())),
                () -> assertThat(t.get("UserCreate")).isEqualTo(FixturePatrimoine.AUTEUR));
    }

    /**
     * §2 défauts du constructeur OMRAN : DocDate = aujourd'hui (minuit), Area = 1, IsFinished vrai, pays = DefaultCountry,
     * textes '' (jamais NULL), décimaux 0, booléens faux, dates facultatives NULL.
     */
    @Test
    void r2_creation_defautsDuConstructeur() throws Exception {
        fixture.paysParDefaut(c.pays().getId());

        UUID id = api.creerOk(corps().avec(CODE, code("DEF")).avec(DESCRIPTION, "Defauts")
                .avec(CATEGORIE_ID, c.categorie()).avec(TYPE_ID, c.type()).avec(PAYS_ID, c.pays().getId())
                .avec(VILLE_ID, c.ville()));

        Tuple t = ligne(id);
        assertAll(
                () -> assertThat(horodatage(t.get("DocDate"))).isEqualTo(LocalDate.now(clock).atStartOfDay()),
                () -> assertThat(decimal(t.get("Area"))).isEqualByComparingTo("1"),
                () -> assertThat(t.get("IsFinished")).isEqualTo(true),
                () -> assertThat(t.get("IsInArchive")).isEqualTo(false),
                () -> assertThat(t.get("IsTransformedToLand")).isEqualTo(false),
                () -> assertThat(t.get("IsTopographicSurvey")).isEqualTo(false),
                () -> assertThat(t.get("Notes")).isEqualTo(""),
                () -> assertThat(t.get("Anomalies")).isEqualTo(""),
                () -> assertThat(t.get("Adress")).isEqualTo(""),
                () -> assertThat(t.get("Owner")).isEqualTo(""),
                () -> assertThat(t.get("RegistryNumber")).isEqualTo(""),
                () -> assertThat(t.get("ImmatriculationCPF")).isEqualTo(""),
                () -> assertThat(t.get("ZoneIdentification")).isEqualTo(""),
                () -> assertThat(t.get("DealerConnectionConditions")).isEqualTo(""),
                () -> assertThat(t.get("LandBase")).isEqualTo(""),
                () -> assertThat(decimal(t.get("Cos"))).isEqualByComparingTo("0"),
                () -> assertThat(decimal(t.get("ProspectionPriceProposed"))).isEqualByComparingTo("0"),
                () -> assertThat(t.get("RegistryDate")).isNull(),
                () -> assertThat(t.get("ImmatriculationDate")).isNull(),
                () -> assertThat(t.get("CfgMiddlemanId")).isNull());
    }

    /** §2 : pays absent du corps → AdmParamsGuid.DefaultCountry (constructeur Prospection.cs:1923). */
    @Test
    void r2_creation_paysAbsent_paysParDefaut() throws Exception {
        fixture.paysParDefaut(c.pays().getId());

        UUID id = api.creerOk(corpsProspection(c, code("PPD")).sans(PAYS_ID));

        assertThat(guid(ligne(id).get("AdmCountryId"))).isEqualTo(guid(c.pays().getId()));
    }

    /** §2 : statut initial = premier statut de type « Nouveau » de l'entité, lu en base (un statut de test plus tôt l'emporte). */
    @Test
    void r2_statutInitial_premierStatutDeTypeNouveau_luEnBase() throws Exception {
        UUID premier = fixture.statut(code("NV"), -999_999, TYPE_NOUVEAU, Statut.ordinaire());
        fixture.statut(code("SUC"), -1_000_000, TYPE_SUCCES, Statut.ordinaire());

        UUID id = api.creerOk(corpsProspection(c, code("STI")));

        assertThat(guid(ligne(id).get("StrStatusId"))).isEqualTo(guid(premier));
    }

    /** §1.2 / §4 : la création écrit la ligne StrHistory « Création » au statut initial, entité LndLand. */
    @Test
    void r4_creation_historiqueCreation() throws Exception {
        UUID id = api.creerOk(corpsProspection(c, code("HIS")));

        List<Tuple> lignes = fixture.lignes(HISTORIQUE_SQL, id);
        assertThat(lignes).hasSize(1);
        Tuple h = lignes.get(0);
        assertAll(
                () -> assertThat(h.get("Description")).isEqualTo("Création"),
                () -> assertThat(guid(h.get("StrStatusId"))).isEqualTo(guid(STATUT_EN_COURS)),
                () -> assertThat(h.get("StrStatusDescription")).isEqualTo(EN_COURS),
                () -> assertThat(h.get("StrWorkflowId")).isNull(),
                () -> assertThat(guid(h.get("AdmUserId"))).isEqualTo(guid(admin.getId())),
                () -> assertThat(guid(h.get("StrConfigEntityId"))).isEqualTo(guid(fixture.entiteLndLand())));
    }

    /** §4 validité : statut initial avec Delay > 0 → StatusValidityDate = DocDate + Delay jours. */
    @Test
    void r4_validite_creation_statutAvecDuree_dateDuJourPlusDuree() throws Exception {
        fixture.statut(code("DUR"), -999_999, TYPE_NOUVEAU, new Statut(false, true, false, 30));

        UUID id = api.creerOk(corpsProspection(c, code("VAL")).avec(DATE, "2026-10-01"));

        assertThat(horodatage(ligne(id).get("StatusValidityDate"))).isEqualTo(LocalDate.of(2026, 10, 31).atStartOfDay());
    }

    /** §4 validité : Delay = 0 → la date validité saisie reste telle quelle. */
    @Test
    void r4_validite_creation_statutSansDuree_dateSaisieGardee() throws Exception {
        UUID id = api.creerOk(corpsProspection(c, code("VSD")).avec(DATE_VALIDITE_STATUT, "2026-12-15"));

        assertThat(horodatage(ligne(id).get("StatusValidityDate")).toLocalDate()).isEqualTo(LocalDate.of(2026, 12, 15));
    }

    /** §2 : aucun statut « Nouveau » pour l'entité → ConfigurationManquanteException (500), rien d'écrit. */
    @Test
    void r2_aucunStatutNouveau_500_rienEcrit() throws Exception {
        fixture.executer("""
                UPDATE StrStatus SET StrStatusTypeId = 'D572CE4B-A4DD-4F38-8998-359EAB5A9FCA'
                WHERE StrConfigEntityId IN (SELECT PKey FROM StrEntity WHERE Code = N'LndLand')""");

        api.creer(corpsProspection(c, code("SSN"))).andExpect(status().isInternalServerError());

        assertThat(fixture.prospectionsDeCode(code("SSN"))).isZero();
    }

    /** §2 : entité de workflow LndLand absente → 500, rien d'écrit (StrEntityId jamais figé). */
    @Test
    void r2_entiteLndLandAbsente_500_rienEcrit() throws Exception {
        fixture.executer("UPDATE StrEntity SET Code = N'LndLand-masquee' WHERE Code = N'LndLand'");

        api.creer(corpsProspection(c, code("SEN"))).andExpect(status().isInternalServerError());

        assertThat(fixture.prospectionsDeCode(code("SEN"))).isZero();
    }

    /** §3.3 n° 4 : code unique insensible à la casse → 409 « Le code X est déjà pris par une prospection terrain. ». */
    @Test
    void r3_3_codeDejaPris_insensibleALaCasse_409() throws Exception {
        fixture.prospection(c, code("DBL"));

        api.creer(corpsProspection(c, code("dbl").toLowerCase()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(containsString("est déjà pris par une prospection terrain")));

        assertThat(fixture.prospectionsDeCode(code("DBL"))).isEqualTo(1);
    }

    /** §3.3 n° 4 : PATCH vers le code d'une autre prospection → 409, ligne inchangée. */
    @Test
    void r3_3_patch_codeDUneAutre_409() throws Exception {
        fixture.prospection(c, code("PRI"));
        UUID id = fixture.prospection(c, code("AUT"));

        api.modifier(id, corps().avec(CODE, code("PRI"))).andExpect(status().isConflict());

        assertThat(ligne(id).get("Code")).isEqualTo(code("AUT"));
    }

    // ------------------------------------------------------------------ §5.2 lecture

    /** §5.2 : la fiche expose les champs du fil, statut {id, code, description}, couleur, type, zones, terrain nul. */
    @Test
    void r5_2_fiche_200_champsDuFil() throws Exception {
        UUID id = fixture.prospection(c, code("FIC"));

        String json = api.ficheJson(id);

        assertAll(
                () -> assertThat(JsonPath.<String>read(json, "$.code")).isEqualTo(code("FIC")),
                () -> assertThat(JsonPath.<String>read(json, "$.statut.id"))
                        .isEqualToIgnoringCase(STATUT_EN_COURS.toString()),
                () -> assertThat(JsonPath.<String>read(json, "$.statut.code")).isEqualTo(EN_COURS),
                () -> assertThat(JsonPath.<String>read(json, "$.statutCouleur")).isEqualTo(COULEUR),
                () -> assertThat(JsonPath.<String>read(json, "$.categorieId"))
                        .isEqualToIgnoringCase(c.categorie().toString()),
                () -> assertThat(JsonPath.<String>read(json, "$.villeId"))
                        .isEqualToIgnoringCase(c.ville().toString()),
                () -> assertThat(lireDecimal(json, "$." + SUPERFICIE)).isEqualByComparingTo("1200"),
                () -> assertThat(lireDecimal(json, "$." + SUPERFICIE_NETTE)).isEqualByComparingTo("1100"),
                () -> assertThat(lireDecimal(json, "$." + SUPERFICIE_COS)).isEqualByComparingTo("330"),
                () -> assertThat(lireDecimal(json, "$." + PRIX)).isEqualByComparingTo("4800000"),
                () -> assertThat(lireDecimal(json, "$." + PRIX_PROPOSE)).isEqualByComparingTo("4000000"),
                () -> assertThat(JsonPath.<Boolean>read(json, "$.estAmenage")).isTrue(),
                () -> assertThat(JsonPath.<Boolean>read(json, "$.estArchivee")).isFalse(),
                () -> assertThat(JsonPath.<Boolean>read(json, "$.estTransformee")).isFalse(),
                () -> assertThat(JsonPath.<List<Object>>read(json, "$.zones")).isEmpty(),
                () -> assertThat(JsonPath.<Object>read(json, "$.terrainId")).isNull());
    }

    /** §5.7 : statut sans BoolField001 (toute la production) → peutTransformer faux avec le motif. */
    @Test
    void r5_7_fiche_statutSansBoolField001_peutTransformerFaux_motif() throws Exception {
        UUID id = fixture.prospection(c, code("PTF"));

        api.fiche(id)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.peutTransformer").value(false))
                .andExpect(jsonPath("$.motifNonTransformable").value(containsString(MESSAGE_PAS_TRANSFORMABLE)));
    }

    /** §5.7 : statut avec BoolField001, Area > 0, pas de terrain, Administrateur → peutTransformer vrai. */
    @Test
    void r5_7_fiche_statutTransformable_peutTransformerVrai() throws Exception {
        UUID statut = fixture.statut(code("TRF"), 250, TYPE_SUCCES, new Statut(false, true, true, 0));
        UUID id = fixture.prospection(c, code("PTV"), statut);

        api.fiche(id).andExpect(status().isOk()).andExpect(jsonPath("$.peutTransformer").value(true));
    }

    // ------------------------------------------------------------------ §5.1 liste

    private static String ligneListe(UUID id) {
        return "$.content[?(@.id=='" + id + "')]";
    }

    /** §5.1 : recherche LIKE sur le code ; filtre archivee tri-état (absent = toutes). */
    @Test
    void r5_1_liste_rechercheEtArchiveeTriEtat() throws Exception {
        UUID active = fixture.prospection(c, code("LSA"));
        UUID archivee = fixture.prospection(c, code("LSR"));
        fixture.definir(archivee, "IsInArchive", true);

        api.liste(Map.of("search", c.jeton()))
                .andExpect(status().isOk())
                .andExpect(jsonPath(ligneListe(active)).isNotEmpty())
                .andExpect(jsonPath(ligneListe(archivee)).isNotEmpty());
        api.liste(Map.of("search", c.jeton(), "archivee", "false"))
                .andExpect(jsonPath(ligneListe(active)).isNotEmpty())
                .andExpect(jsonPath(ligneListe(archivee)).isEmpty());
        api.liste(Map.of("search", c.jeton(), "archivee", "true"))
                .andExpect(jsonPath(ligneListe(active)).isEmpty())
                .andExpect(jsonPath(ligneListe(archivee)).isNotEmpty());
    }

    /** §5.1 : recherche sur la ville (ProspectionItem.GetList l.512-523) et filtre statutId. */
    @Test
    void r5_1_liste_rechercheSurLaVille_filtreStatut() throws Exception {
        UUID enCours = fixture.prospection(c, code("LVE"));
        UUID rejetee = fixture.prospection(c, code("LVR"), STATUT_REJETE);

        api.liste(Map.of("search", "Ville VIL-" + c.jeton()))
                .andExpect(status().isOk())
                .andExpect(jsonPath(ligneListe(enCours)).isNotEmpty());
        api.liste(Map.of("search", c.jeton(), "statutId", STATUT_REJETE.toString()))
                .andExpect(jsonPath(ligneListe(rejetee)).isNotEmpty())
                .andExpect(jsonPath(ligneListe(enCours)).isEmpty());
    }

    /** §5.1 : nbEtudesFaisabilite = COUNT(StkFeasibilityStudy.LndLandId) ; ligne au fil (statut, couleur, superficie). */
    @Test
    void r5_1_liste_nbEtudesFaisabiliteEtColonnes() throws Exception {
        UUID id = fixture.prospection(c, code("LNE"));
        fixture.etudeLiee(id, code("ETU1"));
        fixture.etudeLiee(id, code("ETU2"));

        api.liste(Map.of("search", c.jeton()))
                .andExpect(status().isOk())
                .andExpect(jsonPath(ligneListe(id) + ".nbEtudesFaisabilite").value(2))
                .andExpect(jsonPath(ligneListe(id) + ".statut.code").value(EN_COURS))
                .andExpect(jsonPath(ligneListe(id) + ".statutCouleur").value(COULEUR))
                .andExpect(jsonPath(ligneListe(id) + ".ville.id").value(c.ville().toString()));
    }

    /** §5.1 : tri par défaut = code croissant. */
    @Test
    void r5_1_liste_triParDefautCodeCroissant() throws Exception {
        UUID b = fixture.prospection(c, "B-" + c.jeton());
        UUID a = fixture.prospection(c, "A-" + c.jeton());

        api.liste(Map.of("search", c.jeton()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(a.toString()))
                .andExpect(jsonPath("$.content[1].id").value(b.toString()));
    }

    // ------------------------------------------------------------------ §5.3 modification

    /** §5.3 / §3.1 : PATCH de l'emprise seule (montant sans coefficient) → COS dérivé 440 / 1 100 = 0,40. */
    @Test
    void r3_1_patch_empriseSeule_cosDerive() throws Exception {
        UUID id = fixture.prospection(c, code("PEM"));

        api.modifier(id, corps().avec(SUPERFICIE_COS, d("440"))).andExpect(status().isOk());

        Tuple t = ligne(id);
        assertAll(
                () -> assertThat(decimal(t.get("Cos"))).isEqualByComparingTo("0.4"),
                () -> assertThat(decimal(t.get("AreaCos"))).isEqualByComparingTo("440"));
    }

    /** §3.1 : PATCH de la superficie → montants recalculés (net, emprise, plancher, prix total). */
    @Test
    void r3_1_patch_superficie_montantsRecalcules() throws Exception {
        UUID id = fixture.prospection(c, code("PSU"));

        api.modifier(id, corps().avec(SUPERFICIE, d("1500"))).andExpect(status().isOk());

        Tuple t = ligne(id);
        assertAll(
                () -> assertThat(decimal(t.get("AreaNet"))).isEqualByComparingTo("1400"),
                () -> assertThat(decimal(t.get("AreaCos"))).isEqualByComparingTo("420"),
                () -> assertThat(decimal(t.get("AreaCuf"))).isEqualByComparingTo("3500"),
                () -> assertThat(decimal(t.get("ProspectionPrice"))).isEqualByComparingTo("6000000"));
    }

    /** §5.3 : coefEquipements, superficieNette, superficieHabitat sont en lecture seule (envoyés → ignorés). */
    @Test
    void r5_3_patch_champsCalculesEnvoyes_ignores() throws Exception {
        UUID id = fixture.prospection(c, code("PLS"));

        api.modifier(id, corps().avec(SUPERFICIE_NETTE, d("9")).avec(COEF_EQUIPEMENTS, d("0.5"))
                .avec(SUPERFICIE_HABITAT, d("7"))).andExpect(status().isOk());

        Tuple t = ligne(id);
        assertAll(
                () -> assertThat(decimal(t.get("AreaNet"))).isEqualByComparingTo("1100"),
                () -> assertThat(decimal(t.get("EquipementsCoef"))).isEqualByComparingTo("0.090909"),
                () -> assertThat(decimal(t.get("HabitatArea"))).isEqualByComparingTo("2750"));
    }

    /** §5.3 : champsAVider vide une FK facultative ; "" vide un texte ; le statut n'est jamais saisi. */
    @Test
    void r5_3_patch_champsAViderEtTexteVide_statutIgnore() throws Exception {
        UUID id = fixture.prospection(c, code("PCV"));

        api.modifier(id, corps().avec(CHAMPS_A_VIDER, List.of(COURTIER_ID)).avec(NOTES, "")
                .avec("statutId", STATUT_VALIDE)).andExpect(status().isOk());

        Tuple t = ligne(id);
        assertAll(
                () -> assertThat(t.get("CfgMiddlemanId")).isNull(),
                () -> assertThat(t.get("Notes")).isEqualTo(""),
                () -> assertThat(guid(t.get("StrStatusId"))).isEqualTo(guid(STATUT_EN_COURS)));
    }

    /** §4 BeforeUpdate : PATCH d'une prospection en statut d'échec → IsInArchive reposé à 1. */
    @Test
    void r4_beforeUpdate_statutEchec_patchRearchive() throws Exception {
        UUID id = fixture.prospection(c, code("BUE"), STATUT_NON_ABOUTIE);

        api.modifier(id, corps().avec(DESCRIPTION, "Renommee")).andExpect(status().isOk());

        assertThat(ligne(id).get("IsInArchive")).isEqualTo(true);
    }

    /** §13 décision 5 : le corps explicite l'emporte — estArchivee=false sur une prospection « Rejeté » la désarchive. */
    @Test
    void d13_5_beforeUpdate_corpsExplicite_lEmporte() throws Exception {
        UUID id = fixture.prospection(c, code("BUC"), STATUT_REJETE);
        fixture.definir(id, "IsInArchive", true);

        api.modifier(id, corps().avec(EST_ARCHIVEE, false)).andExpect(status().isOk());

        assertThat(ligne(id).get("IsInArchive")).isEqualTo(false);
    }

    /** §4 : statut courant IsReadOnly → PATCH 409, ligne inchangée. */
    @Test
    void r4_statutLectureSeule_patch_409() throws Exception {
        UUID statut = fixture.statut(code("RO"), 250, TYPE_SUCCES, new Statut(true, true, false, 0));
        UUID id = fixture.prospection(c, code("PRO"), statut);

        api.modifier(id, corps().avec(DESCRIPTION, "Interdit"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(containsString(MESSAGE_LECTURE_SEULE)));

        assertThat(ligne(id).get("Description")).isEqualTo("Prospection " + code("PRO"));
    }

    /** §4 : modifiable librement par PATCH (dateValiditeStatut). */
    @Test
    void r4_validite_patch_dateLibre() throws Exception {
        UUID id = fixture.prospection(c, code("PDV"));

        api.modifier(id, corps().avec(DATE_VALIDITE_STATUT, "2027-01-20")).andExpect(status().isOk());

        assertThat(horodatage(ligne(id).get("StatusValidityDate")).toLocalDate()).isEqualTo(LocalDate.of(2027, 1, 20));
    }

    // ------------------------------------------------------------------ 404

    @Test
    void r5_404_idInconnu_ficheModificationSuppressionZonesTransitionHistorique() throws Exception {
        api.fiche(INCONNU).andExpect(status().isNotFound());
        api.modifier(INCONNU, corps().avec(DESCRIPTION, "x")).andExpect(status().isNotFound());
        api.supprimer(INCONNU).andExpect(status().isNotFound());
        api.zones(INCONNU, List.of()).andExpect(status().isNotFound());
        api.executer(INCONNU, T_EN_COURS_VALIDE).andExpect(status().isNotFound());
        api.transitions(INCONNU).andExpect(status().isNotFound());
        api.historique(INCONNU).andExpect(status().isNotFound());
    }
}
