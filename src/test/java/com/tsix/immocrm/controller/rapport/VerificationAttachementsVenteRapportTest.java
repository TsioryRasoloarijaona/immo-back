package com.tsix.immocrm.controller.rapport;

import com.tsix.immocrm.entities.adm.AdmAttachedFileType;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.stk.StkItemCategory;
import com.tsix.immocrm.entities.stk.StkItemType;
import com.tsix.immocrm.entities.str.StrStatus;
import com.tsix.immocrm.fixtures.FixtureEtatVentes;
import com.tsix.immocrm.fixtures.FixtureRapports;
import com.tsix.immocrm.fixtures.FixtureRapportsDossiers;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

import static com.tsix.immocrm.controller.rapport.AppelsRapports.VENTE;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.colonnes;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.lignes;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.nombre;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.resultat;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.section;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.totaux;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static com.tsix.immocrm.fixtures.FixtureRapportsDossiers.VERIF_ATTACHEMENTS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * N° 33 VÉRIFICATION DES ATTACHEMENTS, Vente (FEATURE-tdb-vente-rapports-dossiers.md §7, §9 « N° 33 », décision 4 du
 * manager). Pièce = ligne {@code AdmAttachedFile} dont {@code CategoryId} = id du dossier, quelle que soit {@code Category} :
 * <pre>
 *   T  : d1 (RESA)    : P1 « Yes.Com.Sales.Sale », P1 « Dossier de vente N° … » cochée non jointe, P2, une sans type
 *        d2 (ANNULÉ)  : aucune pièce
 *        d3 (MINUTE)  : P1 ; + une pièce P1 rattachée à son ARTICLE (pas au dossier)
 *        dz (vocation PROJET) : P2
 *   TI (entité INACTIVE) : dx (MINUTE) : P3
 * </pre>
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class VerificationAttachementsVenteRapportTest {

    private static final String TRANCHE_ID = "trancheId";
    private static final String SECTION = "pieces";
    private static final String ARTICLE = "article";
    private static final String TOTAL = "total";
    private static final String COCHES = "coches";
    private static final String SANS_TYPE = "sansType";
    private static final String CATEGORIE_VENTE = "Yes.Com.Sales.Sale";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureRapports fixture;
    @Autowired private FixtureEtatVentes ev;
    @Autowired private FixtureRapportsDossiers rd;

    private AppelsRapports appels;
    private String prefixe;
    private RequestPostProcessor libre;
    private CfgTranche t;
    private CfgTranche ti;
    private StkItemCategory categorie;
    private StkItemType type;
    private AdmAttachedFileType p1;
    private AdmAttachedFileType p2;
    private AdmAttachedFileType p3;
    private StrStatus resa;
    private ComFolder d1;
    private ComFolder d2;
    private ComFolder d3;
    private ComFolder dz;
    private int numero;

    @BeforeEach
    void setUp() {
        appels = new AppelsRapports(mockMvc);
        prefixe = FixtureEtatVentes.prefixe("VAV");
        fixture.tables();
        fixture.rapportVente(VERIF_ATTACHEMENTS, "VERIFICATION DES ATTACHEMENTS", "DOSSIERS DE VENTE", 10000);
        libre = connecte(fixture.compte("vav." + prefixe, fixture.role("Libre " + prefixe, false)));

        t = ev.tranche("T-" + prefixe, ev.projet("P-" + prefixe, true), true);
        ti = ev.tranche("TI-" + prefixe, ev.projet("PI-" + prefixe, false), true);
        categorie = ev.categorie("CAT-" + prefixe);
        type = ev.type("TYP-" + prefixe);
        p1 = rd.typePiece("P1-" + prefixe);
        p2 = rd.typePiece("P2-" + prefixe);
        p3 = rd.typePiece("P3-" + prefixe);
        resa = ev.statutEnCours("RESA-" + prefixe);
        StrStatus minute = ev.statutEnCours("MIN-" + prefixe);

        d1 = dossier(t, resa);
        rd.piece(d1.getId(), p1, CATEGORIE_VENTE, false);
        rd.piece(d1.getId(), p1, "Dossier de vente N° " + d1.getCode(), true);
        rd.piece(d1.getId(), p2, CATEGORIE_VENTE, false);
        rd.piece(d1.getId(), null, CATEGORIE_VENTE, false);
        d2 = dossier(t, rd.statutAnnule("ANN-" + prefixe));
        d3 = dossier(t, minute);
        rd.piece(d3.getId(), p1, CATEGORIE_VENTE, false);
        rd.piece(d3.getStkItem().getId(), p1, "Yes.Stk.Item", false);
        dz = dossier(t, minute);
        dz.setStkVocation(ev.projetVocation());
        dz = ev.enregistrer(dz);
        rd.piece(dz.getId(), p2, CATEGORIE_VENTE, false);
        rd.piece(dossier(ti, minute).getId(), p3, CATEGORIE_VENTE, false);
        fixture.relire();
    }

    private ComFolder dossier(CfgTranche tranche, StrStatus statut) {
        numero++;
        return ev.vendre(ev.article(prefixe + "-A" + numero, tranche, categorie, type),
                "DOS-" + prefixe + "-" + numero, statut, ev.client("C" + numero + "-" + prefixe), null);
    }

    private MockHttpServletRequestBuilder sur(CfgTranche tranche) {
        return get(resultat(VENTE, VERIF_ATTACHEMENTS)).param(TRANCHE_ID, tranche.getId().toString()).with(libre);
    }

    private Map<String, Object> pieces(MockHttpServletRequestBuilder requete) throws Exception {
        return section(appels.sections(requete), SECTION);
    }

    private static Map<String, Object> ligneDe(Map<String, Object> section, ComFolder dossier) {
        List<Map<String, Object>> trouvees = lignes(section).stream()
                .filter(l -> String.valueOf(l.get(ARTICLE)).contains(dossier.getCode())).toList();
        assertThat(trouvees).as("ligne de %s", dossier.getCode()).hasSize(1);
        return trouvees.get(0);
    }

    /** Nom de la colonne dynamique dont le libellé est la description du type de pièce. */
    private static String colonneDe(Map<String, Object> section, AdmAttachedFileType typePiece) {
        return colonnes(section).stream().filter(c -> typePiece.getDescription().equals(c.get("libelle")))
                .map(c -> (String) c.get("nom")).findFirst()
                .orElseThrow(() -> new AssertionError("colonne absente : " + typePiece.getDescription()));
    }

    private static List<Object> libelles(Map<String, Object> section) {
        return colonnes(section).stream().map(c -> c.get("libelle")).toList();
    }

    // ------------------------------------------------------------------ lignes

    @Test
    void n33_dossierSansPiece_ligneAZero_annuleCompris() throws Exception {
        Map<String, Object> ligne = ligneDe(pieces(sur(t)), d2);

        assertThat(nombre(ligne.get(TOTAL))).isEqualByComparingTo("0");
    }

    @Test
    void n33_article_libelleCodeDossierEtStatut() throws Exception {
        Map<String, Object> ligne = ligneDe(pieces(sur(t)), d1);

        assertThat(ligne).containsEntry(ARTICLE,
                "Article " + prefixe + "-A1 (" + d1.getCode() + " " + resa.getCode() + ")");
    }

    @Test
    void n33_pieceSansType_colonneSansType() throws Exception {
        assertThat(nombre(ligneDe(pieces(sur(t)), d1).get(SANS_TYPE))).isEqualByComparingTo("1");
    }

    /** Décision 4 : la pièce cochée « vérifiée non jointe » compte dans {@code total} et dans {@code coches}. */
    @Test
    void n33_cocheeNonJointe_compteeDansTotalEtDansCoches() throws Exception {
        Map<String, Object> ligne = ligneDe(pieces(sur(t)), d1);

        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(nombre(ligne.get(TOTAL))).isEqualByComparingTo("4");
        soft.assertThat(nombre(ligne.get(COCHES))).isEqualByComparingTo("1");
        soft.assertAll();
    }

    @Test
    void n33_rattacheeParCategoryId_quelleQueSoitCategory() throws Exception {
        Map<String, Object> section = pieces(sur(t));

        assertThat(nombre(ligneDe(section, d1).get(colonneDe(section, p1)))).isEqualByComparingTo("2");
    }

    @Test
    void n33_pieceDUnAutrePorteur_nonComptee() throws Exception {
        Map<String, Object> section = pieces(sur(t));
        Map<String, Object> ligne = ligneDe(section, d3);

        assertThat(nombre(ligne.get(colonneDe(section, p1)))).isEqualByComparingTo("1");
        assertThat(nombre(ligne.get(TOTAL))).isEqualByComparingTo("1");
    }

    @Test
    void n33_vocationAutreQueVente_exclue() throws Exception {
        Map<String, Object> section = pieces(sur(t));

        assertThat(lignes(section)).isNotEmpty()
                .noneSatisfy(l -> assertThat(String.valueOf(l.get(ARTICLE))).contains(dz.getCode()));
    }

    // ------------------------------------------------------------------ colonnes dynamiques, totaux

    @Test
    void n33_colonnes_typesPresentsSeulement_triesParLibelle() throws Exception {
        Map<String, Object> section = pieces(sur(t));

        List<Object> libelles = libelles(section);
        assertThat(libelles).isNotEmpty().containsSubsequence(p1.getDescription(), p2.getDescription())
                .doesNotContain(p3.getDescription());
        assertThat(colonnes(section).stream().map(c -> c.get("nom")).toList()).contains(SANS_TYPE, TOTAL, COCHES);
    }

    @Test
    void n33_totauxParColonne_etGroupeParTranche() throws Exception {
        Map<String, Object> section = pieces(sur(t));

        assertThat(section).containsEntry("groupePar", "tranche");
        Map<String, Object> totaux = totaux(section);
        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(nombre(totaux.get(colonneDe(section, p1)))).isEqualByComparingTo("3");
        soft.assertThat(nombre(totaux.get(TOTAL))).isEqualByComparingTo("5");
        soft.assertThat(nombre(totaux.get(COCHES))).isEqualByComparingTo("1");
        soft.assertThat(nombre(totaux.get(SANS_TYPE))).isEqualByComparingTo("1");
        soft.assertAll();
    }

    // ------------------------------------------------------------------ filtre statut, refus

    @Test
    void n33_filtreStatut_contient() throws Exception {
        Map<String, Object> section = pieces(sur(t).param("statut", "NN-" + prefixe));

        assertThat(lignes(section)).singleElement()
                .satisfies(l -> assertThat(String.valueOf(l.get(ARTICLE))).contains(d2.getCode()));
    }

    @Test
    void rv_r4a_trancheDUneEntiteInactive_vide() throws Exception {
        assertThat(lignes(pieces(sur(ti)))).isEmpty();
    }
}
