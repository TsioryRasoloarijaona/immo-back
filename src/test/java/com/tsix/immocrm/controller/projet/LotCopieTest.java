package com.tsix.immocrm.controller.projet;

import com.tsix.immocrm.controller.projet.LotsPariteSupport.Creation;
import com.tsix.immocrm.fixtures.FixtureLotFiche;
import com.tsix.immocrm.fixtures.FixtureLotsParite;
import com.tsix.immocrm.fixtures.FixtureProjetLots;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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

import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static com.tsix.immocrm.fixtures.FixtureLotsParite.DROIT_NOUVEAU;
import static com.tsix.immocrm.fixtures.FixtureLotsParite.DROIT_NOUVEAU_MULTI;
import static com.tsix.immocrm.fixtures.FixtureLotsParite.ENTREPRENEURS;
import static com.tsix.immocrm.fixtures.FixtureLotsParite.FOURNISSEURS;
import static com.tsix.immocrm.fixtures.FixtureLotsParite.GENRES;
import static com.tsix.immocrm.fixtures.FixtureLotsParite.TYPE_STATUT_NOUVEAU;
import static com.tsix.immocrm.fixtures.FixturePatrimoine.guid;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * E8 — FEATURE-lot-fiche-parite.md : {@code POST /api/projet/lots/{id}/copie} → 201 + Location. Code de la séquence 'Market' avec
 * garde d'unicité, « (Copie) », {@code IsClosed = false}, statut de départ du genre, {@code StrHistory} « Création », lignes de
 * bordereau et leurs groupes copiés (réponse du manager 3). Droit {@code toolNew} (Entrepreneurs) / {@code toolNewMultiSupplier}
 * (autres genres), 403 avant 404.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class LotCopieTest {

    private static final String LOT_PAR_ID = "SELECT * FROM PrjMarket WHERE PKey = ?";
    private static final String LIGNES_DU_LOT = "SELECT * FROM PrjMarketLine WHERE PrjMarketId = ?";
    private static final String GROUPES_DU_LOT = "SELECT * FROM PrjMarketLineGrouping WHERE PrjMarketId = ?";
    private static final String COPIES = "SELECT COUNT(*) FROM PrjMarket WHERE Description = ?";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureLotsParite parite;
    @Autowired private FixtureLotFiche fiche;
    @Autowired private FixtureProjetLots lots;
    @Autowired private Clock clock;

    private LotFicheSupport appels;
    private String jeton;
    private UUID projet;
    private UUID statutCourant;
    private UUID lot;
    private RequestPostProcessor admin;

    @BeforeEach
    void setUp() {
        appels = new LotFicheSupport(mockMvc);
        jeton = FixtureProjetLots.jeton();
        parite.genresDeLot();
        parite.statutsDepart(jeton);
        parite.sequenceMarche(ThreadLocalRandom.current().nextInt(50_000, 90_000));
        projet = parite.projet("CPP-" + jeton, parite.entiteJuridique("CPE-" + jeton));
        statutCourant = parite.statut(ENTREPRENEURS, "CPS-" + jeton);
        lot = parite.lot(projet, "CPL-" + jeton, ENTREPRENEURS, statutCourant);
        admin = connecte(parite.administrateur("lots.cp." + jeton, projet), "Administrateur");
    }

    private Map<String, Object> enBase(UUID id) {
        List<Map<String, Object>> l = fiche.lignes(LOT_PAR_ID, id.toString());
        assertThat(l).hasSize(1);
        return l.get(0);
    }

    private String annee() {
        return "%02d".formatted(LocalDate.now(clock).getYear() % 100);
    }

    private Creation copierOk(RequestPostProcessor qui, UUID source) throws Exception {
        Creation c = appels.copier(qui, source);
        assertThat(c.statut()).as(c.reponse().corps()).isEqualTo(201);
        return c;
    }

    // ------------------------------------------------------------------ 201 : en-tête

    @Test
    void e8_copie_201_location_libelleCopie_nouveauCode_memeProjetEtGenre() throws Exception {
        UUID fournisseur = lots.fournisseur("CPF-" + jeton);
        fiche.fournisseurEtType(lot, fournisseur, null);

        Creation c = copierOk(admin, lot);

        UUID id = c.id();
        Map<String, Object> copie = enBase(id);
        SoftAssertions.assertSoftly(s -> {
            s.assertThat(id).isNotEqualTo(lot);
            s.assertThat(c.location()).isNotNull().endsWith("/api/projet/lots/" + id);
            s.assertThat(copie).containsEntry("Description", "Lot CPL-" + jeton + " (Copie)");
            s.assertThat(c.reponse().valeur("$.description")).isEqualTo("Lot CPL-" + jeton + " (Copie)");
            s.assertThat((String) copie.get("Code")).matches("\\d{2}/\\d{5}").startsWith(annee() + "/");
            s.assertThat(c.reponse().valeur("$.code")).isEqualTo(copie.get("Code"));
            s.assertThat(guid(copie.get("PrjProjectId"))).isEqualTo(guid(projet));
            s.assertThat(guid(copie.get("StrEntityId"))).isEqualTo(guid(GENRES.get(ENTREPRENEURS)));
            s.assertThat(guid(copie.get("CfgSupplierId"))).isEqualTo(guid(fournisseur));
            s.assertThat(LotsPariteSupport.nombre(copie.get("WarrantyRatio"))).isEqualByComparingTo("0.1");
        });
    }

    @Test
    void e8_copie_nonClose_statutDeDepartDuGenre() throws Exception {
        Creation c = copierOk(admin, lot);

        Map<String, Object> copie = enBase(c.id());
        List<Map<String, Object>> statut = fiche.lignes("SELECT StrConfigEntityId, StrStatusTypeId FROM StrStatus WHERE PKey = ?",
                String.valueOf(copie.get("StrStatusId")));
        SoftAssertions.assertSoftly(s -> {
            s.assertThat(copie).containsEntry("IsClosed", Boolean.FALSE);
            s.assertThat(guid(copie.get("StrStatusId"))).as("pas le statut de la source").isNotEqualTo(guid(statutCourant));
            s.assertThat(statut).as("statut posé").hasSize(1);
            s.assertThat(statut).allSatisfy(st -> {
                s.assertThat(guid(st.get("StrConfigEntityId"))).isEqualTo(guid(GENRES.get(ENTREPRENEURS)));
                s.assertThat(guid(st.get("StrStatusTypeId"))).isEqualTo(guid(TYPE_STATUT_NOUVEAU));
            });
        });
    }

    @Test
    void e8_copie_sourceClose_copieOuverte() throws Exception {
        fiche.clore(lot);

        Creation c = appels.copier(admin, lot);

        assertThat(c.statut()).as(c.reponse().corps()).isEqualTo(201);
        assertThat(enBase(c.id())).containsEntry("IsClosed", Boolean.FALSE);
    }

    @Test
    void e8_copie_ligneStrHistoryCreation() throws Exception {
        Creation c = copierOk(admin, lot);

        Map<String, Object> copie = enBase(c.id());
        List<Map<String, Object>> historique = fiche.lignes("SELECT * FROM StrHistory WHERE ObjectId = ?", c.id().toString());
        assertThat(historique).singleElement().satisfies(h -> SoftAssertions.assertSoftly(s -> {
            s.assertThat(h.get("Description")).isEqualTo("Création");
            s.assertThat(guid(h.get("StrStatusId"))).isEqualTo(guid(copie.get("StrStatusId")));
            s.assertThat(h.get("StrWorkflowId")).isNull();
        }));
    }

    @Test
    void e8_codeUnique_malgreSequenceEnRetard() throws Exception {
        int retard = ThreadLocalRandom.current().nextInt(50_000, 90_000);
        parite.sequenceMarche(retard);
        UUID autre = parite.projet("CPQ-" + jeton, null);
        parite.lot(autre, "%s/%05d".formatted(annee(), retard + 1), ENTREPRENEURS, statutCourant);
        parite.lot(autre, "%s/%05d".formatted(annee(), retard + 2), ENTREPRENEURS, statutCourant);

        Creation c = copierOk(admin, lot);

        assertThat(c.reponse().valeur("$.code")).isEqualTo("%s/%05d".formatted(annee(), retard + 3));
    }

    // ------------------------------------------------------------------ bordereau et groupes (Q3)

    @Test
    void e8_lignesDeBordereauEtGroupes_copies_rattachesALaCopie() throws Exception {
        UUID groupe = fiche.groupeBordereau(lot, "GRP-" + jeton);
        UUID ligneGroupee = parite.ligneBordereau(lot, "LG-" + jeton);
        fiche.rattacher(ligneGroupee, groupe);
        UUID ligneSeule = parite.ligneBordereau(lot, "LS-" + jeton);
        fiche.rattacher(ligneSeule, null);

        Creation c = copierOk(admin, lot);

        UUID copie = c.id();
        List<Map<String, Object>> lignes = fiche.lignes(LIGNES_DU_LOT, copie.toString());
        List<Map<String, Object>> groupes = fiche.lignes(GROUPES_DU_LOT, copie.toString());
        SoftAssertions.assertSoftly(s -> {
            s.assertThat(lignes).extracting(l -> l.get("Code")).containsExactlyInAnyOrder("LG-" + jeton, "LS-" + jeton);
            s.assertThat(lignes).extracting(l -> guid(l.get("PKey"))).doesNotContain(guid(ligneGroupee), guid(ligneSeule));
            s.assertThat(groupes).singleElement().satisfies(g -> {
                s.assertThat(g.get("Code")).isEqualTo("GRP-" + jeton);
                s.assertThat(guid(g.get("PKey"))).as("nouveau groupe").isNotEqualTo(guid(groupe));
                s.assertThat(lignes).filteredOn(l -> ("LG-" + jeton).equals(l.get("Code"))).singleElement()
                        .satisfies(l -> s.assertThat(guid(l.get("PrjMarketLineGroupingId")))
                                .as("ligne copiée → groupe copié").isEqualTo(guid(g.get("PKey"))));
            });
            s.assertThat(fiche.lignes(LIGNES_DU_LOT, lot.toString())).as("source intacte").hasSize(2);
        });
    }

    // ------------------------------------------------------------------ droits par genre, 403 avant 404

    @Test
    void e8_entrepreneurs_droitToolNew_201() throws Exception {
        RequestPostProcessor qui = connecte(parite.avecDroits("lots.cp.n." + jeton, List.of(DROIT_NOUVEAU), projet), "Commercial");

        assertThat(appels.copier(qui, lot).statut()).isEqualTo(201);
    }

    @Test
    void e8_entrepreneurs_droitMultiSeul_403_aucuneCopie() throws Exception {
        RequestPostProcessor qui = connecte(parite.avecDroits("lots.cp.m." + jeton, List.of(DROIT_NOUVEAU_MULTI), projet),
                "Commercial");

        Creation c = appels.copier(qui, lot);

        assertThat(c.statut()).as(c.reponse().corps()).isEqualTo(403);
        assertThat(fiche.lignes(COPIES, "Lot CPL-" + jeton + " (Copie)")).singleElement()
                .satisfies(l -> assertThat(l.values()).containsExactly(0));
    }

    @Test
    void e8_fournisseurs_droitMulti_201() throws Exception {
        UUID multi = parite.lot(projet, "CPM-" + jeton, FOURNISSEURS, parite.statut(FOURNISSEURS, "CPMS-" + jeton));
        RequestPostProcessor qui = connecte(parite.avecDroits("lots.cp.fm." + jeton, List.of(DROIT_NOUVEAU_MULTI), projet),
                "Commercial");

        assertThat(appels.copier(qui, multi).statut()).isEqualTo(201);
    }

    @Test
    void e8_fournisseurs_droitToolNewSeul_403() throws Exception {
        UUID multi = parite.lot(projet, "CPN-" + jeton, FOURNISSEURS, parite.statut(FOURNISSEURS, "CPNS-" + jeton));
        RequestPostProcessor qui = connecte(parite.avecDroits("lots.cp.fn." + jeton, List.of(DROIT_NOUVEAU), projet),
                "Commercial");

        assertThat(appels.copier(qui, multi).statut()).isEqualTo(403);
    }

    @Test
    void e8_sansDroit_lotInvisible_403_avant404() throws Exception {
        UUID autre = parite.projet("CPX-" + jeton, null);
        UUID invisible = parite.lot(autre, "CPX-" + jeton, ENTREPRENEURS, statutCourant);
        RequestPostProcessor qui = connecte(parite.avecDroits("lots.cp.x." + jeton, List.of(), projet), "Commercial");

        assertThat(appels.copier(qui, invisible).statut()).isEqualTo(403);
    }

    // Vert avant le code (route absente = 404) ; le témoin 201 est e8_entrepreneurs_droitToolNew_201.
    @Test
    void e8_avecDroit_lotInvisible_404() throws Exception {
        UUID autre = parite.projet("CPY-" + jeton, null);
        UUID invisible = parite.lot(autre, "CPY-" + jeton, ENTREPRENEURS, statutCourant);

        assertThat(appels.copier(admin, invisible).statut()).isEqualTo(404);
    }
}
