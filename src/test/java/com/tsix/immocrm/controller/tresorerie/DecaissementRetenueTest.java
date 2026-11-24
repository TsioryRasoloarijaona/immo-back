package com.tsix.immocrm.controller.tresorerie;

import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;
import com.tsix.immocrm.controller.tresorerie.AppelsTresorerie.Reponse;
import com.tsix.immocrm.fixtures.FixtureDecaissements;
import com.tsix.immocrm.fixtures.FixtureDecaissements.Saisie;
import com.tsix.immocrm.fixtures.FixtureTresorerie;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.tsix.immocrm.controller.tresorerie.AppelsSaisieDecaissement.corps;
import static com.tsix.immocrm.controller.tresorerie.AppelsSaisieDecaissement.detail;
import static com.tsix.immocrm.controller.tresorerie.AppelsSaisieDecaissement.ligne;
import static com.tsix.immocrm.controller.tresorerie.AppelsSaisieDecaissement.retenueMontant;
import static com.tsix.immocrm.controller.tresorerie.AppelsSaisieDecaissement.retenueTaux;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * FEATURE-n3bis-b2a-retenue.md §6 RS-a à RS-p (hors RS-m/n/o, voir {@link DecaissementCodesFeuilleTest}) : retenues à la
 * source de la saisie d'un décaissement, sortie sous {@code $.saisie.*} (T1), taux en fraction (T6).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class DecaissementRetenueTest {

    private static final String RETENUES = "retenues";
    private static final String MILLE = "1000";
    private static final String MONTANT_NET = "$.saisie.montantNet";
    private static final String MESSAGE_LIGNE_NULLE = "Vous devez corriger le montant ou supprimer la ligne !";
    private static final Configuration TOLERANTE =
            Configuration.defaultConfiguration().addOptions(Option.DEFAULT_PATH_LEAF_TO_NULL);

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureDecaissements fixture;

    private AppelsSaisieDecaissement appels;
    private Saisie s;
    private RequestPostProcessor qui;
    private UUID type;
    private UUID piece;

    @BeforeEach
    void setUp() {
        appels = new AppelsSaisieDecaissement(mockMvc);
        s = fixture.saisie("DRT");
        fixture.gestionRs(true);
        type = fixture.typeRetenue("RS-" + s.c().jeton(), "0.1");
        piece = fixture.piece(s, "-FA", s.c().projet(), "2000");
        qui = connecte(fixture.compte(s, "DRTA", FixtureDecaissements.DROIT_LIRE, FixtureDecaissements.DROIT_NOUVEAU),
                FixtureTresorerie.ROLE);
    }

    private Map<String, Object> corpsAvec(String suffixe, String ligneMontant, List<Map<String, Object>> retenues) {
        Map<String, Object> c = corps(s, s.code(suffixe), MILLE, List.of(ligne(piece, ligneMontant)));
        if (retenues != null) {
            c.put(RETENUES, retenues);
        }
        return c;
    }

    private static BigDecimal nombre(Reponse r, String chemin) {
        Object v = JsonPath.using(TOLERANTE).parse(r.corps()).read(chemin);
        return v == null ? null : new BigDecimal(v.toString());
    }

    private static BigDecimal dec(Object valeur) {
        return valeur == null ? null : new BigDecimal(valeur.toString());
    }

    private static void montantsEnBase(SoftAssertions sa, Map<String, Object> rs, String net, String rsTotal,
                                       String taux) {
        sa.assertThat(dec(rs.get("montant"))).as("Amount").isEqualByComparingTo(net);
        sa.assertThat(dec(rs.get("base"))).as("AmountBase").isEqualByComparingTo(MILLE);
        sa.assertThat(dec(rs.get("montantRs"))).as("AmountRS").isEqualByComparingTo(rsTotal);
        sa.assertThat(dec(rs.get("taux"))).as("RsRatio").isEqualByComparingTo(taux);
    }

    // RS-a, RS-b, RS-c, RS-d (saisie par taux) ; T1 : net sur la création et la fiche.
    @Test
    void rsD_parTaux_retenueEtNetEnBaseEtSurLeFil() throws Exception {
        Reponse r = appels.creer(qui, corpsAvec("-T", MILLE, List.of(retenueTaux(type, MILLE, "0.1"))));
        assertThat(r.statut()).as(r.corps()).isEqualTo(201);
        UUID id = UUID.fromString(JsonPath.read(r.corps(), "$.decaissement.id"));
        Reponse fiche = appels.fiche(qui, id);
        List<Map<String, Object>> retenues = fixture.retenuesEnBase(id);

        SoftAssertions sa = new SoftAssertions();
        sa.assertThat(retenues).hasSize(1);
        if (retenues.size() == 1) {
            sa.assertThat(dec(retenues.get(0).get("montantRs"))).as("ligne AmountRS").isEqualByComparingTo("100");
            sa.assertThat(dec(retenues.get(0).get("montant"))).as("ligne Amount").isEqualByComparingTo("900");
            sa.assertThat(retenues.get(0)).containsEntry("type", FixtureDecaissements.texte(type));
        }
        montantsEnBase(sa, fixture.rsEnBase(id), "900", "100", "0.1");
        sa.assertThat(nombre(r, MONTANT_NET)).as("création montantNet").isEqualByComparingTo("900");
        sa.assertThat(nombre(r, "$.saisie.tauxRs")).as("création tauxRs").isEqualByComparingTo("0.1");
        sa.assertThat(nombre(fiche, MONTANT_NET)).as("fiche montantNet").isEqualByComparingTo("900");
        sa.assertThat(nombre(fiche, "$.saisie.tauxRs")).as("fiche tauxRs").isEqualByComparingTo("0.1");
        sa.assertThat(nombre(fiche, "$.saisie.montantRs")).as("fiche montantRs").isEqualByComparingTo("100");
        sa.assertAll();
    }

    // RS-d : montantRs saisi → taux = montantRs / base.
    @Test
    void rsD_parMontant_tauxDeLaLigneDeduit() throws Exception {
        UUID id = appels.creerOk(qui, corpsAvec("-M", MILLE, List.of(retenueMontant(type, MILLE, "50"))));
        List<Map<String, Object>> retenues = fixture.retenuesEnBase(id);

        SoftAssertions sa = new SoftAssertions();
        sa.assertThat(retenues).hasSize(1);
        if (retenues.size() == 1) {
            sa.assertThat(dec(retenues.get(0).get("taux"))).as("ligne RsRatio").isEqualByComparingTo("0.05");
            sa.assertThat(dec(retenues.get(0).get("montant"))).as("ligne Amount").isEqualByComparingTo("950");
        }
        montantsEnBase(sa, fixture.rsEnBase(id), "950", "50", "0.05");
        sa.assertThat(nombre(appels.fiche(qui, id), MONTANT_NET)).isEqualByComparingTo("950");
        sa.assertAll();
    }

    // RS-a, RS-b, RS-c sur deux lignes : Σ AmountRS, RsRatio = AmountRS / AmountBase.
    @Test
    void rsA_deuxLignes_sommeDesRetenues() throws Exception {
        UUID id = appels.creerOk(qui, corpsAvec("-2L", MILLE,
                List.of(retenueTaux(type, "600", "0.1"), retenueMontant(type, "400", "20"))));
        List<Map<String, Object>> retenues = fixture.retenuesEnBase(id);

        SoftAssertions sa = new SoftAssertions();
        sa.assertThat(retenues).hasSize(2);
        if (retenues.size() == 2) {
            sa.assertThat(dec(retenues.get(0).get("montantRs"))).as("ligne 400").isEqualByComparingTo("20");
            sa.assertThat(dec(retenues.get(1).get("montantRs"))).as("ligne 600").isEqualByComparingTo("60");
        }
        montantsEnBase(sa, fixture.rsEnBase(id), "920", "80", "0.08");
        sa.assertAll();
    }

    // RS-h : ligne à AmountRS nul refusée.
    @Test
    void rsH_ligneMontantRsNul_400() throws Exception {
        Reponse r = appels.creer(qui, corpsAvec("-H0", MILLE, List.of(retenueMontant(type, MILLE, "0"))));

        assertThat(r.statut()).as(r.corps()).isEqualTo(400);
        assertThat(detail(r)).isEqualTo(MESSAGE_LIGNE_NULLE);
    }

    // RS-h, témoin : même corps avec 10.
    @Test
    void rsH_temoin_montantRsDix_201() throws Exception {
        UUID id = appels.creerOk(qui, corpsAvec("-H10", MILLE, List.of(retenueMontant(type, MILLE, "10"))));

        assertThat(fixture.retenuesEnBase(id)).hasSize(1);
    }

    // RS-i : reste à affecter sur le brut — retenue de 100 sur 1000, on affecte 1000.
    @Test
    void rsI_resteSurLeBrut_affecterLeBrutAccepte() throws Exception {
        UUID id = appels.creerOk(qui, corpsAvec("-I", MILLE, List.of(retenueTaux(type, MILLE, "0.1"))));

        assertThat(dec(fixture.rsEnBase(id).get("montant"))).isEqualByComparingTo("900");
    }

    // RS-i : 1000.01 sur un brut de 1000 refusé.
    @Test
    void rsI_affecterAuDelaDuBrut_400() throws Exception {
        Reponse r = appels.creer(qui, corpsAvec("-I2", "1000.01", List.of(retenueTaux(type, MILLE, "0.1"))));

        assertThat(r.statut()).as(r.corps()).isEqualTo(400);
        assertThat(detail(r)).isEqualTo("Le reste à affecter doit être >= 0 !");
    }

    // RS-j : sans retenue (fournisseur sans RS), Amount = AmountBase.
    @Test
    void rsJ_sansRetenue_netEgalBrut() throws Exception {
        Reponse r = appels.creer(qui, corpsAvec("-J", MILLE, null));
        assertThat(r.statut()).as(r.corps()).isEqualTo(201);
        UUID id = UUID.fromString(JsonPath.read(r.corps(), "$.decaissement.id"));
        Map<String, Object> rs = fixture.rsEnBase(id);

        SoftAssertions sa = new SoftAssertions();
        sa.assertThat(dec(rs.get("montant"))).as("Amount").isEqualByComparingTo(MILLE);
        sa.assertThat(dec(rs.get("base"))).as("AmountBase").isEqualByComparingTo(MILLE);
        sa.assertThat(fixture.retenuesEnBase(id)).isEmpty();
        sa.assertThat(nombre(r, MONTANT_NET)).as("montantNet").isEqualByComparingTo(MILLE);
        sa.assertAll();
    }

    // RS-p : ManageRS = 0 et retenues non vide → 409.
    @Test
    void rsP_gestionRsInactive_retenues_409() throws Exception {
        fixture.gestionRs(false);

        Reponse r = appels.creer(qui, corpsAvec("-P", MILLE, List.of(retenueTaux(type, MILLE, "0.1"))));

        assertThat(r.statut()).as(r.corps()).isEqualTo(409);
        assertThat(detail(r)).isEqualTo("La retenue à la source n'est pas activée.");
    }

    // RS-p, témoin : ManageRS = 1, même corps.
    @Test
    void rsP_temoin_gestionRsActive_201() throws Exception {
        UUID id = appels.creerOk(qui, corpsAvec("-P1", MILLE, List.of(retenueTaux(type, MILLE, "0.1"))));

        assertThat(fixture.retenuesEnBase(id)).hasSize(1);
    }

    // RS-g / T4 : retenues absent, fournisseur avec RS, ManageRS = 1 → ligne automatique au type du tiers.
    @Test
    void rsG_retenuesAbsent_ligneAutomatiqueDuFournisseur() throws Exception {
        fixture.fournisseurAvecRs(s, type, "0.1");

        UUID id = appels.creerOk(qui, corpsAvec("-G", MILLE, null));
        List<Map<String, Object>> retenues = fixture.retenuesEnBase(id);

        SoftAssertions sa = new SoftAssertions();
        sa.assertThat(retenues).hasSize(1);
        if (retenues.size() == 1) {
            sa.assertThat(retenues.get(0)).containsEntry("type", FixtureDecaissements.texte(type));
            sa.assertThat(dec(retenues.get(0).get("base"))).as("ligne base").isEqualByComparingTo(MILLE);
            sa.assertThat(dec(retenues.get(0).get("montantRs"))).as("ligne AmountRS").isEqualByComparingTo("100");
        }
        sa.assertThat(dec(fixture.rsEnBase(id).get("montant"))).as("Amount").isEqualByComparingTo("900");
        sa.assertAll();
    }

    // RS-g / T4 : retenues: [] = aucune retenue voulue.
    @Test
    void rsG_retenuesVide_aucuneLigne() throws Exception {
        fixture.fournisseurAvecRs(s, type, "0.1");

        UUID id = appels.creerOk(qui, corpsAvec("-GV", MILLE, List.of()));

        assertThat(fixture.retenuesEnBase(id)).isEmpty();
    }

    // RS-g : ManageRS = 0 → aucune ligne automatique, Amount = brut.
    @Test
    void rsG_gestionRsInactive_aucuneLigne() throws Exception {
        fixture.fournisseurAvecRs(s, type, "0.1");
        fixture.gestionRs(false);

        UUID id = appels.creerOk(qui, corpsAvec("-G0", MILLE, null));

        SoftAssertions sa = new SoftAssertions();
        sa.assertThat(fixture.retenuesEnBase(id)).isEmpty();
        sa.assertThat(dec(fixture.rsEnBase(id).get("montant"))).as("Amount").isEqualByComparingTo(MILLE);
        sa.assertAll();
    }

    // RS-e / Q6 : le référentiel rend tous les types avec anneeRas, et gestionRs.
    @Test
    void rsE_referentiels_typesRetenue_avecAnneeRas() throws Exception {
        UUID type2093 = fixture.typeRetenue("RS93-" + s.c().jeton(), "0.15", 2093);

        Reponse r = appels.lire(qui, AppelsSaisieDecaissement.REFERENTIELS, "module", AppelsTresorerie.PRJ);

        assertThat(r.statut()).as(r.corps()).isEqualTo(200);
        List<Object> annees = JsonPath.read(r.corps(), "$.typesRetenue[?(@.id == '" + type2093 + "')].anneeRas");
        Boolean gestion = JsonPath.using(TOLERANTE).parse(r.corps()).read("$.gestionRs");
        SoftAssertions sa = new SoftAssertions();
        sa.assertThat(annees).as("typesRetenue[id].anneeRas").containsExactly(2093);
        sa.assertThat(gestion).as("gestionRs").isTrue();
        sa.assertAll();
    }
}
