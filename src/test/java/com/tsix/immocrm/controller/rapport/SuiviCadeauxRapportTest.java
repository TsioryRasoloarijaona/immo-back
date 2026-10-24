package com.tsix.immocrm.controller.rapport;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.cfg.CfgProject;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.com.ComGift;
import com.tsix.immocrm.entities.str.StrStatus;
import com.tsix.immocrm.fixtures.FixtureEtatVentes;
import com.tsix.immocrm.fixtures.FixtureRapports;
import com.tsix.immocrm.fixtures.FixtureRapportsCommerciaux;
import com.tsix.immocrm.services.equipe.RolesEquipe;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static com.tsix.immocrm.controller.rapport.AppelsRapports.VENTE;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.descripteur;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.lignes;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.lignesOu;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.resultat;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.section;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.somme;
import static com.tsix.immocrm.controller.rapport.LecturesRapportsCommerciaux.lignesContenant;
import static com.tsix.immocrm.controller.rapport.LecturesRapportsCommerciaux.typeParametre;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * N° 16 SUIVI DES CADEAUX (FEATURE-tdb-vente-rapports-commercial.md §8, décision 4, §10). Statut seuil
 * {@code DefaultComFolderStatusFinalisation} = MIN (Sorting 7, {@code BoolField014} = 1) ; RES : Sorting 1, 0 :
 * <pre>
 *   T  : A1 (MIN) : cadeau CAD livré sans bon + cadeau NULL non livré sans bon
 *        A2 (RES) : CAD non livré, bon d'achat livré          A3 (annulé) : CAD non livré sans bon
 *   TI (société inactive) : A4 (MIN) : CAD                   TX (projet P2) : A5 (MIN) : CAD2
 * </pre>
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SuiviCadeauxRapportTest {

    private static final String CODE = "ComTdbGift";
    private static final String TRANCHE_ID = "trancheId";
    private static final String CADEAU_LIVRE = "cadeauLivre";
    private static final String BA_LIVRE = "baLivre";
    private static final String CADEAUX = "cadeaux";
    private static final String NON_LIVRES = "non-livres";
    private static final String ARTICLE = "article";
    private static final String CADEAU = "cadeau";
    private static final String OUI = "oui";
    private static final LocalDateTime AVRIL = LocalDateTime.of(2025, 4, 1, 10, 0);

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureRapports fixture;
    @Autowired private FixtureRapportsCommerciaux rc;
    @Autowired private FixtureEtatVentes etatVentes;

    private AppelsRapports appels;
    private String prefixe;
    private CfgProject projetP2;
    private CfgTranche tranche;
    private CfgTranche trancheInactive;
    private AdmUser direction;

    @BeforeEach
    void setUp() {
        appels = new AppelsRapports(mockMvc);
        prefixe = FixtureEtatVentes.prefixe("R16");
        fixture.tables();
        fixture.rapportVente(CODE, "SUIVI DES CADEAUX", "", 1400);
        direction = rc.compte("r16.dg." + prefixe, new CfgProject[0], fixture.role("Direction Générale", false));

        tranche = rc.tranche("T-" + prefixe, rc.projet("P-" + prefixe, true));
        trancheInactive = rc.tranche("TI-" + prefixe, rc.projet("PI-" + prefixe, false));
        projetP2 = rc.projet("P2-" + prefixe, true);
        CfgTranche trancheX = rc.tranche("TX-" + prefixe, projetP2);

        StrStatus reservation = rc.statutVente("RES-" + prefixe, 1, false);
        StrStatus minute = rc.statutVente("MIN-" + prefixe, 7, true);
        StrStatus annule = etatVentes.statutSortie("ANN-" + prefixe, true);
        rc.parametreStatut("DefaultComFolderStatusFinalisation", minute);
        ComGift cad = rc.cadeau("CAD-" + prefixe);

        ComFolder d1 = rc.dossier(article("A1"), tranche, minute);
        rc.cadeauDossier(d1, cad, AVRIL, null);
        rc.cadeauDossier(d1, null, null, null);
        rc.cadeauDossier(rc.dossier(article("A2"), tranche, reservation), cad, null, AVRIL);
        rc.cadeauDossier(rc.dossier(article("A3"), tranche, annule), cad, null, null);
        rc.cadeauDossier(rc.dossier(article("A4"), trancheInactive, minute), cad, null, null);
        rc.cadeauDossier(rc.dossier(article("A5"), trancheX, minute), rc.cadeau("CAD2-" + prefixe), null, null);
        rc.relire();
    }

    private String article(String nom) {
        return prefixe + "-" + nom;
    }

    private MockHttpServletRequestBuilder sur(CfgTranche t) {
        return get(resultat(VENTE, CODE)).param(TRANCHE_ID, t.getId().toString()).with(connecte(direction));
    }

    private List<Map<String, Object>> cadeaux(MockHttpServletRequestBuilder requete) throws Exception {
        return lignes(section(appels.sections(requete), CADEAUX));
    }

    private static List<Object> articles(List<Map<String, Object>> lignes) {
        return lignes.stream().map(l -> l.get(ARTICLE)).toList();
    }

    // ------------------------------------------------------------------ descripteur, droits

    @Test
    void r16_descripteur_tranches_filtresLivreEtBonAchat() throws Exception {
        String corps = appels.corps(get(descripteur(VENTE, CODE)).with(connecte(direction)));

        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(typeParametre(corps, TRANCHE_ID)).isEqualTo("TRANCHES");
        soft.assertThat(typeParametre(corps, CADEAU_LIVRE)).isEqualTo("CHOIX");
        soft.assertThat(typeParametre(corps, BA_LIVRE)).isEqualTo("CHOIX");
        soft.assertThat(JsonPath.<List<String>>read(corps,
                "$.parametres[?(@.nom == 'cadeauLivre')].options[*].valeur")).containsExactly("tous", OUI, "non");
        soft.assertThat(JsonPath.<List<String>>read(corps,
                "$.parametres[?(@.nom == 'cadeauLivre')].valeurParDefaut")).containsExactly("tous");
        soft.assertAll();
    }

    @Test
    void transversal_roleNonLie_403() throws Exception {
        AdmUser restreint = rc.compte("r16.r." + prefixe, new CfgProject[0], fixture.role("Restreint " + prefixe, true));
        fixture.relire();

        assertThat(appels.statut(get(resultat(VENTE, CODE)).with(connecte(restreint)))).isEqualTo(403);
    }

    // ------------------------------------------------------------------ section cadeaux

    /** Une ligne par cadeau de dossier, dossier annulé inclus ; groupée par tranche. */
    @Test
    void r16_cadeaux_uneLigneParCadeauDeDossier_annuleInclus() throws Exception {
        Map<String, Object> s = section(appels.sections(sur(tranche)), CADEAUX);

        assertThat(s).containsEntry("groupePar", "tranche");
        assertThat(articles(lignes(s))).hasSize(4);
        assertThat(lignesContenant(s, ARTICLE, article("A1"))).hasSize(2);
        assertThat(lignesContenant(s, ARTICLE, article("A3"))).hasSize(1);
    }

    @Test
    void r16_cadeauLivre_ouiEtNon() throws Exception {
        List<Map<String, Object>> oui = cadeaux(sur(tranche).param(CADEAU_LIVRE, OUI));
        List<Map<String, Object>> non = cadeaux(sur(tranche).param(CADEAU_LIVRE, "non"));

        assertThat(oui).hasSize(1);
        assertThat(oui.get(0)).containsEntry(CADEAU_LIVRE, true);
        assertThat(non).hasSize(3);
    }

    @Test
    void r16_bonAchatLivre_oui() throws Exception {
        List<Map<String, Object>> oui = cadeaux(sur(tranche).param(BA_LIVRE, OUI));

        assertThat(oui).hasSize(1);
        assertThat(String.valueOf(oui.get(0).get(ARTICLE))).contains(article("A2"));
        assertThat(oui.get(0)).containsEntry(BA_LIVRE, true);
    }

    /** Décision 4 : ComGiftId NULL ⇒ LEFT JOIN, « Cadeau non précisé ». */
    @Test
    void r16_cadeauNul_cadeauNonPrecise() throws Exception {
        Map<String, Object> s = section(appels.sections(sur(tranche)), CADEAUX);

        assertThat(lignesOu(s, CADEAU, "Cadeau non précisé")).hasSize(1);
        assertThat(lignesOu(s, CADEAU, "CAD-" + prefixe)).hasSize(3);
    }

    @Test
    void r16_dossierAnnule_marque() throws Exception {
        Map<String, Object> s = section(appels.sections(sur(tranche)), CADEAUX);

        assertThat(lignesContenant(s, ARTICLE, article("A3"))).singleElement()
                .satisfies(l -> assertThat(l).containsEntry("annule", true));
        assertThat(lignesContenant(s, ARTICLE, article("A1"))).isNotEmpty()
                .allSatisfy(l -> assertThat(l).containsEntry("annule", false));
    }

    // ------------------------------------------------------------------ section non-livres

    /** Cadeaux sans bon d'achat sur dossiers au statut ≥ Finalisation : A1 seul (2 cadeaux), pas A2 (RES) ni A3. */
    @Test
    void r16_nonLivres_statutsFinalisesSansBonAchat() throws Exception {
        Map<String, Object> s = section(appels.sections(sur(tranche)), NON_LIVRES);

        assertThat(somme(lignes(s), "nombre")).isEqualByComparingTo("2");
        assertThat(lignesOu(s, CADEAU, "CAD-" + prefixe)).singleElement()
                .satisfies(l -> assertThat(AppelsRapports.nombre(l.get("nombre"))).isEqualByComparingTo("1"));
    }

    // ------------------------------------------------------------------ RV-R3a

    @Test
    void rv_trancheSocieteInactive_aucuneLigne() throws Exception {
        assertThat(cadeaux(sur(trancheInactive))).isEmpty();
    }

    @Test
    void rv_sansTrancheId_trancheInactiveAbsente() throws Exception {
        Map<String, Object> s = section(appels.sections(get(resultat(VENTE, CODE)).with(connecte(direction))), CADEAUX);

        assertThat(lignesContenant(s, ARTICLE, article("A1"))).hasSize(2);
        assertThat(lignesContenant(s, ARTICLE, article("A4"))).isEmpty();
    }

    @Test
    void rv_trancheHorsPerimetre_aucuneLigne() throws Exception {
        AdmUser u = rc.compte("r16.u." + prefixe, new CfgProject[] {projetP2}, fixture.role("Libre " + prefixe, false),
                fixture.role(RolesEquipe.COMMERCIAL, true));
        fixture.relire();

        assertThat(cadeaux(get(resultat(VENTE, CODE)).param(TRANCHE_ID, tranche.getId().toString())
                .with(connecte(u)))).isEmpty();
    }
}
