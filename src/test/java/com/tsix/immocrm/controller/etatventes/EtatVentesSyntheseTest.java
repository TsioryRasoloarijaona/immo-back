package com.tsix.immocrm.controller.etatventes;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.domain.StatutDossier;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.cfg.CfgProject;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.entities.stk.StkItemCategory;
import com.tsix.immocrm.entities.stk.StkItemType;
import com.tsix.immocrm.fixtures.FixtureDossierVente;
import com.tsix.immocrm.fixtures.FixtureEtatVentes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * {@code GET /api/etat-des-ventes/synthese} — FEATURE-etat-des-ventes.md §2 (R1–R4), §3.1, §6. Chaque test filtre sur
 * ses propres tranches : les comptes ne dépendent d'aucune autre ligne de la base.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser
class EtatVentesSyntheseTest {

    private static final String URL = "/api/etat-des-ventes/synthese";
    private static final String TRANCHE_ID = "trancheId";
    private static final String LIBRE = "LIBRE";
    private static final String RESERVATION = "RESERVATION";
    private static final String CONCRETISATION = "CONCRETISATION";
    private static final String SANS_STATUT = "SANS_STATUT";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private FixtureEtatVentes fixture;
    @Autowired
    private FixtureDossierVente fixtureDossier;

    private String prefixe;
    private CfgProject projet;
    private CfgTranche tranche;
    private StkItemCategory appartement;
    private StkItemCategory parking;
    private StkItemType type;

    @BeforeEach
    void setUp() {
        prefixe = FixtureEtatVentes.prefixe("EDVS");
        projet = fixture.projet("PRJ-" + prefixe, true);
        tranche = fixture.tranche("TR-" + prefixe, projet, true);
        appartement = fixture.categorie("APP-" + prefixe);
        parking = fixture.categorie("PAR-" + prefixe);
        type = fixture.type("T3-" + prefixe);
    }

    private StkItem article(String suffixe, StkItemCategory categorie) {
        return fixture.article(prefixe + suffixe, tranche, categorie, type);
    }

    private void vendre(StkItem article, StatutDossier statut) {
        fixture.vendre(article, "DOS-" + article.getCode(), statut == null ? null : fixture.statut(statut),
                null, LocalDateTime.of(2025, 3, 15, 10, 0));
    }

    /** Chemin JSON de la ligne tranche × catégorie (tableau vide si la ligne n'existe pas). */
    private static String ligne(CfgTranche t, StkItemCategory c) {
        return "$.lignes[?(@.trancheId=='" + t.getId() + "' && @.categorieId=='" + c.getId() + "')]";
    }

    private static String trancheLignes(CfgTranche t) {
        return "$.lignes[?(@.trancheId=='" + t.getId() + "')]";
    }

    // ------------------------------------------------------------------ R1 / R2 / R3 — comptes

