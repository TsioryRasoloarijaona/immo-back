package com.tsix.immocrm.controller.projet;

import com.tsix.immocrm.controller.projet.AppelsProjet.Reponse;
import com.tsix.immocrm.fixtures.CorpsJson;
import com.tsix.immocrm.fixtures.FixtureBordereauxEcriture;
import com.tsix.immocrm.fixtures.FixtureBordereauxEcriture.Ligne;
import com.tsix.immocrm.fixtures.FixtureLotsParite;
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

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.tsix.immocrm.controller.projet.BordereauxEcritureSupport.CATEGORIE;
import static com.tsix.immocrm.controller.projet.BordereauxEcritureSupport.CODE;
import static com.tsix.immocrm.controller.projet.BordereauxEcritureSupport.DEFINITIF;
import static com.tsix.immocrm.controller.projet.BordereauxEcritureSupport.DESCRIPTION;
import static com.tsix.immocrm.controller.projet.BordereauxEcritureSupport.DESIGNATION;
import static com.tsix.immocrm.controller.projet.BordereauxEcritureSupport.ESTIMATIF;
import static com.tsix.immocrm.controller.projet.BordereauxEcritureSupport.FODEC;
import static com.tsix.immocrm.controller.projet.BordereauxEcritureSupport.GROUPE_ID;
import static com.tsix.immocrm.controller.projet.BordereauxEcritureSupport.LIGNES;
import static com.tsix.immocrm.controller.projet.BordereauxEcritureSupport.NOTES;
import static com.tsix.immocrm.controller.projet.BordereauxEcritureSupport.ORDRE;
import static com.tsix.immocrm.controller.projet.BordereauxEcritureSupport.PARENT_ID;
import static com.tsix.immocrm.controller.projet.BordereauxEcritureSupport.PU_HT;
import static com.tsix.immocrm.controller.projet.BordereauxEcritureSupport.QUANTITE;
import static com.tsix.immocrm.controller.projet.BordereauxEcritureSupport.TVA;
import static com.tsix.immocrm.controller.projet.BordereauxEcritureSupport.UNITE;
import static com.tsix.immocrm.controller.projet.BordereauxEcritureSupport.bordereau;
import static com.tsix.immocrm.controller.projet.BordereauxEcritureSupport.detail;
import static com.tsix.immocrm.controller.projet.BordereauxEcritureSupport.groupe;
import static com.tsix.immocrm.controller.projet.BordereauxEcritureSupport.guid;
import static com.tsix.immocrm.controller.projet.BordereauxEcritureSupport.ligne;
import static com.tsix.immocrm.controller.projet.BordereauxEcritureSupport.vide;
import static com.tsix.immocrm.fixtures.FixtureBordereauxEcriture.GROUPES_DEFINITIF;
import static com.tsix.immocrm.fixtures.FixtureBordereauxEcriture.GROUPES_ESTIMATIF;
import static com.tsix.immocrm.fixtures.FixtureBordereauxEcriture.LIGNES_DEFINITIF;
import static com.tsix.immocrm.fixtures.FixtureBordereauxEcriture.LIGNES_ESTIMATIF;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static com.tsix.immocrm.fixtures.FixtureLotsParite.ENTREPRENEURS;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Écritures des bordereaux (FEATURE-lot-ecritures.md §3, §5 R4-R15, §9) par un Administrateur sur un lot au statut « avec
 * bordereau estimatif » : validation, codes uniques, ids, suppressions décomptées, insertion / modification / suppression, PU TTC,
 * audit, ventilation, lignes de stock, totaux ; champs ajoutés à la lecture. Le rollback et l'historique sont dans
 * {@code LotBordereauxEcritureCommitTest} (non transactionnel).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class LotBordereauxEcritureTest {

    private static final String MSG_LIGNE = "Vous ne pouvez pas supprimer cette ligne";
    private static final String MSG_GROUPE = "Vous ne pouvez pas supprimer ce groupe";
    private static final String LIGNES_IDS = "$.lignes[*].id";
    private static final String GROUPES_IDS = "$.groupes[*].id";
    private static final String QUANTITY = "Quantity";
    private static final String DESCRIPTION_COL = "Description";
    private static final String GROUPING = "PrjMarketLineGroupingId";
    private static final String TAUX = "taux";
    private static final String CATEGORIE_COL = "categorie";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureLotsParite parite;
    @Autowired private FixtureProjetLots lots;
    @Autowired private FixtureBordereauxEcriture b;

    private BordereauxEcritureSupport appels;
    private String jeton;
    private UUID projet;
    private UUID statutOuvert;
    private UUID categorie;
    private UUID autreCategorie;
    private RequestPostProcessor admin;

    @BeforeEach
    void setUp() {
        appels = new BordereauxEcritureSupport(mockMvc);
        jeton = FixtureProjetLots.jeton();
        parite.genresDeLot();
        b.tableVentilation();
        projet = parite.projet("BWP-" + jeton, parite.entiteJuridique("BWE-" + jeton));
        statutOuvert = parite.statut(ENTREPRENEURS, "BWO-" + jeton);
        b.ouvrirEstimatif(statutOuvert);
        categorie = b.categorie("BWA-" + jeton);
        autreCategorie = b.categorie("BWB-" + jeton);
        admin = connecte(parite.administrateur("lot.bw." + jeton, projet), "Administrateur");
    }

    private UUID lot(String code) {
        return parite.lot(projet, code + "-" + jeton, ENTREPRENEURS, statutOuvert);
    }

    private Reponse ecrire(UUID lot, String route, CorpsJson corps) throws Exception {
        return appels.ecrire(admin, lot, route, corps);
    }

    private Reponse ok(UUID lot, String route, CorpsJson corps) throws Exception {
        Reponse r = ecrire(lot, route, corps);
        assertThat(r.statut()).as(route + " : " + r.corps()).isEqualTo(200);
        return r;
    }

    private static String tableLignes(String route) {
        return ESTIMATIF.equals(route) ? LIGNES_ESTIMATIF : LIGNES_DEFINITIF;
    }

    private static String tableGroupes(String route) {
        return ESTIMATIF.equals(route) ? GROUPES_ESTIMATIF : GROUPES_DEFINITIF;
    }

    private UUID ligneEnBase(String table, UUID lot, UUID groupe, int ordre, String code, String quantite, String pu, UUID cat) {
        return b.ligne(table, new Ligne(lot, groupe, ordre, code, quantite, pu, cat, false));
    }

    private Map<String, Object> enBase(String table, UUID id) {
        List<Map<String, Object>> l = b.parId(table, id);
        assertThat(l).as(table + " " + id).hasSize(1);
        return l.get(0);
    }

    private int nombre(String table, UUID id) {
        return b.parId(table, id).size();
    }

    private static BigDecimal decimal(Object valeur) {
        return new BigDecimal(String.valueOf(valeur));
    }

    // ------------------------------------------------------------------ R4 : corps invalide → 400

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"categorieManquante", "categorieInconnue", "designationManquante", "ordreLigneManquant",
            "descriptionGroupeManquante", "ordreGroupeManquant", "quantiteNulle", "quantiteNegative", "prixNul", "prixNegatif",
            "tvaAuDessusDe100", "tvaNegative", "groupeIdHorsEnvoi", "groupeIdInconnu", "parentIdHorsEnvoi", "cycleDeParents",
            "idLigneEnDouble", "codeTropLong", "designationTropLongue", "uniteTropLongue", "notesTropLongues",
            "descriptionGroupeTropLongue", "listeLignesNulle"})
    void r4_corpsInvalide_400(String cas) throws Exception {
        UUID lot = lot("R4");
        UUID groupeEnBase = b.groupe(GROUPES_ESTIMATIF, lot, null, 1, "G", false);
        UUID g = UUID.randomUUID();
        UUID idLigne = UUID.randomUUID();
        CorpsJson groupe = groupe(g, null, 1, "1");
        CorpsJson ligne = ligne(idLigne, g, 1, "1.1", "2", "100", categorie);
        List<CorpsJson> groupes = new ArrayList<>(List.of(groupe));
        List<CorpsJson> lignes = new ArrayList<>(List.of(ligne));
        switch (cas) {
            case "categorieManquante" -> ligne.avec(CATEGORIE, null);
            case "categorieInconnue" -> ligne.avec(CATEGORIE, UUID.randomUUID());
            case "designationManquante" -> ligne.avec(DESIGNATION, null);
            case "ordreLigneManquant" -> ligne.avec(ORDRE, null);
            case "descriptionGroupeManquante" -> groupe.avec(DESCRIPTION, null);
            case "ordreGroupeManquant" -> groupe.avec(ORDRE, null);
            case "quantiteNulle" -> ligne.avec(QUANTITE, null);
            case "quantiteNegative" -> ligne.avec(QUANTITE, new BigDecimal("-1"));
            case "prixNul" -> ligne.avec(PU_HT, null);
            case "prixNegatif" -> ligne.avec(PU_HT, new BigDecimal("-0.01"));
            case "tvaAuDessusDe100" -> ligne.avec(TVA, new BigDecimal("100.01"));
            case "tvaNegative" -> ligne.avec(TVA, new BigDecimal("-1"));
            case "groupeIdHorsEnvoi" -> ligne.avec(GROUPE_ID, groupeEnBase);
            case "groupeIdInconnu" -> ligne.avec(GROUPE_ID, UUID.randomUUID());
            case "parentIdHorsEnvoi" -> groupe.avec(PARENT_ID, groupeEnBase);
            case "cycleDeParents" -> {
                UUID h = UUID.randomUUID();
                groupe.avec(PARENT_ID, h);
                groupes.add(groupe(h, g, 2, "2"));
            }
            case "idLigneEnDouble" -> lignes.add(ligne(idLigne, g, 2, "1.2", "1", "10", categorie));
            case "codeTropLong" -> ligne.avec(CODE, "C".repeat(256));
            case "designationTropLongue" -> ligne.avec(DESIGNATION, "D".repeat(256));
            case "uniteTropLongue" -> ligne.avec(UNITE, "U".repeat(51));
            case "notesTropLongues" -> ligne.avec(NOTES, "N".repeat(1001));
            case "descriptionGroupeTropLongue" -> groupe.avec(DESCRIPTION, "G".repeat(256));
            case "listeLignesNulle" -> lignes = null;
            default -> throw new IllegalArgumentException(cas);
        }
        CorpsJson corps = bordereau(groupes, lignes == null ? List.of() : lignes);
        if (lignes == null) {
            corps.avec(LIGNES, null);
        }

        Reponse r = ecrire(lot, ESTIMATIF, corps);

        assertThat(r.statut()).as(cas + " : " + r.corps()).isEqualTo(400);
    }

    // R4 (témoin) : le corps de base des cas invalides est accepté.
    @Test
    void r4_temoin_corpsDeBase_200() throws Exception {
        UUID g = UUID.randomUUID();
        Reponse r = ecrire(lot("R4T"), ESTIMATIF,
                bordereau(List.of(groupe(g, null, 1, "1")), List.of(ligne(UUID.randomUUID(), g, 1, "1.1", "2", "100", categorie))));

        assertThat(r.statut()).as(r.corps()).isEqualTo(200);
    }

    // ------------------------------------------------------------------ R5 : codes uniques (estimatif seulement)

    @Test
    void r5_codeEnDouble_estimatif_400_messageOmranExact() throws Exception {
        CorpsJson corps = bordereau(List.of(), List.of(
                ligne(UUID.randomUUID(), null, 1, "1.1", "1", "10", categorie),
                ligne(UUID.randomUUID(), null, 2, "1.1", "1", "10", categorie)));

        Reponse r = ecrire(lot("R5"), ESTIMATIF, corps);

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(r.statut()).as(r.corps()).isEqualTo(400);
            s.assertThat(detail(r)).isEqualTo("Le code 1.1 doit être unique pour les lignes du bordereaux !");
        });
    }

    @Test
    void r5_codeEnDouble_definitif_200() throws Exception {
        CorpsJson corps = bordereau(List.of(), List.of(
                ligne(UUID.randomUUID(), null, 1, "1.1", "1", "10", categorie),
                ligne(UUID.randomUUID(), null, 2, "1.1", "1", "10", categorie)));

        Reponse r = ecrire(lot("R5D"), DEFINITIF, corps);

        assertThat(r.statut()).as(r.corps()).isEqualTo(200);
    }

    // ------------------------------------------------------------------ R7 : ids d'un autre lot / de l'autre bordereau

    @Test
    void r7_idLigneDUnAutreLot_400() throws Exception {
        UUID lot = lot("R7A");
        UUID autreLot = lot("R7B");
        UUID etrangere = ligneEnBase(LIGNES_ESTIMATIF, autreLot, null, 1, "1", "2", "100", categorie);

        Reponse r = ecrire(lot, ESTIMATIF, bordereau(List.of(), List.of(ligne(etrangere, null, 1, "1", "9", "100", categorie))));

        assertThat(r.statut()).as(r.corps()).isEqualTo(400);
    }

    @Test
    void r7_idLigneDuDefinitifEnvoyeeALEstimatif_400() throws Exception {
        UUID lot = lot("R7C");
        UUID definitive = ligneEnBase(LIGNES_DEFINITIF, lot, null, 1, "1", "2", "100", categorie);

        Reponse r = ecrire(lot, ESTIMATIF, bordereau(List.of(), List.of(ligne(definitive, null, 1, "1", "2", "100", categorie))));

        assertThat(r.statut()).as(r.corps()).isEqualTo(400);
    }

    @Test
    void r7_idLigneDeLEstimatifEnvoyeeAuDefinitif_400() throws Exception {
        UUID lot = lot("R7E");
        UUID estimative = ligneEnBase(LIGNES_ESTIMATIF, lot, null, 1, "1", "2", "100", categorie);

        Reponse r = ecrire(lot, DEFINITIF, bordereau(List.of(), List.of(ligne(estimative, null, 1, "1", "2", "100", categorie))));

        assertThat(r.statut()).as(r.corps()).isEqualTo(400);
    }

    @Test
    void r7_idGroupeDUnAutreLot_400() throws Exception {
        UUID lot = lot("R7D");
        UUID groupeEtranger = b.groupe(GROUPES_DEFINITIF, lot("R7F"), null, 1, "G", false);

        Reponse r = ecrire(lot, DEFINITIF, bordereau(List.of(groupe(groupeEtranger, null, 1, "1")),
                List.of(ligne(UUID.randomUUID(), groupeEtranger, 1, "1.1", "1", "10", categorie))));

        assertThat(r.statut()).as(r.corps()).isEqualTo(400);
    }

    // ------------------------------------------------------------------ R9 / R11 : lignes décomptées du définitif

    @Test
    void r9_suppressionLigneDecomptee_409_message() throws Exception {
        UUID lot = lot("R9A");
        UUID decomptee = ligneEnBase(LIGNES_DEFINITIF, lot, null, 1, "1", "2", "100", categorie);
        UUID libre = ligneEnBase(LIGNES_DEFINITIF, lot, null, 2, "2", "1", "50", categorie);
        b.decompter(lot, decomptee, "BWD1-" + jeton);

        Reponse r = ecrire(lot, DEFINITIF, bordereau(List.of(), List.of(ligne(libre, null, 2, "2", "1", "50", categorie))));

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(r.statut()).as(r.corps()).isEqualTo(409);
            s.assertThat(detail(r)).contains(MSG_LIGNE);
        });
    }

    // R9 : groupe absent dont une ligne d'un sous-groupe est décomptée (la ligne est forcément absente aussi : voir rapport).
    @Test
    void r9_suppressionGroupeContenantUneLigneDecompteeDansUnSousGroupe_409() throws Exception {
        UUID lot = lot("R9B");
        UUID g = b.groupe(GROUPES_DEFINITIF, lot, null, 1, "1", false);
        UUID sg = b.groupe(GROUPES_DEFINITIF, lot, g, 1, "1.1", false);
        UUID decomptee = ligneEnBase(LIGNES_DEFINITIF, lot, sg, 1, "1.1.1", "2", "100", categorie);
        b.decompter(lot, decomptee, "BWD2-" + jeton);

        Reponse r = ecrire(lot, DEFINITIF, vide());

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(r.statut()).as(r.corps()).isEqualTo(409);
            s.assertThat(detail(r)).containsAnyOf(MSG_GROUPE, MSG_LIGNE);
        });
    }

    @Test
    void r9_deplacerLigneDecomptee_autreGroupeOrdreEtCode_200() throws Exception {
        UUID lot = lot("R9C");
        UUID g1 = b.groupe(GROUPES_DEFINITIF, lot, null, 1, "1", false);
        UUID g2 = b.groupe(GROUPES_DEFINITIF, lot, null, 2, "2", false);
        UUID decomptee = ligneEnBase(LIGNES_DEFINITIF, lot, g1, 1, "1.1", "2", "100", categorie);
        b.decompter(lot, decomptee, "BWD3-" + jeton);

        ok(lot, DEFINITIF, bordereau(List.of(groupe(g1, null, 1, "1"), groupe(g2, null, 2, "2")),
                List.of(ligne(decomptee, g2, 5, "2.9", "2", "100", categorie))));

        Map<String, Object> l = enBase(LIGNES_DEFINITIF, decomptee);
        SoftAssertions.assertSoftly(s -> {
            s.assertThat(guid(l.get(GROUPING))).isEqualTo(g2);
            s.assertThat(l.get("Code")).isEqualTo("2.9");
            s.assertThat(l.get("Sorting")).isEqualTo(5);
        });
    }

    // R11 + R15 : quantité et PU d'une ligne décomptée modifiables ; la ligne de décompte n'est pas touchée.
    @Test
    void r11_modifierQuantiteEtPuDUneLigneDecomptee_200_ligneDeDecompteIntacte() throws Exception {
        UUID lot = lot("R11");
        UUID decomptee = ligneEnBase(LIGNES_DEFINITIF, lot, null, 1, "1", "2", "100", categorie);
        UUID ligneDecompte = b.decompter(lot, decomptee, "BWD4-" + jeton);

        ok(lot, DEFINITIF, bordereau(List.of(), List.of(ligne(decomptee, null, 1, "1", "3", "150", categorie))));

        Map<String, Object> l = enBase(LIGNES_DEFINITIF, decomptee);
        Map<String, Object> d = b.lignes("SELECT * FROM PrjWorkingOutLine WHERE Pkey = ?", ligneDecompte.toString()).get(0);
        SoftAssertions.assertSoftly(s -> {
            s.assertThat(decimal(l.get(QUANTITY))).isEqualByComparingTo("3");
            s.assertThat(decimal(l.get("UnitPriceHT"))).isEqualByComparingTo("150");
            s.assertThat(decimal(d.get(QUANTITY))).isEqualByComparingTo("1");
            s.assertThat(decimal(d.get("UnitPriceHT"))).isEqualByComparingTo("100");
            s.assertThat(guid(d.get("PrjMarketLineId"))).isEqualTo(decomptee);
        });
    }

    // ------------------------------------------------------------------ §3 : insertion + modification + suppression

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {ESTIMATIF, DEFINITIF})
    void insertionModificationSuppression_memeEnvoi_200_bordereauRelu(String route) throws Exception {
        UUID lot = lot("IMS");
        String table = tableLignes(route);
        UUID a = ligneEnBase(table, lot, null, 1, "1", "2", "100", categorie);
        UUID supprimee = ligneEnBase(table, lot, null, 2, "2", "1", "100", categorie);
        UUID nouvelle = UUID.randomUUID();

        Reponse r = ok(lot, route, bordereau(List.of(), List.of(
                ligne(a, null, 1, "1", "5", "100", categorie).avec(DESIGNATION, "Modifiée"),
                ligne(nouvelle, null, 3, "3", "1", "40", categorie))));

        Map<String, Object> la = enBase(table, a);
        SoftAssertions.assertSoftly(s -> {
            s.assertThat(r.ids(LIGNES_IDS)).as("relu").containsExactly(a.toString(), nouvelle.toString());
            s.assertThat(r.valeur("$.lotId")).isEqualTo(lot.toString());
            s.assertThat(decimal(la.get(QUANTITY))).isEqualByComparingTo("5");
            s.assertThat(la.get(DESCRIPTION_COL)).isEqualTo("Modifiée");
            s.assertThat(nombre(table, supprimee)).as("supprimée").isZero();
            s.assertThat(guid(enBase(table, nouvelle).get("PrjMarketId"))).as("insérée").isEqualTo(lot);
        });
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {ESTIMATIF, DEFINITIF})
    void nouvelleLigne_rattacheeANouveauSousGroupeDUnNouveauGroupe_200(String route) throws Exception {
        UUID lot = lot("NG");
        UUID n = UUID.randomUUID();
        UUID ns = UUID.randomUUID();
        UUID l = UUID.randomUUID();

        Reponse r = ok(lot, route, bordereau(List.of(groupe(n, null, 1, "1"), groupe(ns, n, 1, "1.1")),
                List.of(ligne(l, ns, 1, "1.1.1", "1", "10", categorie))));

        Map<String, Object> gn = enBase(tableGroupes(route), n);
        Map<String, Object> gns = enBase(tableGroupes(route), ns);
        SoftAssertions.assertSoftly(s -> {
            s.assertThat(r.ids(GROUPES_IDS)).containsExactlyInAnyOrder(n.toString(), ns.toString());
            s.assertThat(guid(gn.get("PrjMarketId"))).isEqualTo(lot);
            s.assertThat(gn.get(DESCRIPTION_COL)).isEqualTo("GROUPE 1");
            s.assertThat(guid(gns.get("ParentId"))).isEqualTo(n);
            s.assertThat(guid(enBase(tableLignes(route), l).get(GROUPING))).isEqualTo(ns);
        });
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {ESTIMATIF, DEFINITIF})
    void r10_groupeSupprimeAvecSousGroupeEtLignes_200_autreGroupeConserve(String route) throws Exception {
        UUID lot = lot("GS");
        String groupes = tableGroupes(route);
        UUID g = b.groupe(groupes, lot, null, 1, "1", false);
        UUID sg = b.groupe(groupes, lot, g, 1, "1.1", false);
        UUID ls = ligneEnBase(tableLignes(route), lot, sg, 1, "1.1.1", "1", "10", categorie);
        UUID k = b.groupe(groupes, lot, null, 2, "2", false);
        UUID lk = ligneEnBase(tableLignes(route), lot, k, 2, "2.1", "1", "10", categorie);

        ok(lot, route, bordereau(List.of(groupe(k, null, 2, "2")), List.of(ligne(lk, k, 2, "2.1", "1", "10", categorie))));

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(nombre(groupes, g)).isZero();
            s.assertThat(nombre(groupes, sg)).isZero();
            s.assertThat(nombre(tableLignes(route), ls)).isZero();
            s.assertThat(nombre(groupes, k)).isEqualTo(1);
            s.assertThat(nombre(tableLignes(route), lk)).isEqualTo(1);
        });
    }

    // ------------------------------------------------------------------ R6 : PU TTC recalculé, quantité à 6 décimales

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {ESTIMATIF, DEFINITIF})
    void r6_puTTC_recalcule_htFodecTva_puTTCEnvoyeIgnore(String route) throws Exception {
        UUID lot = lot("R6");
        UUID l = UUID.randomUUID();

        Reponse r = ok(lot, route, bordereau(List.of(), List.of(ligne(l, null, 1, "1", "2", "100", categorie)
                .avec(FODEC, BigDecimal.ONE).avec("prixUnitaireTTC", new BigDecimal("999")))));

        Map<String, Object> enBase = enBase(tableLignes(route), l);
        SoftAssertions.assertSoftly(s -> {
            s.assertThat(decimal(enBase.get("UnitPriceTTC"))).as("100 × 1,01 × 1,2").isEqualByComparingTo("121.2");
            s.assertThat(decimal(enBase.get("VatRatio"))).as("ratio").isEqualByComparingTo("0.2");
            s.assertThat(decimal(enBase.get("FODECRatio"))).as("ratio").isEqualByComparingTo("0.01");
            s.assertThat(r.nombre("$.lignes[0].tauxTVA")).isEqualByComparingTo("20");
            s.assertThat(r.nombre("$.lignes[0].tauxFODEC")).isEqualByComparingTo("1");
            s.assertThat(r.nombre("$.lignes[0].totalTTC")).isEqualByComparingTo("242.4");
        });
    }

    @Test
    void r6_quantiteArrondieA6Decimales() throws Exception {
        UUID lot = lot("R6Q");
        UUID l = UUID.randomUUID();

        Reponse r = ok(lot, ESTIMATIF, bordereau(List.of(),
                List.of(ligne(l, null, 1, "1", "1.2345675", "100", categorie))));

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(decimal(enBase(LIGNES_ESTIMATIF, l).get(QUANTITY))).isEqualByComparingTo("1.234568");
            s.assertThat(r.nombre("$.lignes[0].quantite")).isEqualByComparingTo("1.234568");
        });
    }

    // ------------------------------------------------------------------ R14 : colonnes d'audit

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {ESTIMATIF, DEFINITIF})
    void r14_insertion_userCreateEtDate_surLigneGroupeEtVentilation(String route) throws Exception {
        UUID lot = lot("R14I");
        UUID g = UUID.randomUUID();
        UUID l = UUID.randomUUID();

        ok(lot, route, bordereau(List.of(groupe(g, null, 1, "1")), List.of(ligne(l, g, 1, "1.1", "1", "10", categorie))));

        Map<String, Object> ligne = enBase(tableLignes(route), l);
        Map<String, Object> groupe = enBase(tableGroupes(route), g);
        List<Map<String, Object>> ventilations = b.ventilations(lot);
        SoftAssertions.assertSoftly(s -> {
            s.assertThat(String.valueOf(ligne.get("UserCreate"))).isNotBlank().isNotEqualTo("null");
            s.assertThat(ligne.get("UserCreateDateTime")).isNotNull();
            s.assertThat(String.valueOf(groupe.get("UserCreate"))).isNotBlank().isNotEqualTo("null");
            s.assertThat(groupe.get("UserCreateDateTime")).isNotNull();
            s.assertThat(ventilations).isNotEmpty().allSatisfy(v -> {
                assertThat(v.get("UserCreate")).isNotNull();
                assertThat(v.get("UserCreateDateTime")).isNotNull();
            });
        });
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {ESTIMATIF, DEFINITIF})
    void r14_modification_userUpdateEtDate_surLigneEtGroupe(String route) throws Exception {
        UUID lot = lot("R14M");
        UUID g = b.groupe(tableGroupes(route), lot, null, 1, "1", false);
        UUID l = ligneEnBase(tableLignes(route), lot, g, 1, "1.1", "1", "10", categorie);

        ok(lot, route, bordereau(List.of(groupe(g, null, 1, "1").avec(DESCRIPTION, "Groupe modifié")),
                List.of(ligne(l, g, 1, "1.1", "4", "10", categorie))));

        Map<String, Object> ligne = enBase(tableLignes(route), l);
        Map<String, Object> groupe = enBase(tableGroupes(route), g);
        SoftAssertions.assertSoftly(s -> {
            s.assertThat(ligne.get("UserUpdate")).isNotNull();
            s.assertThat(ligne.get("UserUpdateDateTime")).isNotNull();
            s.assertThat(groupe.get("UserUpdate")).isNotNull();
            s.assertThat(groupe.get("UserUpdateDateTime")).isNotNull();
        });
    }

    // ------------------------------------------------------------------ R12 : ventilation par catégorie

    @Test
    void r12_deuxCategories_75_25_horsStock_lotSansFournisseur_baseEstimative() throws Exception {
        UUID lot = lot("V1");
        UUID stock = b.categorie("BWS-" + jeton);
        b.ligne(LIGNES_ESTIMATIF, new Ligne(lot, null, 9, "S", "10", "1000", stock, true));

        ok(lot, ESTIMATIF, bordereau(List.of(), List.of(
                ligne(UUID.randomUUID(), null, 1, "1", "3", "100", categorie),
                ligne(UUID.randomUUID(), null, 2, "2", "1", "100", autreCategorie))));

        List<Map<String, Object>> v = b.ventilations(lot);
        SoftAssertions.assertSoftly(s -> {
            s.assertThat(v).hasSize(2);
            s.assertThat(guid(v.get(0).get(CATEGORIE_COL))).isEqualTo(categorie);
            s.assertThat(decimal(v.get(0).get(TAUX))).isEqualByComparingTo("0.75");
            s.assertThat(guid(v.get(1).get(CATEGORIE_COL))).isEqualTo(autreCategorie);
            s.assertThat(decimal(v.get(1).get(TAUX))).isEqualByComparingTo("0.25");
        });
    }

    @Test
    void r12_troisCategoriesEgales_tauxA6Decimales_sommeExactementUn() throws Exception {
        UUID lot = lot("V2");
        UUID troisieme = b.categorie("BWT-" + jeton);

        ok(lot, ESTIMATIF, bordereau(List.of(), List.of(
                ligne(UUID.randomUUID(), null, 1, "1", "1", "100", categorie),
                ligne(UUID.randomUUID(), null, 2, "2", "1", "100", autreCategorie),
                ligne(UUID.randomUUID(), null, 3, "3", "1", "100", troisieme))));

        List<BigDecimal> taux = b.ventilations(lot).stream().map(v -> decimal(v.get(TAUX))).toList();
        SoftAssertions.assertSoftly(s -> {
            s.assertThat(taux).hasSize(3).allSatisfy(t -> assertThat(t).isIn(
                    new BigDecimal("0.333333"), new BigDecimal("0.333334")));
            s.assertThat(taux.stream().reduce(BigDecimal.ZERO, BigDecimal::add)).isEqualByComparingTo("1");
        });
    }

    @Test
    void r12_lotAvecFournisseur_baseDefinitive_memeApresEcritureDeLEstimatif() throws Exception {
        UUID lot = lot("V3");
        b.fournisseur(lot, lots.fournisseur("BWF-" + jeton));
        ligneEnBase(LIGNES_DEFINITIF, lot, null, 1, "1", "1", "100", autreCategorie);

        ok(lot, ESTIMATIF, bordereau(List.of(), List.of(ligne(UUID.randomUUID(), null, 1, "1", "5", "100", categorie))));

        List<Map<String, Object>> v = b.ventilations(lot);
        SoftAssertions.assertSoftly(s -> {
            s.assertThat(v).hasSize(1);
            s.assertThat(guid(v.get(0).get(CATEGORIE_COL))).isEqualTo(autreCategorie);
            s.assertThat(decimal(v.get(0).get(TAUX))).isEqualByComparingTo("1");
        });
    }

    @Test
    void r12_lotSansFournisseur_ecritureDuDefinitif_baseEstimative() throws Exception {
        UUID lot = lot("V4");
        ligneEnBase(LIGNES_ESTIMATIF, lot, null, 1, "1", "1", "100", categorie);

        ok(lot, DEFINITIF, bordereau(List.of(), List.of(ligne(UUID.randomUUID(), null, 1, "1", "5", "100", autreCategorie))));

        List<Map<String, Object>> v = b.ventilations(lot);
        SoftAssertions.assertSoftly(s -> {
            s.assertThat(v).hasSize(1);
            s.assertThat(guid(v.get(0).get(CATEGORIE_COL))).isEqualTo(categorie);
            s.assertThat(decimal(v.get(0).get(TAUX))).isEqualByComparingTo("1");
        });
    }

    @Test
    void r12_bordereauVide_uneLigneAUnSurLaCategorieDuLot_ancienneVentilationRemplacee() throws Exception {
        UUID lot = lot("V5");
        UUID categorieDuLot = b.categorie("BWL-" + jeton);
        b.defauts(lot, null, categorieDuLot);
        ligneEnBase(LIGNES_ESTIMATIF, lot, null, 1, "1", "1", "100", categorie);
        b.ventilation(lot, autreCategorie, "1");

        ok(lot, ESTIMATIF, vide());

        List<Map<String, Object>> v = b.ventilations(lot);
        SoftAssertions.assertSoftly(s -> {
            s.assertThat(v).hasSize(1);
            s.assertThat(guid(v.get(0).get(CATEGORIE_COL))).isEqualTo(categorieDuLot);
            s.assertThat(decimal(v.get(0).get(TAUX))).isEqualByComparingTo("1");
        });
    }

    // ------------------------------------------------------------------ R15 : stock et autre bordereau intacts

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {ESTIMATIF, DEFINITIF})
    void r15_lignesEtGroupesDeStock_etAutreBordereau_intacts(String route) throws Exception {
        UUID lot = lot("ST");
        String autre = ESTIMATIF.equals(route) ? DEFINITIF : ESTIMATIF;
        UUID groupeStock = b.groupe(tableGroupes(route), lot, null, 9, "S", true);
        UUID ligneStock = b.ligne(tableLignes(route), new Ligne(lot, groupeStock, 9, "S1", "7", "10", categorie, true));
        UUID ligneAutre = ligneEnBase(tableLignes(autre), lot, null, 1, "1", "3", "10", categorie);

        ok(lot, route, vide());

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(nombre(tableGroupes(route), groupeStock)).isEqualTo(1);
            s.assertThat(decimal(enBase(tableLignes(route), ligneStock).get(QUANTITY))).isEqualByComparingTo("7");
            s.assertThat(guid(enBase(tableLignes(route), ligneStock).get(GROUPING))).isEqualTo(groupeStock);
            s.assertThat(decimal(enBase(tableLignes(autre), ligneAutre).get(QUANTITY))).isEqualByComparingTo("3");
        });
    }

    // ------------------------------------------------------------------ R13 : totaux relus, aucune colonne montant du lot écrite

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {ESTIMATIF, DEFINITIF})
    void r13_totauxRelus_egalSommeDesLignesEnvoyees_montantsDuLotInchanges(String route) throws Exception {
        UUID lot = lot("TO");

        Reponse r = ok(lot, route, bordereau(List.of(), List.of(
                ligne(UUID.randomUUID(), null, 1, "1", "2", "100", categorie),
                ligne(UUID.randomUUID(), null, 2, "2", "3", "50", categorie).avec(TVA, BigDecimal.ZERO))));

        Map<String, Object> lotEnBase = b.lignes("SELECT * FROM PrjMarket WHERE PKey = ?", lot.toString()).get(0);
        SoftAssertions.assertSoftly(s -> {
            s.assertThat(r.nombre("$.totaux.totalHT")).as("200 + 150").isEqualByComparingTo("350");
            s.assertThat(r.nombre("$.totaux.totalTVA")).isEqualByComparingTo("40");
            s.assertThat(r.nombre("$.totaux.totalTTC")).isEqualByComparingTo("390");
            s.assertThat(r.nombre("$.totaux.nbLignes")).isEqualByComparingTo("2");
            s.assertThat(decimal(lotEnBase.get("EstimatedPriceHTManual"))).isEqualByComparingTo("0");
            s.assertThat(decimal(lotEnBase.get("EstimatedPriceTTCManual"))).isEqualByComparingTo("0");
        });
    }

    // ------------------------------------------------------------------ §4 lecture : champs ajoutés

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {ESTIMATIF, DEFINITIF})
    void lecture_ligne_tauxFODEC_categorieId_notes(String route) throws Exception {
        UUID lot = lot("LF");
        UUID l = ligneEnBase(tableLignes(route), lot, null, 1, "1", "1", "100", categorie);
        b.fodec(tableLignes(route), l, "0.01");

        Reponse r = appels.lire(admin, lot, route);

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(r.nombre("$.lignes[0].tauxFODEC")).as(r.corps()).isEqualByComparingTo("1");
            s.assertThat(r.valeur("$.lignes[0].categorieId")).isEqualTo(categorie.toString());
            s.assertThat(r.valeur("$.lignes[0].notes")).isEqualTo("Note 1");
        });
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {ESTIMATIF, DEFINITIF})
    void r8_lecture_tvaParDefautEnPourcent_categorieParDefautId(String route) throws Exception {
        UUID lot = lot("LD");
        b.defauts(lot, "0.2", categorie);

        Reponse r = appels.lire(admin, lot, route);

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(r.nombre("$.tvaParDefaut")).as(r.corps()).isEqualByComparingTo("20");
            s.assertThat(r.valeur("$.categorieParDefautId")).isEqualTo(categorie.toString());
        });
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {ESTIMATIF, DEFINITIF})
    void r8_lecture_lotSansDefauts_null(String route) throws Exception {
        UUID lot = lot("LN");
        b.defauts(lot, null, null);

        Reponse r = appels.lire(admin, lot, route);

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(r.valeur("$.tvaParDefaut")).as(r.corps()).isNull();
            s.assertThat(r.valeur("$.categorieParDefautId")).isNull();
        });
    }

    // §11.5 : decomptee = ligne définitive citée par une PrjWorkingOutLine ; toujours faux à l'estimatif.
    @Test
    void lecture_definitif_decomptee_vraiSiCiteeParUnDecompte() throws Exception {
        UUID lot = lot("LC");
        UUID citee = ligneEnBase(LIGNES_DEFINITIF, lot, null, 1, "1", "1", "100", categorie);
        UUID libre = ligneEnBase(LIGNES_DEFINITIF, lot, null, 2, "2", "1", "100", categorie);
        b.decompter(lot, citee, "BWD5-" + jeton);

        Reponse r = appels.lire(admin, lot, DEFINITIF);

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(String.valueOf(r.valeur("$.lignes[?(@.id == '" + citee + "')].decomptee"))).as(r.corps())
                    .isEqualTo("[true]");
            s.assertThat(String.valueOf(r.valeur("$.lignes[?(@.id == '" + libre + "')].decomptee"))).isEqualTo("[false]");
        });
    }

    @Test
    void lecture_estimatif_decomptee_toujoursFaux() throws Exception {
        UUID lot = lot("LE");
        ligneEnBase(LIGNES_ESTIMATIF, lot, null, 1, "1", "1", "100", categorie);
        UUID definitive = ligneEnBase(LIGNES_DEFINITIF, lot, null, 1, "1", "1", "100", categorie);
        b.decompter(lot, definitive, "BWD6-" + jeton);

        Reponse r = appels.lire(admin, lot, ESTIMATIF);

        assertThat(r.valeur("$.lignes[0].decomptee")).as(r.corps()).isEqualTo(false);
    }
}
