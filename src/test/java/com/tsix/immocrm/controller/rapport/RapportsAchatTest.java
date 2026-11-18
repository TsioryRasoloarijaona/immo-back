package com.tsix.immocrm.controller.rapport;

import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.fixtures.FixtureAchat;
import com.tsix.immocrm.fixtures.FixtureLotFiche;
import com.tsix.immocrm.fixtures.FixtureLotsParite;
import com.tsix.immocrm.fixtures.FixtureRapportsAchat;
import com.tsix.immocrm.fixtures.FixtureRapportsAchat.Pret;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import static com.tsix.immocrm.controller.rapport.AppelsRapports.nombre;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.resultat;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.somme;
import static com.tsix.immocrm.controller.rapport.RapportsAchatSupport.ACHAT;
import static com.tsix.immocrm.controller.rapport.RapportsAchatSupport.ANNEE;
import static com.tsix.immocrm.controller.rapport.RapportsAchatSupport.AU;
import static com.tsix.immocrm.controller.rapport.RapportsAchatSupport.DU;
import static com.tsix.immocrm.controller.rapport.RapportsAchatSupport.lignesAvec;
import static com.tsix.immocrm.controller.rapport.RapportsAchatSupport.lignesMentionnant;
import static com.tsix.immocrm.controller.rapport.RapportsAchatSupport.unePorte;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static com.tsix.immocrm.fixtures.FixtureLotsParite.ENTREPRENEURS;
import static com.tsix.immocrm.fixtures.FixtureRapportsAchat.BONS_COMMANDE;
import static com.tsix.immocrm.fixtures.FixtureRapportsAchat.DEPENSES_PAR_MOIS;
import static com.tsix.immocrm.fixtures.FixtureRapportsAchat.ECHEANCIER_CREDITS;
import static com.tsix.immocrm.fixtures.FixtureRapportsAchat.RETENUES_LOTS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * Les 4 rapports Achat sur le socle {@code /api/rapports/achat} (FEATURE-n7-etats-achat.md N7b, R12-R15, D3).
 * <p>Compte libre pour les rapports, habilité à la société S1 et aux projets PA, PA2 (S1) et P2 (S2, société non habilitée).
 * PB (S1) n'est pas habilité. Année {@link FixtureRapportsAchat#ANNEE} : vide dans yesRemsTest.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class RapportsAchatTest {

    private static final String PREVU = "prevu";
    private static final String REEL = "reel";
    private static final String DECAISSEMENTS = "decaissements";
    private static final String CODE = "code";
    private static final String CODE_BC = "codeBC";
    private static final String CODE_BL = "codeBL";
    private static final String CODE_ARTICLE = "codeArticle";
    private static final String QTE_COMMANDEE = "quantiteCommandee";
    private static final String QTE_LIVREE = "quantiteLivree";
    private static final String RESTE = "reste";
    private static final String BUY = "BUY";
    private static final String PRJ = "PRJ";
    private static final String EN_COURS = "IN_WORK";
    private static final String ANNEE_TEST = String.valueOf(FixtureRapportsAchat.ANNEE);

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureAchat achat;
    @Autowired private FixtureRapportsAchat fixture;
    @Autowired private FixtureLotsParite parite;
    @Autowired private FixtureLotFiche fiche;

    private AppelsRapports appels;
    private String jeton;
    private UUID s1;
    private UUID s2;
    private UUID pa;
    private UUID pa2;
    private UUID pb;
    private UUID p2;
    private UUID fournisseur;
    private String nom;
    private AdmUser compte;

    @BeforeEach
    void setUp() {
        appels = new AppelsRapports(mockMvc);
        jeton = FixtureAchat.jeton("N7B");
        fixture.rapports();
        s1 = achat.societe("S1-" + jeton);
        s2 = achat.societe("S2-" + jeton);
        pa = achat.projet("PA-" + jeton, s1);
        pa2 = achat.projet("PA2-" + jeton, s1);
        pb = achat.projet("PB-" + jeton, s1);
        p2 = achat.projet("P2-" + jeton, s2);
        nom = "Fournisseur N7B " + jeton;
        fournisseur = achat.fournisseur("FOU-" + jeton, nom);
        compte = fixture.compte("N7B-" + jeton, false, s1, pa, pa2, p2);
    }

    private RequestPostProcessor qui() {
        return connecte(compte, FixtureRapportsAchat.role());
    }

    private MockHttpServletRequestBuilder requete(String code, String... parametres) {
        MockHttpServletRequestBuilder r = get(resultat(ACHAT, code)).with(qui());
        for (int i = 0; i + 1 < parametres.length; i += 2) {
            r.param(parametres[i], parametres[i + 1]);
        }
        return r;
    }

    private List<Map<String, Object>> sections(String code, String... parametres) throws Exception {
        return appels.sections(requete(code, parametres));
    }

    private static LocalDate jour(int mois, int jour) {
        return LocalDate.of(FixtureRapportsAchat.ANNEE, mois, jour);
    }

    private static BigDecimal montant(Map<String, Object> ligne, String colonne) {
        return Objects.requireNonNullElse(nombre(ligne.get(colonne)), BigDecimal.ZERO);
    }

    // ------------------------------------------------------------------ R12 : dépenses par mois

    /** Les 12 lignes mensuelles de l'année 2093 (section qui porte {@code prevu}), dans l'ordre des mois. */
    private List<Map<String, Object>> mois() throws Exception {
        List<Map<String, Object>> lignes = lignesAvec(sections(DEPENSES_PAR_MOIS, ANNEE, ANNEE_TEST), PREVU);
        assertThat(lignes).hasSize(12);
        return lignes;
    }

    @Test
    void r12_douzeLignes_memeSansAucunMontant() throws Exception {
        List<Map<String, Object>> lignes = mois();

        assertAll(
                () -> assertThat(somme(lignes, PREVU)).isEqualByComparingTo("0"),
                () -> assertThat(somme(lignes, REEL)).isEqualByComparingTo("0"),
                () -> assertThat(somme(lignes, DECAISSEMENTS)).isEqualByComparingTo("0"));
    }

    @Test
    void r12_prevu_categorie_plusProjet_plusEcheancesDePret_duMois() throws Exception {
        UUID categorie = achat.categorie("CAT-" + jeton);
        fixture.previsionCategorie(FixtureRapportsAchat.ANNEE, 1, "1000", categorie, s1);
        fixture.previsionProjet(FixtureRapportsAchat.ANNEE, 1, "200", pa);
        fixture.echeance(fixture.pret("PR-" + jeton, s1, pa), jour(1, 15), "30");
        fixture.previsionCategorie(FixtureRapportsAchat.ANNEE, 2, "7", categorie, s1);
        fixture.previsionCategorie(FixtureRapportsAchat.ANNEE + 1, 1, "5000", categorie, s1);

        List<Map<String, Object>> lignes = mois();

        assertAll(
                () -> assertThat(montant(lignes.get(0), PREVU)).as("janvier : 1 000 + 200 + 30").isEqualByComparingTo("1230"),
                () -> assertThat(montant(lignes.get(1), PREVU)).as("février").isEqualByComparingTo("7"),
                () -> assertThat(somme(lignes, PREVU)).as("année suivante exclue").isEqualByComparingTo("1237"));
    }

    @Test
    void r12_reel_moisDeLEcheanceSinonDateDePiece_payablesSeulement() throws Exception {
        fixture.facturePayable(jeton + "-P1", jour(2, 15), fournisseur, nom, s1, pa, "100");
        UUID sansEcheance = fixture.facturePayable(jeton + "-P2", jour(3, 5), fournisseur, nom, s1, null, "20");
        achat.effacer(sansEcheance, "MaturityDate");
        UUID nonPayable = fixture.facturePayable(jeton + "-P3", jour(3, 5), fournisseur, nom, s1, pa, "5000");
        achat.poser(nonPayable, "CanBePayed", false);

        List<Map<String, Object>> lignes = mois();

        assertAll(
                () -> assertThat(montant(lignes.get(1), REEL)).as("février : P1 échoit le 17/03").isEqualByComparingTo("0"),
                () -> assertThat(montant(lignes.get(2), REEL)).as("mars : P1 100 + P2 20").isEqualByComparingTo("120"),
                () -> assertThat(somme(lignes, REEL)).as("CanBePayed = 0 exclu").isEqualByComparingTo("120"));
    }

    @Test
    void r12_reel_piecesHorsPerimetre_exclues_d3() throws Exception {
        fixture.facturePayable(jeton + "-V", jour(4, 10), fournisseur, nom, s1, pa, "1");
        fixture.facturePayable(jeton + "-HB", jour(4, 10), fournisseur, nom, s1, pb, "1000");
        fixture.facturePayable(jeton + "-HS", jour(4, 10), fournisseur, nom, s2, p2, "30000");

        List<Map<String, Object>> lignes = mois();

        assertAll(
                () -> assertThat(montant(lignes.get(4), REEL)).as("mai : seule la pièce visible").isEqualByComparingTo("1"),
                () -> assertThat(somme(lignes, REEL)).isEqualByComparingTo("1"));
    }

    @Test
    void r12_decaissements_buyEtPrj_signeIsCredit_horsAnnules_moisEcheanceSinonReglement() throws Exception {
        fixture.reglement(jeton + "-R1", BUY, false, "300", EN_COURS, jour(4, 10), jour(3, 1), s1);
        fixture.reglement(jeton + "-R2", PRJ, true, "50", EN_COURS, null, jour(4, 20), s1);
        fixture.reglement(jeton + "-R3", BUY, false, "11", EN_COURS, jour(5, 2), jour(4, 25), s1);
        fixture.reglement(jeton + "-R4", "STL", false, "1000", EN_COURS, jour(4, 10), jour(4, 10), s1);
        fixture.reglement(jeton + "-R5", BUY, false, "7000", FixtureAchat.ANNULE, jour(4, 10), jour(4, 10), s1);

        List<Map<String, Object>> lignes = mois();

        assertAll(
                () -> assertThat(montant(lignes.get(2), DECAISSEMENTS)).as("mars : R1 échoit en avril").isEqualByComparingTo("0"),
                () -> assertThat(montant(lignes.get(3), DECAISSEMENTS)).as("avril : +300 − 50").isEqualByComparingTo("250"),
                () -> assertThat(montant(lignes.get(4), DECAISSEMENTS)).as("mai : R3").isEqualByComparingTo("11"),
                () -> assertThat(somme(lignes, DECAISSEMENTS)).as("STL et annulé exclus").isEqualByComparingTo("261"));
    }

    // ------------------------------------------------------------------ R13 : retenues par lot

    /** Lot ENTREPRENEURS du projet ; {@code retenue} non nulle = facture du lot portant cette RG. */
    private String lot(UUID projet, String prefixe, UUID statut, String retenue) {
        String code = prefixe + "-" + jeton;
        UUID lot = parite.lot(projet, code, ENTREPRENEURS, statut);
        if (retenue != null) {
            UUID facture = fiche.factureLot(lot, code + "-F", "800", "960");
            achat.poser(facture, "WarrantyAmount", new BigDecimal(retenue));
        }
        return code;
    }

    private record Lots(String visible, String sansMontant, String horsPerimetre, String autreProjet) {
    }

    private Lots lots() {
        parite.genresDeLot();
        fiche.entitesPiecesLot();
        UUID statut = parite.statut(ENTREPRENEURS, "N7S-" + jeton);
        return new Lots(lot(pa, "LA", statut, "96"), lot(pa, "LZ", statut, null), lot(pb, "LB", statut, "50"),
                lot(pa2, "LA2", statut, "30"));
    }

    private List<Object> codesLots(String... parametres) throws Exception {
        return lignesAvec(sections(RETENUES_LOTS, parametres), CODE).stream().map(l -> l.get(CODE)).toList();
    }

    @Test
    void r13_lotVisibleAvecMontant_uneLigne_codeDescription_etSaRetenue() throws Exception {
        Lots l = lots();

        List<Map<String, Object>> lignes = lignesAvec(sections(RETENUES_LOTS), CODE).stream()
                .filter(m -> l.visible().equals(m.get(CODE))).toList();

        assertThat(lignes).hasSize(1).first()
                .satisfies(m -> assertThat(m).containsEntry("description", "Lot " + l.visible()))
                .satisfies(m -> assertThat(RapportsAchatSupport.porteMontant(m, "96")).as("RG retenue 96").isTrue());
    }

    @Test
    void r13_lotSansMontant_absent() throws Exception {
        Lots l = lots();

        assertThat(codesLots()).contains(l.visible()).doesNotContain(l.sansMontant());
    }

    @Test
    void r13_lotDUnProjetNonHabilite_absent() throws Exception {
        Lots l = lots();

        assertThat(codesLots()).contains(l.visible(), l.autreProjet()).doesNotContain(l.horsPerimetre());
    }

    @Test
    void r13_parametreProjets_neGardeQueLesLotsDeCeProjet() throws Exception {
        Lots l = lots();

        assertThat(codesLots("projets", pa.toString())).contains(l.visible()).doesNotContain(l.autreProjet());
    }

    @Test
    void r13_projetsHorsPerimetre_jamaisSesLots() throws Exception {
        Lots l = lots();
        assertThat(codesLots()).contains(l.visible());

        int statut = appels.statut(requete(RETENUES_LOTS, "projets", pb.toString()));

        assertThat(statut).as("refus 400 ou demande ignorée").isIn(200, 400);
        if (statut == 200) {
            assertThat(codesLots("projets", pb.toString())).doesNotContain(l.horsPerimetre());
        }
    }

    // ------------------------------------------------------------------ R14 : bons de commande et livraisons

    private record Commandes(String bc1, String bl1Ref, String bl2, String bc2, String bc0, String bcb, String bcs,
                             String bcx, String articleA) {
    }

    private Commandes commandes() {
        String articleA = "A-" + jeton;
        UUID bc1 = fixture.bonCommande(jeton + "-BC1", jour(3, 1), fournisseur, nom, s1, pa);
        fixture.ligne(bc1, 1, articleA, "Article A", "10");
        fixture.ligne(bc1, 2, "B-" + jeton, "Article B", "5");
        String bl1Ref = "REF-BL1-" + jeton;
        UUID bl1 = fixture.bonLivraison(jeton + "-BL1", jour(3, 10), bc1, fournisseur, nom, s1, pa, bl1Ref);
        fixture.ligne(bl1, 1, articleA, "Article A", "4");
        UUID bl2 = fixture.bonLivraison(jeton + "-BL2", jour(3, 20), bc1, fournisseur, nom, s1, pa, "");
        fixture.ligne(bl2, 1, articleA, "Article A", "3");
        UUID bc2 = fixture.bonCommande(jeton + "-BC2", jour(4, 1), fournisseur, nom, s1, pa2);
        fixture.ligne(bc2, 1, "C-" + jeton, "Article C", "8");
        UUID bc0 = fixture.bonCommande(jeton + "-BC0", jour(5, 1), fournisseur, nom, s1, null);
        fixture.ligne(bc0, 1, "D-" + jeton, "Article D", "2");
        UUID bcb = fixture.bonCommande(jeton + "-BCB", jour(5, 1), fournisseur, nom, s1, pb);
        fixture.ligne(bcb, 1, "E-" + jeton, "Article E", "1");
        UUID bcs = fixture.bonCommande(jeton + "-BCS", jour(5, 1), fournisseur, nom, s2, p2);
        fixture.ligne(bcs, 1, "E-" + jeton, "Article E", "1");
        UUID bcx = fixture.bonCommande(jeton + "-BCX", LocalDate.of(FixtureRapportsAchat.ANNEE + 1, 2, 1), fournisseur, nom,
                s1, pa);
        fixture.ligne(bcx, 1, "F-" + jeton, "Article F", "1");
        return new Commandes(jeton + "-BC1", bl1Ref, jeton + "-BL2", jeton + "-BC2", jeton + "-BC0", jeton + "-BCB",
                jeton + "-BCS", jeton + "-BCX", articleA);
    }

    private List<Map<String, Object>> lignesBC(String... filtres) throws Exception {
        String[] parametres = new String[filtres.length + 4];
        parametres[0] = DU;
        parametres[1] = FixtureRapportsAchat.DU;
        parametres[2] = AU;
        parametres[3] = FixtureRapportsAchat.AU;
        System.arraycopy(filtres, 0, parametres, 4, filtres.length);
        return lignesAvec(sections(BONS_COMMANDE, parametres), CODE_BC);
    }

    private static List<Object> codesBC(List<Map<String, Object>> lignes) {
        return lignes.stream().map(l -> l.get(CODE_BC)).distinct().toList();
    }

    @Test
    void r14_ligneBCAvecDeuxBL_uneLigneParLivraison_resteCumuleParCode() throws Exception {
        Commandes c = commandes();

        List<Map<String, Object>> lignes = lignesBC().stream()
                .filter(l -> c.bc1().equals(l.get(CODE_BC)) && c.articleA().equals(l.get(CODE_ARTICLE))).toList();
        Map<String, Object> premier = lignes.stream().filter(l -> c.bl1Ref().equals(l.get(CODE_BL))).findFirst().orElseThrow();
        Map<String, Object> second = lignes.stream().filter(l -> c.bl2().equals(l.get(CODE_BL))).findFirst().orElseThrow();

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(lignes).as("une ligne par BL").hasSize(2);
            s.assertThat(premier).as("BL1 : codeBL = DocRef").containsEntry("designation", "Article A");
            s.assertThat(montant(premier, QTE_COMMANDEE)).isEqualByComparingTo("10");
            s.assertThat(montant(premier, QTE_LIVREE)).isEqualByComparingTo("4");
            s.assertThat(montant(premier, RESTE)).as("10 − 4").isEqualByComparingTo("6");
            s.assertThat(montant(second, QTE_LIVREE)).as("BL2 : DocRef vide → Code").isEqualByComparingTo("3");
            s.assertThat(montant(second, RESTE)).as("10 − (4 + 3)").isEqualByComparingTo("3");
            s.assertThat(String.valueOf(premier.get("dateBC"))).startsWith(FixtureRapportsAchat.ANNEE + "-03-01");
            s.assertThat(String.valueOf(premier.get("dateBL"))).startsWith(FixtureRapportsAchat.ANNEE + "-03-10");
        });
    }

    @Test
    void r14_ligneBCSansLivraison_uneLigne_resteEgalCommande() throws Exception {
        Commandes c = commandes();

        List<Map<String, Object>> lignes = lignesBC().stream().filter(l -> c.bc2().equals(l.get(CODE_BC))).toList();

        assertThat(lignes).hasSize(1).first().satisfies(l -> assertAll(
                () -> assertThat(l).containsEntry(CODE_ARTICLE, "C-" + jeton),
                () -> assertThat(montant(l, QTE_COMMANDEE)).isEqualByComparingTo("8"),
                () -> assertThat(montant(l, RESTE)).isEqualByComparingTo("8"),
                () -> assertThat(Objects.toString(l.get(CODE_BL), "")).as("sans BL").isEmpty()));
    }

    @Test
    void r14_bcSansProjet_present_d3() throws Exception {
        Commandes c = commandes();

        assertThat(codesBC(lignesBC())).contains(c.bc1(), c.bc0());
    }

    @Test
    void r14_bcProjetOuSocieteNonHabilites_absents() throws Exception {
        Commandes c = commandes();

        assertThat(codesBC(lignesBC())).contains(c.bc1()).doesNotContain(c.bcb(), c.bcs());
    }

    @Test
    void r14_bcHorsPeriode_absent() throws Exception {
        Commandes c = commandes();

        assertThat(codesBC(lignesBC())).contains(c.bc1()).doesNotContain(c.bcx());
    }

    @Test
    void r14_filtreCodeBC() throws Exception {
        Commandes c = commandes();

        assertThat(codesBC(lignesBC("codeBC", c.bc2()))).containsExactly(c.bc2());
    }

    @Test
    void r14_filtreCodeArticle() throws Exception {
        Commandes c = commandes();

        List<Map<String, Object>> lignes = lignesBC(CODE_ARTICLE, c.articleA());

        assertThat(lignes).isNotEmpty().allMatch(l -> c.articleA().equals(l.get(CODE_ARTICLE)));
    }

    @Test
    void r14_filtreProjetIdParIdentifiant_bcSansProjetExclu() throws Exception {
        Commandes c = commandes();

        assertThat(codesBC(lignesBC("projetId", pa2.toString()))).containsExactly(c.bc2());
    }

    @Test
    void r14_filtreSocieteIdParIdentifiant() throws Exception {
        Commandes c = commandes();

        assertThat(codesBC(lignesBC("societeId", s1.toString()))).contains(c.bc1(), c.bc0()).doesNotContain(c.bcs());
    }

    // ------------------------------------------------------------------ R15 : échéancier des crédits

    private record Prets(String visible, String sansProjet, String horsPerimetre) {
    }

    private Prets prets() {
        Pret l1 = fixture.pret("L1-" + jeton, s1, pa);
        fixture.echeance(l1, jour(2, 10), "500");
        fixture.echeance(l1, jour(2, 25), "250");
        fixture.echeance(l1, jour(5, 10), "100");
        fixture.echeance(l1, LocalDate.of(FixtureRapportsAchat.ANNEE + 1, 1, 10), "999");
        Pret l0 = fixture.pret("L0-" + jeton, s1, null);
        fixture.echeance(l0, jour(3, 10), "40");
        Pret lx = fixture.pret("LX-" + jeton, s1, pb);
        fixture.echeance(lx, jour(2, 10), "7000");
        return new Prets("L1-" + jeton, "L0-" + jeton, "LX-" + jeton);
    }

    private List<Map<String, Object>> echeancier(String marqueur) throws Exception {
        return lignesMentionnant(sections(ECHEANCIER_CREDITS, ANNEE, ANNEE_TEST), marqueur);
    }

    @Test
    void r15_echeancesDuPret_sommeesParMois() throws Exception {
        Prets p = prets();

        List<Map<String, Object>> lignes = echeancier(p.visible());

        assertThat(lignes).isNotEmpty();
        assertAll(
                () -> assertThat(unePorte(lignes, "750")).as("février : 500 + 250").isTrue(),
                () -> assertThat(unePorte(lignes, "100")).as("mai").isTrue(),
                () -> assertThat(unePorte(lignes, "500")).as("pas d'échéance isolée").isFalse());
    }

    @Test
    void r15_anneeDePaiementSeulement() throws Exception {
        Prets p = prets();

        List<Map<String, Object>> lignes = echeancier(p.visible());

        assertThat(lignes).isNotEmpty();
        assertThat(unePorte(lignes, "999")).as("échéance de l'année suivante").isFalse();
    }

    @Test
    void r15_pretSansProjet_visible() throws Exception {
        Prets p = prets();

        assertThat(unePorte(echeancier(p.sansProjet()), "40")).isTrue();
    }

    @Test
    void r15_pretDUnProjetNonHabilite_absent() throws Exception {
        Prets p = prets();

        assertAll(
                () -> assertThat(echeancier(p.visible())).as("témoin").isNotEmpty(),
                () -> assertThat(echeancier(p.horsPerimetre())).isEmpty());
    }
}
