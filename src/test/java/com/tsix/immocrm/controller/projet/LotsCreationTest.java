package com.tsix.immocrm.controller.projet;

import com.tsix.immocrm.controller.projet.LotsPariteSupport.Creation;
import com.tsix.immocrm.fixtures.CorpsJson;
import com.tsix.immocrm.fixtures.FixtureLotsParite;
import com.tsix.immocrm.fixtures.FixtureProjetConfig;
import com.tsix.immocrm.fixtures.FixtureProjetLots;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import static com.tsix.immocrm.controller.projet.LotsPariteSupport.CATEGORIE_ID;
import static com.tsix.immocrm.controller.projet.LotsPariteSupport.DESCRIPTION;
import static com.tsix.immocrm.controller.projet.LotsPariteSupport.FOURNISSEUR_ID;
import static com.tsix.immocrm.controller.projet.LotsPariteSupport.GENRE;
import static com.tsix.immocrm.controller.projet.LotsPariteSupport.PROJET_ID;
import static com.tsix.immocrm.controller.projet.LotsPariteSupport.TYPE_ID;
import static com.tsix.immocrm.fixtures.CorpsJson.corps;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static com.tsix.immocrm.fixtures.FixtureLotsParite.ENTREPRENEURS;
import static com.tsix.immocrm.fixtures.FixtureLotsParite.FOURNISSEURS;
import static com.tsix.immocrm.fixtures.FixtureLotsParite.GENRES;
import static com.tsix.immocrm.fixtures.FixtureLotsParite.TYPE_STATUT_NOUVEAU;
import static com.tsix.immocrm.fixtures.FixturePatrimoine.guid;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * E5 — FEATURE-lots-liste-parite.md : création d'un lot (Administrateur = tous les droits ; droits seuls dans
 * {@code LotsDroitsTest}). Code {@code yy/nnnnn} de la séquence 'Market' avec garde de collision, statut de départ et
 * {@code StrEntityId} du genre, {@code IsMultiSupplier}, {@code IncludedInPaymentPrevision}, ligne {@code StrHistory} « Création ».
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class LotsCreationTest {

    private static final String LOT_PAR_ID = "SELECT * FROM PrjMarket WHERE PKey = ?";
    private static final String PAR_DESCRIPTION = "SELECT COUNT(*) FROM PrjMarket WHERE Description = ?";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureLotsParite fixture;
    @Autowired private FixtureProjetLots lots;
    @Autowired private FixtureProjetConfig config;
    @Autowired private Clock clock;

    private LotsPariteSupport appels;
    private String jeton;
    private UUID projet;
    private UUID categorie;
    private UUID type;
    private RequestPostProcessor admin;

    @BeforeEach
    void setUp() {
        appels = new LotsPariteSupport(mockMvc);
        jeton = FixtureProjetLots.jeton();
        fixture.genresDeLot();
        fixture.statutsDepart(jeton);
        fixture.sequenceMarche(ThreadLocalRandom.current().nextInt(50_000, 90_000));
        projet = fixture.projet("CRP-" + jeton, fixture.entiteJuridique("CRE-" + jeton));
        categorie = config.categorie("CAT-" + jeton, false);
        type = config.typeLot("TYP-" + jeton);
        admin = connecte(fixture.administrateur("lots.cre." + jeton, projet), "Administrateur");
    }

    private String libelle() {
        return "Lot cree " + jeton;
    }

    private CorpsJson corpsValide(String genre) {
        return corps().avec(GENRE, genre).avec(DESCRIPTION, libelle()).avec(PROJET_ID, projet)
                .avec(CATEGORIE_ID, categorie).avec(TYPE_ID, type);
    }

    private Map<String, Object> enBase(UUID lot) {
        List<Map<String, Object>> l = fixture.lignes(LOT_PAR_ID, lot.toString());
        assertThat(l).hasSize(1);
        return l.get(0);
    }

    private String annee() {
        return "%02d".formatted(LocalDate.now(clock).getYear() % 100);
    }

    // ------------------------------------------------------------------ 201

    @Test
    void e5_creation_201_location_codeYyNnnnn_champsEcrits() throws Exception {
        UUID fournisseur = lots.fournisseur("CRF-" + jeton);

        Creation c = appels.creer(admin, corpsValide(ENTREPRENEURS).avec(FOURNISSEUR_ID, fournisseur));

        assertThat(c.statut()).as(c.reponse().corps()).isEqualTo(201);
        UUID id = c.id();
        Map<String, Object> ligne = enBase(id);
        SoftAssertions.assertSoftly(s -> {
            s.assertThat(c.location()).isNotNull().endsWith("/api/projet/lots/" + id);
            s.assertThat((String) c.reponse().valeur("$.code")).matches("\\d{2}/\\d{5}").startsWith(annee() + "/");
            s.assertThat(ligne.get("Code")).isEqualTo(c.reponse().valeur("$.code"));
            s.assertThat(ligne.get("Description")).isEqualTo(libelle());
            s.assertThat(guid(ligne.get("PrjProjectId"))).isEqualTo(guid(projet));
            s.assertThat(guid(ligne.get("StlCategoryId"))).isEqualTo(guid(categorie));
            s.assertThat(guid(ligne.get("PrjMarketTypeId"))).isEqualTo(guid(type));
            s.assertThat(guid(ligne.get("CfgSupplierId"))).isEqualTo(guid(fournisseur));
        });
    }

    @Test
    void e5_codeUnique_malgreSequenceEnRetard_incrementeJusquAuCodeLibre() throws Exception {
        int retard = ThreadLocalRandom.current().nextInt(50_000, 90_000);
        fixture.sequenceMarche(retard);
        UUID autreProjet = fixture.projet("CRQ-" + jeton, null);
        UUID statut = fixture.statut(ENTREPRENEURS, "CRS-" + jeton);
        fixture.lot(autreProjet, "%s/%05d".formatted(annee(), retard + 1), ENTREPRENEURS, statut);
        fixture.lot(autreProjet, "%s/%05d".formatted(annee(), retard + 2), ENTREPRENEURS, statut);

        Creation c = appels.creer(admin, corpsValide(ENTREPRENEURS));

        assertThat(c.statut()).as(c.reponse().corps()).isEqualTo(201);
        assertThat(c.reponse().valeur("$.code")).isEqualTo("%s/%05d".formatted(annee(), retard + 3));
    }

    @ParameterizedTest
    @ValueSource(strings = {"ENTREPRENEURS", "FOURNISSEURS", "ADMINISTRATION", "MAITRES_OEUVRE", "CONCESSIONNAIRES"})
    void e5_genre_strEntityIdEtStatutDeDepartDuGenre(String genre) throws Exception {
        Creation c = appels.creer(admin, corpsValide(genre));

        assertThat(c.statut()).as(c.reponse().corps()).isEqualTo(201);
        Map<String, Object> ligne = enBase(c.id());
        List<Map<String, Object>> statut = fixture.lignes(
                "SELECT StrConfigEntityId, StrStatusTypeId FROM StrStatus WHERE PKey = ?", String.valueOf(ligne.get("StrStatusId")));
        SoftAssertions.assertSoftly(s -> {
            s.assertThat(guid(ligne.get("StrEntityId"))).isEqualTo(guid(GENRES.get(genre)));
            s.assertThat(statut).as("statut posé").hasSize(1);
            s.assertThat(statut).allSatisfy(st -> {
                s.assertThat(guid(st.get("StrConfigEntityId"))).as("statut du genre").isEqualTo(guid(GENRES.get(genre)));
                s.assertThat(guid(st.get("StrStatusTypeId"))).as("statut « de départ »").isEqualTo(guid(TYPE_STATUT_NOUVEAU));
            });
        });
    }

    @Test
    void e5_entrepreneurs_pasMultiFournisseur() throws Exception {
        Creation c = appels.creer(admin, corpsValide(ENTREPRENEURS));

        assertThat(c.statut()).as(c.reponse().corps()).isEqualTo(201);
        assertThat(enBase(c.id()).get("IsMultiSupplier")).isEqualTo(Boolean.FALSE);
    }

    // 2ᵉ passe point 8 : plusieurs statuts Nouveau du genre → celui de plus petit Sorting (ni le premier ni le dernier créé).
    @Test
    void p8_statutDeDepart_plusPetitOrdreDuGenre() throws Exception {
        fixture.statutDepart(FOURNISSEURS, "ORD-M-" + jeton, -999_998);
        UUID attendu = fixture.statutDepart(FOURNISSEURS, "ORD-P-" + jeton, -999_999);
        fixture.statutDepart(FOURNISSEURS, "ORD-G-" + jeton, -999_997);

        Creation c = appels.creer(admin, corpsValide(FOURNISSEURS));

        assertThat(c.statut()).as(c.reponse().corps()).isEqualTo(201);
        assertThat(guid(enBase(c.id()).get("StrStatusId"))).isEqualTo(guid(attendu));
    }

    @ParameterizedTest
    @ValueSource(strings = {"FOURNISSEURS", "ADMINISTRATION", "MAITRES_OEUVRE", "CONCESSIONNAIRES"})
    void e5_autresGenres_multiFournisseur_horsPrevisionsDePaiement(String genre) throws Exception {
        Creation c = appels.creer(admin, corpsValide(genre));

        assertThat(c.statut()).as(c.reponse().corps()).isEqualTo(201);
        Map<String, Object> ligne = enBase(c.id());
        SoftAssertions.assertSoftly(s -> {
            s.assertThat(ligne.get("IsMultiSupplier")).as("IsMultiSupplier").isEqualTo(Boolean.TRUE);
            s.assertThat(ligne.get("IncludedInPaymentPrevision")).as("IncludedInPaymentPrevision").isEqualTo(Boolean.FALSE);
        });
    }

    @Test
    void e5_ligneStrHistoryCreation_auStatutDeDepart() throws Exception {
        Creation c = appels.creer(admin, corpsValide(FOURNISSEURS));

        assertThat(c.statut()).as(c.reponse().corps()).isEqualTo(201);
        Map<String, Object> ligne = enBase(c.id());
        List<Map<String, Object>> historique = fixture.lignes("SELECT * FROM StrHistory WHERE ObjectId = ?", c.id().toString());
        assertThat(historique).singleElement().satisfies(h -> SoftAssertions.assertSoftly(s -> {
            s.assertThat(h.get("Description")).isEqualTo("Création");
            s.assertThat(guid(h.get("StrConfigEntityId"))).isEqualTo(guid(GENRES.get(FOURNISSEURS)));
            s.assertThat(guid(h.get("StrStatusId"))).isEqualTo(guid(ligne.get("StrStatusId")));
            s.assertThat(h.get("StrWorkflowId")).isNull();
        }));
    }

    // ------------------------------------------------------------------ 400

    @ParameterizedTest
    @ValueSource(strings = {GENRE, DESCRIPTION, PROJET_ID, CATEGORIE_ID, TYPE_ID})
    void e5_champObligatoireAbsent_400_aucunLot(String champ) throws Exception {
        Creation c = appels.creer(admin, corpsValide(ENTREPRENEURS).sans(champ));

        assertThat(c.statut()).as(c.reponse().corps()).isEqualTo(400);
        assertThat(fixture.compter(PAR_DESCRIPTION, libelle())).isZero();
    }

    @Test
    void e5_genreInconnu_400() throws Exception {
        Creation c = appels.creer(admin, corpsValide("INCONNU"));

        assertThat(c.statut()).as(c.reponse().corps()).isEqualTo(400);
        assertThat(fixture.compter(PAR_DESCRIPTION, libelle())).isZero();
    }

    @Test
    void e5_categorieInexistante_400() throws Exception {
        Creation c = appels.creer(admin, corpsValide(ENTREPRENEURS).avec(CATEGORIE_ID, UUID.randomUUID()));

        assertThat(c.statut()).as(c.reponse().corps()).isEqualTo(400);
        assertThat(fixture.compter(PAR_DESCRIPTION, libelle())).isZero();
    }

    @Test
    void e5_typeInexistant_400() throws Exception {
        Creation c = appels.creer(admin, corpsValide(ENTREPRENEURS).avec(TYPE_ID, UUID.randomUUID()));

        assertThat(c.statut()).as(c.reponse().corps()).isEqualTo(400);
        assertThat(fixture.compter(PAR_DESCRIPTION, libelle())).isZero();
    }

    // ------------------------------------------------------------------ 404

    @Test
    void e5_projetNonHabilite_404_aucunLot() throws Exception {
        UUID horsPerimetre = fixture.projet("CRH-" + jeton, null);

        Creation c = appels.creer(admin, corpsValide(ENTREPRENEURS).avec(PROJET_ID, horsPerimetre));

        assertThat(c.statut()).as(c.reponse().corps()).isEqualTo(404);
        assertThat(fixture.compter(PAR_DESCRIPTION, libelle())).isZero();
    }

    @Test
    void e5_projetInconnu_404() throws Exception {
        Creation c = appels.creer(admin, corpsValide(ENTREPRENEURS).avec(PROJET_ID, UUID.randomUUID()));

        assertThat(c.statut()).as(c.reponse().corps()).isEqualTo(404);
    }
}
