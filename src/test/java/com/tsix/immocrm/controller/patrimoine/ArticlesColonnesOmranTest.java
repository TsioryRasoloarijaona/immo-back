package com.tsix.immocrm.controller.patrimoine;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.com.ComFolderItem;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.entities.stk.StkItemCategory;
import com.tsix.immocrm.entities.str.StrStatus;
import com.tsix.immocrm.fixtures.FixtureEtatVentes;
import com.tsix.immocrm.fixtures.FixtureEtatVentes.Nature;
import com.tsix.immocrm.fixtures.FixturePatrimoine;
import com.tsix.immocrm.fixtures.FixtureSousArticles;
import com.tsix.immocrm.fixtures.FixtureSousArticles.Contexte;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Liste des articles, colonnes et libellés d'OMRAN (FEATURE-articles-colonnes-omran.md §2, §5) : superficie titrée,
 * superficie terrasse, parking choisi, reste à régler (règle 6 : égal au {@code prixReste} de la liste des dossiers).
 * Catégories reconnues par leur code ({@code StkItemCategory.Code} TER / PAR), comme en production.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ArticlesColonnesOmranTest {

    private static final String CATALOGUE = "/api/referentiels/colonnes/articles";
    private static final String ARTICLES = "/api/articles";
    private static final String DOSSIERS = "/api/dossiers";
    private static final String COLONNES = "colonnes";
    private static final String TRANCHE_ID = "trancheId";
    private static final String SIZE = "size";
    private static final String TOUS = "1000";

    private static final String SUPERFICIE_TITREE = "superficieTitree";
    private static final String SUPERFICIE_TERRASSE = "superficieTerrasse";
    private static final String RESTE_A_REGLER = "resteARegler";
    private static final String PARKING_CHOISI = "parkingChoisi";
    private static final String DOSSIER_CODE = "dossierCode";
    private static final String PRIX_RESTE = "prixReste";

    private static final String CODE_TERRASSE = "TER";
    private static final String CODE_PARKING = "PAR";
    private static final String CODE_JARDIN = "JDN";
    private static final String PRIX_DOSSIER = "1200000";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixturePatrimoine patrimoine;
    @Autowired private FixtureSousArticles sousArticles;
    @Autowired private FixtureEtatVentes ventes;
    @Autowired private EntityManager em;
    @Autowired private EntityManagerFactory emf;

    private AdmUser admin;
    private Contexte c;

    @BeforeEach
    void setUp() {
        admin = patrimoine.administrateur();
        c = sousArticles.contexte("D1A");
    }

    // ------------------------------------------------------------------ jeu

    private StkItemCategory categorie(String code) {
        return patrimoine.categorie(code, patrimoine.natureBat(), true);
    }

    private StkItem lot(String suffixe) {
        return sousArticles.principal(c, suffixe + "-" + c.jeton(), c.tranche());
    }

    private StkItem lot(String suffixe, CfgTranche tranche) {
        return sousArticles.principal(c, suffixe + "-" + c.jeton(), tranche);
    }

    /** Sous-article rattaché ({@code ParentId}) d'une catégorie et d'une surface données. */
    private StkItem enfant(String suffixe, StkItem parent, StkItemCategory categorie, String surface) {
        StkItem s = sousArticles.sousArticle(c, suffixe + "-" + c.jeton(), parent);
        s.setStkItemCategory(categorie);
        s.setArea(new BigDecimal(surface));
        return sousArticles.enregistrer(s);
    }

    /** Article autonome de la tranche (parking non rattaché, comme 241 parkings sur 261 en production). */
    private StkItem autonome(String suffixe, StkItemCategory categorie) {
        StkItem a = lot(suffixe);
        a.setStkItemCategory(categorie);
        a.setIsUnderItem(true);
        return sousArticles.enregistrer(a);
    }

    private StrStatus enCours() {
        return ventes.statutEnCours("EC-" + c.jeton());
    }

    private ComFolder vendre(StkItem article, StrStatus statut) {
        return ventes.vendre(article, "DOS-" + article.getCode(), statut, ventes.client("D1A" + c.jeton()), null);
    }

    /** Avance de 200 000 réglée à 150 000, frais de 15 000 réglés (hors prix) : prixReste attendu 1 050 000. */
    private void reglementPartiel(ComFolder d) {
        ventes.regler(ventes.echeance(d, Nature.AVANCE, true, new BigDecimal("200000")), new BigDecimal("150000"),
                true, "POINTED");
        ventes.regler(ventes.echeance(d, Nature.FRAIS, true, new BigDecimal("15000")), new BigDecimal("15000"),
                true, "POINTED");
    }

    /** Ligne du dossier : article du dossier ou annexe (parking) copiée comme OMRAN (code, article, catégorie). */
    private void ligneDuDossier(ComFolder d, StkItem article, boolean annexe) {
        ComFolderItem item = new ComFolderItem();
        item.setId(UUID.randomUUID());
        item.setComFolder(d);
        item.setStkItem(article);
        item.setStkItemCategory(article.getStkItemCategory());
        item.setStkItemType(article.getStkItemType());
        item.setCode(article.getCode());
        item.setDescription(article.getDescription());
        item.setArea(article.getArea());
        item.setIsUnderItem(annexe);
        item.setIsUnderItemOrigin(annexe);
        em.persist(item);
    }

    // ------------------------------------------------------------------ lecture

    private String corps(MockHttpServletRequestBuilder requete) throws Exception {
        return mockMvc.perform(requete.with(connecte(admin))).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    /** Ligne de l'article dans {@code /api/articles?colonnes=code,…} de sa tranche. */
    private Map<String, Object> ligneArticle(StkItem article, String colonnes) throws Exception {
        ventes.relire();
        List<Map<String, Object>> lignes = JsonPath.read(corps(get(ARTICLES)
                .param(TRANCHE_ID, article.getStkHierarchy().getId().toString())
                .param(COLONNES, "code," + colonnes).param(SIZE, TOUS)),
                "$.content[?(@.id == '" + article.getId() + "')]");
        assertThat(lignes).as("ligne de %s", article.getCode()).hasSize(1);
        return lignes.getFirst();
    }

    /** {@code prixReste} du dossier de ce code dans {@code /api/dossiers} (sortis compris), sous le même compte. */
    private Object prixResteDeLaListeDesDossiers(CfgTranche tranche, Object codeDossier) throws Exception {
        List<Map<String, Object>> lignes = JsonPath.read(corps(get(DOSSIERS)
                .param(TRANCHE_ID, tranche.getId().toString()).param("inclureSortis", "true")
                .param(COLONNES, "id,code," + PRIX_RESTE).param(SIZE, "100")),
                "$.content[?(@.code == '" + codeDossier + "')]");
        assertThat(lignes).as("dossier %s dans la liste des dossiers", codeDossier).hasSize(1);
        return lignes.getFirst().get(PRIX_RESTE);
    }

    private Map<String, Map<String, Object>> catalogue() throws Exception {
        List<Map<String, Object>> colonnes = JsonPath.read(corps(get(CATALOGUE)), "$");
        Map<String, Map<String, Object>> parNom = new HashMap<>();
        colonnes.forEach(col -> parNom.put((String) col.get("nom"), col));
        return parNom;
    }

    private static BigDecimal nombre(Object valeur) {
        return valeur == null ? null : new BigDecimal(String.valueOf(valeur));
    }

    private static List<String> codes(Object valeur) {
        return List.of(String.valueOf(valeur).split(", "));
    }

    // ------------------------------------------------------------------ §2.1 / §2.3 catalogue

    /** §2.1, §2.3 : les quatre colonnes nouvelles, leur libellé, leur type, hors défaut. */
    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "superficieTitree|Superficie Titrée (m²)|NOMBRE",
            "superficieTerrasse|Superficie terrasse (m²)|NOMBRE",
            "resteARegler|Reste à régler|MONTANT",
            "parkingChoisi|Parking choisi|TEXTE"})
    void s2_1_catalogue_colonneNouvelle_libelleTypeEtHorsDefaut(String nom, String libelle, String type)
            throws Exception {
        assertThat(catalogue()).as(nom).containsKey(nom)
                .extractingByKey(nom).satisfies(col -> assertThat(col)
                        .containsEntry("libelle", libelle)
                        .containsEntry("type", type)
                        .containsEntry("parDefaut", false));
    }

    /** §1 bis, §2.3 : colonnes de parité à créer (vides en base), libellé OMRAN, hors défaut ; type non fixé par le spec. */
    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "facade|Façade",
            "vues|Vues",
            "quotaTerrain|Quota terrain groupe"})
    void s1bis_catalogue_colonneDeParite_libelleEtHorsDefaut(String nom, String libelle) throws Exception {
        assertThat(catalogue()).as(nom).containsKey(nom)
                .extractingByKey(nom).satisfies(col -> assertThat(col)
                        .containsEntry("libelle", libelle)
                        .containsEntry("parDefaut", false));
    }

    /** §2.2, §1 bis : libellés OMRAN, nom technique inchangé (« superficie » du spec = colonne {@code area}). */
    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "prixM2DefinitifTTC|Defi. Prix du m² TTC",
            "prixSousArticlesTTC|Defi. Prix Sous Articles TTC",
            "comSalePrice|Defi. Prix Article TTC",
            "salePrice|Defi. Prix de Vente TTC",
            "area|Defi. Superficie (m²)",
            "superficieSousArticles|Defi. Superficie Sous Articles (m²)",
            "titreFoncier|N° Titre foncier",
            "etat|État"})
    void s2_2_catalogue_libelleOmran_nomInchange(String nom, String libelle) throws Exception {
        assertThat(catalogue()).as(nom).containsKey(nom)
                .extractingByKey(nom).satisfies(col -> assertThat(col).containsEntry("libelle", libelle));
    }

    // ------------------------------------------------------------------ superficie titrée

    /** §2.1 : une seule implémentation — la liste rend la superficie titrée de la fiche (63 titré + 10 titré). */
    @Test
    void superficieTitree_lotTitre_egaleALaFiche() throws Exception {
        StkItem a = lot("TIT");
        StkItem titre = sousArticles.sousArticle(c, "TIT1-" + c.jeton(), a);
        titre.setIsTitled(true);
        sousArticles.enregistrer(titre);
        sousArticles.sousArticle(c, "TIT2-" + c.jeton(), a);
        ventes.relire();
        Object fiche = JsonPath.read(corps(get(ARTICLES + "/{id}", a.getId())), "$." + SUPERFICIE_TITREE);

        Object liste = ligneArticle(a, SUPERFICIE_TITREE).get(SUPERFICIE_TITREE);

        assertAll(
                () -> assertThat(nombre(fiche)).isEqualByComparingTo("73"),
                () -> assertThat(nombre(liste)).isEqualByComparingTo(nombre(fiche)));
    }

    /** Lot non titré : seuls les sous-articles titrés comptent (fiche = 10), la liste dit pareil. */
    @Test
    void superficieTitree_lotNonTitre_egaleALaFiche() throws Exception {
        StkItem a = lot("NTI");
        a.setIsTitled(false);
        sousArticles.enregistrer(a);
        StkItem titre = sousArticles.sousArticle(c, "NTI1-" + c.jeton(), a);
        titre.setIsTitled(true);
        sousArticles.enregistrer(titre);
        ventes.relire();
        Object fiche = JsonPath.read(corps(get(ARTICLES + "/{id}", a.getId())), "$." + SUPERFICIE_TITREE);

        Object liste = ligneArticle(a, SUPERFICIE_TITREE).get(SUPERFICIE_TITREE);

        assertAll(
                () -> assertThat(nombre(fiche)).isEqualByComparingTo("10"),
                () -> assertThat(nombre(liste)).isEqualByComparingTo(nombre(fiche)));
    }

    // ------------------------------------------------------------------ superficie terrasse

    /** §1, §2.1 : Σ des sous-articles TER seulement (12 + 8) ; jardin et parking rattachés exclus. */
    @Test
    void superficieTerrasse_sommeDesSousArticlesTerSeulement() throws Exception {
        StkItem a = lot("TRS");
        StkItemCategory terrasse = categorie(CODE_TERRASSE);
        enfant("TRS1", a, terrasse, "12");
        enfant("TRS2", a, terrasse, "8");
        enfant("TRSJ", a, categorie(CODE_JARDIN), "30");
        enfant("TRSP", a, categorie(CODE_PARKING), "15");

        Map<String, Object> ligne = ligneArticle(a, SUPERFICIE_TERRASSE);

        assertThat(nombre(ligne.get(SUPERFICIE_TERRASSE))).isEqualByComparingTo("20");
    }

    /** §1 : superficieSousArticles garde sa valeur (Σ de tous les sous-articles : 12 + 30 + 15). */
    @Test
    void superficieSousArticles_garde_sommeDeTousLesSousArticles() throws Exception {
        StkItem a = lot("SSA");
        enfant("SSA1", a, categorie(CODE_TERRASSE), "12");
        enfant("SSAJ", a, categorie(CODE_JARDIN), "30");
        enfant("SSAP", a, categorie(CODE_PARKING), "15");

        Map<String, Object> ligne = ligneArticle(a, "superficieSousArticles");

        assertThat(nombre(ligne.get("superficieSousArticles"))).isEqualByComparingTo("57");
    }

    // ------------------------------------------------------------------ parking choisi

    /** §1, §5 : dossier avec deux parkings → leurs deux codes séparés par « , » ; l'article du dossier et un jardin exclus. */
    @Test
    void parkingChoisi_dossierADeuxParkings_codesSeparesParVirgule() throws Exception {
        StkItemCategory parking = categorie(CODE_PARKING);
        StkItem a = lot("PK");
        StkItem p1 = autonome("P1", parking);
        StkItem p2 = autonome("P2", parking);
        StkItem jardin = autonome("J1", categorie(CODE_JARDIN));
        ComFolder d = vendre(a, enCours());
        ligneDuDossier(d, a, false);
        ligneDuDossier(d, p1, true);
        ligneDuDossier(d, p2, true);
        ligneDuDossier(d, jardin, true);

        Object valeur = ligneArticle(a, PARKING_CHOISI).get(PARKING_CHOISI);

        assertThat(codes(valeur)).containsExactlyInAnyOrder(p1.getCode(), p2.getCode());
    }

    /** §1, §5 : lot sans dossier, sous-article PAR rattaché ({@code ParentId}) → présent ; terrasse rattachée exclue. */
    @Test
    void parkingChoisi_sousArticleParRattacheAuLot_present() throws Exception {
        StkItem a = lot("PR");
        StkItem rattache = enfant("PRP", a, categorie(CODE_PARKING), "15");
        enfant("PRT", a, categorie(CODE_TERRASSE), "12");

        Object valeur = ligneArticle(a, PARKING_CHOISI).get(PARKING_CHOISI);

        assertThat(codes(valeur)).containsExactly(rattache.getCode());
    }

    /** §1 : parkings du dossier + sous-articles PAR rattachés, sans doublon quand c'est le même parking. */
    @Test
    void parkingChoisi_dossierEtRattache_cumulesSansDoublon() throws Exception {
        StkItemCategory parking = categorie(CODE_PARKING);
        StkItem a = lot("PD");
        StkItem rattache = enfant("PDR", a, parking, "15");
        StkItem duDossier = autonome("PDD", parking);
        ComFolder d = vendre(a, enCours());
        ligneDuDossier(d, a, false);
        ligneDuDossier(d, rattache, true);
        ligneDuDossier(d, duDossier, true);

        Object valeur = ligneArticle(a, PARKING_CHOISI).get(PARKING_CHOISI);

        assertThat(codes(valeur)).containsExactlyInAnyOrder(rattache.getCode(), duDossier.getCode());
    }

    /** §5 : sans dossier ni parking → vide. */
    @Test
    void parkingChoisi_sansDossierNiParking_vide() throws Exception {
        StkItem a = lot("PV");
        enfant("PVT", a, categorie(CODE_TERRASSE), "12");

        Map<String, Object> ligne = ligneArticle(a, PARKING_CHOISI);

        assertThat(ligne).containsKey(PARKING_CHOISI);
        assertThat((String) ligne.get(PARKING_CHOISI)).isNullOrEmpty();
    }

    // ------------------------------------------------------------------ règle 6 : reste à régler

    /** §2.4, §5 : resteARegler = prixReste de /api/dossiers pour le dossier dossierCode (règlement partiel, frais exclus). */
    @Test
    void r6_resteARegler_egalPrixResteDeLaListeDesDossiers_reglementPartiel() throws Exception {
        StkItem a = lot("RR");
        ComFolder d = vendre(a, enCours());
        ventes.prix(d, new BigDecimal(PRIX_DOSSIER));
        reglementPartiel(d);

        Map<String, Object> ligne = ligneArticle(a, DOSSIER_CODE + "," + RESTE_A_REGLER);
        Object prixReste = prixResteDeLaListeDesDossiers(c.tranche(), ligne.get(DOSSIER_CODE));

        assertAll(
                () -> assertThat(ligne).containsEntry(DOSSIER_CODE, d.getCode()),
                () -> assertThat(nombre(prixReste)).isEqualByComparingTo("1050000"),
                () -> assertThat(nombre(ligne.get(RESTE_A_REGLER))).isEqualByComparingTo(nombre(prixReste)));
    }

    /** §1, §2.4 : pas de dossier → null, jamais 0. */
    @Test
    void r6_resteARegler_articleSansDossier_null() throws Exception {
        StkItem a = lot("RL");

        Map<String, Object> ligne = ligneArticle(a, DOSSIER_CODE + "," + RESTE_A_REGLER);

        assertThat(ligne).containsEntry(DOSSIER_CODE, null).containsEntry(RESTE_A_REGLER, null);
    }

    /**
     * §2.4, §5 : seul dossier annulé, l'article ne le pointe plus ({@code ComFolderId} NULL, cas OMRAN) →
     * dossierCode vide, donc resteARegler null, même si {@code ComFolder.StkItemId} vise encore l'article.
     */
    @Test
    void r6_resteARegler_seulDossierAnnuleNonPointe_videCommeDossierCode() throws Exception {
        StkItem a = lot("RA");
        ComFolder d = vendre(a, ventes.statutSortie("ANN-" + c.jeton(), true));
        reglementPartiel(d);
        a.setComFolderId(null);
        sousArticles.enregistrer(a);

        Map<String, Object> ligne = ligneArticle(a, DOSSIER_CODE + "," + RESTE_A_REGLER);

        assertThat(ligne).containsEntry(DOSSIER_CODE, null).containsEntry(RESTE_A_REGLER, null);
    }

    /**
     * §2.4, §5 : le dossier affiché ({@code StkItem.ComFolderId}) est annulé (true) ou désisté (false) →
     * dossierCode toujours renseigné, mais resteARegler null — jamais le montant dû, jamais 0.
     */
    @ParameterizedTest
    @CsvSource({"true", "false"})
    void r6_resteARegler_dossierAfficheAnnuleOuDesiste_dossierCodeRenseigne_resteNull(boolean annulation)
            throws Exception {
        StkItem a = lot(annulation ? "RB" : "RD");
        ComFolder d = vendre(a, ventes.statutSortie((annulation ? "ANB-" : "DES-") + c.jeton(), annulation));
        ventes.prix(d, new BigDecimal(PRIX_DOSSIER));
        reglementPartiel(d);

        Map<String, Object> ligne = ligneArticle(a, DOSSIER_CODE + "," + RESTE_A_REGLER);

        assertThat(ligne).containsEntry(DOSSIER_CODE, d.getCode()).containsEntry(RESTE_A_REGLER, null);
    }

    // ------------------------------------------------------------------ pas de requête par ligne

    private static final String NOUVELLES = String.join(",", SUPERFICIE_TITREE, SUPERFICIE_TERRASSE, RESTE_A_REGLER,
            PARKING_CHOISI);

    /** §2.1, §5 : coût des 4 colonnes = (requêtes avec elles) − (requêtes avec code seul), indépendant de la page. */
    @Test
    void aucuneRequeteParLigne_coutDesColonnesIndependantDuNombreDeLignes() throws Exception {
        StkItemCategory parking = categorie(CODE_PARKING);
        StkItemCategory terrasse = categorie(CODE_TERRASSE);
        StrStatus statut = enCours();
        CfgTranche petite = sousArticles.autreTranche(c, "S");
        CfgTranche grande = sousArticles.autreTranche(c, "G");
        peupler(petite, "S", 2, parking, terrasse, statut);
        peupler(grande, "G", 20, parking, terrasse, statut);

        long coutPetit = requetes(petite, NOUVELLES) - requetes(petite, "");
        long coutGrand = requetes(grande, NOUVELLES) - requetes(grande, "");

        assertThat(coutGrand).isLessThanOrEqualTo(coutPetit + 2);
    }

    private void peupler(CfgTranche tranche, String racine, int nombre, StkItemCategory parking,
                         StkItemCategory terrasse, StrStatus statut) {
        for (int i = 0; i < nombre; i++) {
            StkItem a = lot(racine + i, tranche);
            enfant(racine + i + "T", a, terrasse, "10");
            StkItem p = enfant(racine + i + "P", a, parking, "12");
            ComFolder d = vendre(a, statut);
            reglementPartiel(d);
            ligneDuDossier(d, a, false);
            ligneDuDossier(d, p, true);
        }
    }

    private long requetes(CfgTranche tranche, String colonnes) throws Exception {
        Statistics statistiques = emf.unwrap(SessionFactory.class).getStatistics();
        boolean active = statistiques.isStatisticsEnabled();
        statistiques.setStatisticsEnabled(true);
        try {
            ventes.relire();
            statistiques.clear();
            mockMvc.perform(get(ARTICLES).param(TRANCHE_ID, tranche.getId().toString()).param(SIZE, "100")
                            .param(COLONNES, colonnes.isEmpty() ? "code" : "code," + colonnes)
                            .with(connecte(admin)))
                    .andExpect(status().isOk());
            return statistiques.getPrepareStatementCount();
        } finally {
            statistiques.setStatisticsEnabled(active);
        }
    }
}
