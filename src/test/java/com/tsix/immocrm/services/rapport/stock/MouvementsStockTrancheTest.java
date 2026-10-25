package com.tsix.immocrm.services.rapport.stock;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.entities.cfg.CfgProject;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.entities.stk.StkItemCategory;
import com.tsix.immocrm.entities.stk.StkItemType;
import com.tsix.immocrm.entities.str.StrStatus;
import com.tsix.immocrm.fixtures.FixtureEtatVentes;
import com.tsix.immocrm.fixtures.FixtureRapports;
import com.tsix.immocrm.fixtures.FixtureRapportsPatrimoine;
import com.tsix.immocrm.repositories.cfg.CfgTrancheRepository;
import com.tsix.immocrm.services.rapport.stock.MouvementsStockParReflexion.Ligne;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.ApplicationContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static com.tsix.immocrm.fixtures.FixtureRapportsPatrimoine.SEUIL_ANNULATION;
import static com.tsix.immocrm.fixtures.FixtureRapportsPatrimoine.SEUIL_CONCRETISATION;
import static com.tsix.immocrm.fixtures.FixtureRapportsPatrimoine.SEUIL_RESILIATION;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Composant partagé {@code MouvementsStockTranche} extrait de P10a (FEATURE-tdb-vente-rapports-rythme.md §3.5, décision du
 * manager 3 ; carte R2b) : mêmes lignes que le rapport patrimoine {@code StkTdbMotion}, qui ne change pas (non-régression).
 * Jeu P10a (année 2074) :
 * <pre>
 *   TM (en vente, lancée le 10/01) : m1..m3 VENTE + ms sous-article de m1 + mp article PROJET
 *     m1 avancé 05/02 · ms avancé 10/02 · mp avancé 11/02 · m2 avancé 01/03, désisté 31/03 · m3 avancé 01/04, annulé 15/04
 *   TX, TY (en vente) : dossier de y1 transféré depuis x1 le 01/06 (historique au seuil de concrétisation)
 *   TN (PAS en vente) : n1 avancé 01/02
 * </pre>
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class MouvementsStockTrancheTest {

    private static final String RESULTAT_P10A = "/api/rapports/patrimoine/" + FixtureRapportsPatrimoine.MOUVEMENTS
            + "/resultat";

    @Autowired private MockMvc mockMvc;
    @Autowired private ApplicationContext contexte;
    @Autowired private FixtureRapports fixture;
    @Autowired private FixtureRapportsPatrimoine rp;
    @Autowired private FixtureEtatVentes ev;
    @Autowired private CfgTrancheRepository tranches;

    private RequestPostProcessor libre;
    private String prefixe;
    private StkItemCategory categorie;
    private StkItemType type;
    private CfgTranche tm;
    private CfgTranche tx;
    private CfgTranche ty;
    private CfgTranche tn;

    @BeforeEach
    void setUp() {
        prefixe = FixtureEtatVentes.prefixe("MST");
        rp.rapportsP10a();
        libre = connecte(fixture.compte("mst." + prefixe, fixture.role("Libre " + prefixe, false)));
        categorie = ev.categorie("CAT-" + prefixe);
        type = ev.type("TYP-" + prefixe);
        StrStatus concret = rp.statut("CONC-" + prefixe, 2, false, false);
        StrStatus annule = rp.statut("ANN-" + prefixe, 30, true, false);
        StrStatus resilie = rp.statut("RES-" + prefixe, 31, false, true);
        rp.seuil(SEUIL_CONCRETISATION, concret);
        rp.seuil(SEUIL_ANNULATION, annule);
        rp.seuil(SEUIL_RESILIATION, resilie);

        CfgProject projet = ev.projet("P-" + prefixe, true);
        tm = ev.tranche("TM-" + prefixe, projet, true);
        tm.setMarketingStartDate(LocalDateTime.of(2074, 1, 10, 0, 0));
        tm = tranches.save(tm);
        StkItem m1 = article("M1", tm);
        StkItem m2 = article("M2", tm);
        StkItem m3 = article("M3", tm);
        StkItem ms = ev.sousArticle(prefixe + "-MS", m1, categorie, type);
        StkItem mp = ev.article(prefixe + "-MP", tm, categorie, type, ev.projetVocation());
        avancer(m1, concret, LocalDateTime.of(2074, 2, 5, 0, 0), null);
        avancer(ms, concret, LocalDateTime.of(2074, 2, 10, 0, 0), null);
        avancer(mp, concret, LocalDateTime.of(2074, 2, 11, 0, 0), null);
        avancer(m2, resilie, LocalDateTime.of(2074, 3, 1, 0, 0), LocalDateTime.of(2074, 3, 31, 18, 30));
        avancer(m3, annule, LocalDateTime.of(2074, 4, 1, 10, 30), LocalDateTime.of(2074, 4, 15, 0, 0));

        CfgProject projetT = ev.projet("PT-" + prefixe, true);
        tx = ev.tranche("TX-" + prefixe, projetT, true);
        ty = ev.tranche("TY-" + prefixe, projetT, true);
        StkItem x1 = article("X1", tx);
        StkItem y1 = article("Y1", ty);
        rp.transfert(avancer(y1, concret, LocalDateTime.of(2074, 5, 1, 0, 0), null), x1, y1,
                LocalDateTime.of(2074, 6, 1, 0, 0), concret);

        tn = ev.tranche("TN-" + prefixe, projet, false);
        avancer(article("N1", tn), concret, LocalDateTime.of(2074, 2, 1, 0, 0), null);
        fixture.relire();
    }

    private StkItem article(String suffixe, CfgTranche tranche) {
        return ev.article(prefixe + "-" + suffixe, tranche, categorie, type);
    }

    private ComFolder avancer(StkItem article, StrStatus statut, LocalDateTime avance, LocalDateTime sortie) {
        ComFolder dossier = ev.vendre(article, "DOS-" + article.getCode(), statut, null, null);
        dossier.setAdvanceDate(avance);
        dossier.setCancelDate(sortie);
        return ev.enregistrer(dossier);
    }

    /** Lignes de la section {@code mouvements} du rapport P10a sur toute la plage {@code smalldatetime}. */
    private List<Map<String, Object>> lignesDuRapportP10a(CfgTranche... demandees) throws Exception {
        MockHttpServletRequestBuilder requete = get(RESULTAT_P10A).param("du", "1900-01-01").param("au", "2078-12-31")
                .with(libre);
        for (CfgTranche t : demandees) {
            requete.param("trancheId", t.getId().toString());
        }
        String corps = mockMvc.perform(requete).andExpect(status().isOk()).andReturn().getResponse()
                .getContentAsString();
        return JsonPath.read(corps, "$.sections[?(@.id == 'mouvements')].lignes[*]");
    }

    private static String cle(String article, String dossier, String libelle, Object date, Object quantite) {
        return "%s|%s|%s|%s|%s".formatted(article, dossier, libelle, date, quantite);
    }

    private static boolean memeTranche(Object rendue, CfgTranche t) {
        String texte = String.valueOf(rendue);
        return texte.equalsIgnoreCase(t.getId().toString()) || texte.equals(t.getDescription());
    }

    // ------------------------------------------------------------------ extraction (§3.5)

    @Test
    void lire_memesLignesQueLeRapportMouvementsDuStockP10a() throws Exception {
        List<Ligne> composant = MouvementsStockParReflexion.lire(contexte, List.of(tm.getId(), tx.getId(), ty.getId()));
        List<Map<String, Object>> rapport = lignesDuRapportP10a(tm, tx, ty);

        assertThat(composant).extracting(l -> cle(l.article(), l.dossier(), l.libelle(), l.date(), l.quantite()))
                .isNotEmpty()
                .containsExactlyInAnyOrderElementsOf(rapport.stream().map(l -> cle((String) l.get("article"),
                        (String) l.get("dossier"), (String) l.get("libelle"), l.get("date"),
                        Long.parseLong(String.valueOf(l.get("mouvement"))))).toList());
    }

    @Test
    void lire_chaqueLignePorteSaTranche() {
        List<Ligne> composant = MouvementsStockParReflexion.lire(contexte, List.of(tm.getId(), tx.getId(), ty.getId()));

        assertThat(composant).isNotEmpty().allSatisfy(l -> assertThat(memeTranche(l.tranche(), tm)
                || memeTranche(l.tranche(), tx) || memeTranche(l.tranche(), ty)).as("%s", l).isTrue());
        assertThat(composant).filteredOn(l -> MouvementsStockParReflexion.LANCEMENT.equals(l.libelle()))
                .singleElement().satisfies(l -> assertThat(memeTranche(l.tranche(), tm)).isTrue())
                .satisfies(l -> assertThat(l.quantite()).as("3 articles VENTE racines").isEqualTo(3L));
    }

    @Test
    void lire_trancheHorsVente_aucuneLigne() {
        List<Ligne> composant = MouvementsStockParReflexion.lire(contexte, List.of(tn.getId()));

        assertThat(composant).isEmpty();
    }

    // ------------------------------------------------------------------ non-régression P10a

    @Test
    void p10a_rapportInchange_lancementConcretisationsRetoursTransferts() throws Exception {
        List<Map<String, Object>> rapport = lignesDuRapportP10a(tm, tx, ty);

        assertThat(rapport).extracting(l -> l.get("libelle") + " " + l.get("mouvement")).containsExactlyInAnyOrder(
                "LANCEMENT DE LA COMMERCIALISATION 3",
                "CONCRETISATION -1", "CONCRETISATION -1", "CONCRETISATION -1", "CONCRETISATION -1",
                "CONCRETISATION -1", "DÉSISTEMENT 1", "ANNULATION 1",
                "CONCRETISATION -1", "TRANSFERT SORTIE 1", "TRANSFERT ENTREE -1");
        assertThat(rapport).extracting(l -> l.get("article")).filteredOn(Objects::nonNull).isNotEmpty();
    }
}
