package com.tsix.immocrm.controller.projet;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.controller.projet.AppelsProjet.Reponse;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.fixtures.FixtureDecomptes;
import com.tsix.immocrm.fixtures.FixtureDossierVente;
import com.tsix.immocrm.fixtures.FixtureProjetLots;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.tsix.immocrm.controller.projet.AppelsProjet.DECOMPTES;
import static com.tsix.immocrm.fixtures.FixtureDecomptes.DROIT_MODIFIER;
import static com.tsix.immocrm.fixtures.FixtureDecomptes.DROIT_RECALCULER;
import static com.tsix.immocrm.fixtures.FixtureDecomptes.DROIT_SUPPRIMER;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static com.tsix.immocrm.fixtures.FixtureProjetLots.A_PAYER;
import static com.tsix.immocrm.fixtures.FixtureProjetLots.TTC;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

/**
 * Écritures d'un décompte (FEATURE-decomptes-parite.md « Règles d'écriture (OMRAN) » §0, §3, §5-§8 et « Réponses du
 * manager » Q8-Q11, Contrat) : verrouillé (facturé ou pas le dernier du lot) → 409 ; droit {@code AdmRight} manquant → 403
 * (Administrateur = tous les droits) ; id inconnu ou hors périmètre → 404. Les montants recalculés sont dans
 * {@link DecomptesPariteCalculTest}, la création dans {@link DecomptesPariteCreationTest}.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class DecomptesPariteEcrituresTest {

    static final String DERNIER = "/dernier";
    static final String RECALCUL = "/recalcul";
    static final String RECHARGER = "/lignes-rechargees";
    private static final String ADMINISTRATEUR = "Administrateur";
    private static final String TVA = "0.2";
    private static final String FINAL = "FinalWorkingOut";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureProjetLots fixture;
    @Autowired private FixtureDossierVente vente;
    @Autowired private FixtureDecomptes decomptes;
    @Autowired private JdbcTemplate jdbc;

    private AppelsProjet appels;
    private String jeton;
    private UUID projet;
    private UUID lot;
    private RequestPostProcessor admin;

    @BeforeEach
    void setUp() {
        appels = new AppelsProjet(mockMvc);
        jeton = FixtureProjetLots.jeton();
        projet = fixture.projet("PPE-" + jeton);
        lot = fixture.lot(projet, "LPE-" + jeton, fixture.fournisseur("FPE-" + jeton));
        AdmUser compte = vente.compte("prj.pe.a." + jeton, ADMINISTRATEUR);
        fixture.habiliter(compte, projet);
        admin = connecte(compte, ADMINISTRATEUR);
    }

    private RequestPostProcessor avecDroits(String... droits) {
        return decomptes.compte(projet, jeton, droits);
    }

    private UUID decompte(String code, int numero) {
        return fixture.decompte(lot, code + jeton, numero,
                Map.of(TTC, new BigDecimal("1000"), A_PAYER, new BigDecimal("1000")));
    }

    private void facturer(UUID decompte) {
        fixture.facture(decompte, "FPE" + decompte.toString().substring(0, 4) + jeton, new BigDecimal("1000"));
    }

    private MockHttpServletResponse executer(MockHttpServletRequestBuilder requete, RequestPostProcessor qui)
            throws Exception {
        decomptes.flush();
        MockHttpServletResponse r = mockMvc.perform(requete.with(qui)).andReturn().getResponse();
        assertThat(r.getStatus()).as(r.getContentAsString(StandardCharsets.UTF_8)).isNotIn(405, 500);
        return r;
    }

    private int action(UUID decompte, String suffixe, RequestPostProcessor qui) throws Exception {
        return executer(post(DECOMPTES + "/" + decompte + suffixe), qui).getStatus();
    }

    private int supprimer(UUID decompte, RequestPostProcessor qui) throws Exception {
        return executer(delete(DECOMPTES + "/" + decompte), qui).getStatus();
    }

    private MockHttpServletResponse recharger(UUID decompte, RequestPostProcessor qui) throws Exception {
        return executer(get(DECOMPTES + "/" + decompte + RECHARGER), qui);
    }

    /** Fiche lue par l'administrateur : corps d'Enregistrer (Contrat : {@code PUT /{id}}, corps = la fiche relue). */
    private String fiche(UUID decompte) throws Exception {
        Reponse r = appels.lire(admin, DECOMPTES + "/" + decompte);
        assertThat(r.statut()).as(r.corps()).isEqualTo(200);
        return r.corps();
    }

    private int enregistrer(UUID decompte, String corps, RequestPostProcessor qui) throws Exception {
        return executer(put(DECOMPTES + "/" + decompte).contentType(MediaType.APPLICATION_JSON).content(corps), qui)
                .getStatus();
    }

    private boolean estFinal(UUID decompte) {
        return Boolean.TRUE.equals(decomptes.lire(decompte, FINAL).get(FINAL));
    }

    private int existe(UUID decompte) {
        decomptes.flush();
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM PrjWorkingOut WHERE PKey = ?", Integer.class,
                decompte.toString());
        return n == null ? 0 : n;
    }

    private int retenues(UUID decompte) {
        decomptes.flush();
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM PrjWorkingOutRetainedLine WHERE PrjWorkingOutId = ?",
                Integer.class, decompte.toString());
        return n == null ? 0 : n;
    }

    /** Bordereau A (10) et B (90) ; décompte 1 : A = 4 ; décompte 2 (dernier, non facturé) : A = 1 sur 4 antérieurs. */
    private record Chantier(UUID ligneA, UUID ligneB, UUID d1, UUID d2) {
    }

    private Chantier chantier() {
        UUID a = decomptes.ligneMarche(lot, 1, "10", "100", TVA);
        UUID b = decomptes.ligneMarche(lot, 2, "90", "100", TVA);
        UUID d1 = decompte("DC1-", 1);
        decomptes.ligne(d1, a, 1, "4", "0", "100", TVA);
        UUID d2 = decompte("DC2-", 2);
        decomptes.ligne(d2, a, 1, "1", "4", "100", TVA);
        return new Chantier(a, b, d1, d2);
    }

    private static BigDecimal nombre(Map<String, Object> ligne, String colonne) {
        Object v = ligne.get(colonne);
        return v == null ? BigDecimal.ZERO : new BigDecimal(String.valueOf(v));
    }

    private static Map<String, Object> ligneDe(List<Map<String, Object>> lignes, UUID ligneMarche) {
        return lignes.stream().filter(l -> ligneMarche.toString().equalsIgnoreCase(String.valueOf(l.get("marche"))))
                .findFirst().orElseThrow(() -> new AssertionError("aucune ligne pour " + ligneMarche));
    }

    // ------------------------------------------------------------------ §7 Définir en tant que dernier décompte (Q10 : écrit)

    /** §7 : ReloadLines (une ligne par ligne du bordereau) puis cumul = quantité du marché, puis FinalWorkingOut. */
    @Test
    void dernier_nominal_rechargeLesLignes_cumulEgalQuantiteDuMarche_poseFinalWorkingOut() throws Exception {
        Chantier c = chantier();

        int statut = action(c.d2(), DERNIER, admin);

        assertThat(statut).isIn(200, 204);
        List<Map<String, Object>> lignes = decomptes.lignes(c.d2());
        assertThat(lignes).hasSize(2);
        Map<String, Object> a = ligneDe(lignes, c.ligneA());
        Map<String, Object> b = ligneDe(lignes, c.ligneB());
        assertThat(nombre(a, "QuantityOld")).isEqualByComparingTo("4");
        assertThat(nombre(a, "QuantityOld").add(nombre(a, "Quantity"))).isEqualByComparingTo("10");
        assertThat(nombre(b, "QuantityOld")).isEqualByComparingTo("0");
        assertThat(nombre(b, "Quantity")).isEqualByComparingTo("90");
        assertThat(estFinal(c.d2())).isTrue();
        assertThat(appels.lire(admin, DECOMPTES + "/" + c.d2()).valeur("$.dernier")).isEqualTo(true);
    }

    /** Q11 : le droit A_0152_13 suffit, sans être Administrateur. */
    @Test
    void dernier_roleAvecDroitModifier_passe() throws Exception {
        decompte("DE1-", 1);
        UUID d2 = decompte("DE2-", 2);

        assertThat(action(d2, DERNIER, avecDroits(DROIT_MODIFIER))).isIn(200, 204);
        assertThat(estFinal(d2)).isTrue();
    }

    @Test
    void dernier_refus409_siFacture() throws Exception {
        UUID d = decompte("DEF-", 1);
        facturer(d);

        assertThat(action(d, DERNIER, admin)).isEqualTo(409);
        assertThat(estFinal(d)).isFalse();
    }

    @Test
    void dernier_refus409_siPasLeDernierDuLot() throws Exception {
        UUID d1 = decompte("DEN-", 1);
        decompte("DEM-", 2);

        assertThat(action(d1, DERNIER, admin)).isEqualTo(409);
        assertThat(estFinal(d1)).isFalse();
    }

    /** Q11 : rôle portant d'autres droits décomptes, mais pas A_0152_13. */
    @Test
    void dernier_refus403_sansDroitModifier() throws Exception {
        UUID d = decompte("DED-", 1);

        assertThat(action(d, DERNIER, avecDroits(DROIT_SUPPRIMER, DROIT_RECALCULER))).isEqualTo(403);
        assertThat(estFinal(d)).isFalse();
    }

    /** 404 vert tant que la route manque : le cas nominal ci-dessus en est le témoin. */
    @Test
    void dernier_refus404_idInconnu() throws Exception {
        assertThat(action(UUID.randomUUID(), DERNIER, admin)).isEqualTo(404);
    }

    @Test
    void dernier_refus404_horsPerimetre() throws Exception {
        UUID d = decompte("DEH-", 1);
        RequestPostProcessor adminNonHabilite = connecte(vente.compte("prj.pe.h." + jeton, ADMINISTRATEUR),
                ADMINISTRATEUR);

        assertThat(action(d, DERNIER, adminNonHabilite)).isEqualTo(404);
        assertThat(estFinal(d)).isFalse();
    }

    // ------------------------------------------------------------------ §6 Recalculer (Q9 : non verrouillé seulement)

    @Test
    void recalcul_refus409_siFacture() throws Exception {
        UUID d = decompte("DRF-", 1);
        facturer(d);

        assertThat(action(d, RECALCUL, admin)).isEqualTo(409);
    }

    @Test
    void recalcul_refus409_siPasLeDernierDuLot() throws Exception {
        UUID d1 = decompte("DRN-", 1);
        decompte("DRM-", 2);

        assertThat(action(d1, RECALCUL, admin)).isEqualTo(409);
    }

    /** §6 : A_0152_15 manquant. */
    @Test
    void recalcul_refus403_sansDroitRecalculer() throws Exception {
        UUID d = decompte("DRD-", 1);

        assertThat(action(d, RECALCUL, avecDroits(DROIT_MODIFIER, DROIT_SUPPRIMER))).isEqualTo(403);
    }

    /** §6 : sur l'écran sans décomposition (le seul reproduit), Recalculer exige aussi A_0152_13 ({@code FWS} l.337, 351). */
    @Test
    void recalcul_refus403_droitRecalculerSansDroitModifier() throws Exception {
        UUID d = decompte("DRS-", 1);

        assertThat(action(d, RECALCUL, avecDroits(DROIT_RECALCULER))).isEqualTo(403);
    }

    @Test
    void recalcul_roleAvecDroitsRecalculerEtModifier_passe() throws Exception {
        UUID d = decompte("DRO-", 1);

        assertThat(action(d, RECALCUL, avecDroits(DROIT_RECALCULER, DROIT_MODIFIER))).isIn(200, 204);
    }

    @Test
    void recalcul_refus404_idInconnu() throws Exception {
        assertThat(action(UUID.randomUUID(), RECALCUL, admin)).isEqualTo(404);
    }

    // ------------------------------------------------------------------ §5 Suppression (Q8, Q5)

    /** §5 / Q5 : enfants NO_ACTION (retenues forfaitaires) supprimés avec le décompte, le précédent reste. */
    @Test
    void suppression_nominal_dernierDuLotNonFacture_supprimeAussiLesRetenues() throws Exception {
        UUID d1 = decompte("DS1-", 1);
        UUID d2 = decompte("DS2-", 2);
        jdbc.update("INSERT INTO PrjWorkingOutRetainedLine (PKey, PrjWorkingOutId, Sorting, Description, Amount)"
                + " VALUES (?, ?, 1, 'Pénalité', 15)", UUID.randomUUID().toString(), d2.toString());
        decomptes.extra(d2, "1", "10", TVA);

        int statut = supprimer(d2, admin);

        assertThat(statut).isIn(200, 204);
        assertThat(existe(d2)).isZero();
        assertThat(retenues(d2)).isZero();
        assertThat(existe(d1)).isEqualTo(1);
    }

    @Test
    void suppression_roleAvecDroitSupprimer_passe() throws Exception {
        UUID d = decompte("DSR-", 1);

        assertThat(supprimer(d, avecDroits(DROIT_SUPPRIMER))).isIn(200, 204);
        assertThat(existe(d)).isZero();
    }

    @Test
    void suppression_refus409_siFacture() throws Exception {
        UUID d = decompte("DSF-", 1);
        facturer(d);

        assertThat(supprimer(d, admin)).isEqualTo(409);
        assertThat(existe(d)).isEqualTo(1);
    }

    @Test
    void suppression_refus409_siPasLeDernierDuLot() throws Exception {
        UUID d1 = decompte("DSN-", 1);
        decompte("DSM-", 2);

        assertThat(supprimer(d1, admin)).isEqualTo(409);
        assertThat(existe(d1)).isEqualTo(1);
    }

    /** §5 : A_0152_14 manquant. */
    @Test
    void suppression_refus403_sansDroitSupprimer() throws Exception {
        UUID d = decompte("DSD-", 1);

        assertThat(supprimer(d, avecDroits(DROIT_MODIFIER, DROIT_RECALCULER))).isEqualTo(403);
        assertThat(existe(d)).isEqualTo(1);
    }

    @Test
    void suppression_refus404_idInconnu() throws Exception {
        assertThat(supprimer(UUID.randomUUID(), admin)).isEqualTo(404);
    }

    // ------------------------------------------------------------------ §7 Recharger les lignes (Q10 : aperçu, rien d'écrit)

    /** Une ligne rendue par ligne du bordereau ; la base reste intacte (quantités saisies gardées, pas de FinalWorkingOut). */
    @Test
    void recharger_nominal_uneLigneParLigneDuBordereau_rienEcrit() throws Exception {
        Chantier c = chantier();

        MockHttpServletResponse r = recharger(c.d2(), admin);

        String corps = r.getContentAsString(StandardCharsets.UTF_8);
        assertThat(r.getStatus()).as(corps).isEqualTo(200);
        Object racine = JsonPath.read(corps, "$");
        List<?> rendues = racine instanceof List<?> l ? l : JsonPath.read(corps, "$.lignes");
        assertThat(rendues).hasSize(2);
        List<Map<String, Object>> enBase = decomptes.lignes(c.d2());
        assertThat(enBase).hasSize(1);
        assertThat(nombre(enBase.getFirst(), "Quantity")).isEqualByComparingTo("1");
        assertThat(estFinal(c.d2())).isFalse();
    }

    @Test
    void recharger_parametreDate_accepte() throws Exception {
        Chantier c = chantier();

        assertThat(recharger(c.d2(), admin).getStatus()).isEqualTo(200);
        assertThat(executer(get(DECOMPTES + "/" + c.d2() + RECHARGER).param("date", "2026-03-02"), admin).getStatus())
                .isEqualTo(200);
    }

    @Test
    void recharger_refus409_siFacture() throws Exception {
        Chantier c = chantier();
        facturer(c.d2());

        assertThat(recharger(c.d2(), admin).getStatus()).isEqualTo(409);
    }

    @Test
    void recharger_refus409_siPasLeDernierDuLot() throws Exception {
        Chantier c = chantier();

        assertThat(recharger(c.d1(), admin).getStatus()).isEqualTo(409);
    }

    @Test
    void recharger_refus403_sansDroitModifier() throws Exception {
        Chantier c = chantier();

        assertThat(recharger(c.d2(), avecDroits(DROIT_SUPPRIMER, DROIT_RECALCULER)).getStatus()).isEqualTo(403);
    }

    @Test
    void recharger_refus404_idInconnu() throws Exception {
        assertThat(recharger(UUID.randomUUID(), admin).getStatus()).isEqualTo(404);
    }

    // ------------------------------------------------------------------ §3 Enregistrer (refus ; nominal dans DecomptesPariteCalculTest)

    @Test
    void enregistrer_refus409_siFacture() throws Exception {
        UUID d = decompte("DWF-", 1);
        facturer(d);

        assertThat(enregistrer(d, fiche(d), admin)).isEqualTo(409);
    }

    @Test
    void enregistrer_refus409_siPasLeDernierDuLot() throws Exception {
        UUID d1 = decompte("DWN-", 1);
        decompte("DWM-", 2);

        assertThat(enregistrer(d1, fiche(d1), admin)).isEqualTo(409);
    }

    @Test
    void enregistrer_refus403_sansDroitModifier() throws Exception {
        UUID d = decompte("DWD-", 1);

        assertThat(enregistrer(d, fiche(d), avecDroits(DROIT_SUPPRIMER, DROIT_RECALCULER))).isEqualTo(403);
    }

    /** §3 {@code W:Verif} : la date doit être ≥ celle du décompte précédent → 400, rien d'écrit. */
    @Test
    void enregistrer_refus400_dateAnterieureAuDecomptePrecedent() throws Exception {
        decompte("DWA-", 1);
        UUID d2 = decompte("DWB-", 2);
        DocumentContext corps = JsonPath.parse(fiche(d2));
        String date = corps.read("$.dateDocument");
        corps.set("$.dateDocument", date.replace("2026-03-02", "2026-02-15"));

        assertThat(enregistrer(d2, corps.jsonString(), admin)).isEqualTo(400);
        assertThat(String.valueOf(decomptes.lire(d2, "DocDate").get("DocDate"))).startsWith("2026-03-02");
    }
}
