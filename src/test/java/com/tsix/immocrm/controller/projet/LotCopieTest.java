package com.tsix.immocrm.controller.projet;

import com.tsix.immocrm.controller.projet.LotsPariteSupport.Creation;
import com.tsix.immocrm.fixtures.FixtureLotFiche;
import com.tsix.immocrm.fixtures.FixtureLotsParite;
import com.tsix.immocrm.fixtures.FixturePatrimoine;
import com.tsix.immocrm.fixtures.FixtureProjetLots;
import jakarta.persistence.EntityManager;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Stream;

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
 * garde d'unicité, « (Copie) », {@code IsClosed = false}, statut de départ du genre, {@code StrHistory} « Création », bordereau
 * recopié dans l'estimatif de la copie (réponse du manager 3 amendée, {@code MarketMonitoring.cs:Clone}). Droit {@code toolNew}
 * (Entrepreneurs) / {@code toolNewMultiSupplier} (autres genres), 403 avant 404.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class LotCopieTest {

    private static final String LOT_PAR_ID = "SELECT * FROM PrjMarket WHERE PKey = ?";
    private static final String COPIES = "SELECT COUNT(*) FROM PrjMarket WHERE Description = ?";

    private static final String LIGNES_DEF = "PrjMarketLine";
    private static final String GROUPES_DEF = "PrjMarketLineGrouping";
    private static final String LIGNES_EST = "PrjMarketLineEstimation";
    private static final String GROUPES_EST = "PrjMarketLineGroupingEstimation";
    private static final String PKEY = "PKey";
    private static final String CODE = "Code";
    private static final String DESCRIPTION = "Description";
    private static final String NOTES = "Notes";
    private static final String SORTING = "Sorting";
    private static final String PARENT = "ParentId";
    private static final String GROUPE = "PrjMarketLineGroupingId";
    private static final String FOURNISSEUR = "CfgSupplierId";
    private static final String CAUSE = "CancellationCause";
    private static final String DELAI_BASE = "DelayManual";
    private static final List<String> DATES_VIDEES = List.of("LaunchOfTenderDate", "WithdrawalTenderDocumentsDate",
            "TenderMaturityDate", "TendersResortDate", "BidOpeningDate", "TendersReportDate", "TenderDate", "ContractSignatureDate",
            "ServiceOrderDate", "WorkLaunchDate", "WorkCompletionDate", "ReattachmentDate", "CancellationDate");

    // Absente de yesRemsTest (définition de yesRemsProd) ; créée dans la transaction du test, annulée avec elle.
    private static final String TABLE_GROUPES_EST = """
            IF OBJECT_ID('dbo.PrjMarketLineGroupingEstimation', 'U') IS NULL
            CREATE TABLE dbo.PrjMarketLineGroupingEstimation (PKey uniqueidentifier NOT NULL PRIMARY KEY, Code nvarchar(255) NULL,
                Description nvarchar(255) NULL, Sorting int NULL, ParentId uniqueidentifier NULL, PrjMarketId uniqueidentifier NULL,
                UserCreate nvarchar(255) NULL, UserCreateDateTime datetime NULL, UserUpdate nvarchar(255) NULL,
                UserUpdateDateTime datetime NULL, IsStock bit NULL, Notes nvarchar(1000) NULL)""";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureLotsParite parite;
    @Autowired private FixtureLotFiche fiche;
    @Autowired private FixtureProjetLots lots;
    @Autowired private Clock clock;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private EntityManager em;

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
        maj(TABLE_GROUPES_EST);
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
            s.assertThat(copie.get(FOURNISSEUR)).as("Q3 suite (1) : hors multi-fournisseur, fournisseur vidé").isNull();
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
            s.assertThat(statut).isNotEmpty().allSatisfy(st -> {
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

    // ------------------------------------------------------------------ bordereau (Q3 amendée, MarketMonitoring.cs:Clone l.2904-3103)

    // Q3 : lot multi-fournisseur → estimatif source (groupes, sous-groupes, lignes) vers l'estimatif de la copie ; définitif ignoré.
    @Test
    void q3_multiFournisseur_estimatifSource_copieDansEstimatif_definitifCopieVide() throws Exception {
        UUID multi = parite.lot(projet, "CPB-" + jeton, FOURNISSEURS, parite.statut(FOURNISSEURS, "CPBS-" + jeton));
        Arbre source = arbre(GROUPES_EST, LIGNES_EST, multi, "E");
        ligne(LIGNES_DEF, multi, code("D", "L"), null);

        Creation c = copierOk(admin, multi);

        verifierEstimatifCopie(c.id(), source, "E");
        verifierDefinitifVide(c.id());
        verifierSourceIntacte(multi, 1, 0, 3, 2);
    }

    // Q3 : hors multi-fournisseur, avec lignes définitives → définitif source vers l'estimatif de la copie ; estimatif source ignoré.
    @Test
    void q3_horsMulti_definitifSource_copieDansEstimatif_estimatifSourceIgnore() throws Exception {
        Arbre source = arbre(GROUPES_DEF, LIGNES_DEF, lot, "D");
        UUID groupeEstimatif = groupe(GROUPES_EST, lot, code("X", "G"), null);
        ligne(LIGNES_EST, lot, code("X", "L"), groupeEstimatif);

        Creation c = copierOk(admin, lot);

        verifierEstimatifCopie(c.id(), source, "D");
        verifierDefinitifVide(c.id());
        verifierSourceIntacte(lot, 3, 2, 1, 1);
    }

    // Q3 : hors multi-fournisseur, aucune ligne définitive (un groupe définitif vide ne compte pas) → estimatif source recopié.
    @Test
    void q3_horsMulti_sansLigneDefinitive_estimatifSource_copieDansEstimatif() throws Exception {
        Arbre source = arbre(GROUPES_EST, LIGNES_EST, lot, "E");
        groupe(GROUPES_DEF, lot, code("V", "G"), null);

        Creation c = copierOk(admin, lot);

        verifierEstimatifCopie(c.id(), source, "E");
        verifierDefinitifVide(c.id());
        verifierSourceIntacte(lot, 0, 1, 3, 2);
    }

    // ------------------------------------------------------------------ en-tête de la copie (Q3 suite, Clone l.3135-3155)

    // Q3 suite (1) : le fournisseur n'est vidé que hors multi-fournisseur (la branche multi de Clone ne le touche pas).
    @Test
    void q3s_multiFournisseur_fournisseurConserve() throws Exception {
        UUID multi = parite.lot(projet, "CPF2-" + jeton, FOURNISSEURS, parite.statut(FOURNISSEURS, "CPF2S-" + jeton));
        UUID fournisseur = lots.fournisseur("CPF2F-" + jeton);
        fiche.fournisseurEtType(multi, fournisseur, null);

        Creation c = copierOk(admin, multi);

        assertThat(guid(enBase(c.id()).get(FOURNISSEUR))).isEqualTo(guid(fournisseur));
    }

    // Q3 suite (2) : hors multi-fournisseur, 13 dates d'appel d'offres / marché et cause de résiliation vidées ;
    // StartDateManual et DelayManual recopiés.
    @Test
    void q3s_horsMulti_datesMarcheEtResiliationVidees_debutEtDelaiDeBaseRecopies() throws Exception {
        LocalDate jour = LocalDate.of(2026, 3, 14);
        LocalDate debutBase = LocalDate.of(2026, 1, 5);
        String cause = "Cause CP-" + jeton;
        List<Object> valeurs = new ArrayList<>();
        DATES_VIDEES.forEach(d -> valeurs.add(FixtureLotFiche.jour(jour)));
        valeurs.addAll(List.of(cause, FixtureLotFiche.jour(debutBase), 45, s(lot)));
        maj("UPDATE PrjMarket SET " + String.join(" = ?, ", DATES_VIDEES)
                + " = ?, CancellationCause = ?, StartDateManual = ?, DelayManual = ? WHERE PKey = ?", valeurs.toArray());

        Creation c = copierOk(admin, lot);

        Map<String, Object> copie = enBase(c.id());
        SoftAssertions.assertSoftly(s -> {
            DATES_VIDEES.forEach(d -> s.assertThat(copie.get(d)).as(d).isNull());
            s.assertThat((String) copie.get(CAUSE)).as(CAUSE).isNullOrEmpty();
            s.assertThat(copie.get("StartDateManual")).as("StartDateManual").isEqualTo(FixtureLotFiche.jour(debutBase));
            s.assertThat(copie).containsEntry(DELAI_BASE, 45);
        });
        assertThat(enBase(lot)).as("source intacte").containsEntry(DELAI_BASE, 45).containsEntry(CAUSE, cause);
    }

    // Q3 suite (2) : source sans bordereau estimatif → EstimatedPrice*Manual de la copie = forfait saisi sur la source.
    @Test
    void q3s_horsMulti_sourceSansEstimatif_estimationManuelleRecopiee() throws Exception {
        forfait(lot, "1000", "190", "1190");

        Creation c = copierOk(admin, lot);

        verifierForfait(c.id(), "1000", "190", "1190");
    }

    // Q3 suite (2) : source avec bordereau estimatif → EstimatedPrice*Manual de la copie = Σ estimatif source
    // (HT = PU × (1 + FODEC) × Qté, TTC = HT × (1 + TVA) : 250 × 1,01 × 3 = 757,5 ; × 1,19 = 901,425), pas le forfait.
    @Test
    void q3s_horsMulti_sourceAvecEstimatif_estimationManuelle_egaleSommeEstimatifSource() throws Exception {
        forfait(lot, "1000", "190", "1190");
        ligne(LIGNES_EST, lot, code("F", "L"), null);

        Creation c = copierOk(admin, lot);

        verifierForfait(c.id(), "757.5", "143.925", "901.425");
    }

    private void forfait(UUID cible, String ht, String tva, String ttc) {
        maj("UPDATE PrjMarket SET EstimatedPriceHTManual = ?, EstimatedVatAmountManual = ?, EstimatedPriceTTCManual = ? WHERE PKey = ?",
                new BigDecimal(ht), new BigDecimal(tva), new BigDecimal(ttc), s(cible));
    }

    private void verifierForfait(UUID copie, String ht, String tva, String ttc) {
        Map<String, Object> m = enBase(copie);
        SoftAssertions.assertSoftly(s -> {
            s.assertThat(LotsPariteSupport.nombre(m.get("EstimatedPriceHTManual"))).as("HT").isEqualByComparingTo(ht);
            s.assertThat(LotsPariteSupport.nombre(m.get("EstimatedVatAmountManual"))).as("TVA").isEqualByComparingTo(tva);
            s.assertThat(LotsPariteSupport.nombre(m.get("EstimatedPriceTTCManual"))).as("TTC").isEqualByComparingTo(ttc);
        });
    }

    /** Groupe G, sous-groupe SG (ParentId = G) ; lignes LG (dans G), LSG (dans SG), LS (sans groupe). */
    private record Arbre(UUID groupe, UUID sousGroupe, UUID ligneGroupe, UUID ligneSousGroupe, UUID ligneSeule) {
        List<String> cles() {
            return Stream.of(groupe, sousGroupe, ligneGroupe, ligneSousGroupe, ligneSeule).map(FixturePatrimoine::guid).toList();
        }
    }

    private String code(String prefixe, String nom) {
        return prefixe + nom + "-" + jeton;
    }

    private Arbre arbre(String tableGroupes, String tableLignes, UUID source, String prefixe) {
        UUID g = groupe(tableGroupes, source, code(prefixe, "G"), null);
        UUID sg = groupe(tableGroupes, source, code(prefixe, "SG"), g);
        return new Arbre(g, sg, ligne(tableLignes, source, code(prefixe, "LG"), g),
                ligne(tableLignes, source, code(prefixe, "LSG"), sg), ligne(tableLignes, source, code(prefixe, "LS"), null));
    }

    private void maj(String sql, Object... parametres) {
        em.flush();
        jdbc.update(sql, parametres);
        em.clear();
    }

    private static String s(UUID id) {
        return id == null ? null : id.toString();
    }

    private UUID groupe(String table, UUID lot, String code, UUID parent) {
        UUID id = UUID.randomUUID();
        maj("INSERT INTO " + table + " (PKey, Code, Description, Sorting, Notes, ParentId, PrjMarketId, IsStock) VALUES (?, ?, ?, 2, ?, ?, ?, 0)",
                s(id), code, "Groupe " + code, "Note " + code, s(parent), s(lot));
        return id;
    }

    private UUID ligne(String table, UUID lot, String code, UUID groupe) {
        UUID id = UUID.randomUUID();
        maj("INSERT INTO " + table + """
                 (PKey, Code, Description, Sorting, Unity, Quantity, UnitPriceHT, VatRatio, UnitPriceTTC, FODECRatio, Notes, IsStock,
                    PrjMarketId, PrjMarketLineGroupingId)
                VALUES (?, ?, ?, 4, N'm2', 3, 250, 0.19, 297.5, 0.01, ?, 0, ?, ?)""",
                s(id), code, "Ligne " + code, "Note " + code, s(lot), s(groupe));
        return id;
    }

    private List<Map<String, Object>> duLot(String table, UUID lot) {
        return fiche.lignes("SELECT * FROM " + table + " WHERE PrjMarketId = ?", lot.toString());
    }

    private static Map<String, Object> parCode(List<Map<String, Object>> lignes, String code) {
        return lignes.stream().filter(l -> code.equals(l.get(CODE))).findFirst().orElseThrow();
    }

    /** L'arbre source se retrouve, à l'identique et sous de nouvelles clés, dans l'estimatif de la copie — et rien d'autre. */
    private void verifierEstimatifCopie(UUID copie, Arbre source, String prefixe) {
        List<Map<String, Object>> groupes = duLot(GROUPES_EST, copie);
        List<Map<String, Object>> lignes = duLot(LIGNES_EST, copie);
        assertThat(groupes).as("groupes estimatifs de la copie").extracting(l -> l.get(CODE))
                .containsExactlyInAnyOrder(code(prefixe, "G"), code(prefixe, "SG"));
        assertThat(lignes).as("lignes estimatives de la copie").extracting(l -> l.get(CODE))
                .containsExactlyInAnyOrder(code(prefixe, "LG"), code(prefixe, "LSG"), code(prefixe, "LS"));
        Map<String, Object> g = parCode(groupes, code(prefixe, "G"));
        Map<String, Object> sg = parCode(groupes, code(prefixe, "SG"));
        Map<String, Object> lg = parCode(lignes, code(prefixe, "LG"));
        Map<String, Object> lsg = parCode(lignes, code(prefixe, "LSG"));
        Map<String, Object> ls = parCode(lignes, code(prefixe, "LS"));
        SoftAssertions.assertSoftly(s -> {
            s.assertThat(Stream.concat(groupes.stream(), lignes.stream()).map(r -> guid(r.get(PKEY))).toList())
                    .as("nouvelles clés").doesNotContainAnyElementsOf(source.cles());
            s.assertThat(g.get(PARENT)).as("groupe racine").isNull();
            s.assertThat(guid(sg.get(PARENT))).as("sous-groupe → groupe copié").isEqualTo(guid(g.get(PKEY)));
            s.assertThat(guid(lg.get(GROUPE))).as("ligne → groupe copié").isEqualTo(guid(g.get(PKEY)));
            s.assertThat(guid(lsg.get(GROUPE))).as("ligne → sous-groupe copié").isEqualTo(guid(sg.get(PKEY)));
            s.assertThat(ls.get(GROUPE)).as("ligne sans groupe").isNull();
            s.assertThat(g).containsEntry(DESCRIPTION, "Groupe " + code(prefixe, "G")).containsEntry(SORTING, 2)
                    .containsEntry(NOTES, "Note " + code(prefixe, "G"));
            s.assertThat(lg).containsEntry(DESCRIPTION, "Ligne " + code(prefixe, "LG")).containsEntry(SORTING, 4)
                    .containsEntry("Unity", "m2").containsEntry(NOTES, "Note " + code(prefixe, "LG"));
            s.assertThat(LotsPariteSupport.nombre(lg.get("Quantity"))).as("Quantity").isEqualByComparingTo("3");
            s.assertThat(LotsPariteSupport.nombre(lg.get("UnitPriceHT"))).as("UnitPriceHT").isEqualByComparingTo("250");
            s.assertThat(LotsPariteSupport.nombre(lg.get("VatRatio"))).as("VatRatio").isEqualByComparingTo("0.19");
            s.assertThat(LotsPariteSupport.nombre(lg.get("UnitPriceTTC"))).as("UnitPriceTTC").isEqualByComparingTo("297.5");
            s.assertThat(LotsPariteSupport.nombre(lg.get("FODECRatio"))).as("FODECRatio").isEqualByComparingTo("0.01");
        });
    }

    private void verifierDefinitifVide(UUID copie) {
        SoftAssertions.assertSoftly(s -> {
            s.assertThat(duLot(LIGNES_DEF, copie)).as("lignes définitives de la copie").isEmpty();
            s.assertThat(duLot(GROUPES_DEF, copie)).as("groupes définitifs de la copie").isEmpty();
        });
    }

    private void verifierSourceIntacte(UUID source, int lignesDef, int groupesDef, int lignesEst, int groupesEst) {
        SoftAssertions.assertSoftly(s -> {
            s.assertThat(duLot(LIGNES_DEF, source)).as("source : lignes définitives").hasSize(lignesDef);
            s.assertThat(duLot(GROUPES_DEF, source)).as("source : groupes définitifs").hasSize(groupesDef);
            s.assertThat(duLot(LIGNES_EST, source)).as("source : lignes estimatives").hasSize(lignesEst);
            s.assertThat(duLot(GROUPES_EST, source)).as("source : groupes estimatifs").hasSize(groupesEst);
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