    @Test
    void r1r2r3_comptesParTrancheCategorieEtStatut() throws Exception {
        article("-A1", appartement);
        article("-A2", appartement);
        vendre(article("-A3", appartement), StatutDossier.RESERVATION);
        vendre(article("-A4", appartement), StatutDossier.CONCRETISATION);
        article("-P1", parking);
        fixture.relire();

        String app = ligne(tranche, appartement);
        String par = ligne(tranche, parking);
        mockMvc.perform(get(URL).param(TRANCHE_ID, tranche.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath(app + ".trancheCode").value(tranche.getCode()))
                .andExpect(jsonPath(app + ".categorieCode").value(appartement.getCode()))
                .andExpect(jsonPath(app + ".total").value(4))
                .andExpect(jsonPath(app + ".parStatut." + LIBRE).value(2))
                .andExpect(jsonPath(app + ".parStatut." + RESERVATION).value(1))
                .andExpect(jsonPath(app + ".parStatut." + CONCRETISATION).value(1))
                .andExpect(jsonPath(par + ".total").value(1))
                .andExpect(jsonPath(par + ".parStatut." + LIBRE).value(1));
    }

    @Test
    void r1_parkingAutonome_sansParent_estCompte() throws Exception {
        // Vendu : un parking racine libre n'est plus compté (FEATURE-etat-ventes-omran.md §1.1).
        vendre(fixture.parkingAutonome(prefixe + "-PK", tranche, parking, type), StatutDossier.RESERVATION);
        fixture.relire();

        mockMvc.perform(get(URL).param(TRANCHE_ID, tranche.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath(ligne(tranche, parking) + ".total").value(1))
                .andExpect(jsonPath(ligne(tranche, parking) + ".parStatut." + RESERVATION).value(1));
    }

    /** R1 + R3 : le sous-article rattaché n'ouvre pas de ligne sous sa propre catégorie, même si le parent est vendu. */
    @Test
    void r1_sousArticleRattache_estExclu() throws Exception {
        StkItem parent = article("-A1", appartement);
        fixture.sousArticle(prefixe + "-A1-PK", parent, parking, type);
        vendre(parent, StatutDossier.RESERVATION);
        fixture.relire();

        mockMvc.perform(get(URL).param(TRANCHE_ID, tranche.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath(ligne(tranche, appartement) + ".total").value(1))
                .andExpect(jsonPath(ligne(tranche, appartement) + ".parStatut." + RESERVATION).value(1))
                .andExpect(jsonPath(ligne(tranche, parking)).doesNotExist());
    }

    @Test
    void r1_vocationAutreQueVente_estExclue() throws Exception {
        article("-A1", appartement);
        fixture.article(prefixe + "-PRJ", tranche, appartement, type, fixture.projetVocation());
        fixture.relire();

        mockMvc.perform(get(URL).param(TRANCHE_ID, tranche.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath(ligne(tranche, appartement) + ".total").value(1));
    }

    @Test
    void r1_trancheHorsVente_estExclue() throws Exception {
        CfgTranche horsVente = fixture.tranche("TRX-" + prefixe, projet, false);
        article("-A1", appartement);
        fixture.article(prefixe + "-X1", horsVente, appartement, type);
        fixture.relire();

        mockMvc.perform(get(URL).param(TRANCHE_ID, tranche.getId().toString(), horsVente.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath(ligne(tranche, appartement) + ".total").value(1))
                .andExpect(jsonPath(trancheLignes(horsVente)).doesNotExist());
    }

    @Test
    void r2_dossierAnnuleOuDesiste_compteLibre() throws Exception {
        fixture.vendre(article("-A1", appartement), "DOS-" + prefixe + "-ANN",
                fixture.statutSortie("ANN-" + prefixe, true), null, null);
        fixture.vendre(article("-A2", appartement), "DOS-" + prefixe + "-DES",
                fixture.statutSortie("DES-" + prefixe, false), null, null);
        fixture.relire();

        mockMvc.perform(get(URL).param(TRANCHE_ID, tranche.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath(ligne(tranche, appartement) + ".total").value(2))
                .andExpect(jsonPath(ligne(tranche, appartement) + ".parStatut." + LIBRE).value(2));
    }

    @Test
    void r2_dossierSansStatut_bucketSansStatut() throws Exception {
        vendre(article("-A1", appartement), null);
        article("-A2", appartement);
        fixture.relire();

        mockMvc.perform(get(URL).param(TRANCHE_ID, tranche.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath(ligne(tranche, appartement) + ".total").value(2))
                .andExpect(jsonPath(ligne(tranche, appartement) + ".parStatut." + SANS_STATUT).value(1))
                .andExpect(jsonPath(ligne(tranche, appartement) + ".parStatut." + LIBRE).value(1));
    }

    /** §3.1 : Libre en premier, « Sans statut » en dernier, avec ces libellés. */
    @Test
    void statuts_libreEnPremier_sansStatutEnDernier() throws Exception {
        vendre(article("-A1", appartement), StatutDossier.RESERVATION);
        vendre(article("-A2", appartement), null);
        fixture.relire();

        String corps = mockMvc.perform(get(URL).param(TRANCHE_ID, tranche.getId().toString()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        List<String> codes = JsonPath.read(corps, "$.statuts[*].code");
        List<String> libelles = JsonPath.read(corps, "$.statuts[*].libelle");
        assertThat(codes).isNotEmpty().startsWith(LIBRE).endsWith(SANS_STATUT).contains(RESERVATION);
        assertThat(libelles).startsWith("Libre").endsWith("Sans statut");
    }

    // ------------------------------------------------------------------ R4 — visibilité

    @Test
    void r4_trancheDEntiteInactive_estExclue() throws Exception {
        CfgProject projetInactif = fixture.projet("PRJI-" + prefixe, false);
        CfgTranche trancheInactive = fixture.tranche("TRI-" + prefixe, projetInactif, true);
        fixture.article(prefixe + "-I1", trancheInactive, appartement, type);
        fixture.relire();

        mockMvc.perform(get(URL).param(TRANCHE_ID, trancheInactive.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath(trancheLignes(trancheInactive)).doesNotExist());
    }

    @Test
    void r4_commercialHorsPerimetre_neVoitPasLaTranche() throws Exception {
        article("-A1", appartement);
        CfgProject autreProjet = fixture.projet("PRJB-" + prefixe, true);
        AdmUser commercial = fixtureDossier.compte("edv.com." + prefixe);
        fixture.affecter(commercial, autreProjet);
        fixture.relire();

        mockMvc.perform(get(URL).param(TRANCHE_ID, tranche.getId().toString())
                        .with(connecte(commercial, "Commercial")))
                .andExpect(status().isOk())
                .andExpect(jsonPath(trancheLignes(tranche)).doesNotExist());
    }

    @Test
    void r4_commercialDansLePerimetre_voitLaTranche() throws Exception {
        article("-A1", appartement);
        AdmUser commercial = fixtureDossier.compte("edv.com.ok." + prefixe);
        fixture.affecter(commercial, projet);
        fixture.relire();

        mockMvc.perform(get(URL).param(TRANCHE_ID, tranche.getId().toString())
                        .with(connecte(commercial, "Commercial")))
                .andExpect(status().isOk())
                .andExpect(jsonPath(ligne(tranche, appartement) + ".total").value(1));
    }

    @Test
    void r4_administrateurExempte_voitLaTrancheHorsAffectation() throws Exception {
        article("-A1", appartement);
        CfgProject autreProjet = fixture.projet("PRJC-" + prefixe, true);
        AdmUser admin = fixtureDossier.compte("edv.adm." + prefixe, "Administrateur");
        fixture.affecter(admin, autreProjet);
        fixture.relire();

        mockMvc.perform(get(URL).param(TRANCHE_ID, tranche.getId().toString())
                        .with(connecte(admin, "Administrateur")))
                .andExpect(status().isOk())
                .andExpect(jsonPath(ligne(tranche, appartement) + ".total").value(1));
    }

    // ------------------------------------------------------------------ §3.1 — filtres

    @Test
    void filtreProjet_neRendQueLesTranchesDuProjet() throws Exception {
        article("-A1", appartement);
        CfgProject autreProjet = fixture.projet("PRJD-" + prefixe, true);
        CfgTranche autreTranche = fixture.tranche("TRD-" + prefixe, autreProjet, true);
        fixture.article(prefixe + "-D1", autreTranche, appartement, type);
        fixture.relire();

        mockMvc.perform(get(URL).param("projetId", projet.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath(ligne(tranche, appartement) + ".total").value(1))
                .andExpect(jsonPath(trancheLignes(autreTranche)).doesNotExist());
    }

    @Test
    void filtreCategorie_neRendQueLaCategorie() throws Exception {
        article("-A1", appartement);
        article("-P1", parking);
        fixture.relire();

        mockMvc.perform(get(URL).param(TRANCHE_ID, tranche.getId().toString())
                        .param("categorieId", appartement.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath(ligne(tranche, appartement) + ".total").value(1))
                .andExpect(jsonPath(ligne(tranche, parking)).doesNotExist());
    }

    // ------------------------------------------------------------------ 400

    @Test
    void trancheIdInvalide_rend400() throws Exception {
        mockMvc.perform(get(URL).param(TRANCHE_ID, "pas-un-uuid"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void categorieIdInvalide_rend400() throws Exception {
        mockMvc.perform(get(URL).param("categorieId", "pas-un-uuid"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void projetIdInvalide_rend400() throws Exception {
        mockMvc.perform(get(URL).param("projetId", UUID.randomUUID() + "x"))
                .andExpect(status().isBadRequest());
    }
}
