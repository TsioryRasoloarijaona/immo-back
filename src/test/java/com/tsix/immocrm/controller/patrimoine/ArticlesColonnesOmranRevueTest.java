package com.tsix.immocrm.controller.patrimoine;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.entities.adm.AdmRole;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.entities.str.StrStatus;
import com.tsix.immocrm.fixtures.FixtureDossierVente;
import com.tsix.immocrm.fixtures.FixtureEtatVentes;
import com.tsix.immocrm.fixtures.FixtureEtatVentes.Nature;
import com.tsix.immocrm.fixtures.FixtureRapportsDossiers;
import com.tsix.immocrm.fixtures.FixtureSousArticles;
import com.tsix.immocrm.fixtures.FixtureSousArticles.Contexte;
import com.tsix.immocrm.repositories.adm.AdmUserRoleRepository;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Revue (FEATURE-articles-colonnes-omran.md §2.4, règle 6, visibilité) : un dossier hors du périmètre commercial de
 * l'appelant ne doit pas révéler son reste à régler dans la liste des articles.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ArticlesColonnesOmranRevueTest {

    private static final String RESTE_A_REGLER = "resteARegler";
    /** Prix 1 200 000 posé par {@code vendre}, avance réglée 150 000. */
    private static final String RESTE_ATTENDU = "1050000";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureSousArticles sousArticles;
    @Autowired private FixtureEtatVentes ventes;
    @Autowired private FixtureDossierVente dossiers;
    @Autowired private FixtureRapportsDossiers rapports;
    @Autowired private AdmUserRoleRepository rolesComptes;

    private record Vente(Contexte c, StkItem article, ComFolder dossier, StrStatus statut) {
    }

    private Vente vente(String prefixe) {
        Contexte c = sousArticles.contexte(prefixe);
        StkItem a = sousArticles.principal(c, "HP-" + c.jeton(), c.tranche());
        StrStatus statut = ventes.statutEnCours("EC-" + c.jeton());
        ComFolder d = ventes.vendre(a, "DOS-HP-" + c.jeton(), statut, ventes.client(prefixe + c.jeton()), null);
        ventes.regler(ventes.echeance(d, Nature.AVANCE, true, new BigDecimal("200000")), new BigDecimal("150000"),
                true, "POINTED");
        return new Vente(c, a, d, statut);
    }

    private AdmRole roleDe(AdmUser compte) {
        return rolesComptes.findByAdmUserId(compte.getId()).getFirst().getAdmRole();
    }

    private Map<String, Object> ligneArticle(Vente v, RequestPostProcessor connexion) throws Exception {
        String corps = mockMvc.perform(get("/api/articles").param("trancheId", v.c().tranche().getId().toString())
                        .param("colonnes", "code,dossierCode," + RESTE_A_REGLER).param("size", "1000")
                        .with(connexion))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        List<Map<String, Object>> lignes = JsonPath.read(corps, "$.content[?(@.id == '" + v.article().getId() + "')]");
        assertThat(lignes).hasSize(1);
        return lignes.getFirst();
    }

    private List<Map<String, Object>> lignesDossier(Vente v, RequestPostProcessor connexion) throws Exception {
        String corps = mockMvc.perform(get("/api/dossiers").param("trancheId", v.c().tranche().getId().toString())
                        .param("inclureSortis", "true").param("colonnes", "id,code,prixReste").param("size", "100")
                        .with(connexion))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return JsonPath.read(corps, "$.content[?(@.code == '" + v.dossier().getCode() + "')]");
    }

    private static BigDecimal nombre(Object valeur) {
        return valeur == null ? null : new BigDecimal(valeur.toString());
    }

    @Test
    void r6_resteARegler_dossierHorsPerimetre_null() throws Exception {
        Vente v = vente("D1R");
        // Commercial restreint à un autre projet que celui de l'article (et donc du dossier).
        AdmUser restreint = dossiers.compte("d1r.hp." + v.c().jeton());
        ventes.affecter(restreint, ventes.projet("PRJ-HP-" + v.c().jeton(), true));
        ventes.relire();

        assertThat(ligneArticle(v, FixtureDossierVente.connecte(restreint)))
                .as("dossier %s hors périmètre", v.dossier().getCode()).containsEntry(RESTE_A_REGLER, null);
    }

    /** Restreint au projet de l'article : le reste est montré et vaut le prixReste que /api/dossiers lui montre. */
    @Test
    void r6_resteARegler_commercialRestreint_dansPerimetre_egalPrixResteDeSaListe() throws Exception {
        Vente v = vente("D2R");
        AdmUser restreint = dossiers.compte("d2r.in." + v.c().jeton(), "COM-" + v.c().jeton());
        ventes.affecter(restreint, v.c().projet());
        rapports.statutVisiblePar(v.statut(), roleDe(restreint));
        ventes.relire();
        RequestPostProcessor connexion = FixtureDossierVente.connecte(restreint);

        List<Map<String, Object>> dossier = lignesDossier(v, connexion);
        assertThat(dossier).as("dossier visible dans /api/dossiers").hasSize(1);
        BigDecimal reste = nombre(ligneArticle(v, connexion).get(RESTE_A_REGLER));
        assertThat(reste).isNotNull().isEqualByComparingTo(RESTE_ATTENDU)
                .isEqualByComparingTo(nombre(dossier.getFirst().get("prixReste")));
    }

    /** Rôle exempté (§6.2) affecté à un autre projet : aucune restriction, le reste est montré. */
    @Test
    void r6_resteARegler_roleExempte_affecteAilleurs_voitLeReste() throws Exception {
        Vente v = vente("D3R");
        AdmUser direction = dossiers.compte("d3r.dg." + v.c().jeton(), "Direction Générale");
        ventes.affecter(direction, ventes.projet("PRJ-DG-" + v.c().jeton(), true));
        ventes.relire();

        assertThat(nombre(ligneArticle(v, FixtureDossierVente.connecte(direction)).get(RESTE_A_REGLER)))
                .isNotNull().isEqualByComparingTo(RESTE_ATTENDU);
    }

    /** Compte sans aucun projet affecté (§6.3) : non restreint, le reste est montré. */
    @Test
    void r6_resteARegler_compteSansProjet_voitLeReste() throws Exception {
        Vente v = vente("D4R");
        AdmUser libre = dossiers.compte("d4r.np." + v.c().jeton());
        ventes.relire();

        assertThat(nombre(ligneArticle(v, FixtureDossierVente.connecte(libre)).get(RESTE_A_REGLER)))
                .isNotNull().isEqualByComparingTo(RESTE_ATTENDU);
    }

    /**
     * §2.4 « ne révèle rien que l'utilisateur ne voit déjà (même périmètre que la liste) » : /api/dossiers filtre aussi
     * par statuts visibles (StrStatusIntervener) ; un dossier dont le statut est invisible au rôle de l'appelant
     * n'a pas de prixReste pour lui, la colonne doit rester null.
     */
    @Test
    void r6_resteARegler_statutInvisibleAuRole_null() throws Exception {
        Vente v = vente("D5R");
        AdmUser appelant = dossiers.compte("d5r.co." + v.c().jeton(), "AIDE-" + v.c().jeton());
        AdmUser autre = dossiers.compte("d5r.ch." + v.c().jeton(), "CHEF-" + v.c().jeton());
        rapports.statutVisiblePar(v.statut(), roleDe(autre));
        ventes.relire();
        RequestPostProcessor connexion = FixtureDossierVente.connecte(appelant);

        assertThat(lignesDossier(v, connexion)).as("dossier masqué de /api/dossiers pour ce rôle").isEmpty();
        assertThat(ligneArticle(v, connexion))
                .as("dossier %s invisible dans la liste des dossiers", v.dossier().getCode())
                .containsEntry(RESTE_A_REGLER, null);
    }
}
