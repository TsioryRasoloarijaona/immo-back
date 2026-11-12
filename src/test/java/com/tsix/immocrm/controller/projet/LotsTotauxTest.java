package com.tsix.immocrm.controller.projet;

import com.tsix.immocrm.controller.projet.AppelsProjet.Reponse;
import com.tsix.immocrm.fixtures.FixtureLotsParite;
import com.tsix.immocrm.fixtures.FixtureProjetLots;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
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
import java.util.UUID;

import static com.tsix.immocrm.controller.projet.LotsPariteSupport.ENTITE_ID;
import static com.tsix.immocrm.controller.projet.LotsPariteSupport.MONTANTS;
import static com.tsix.immocrm.controller.projet.LotsPariteSupport.PROJET_IDS;
import static com.tsix.immocrm.controller.projet.LotsPariteSupport.SEARCH;
import static com.tsix.immocrm.controller.projet.LotsPariteSupport.STATUT_IDS;
import static com.tsix.immocrm.controller.projet.LotsPariteSupport.TOTAUX;
import static com.tsix.immocrm.controller.projet.LotsPariteSupport.somme;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static com.tsix.immocrm.fixtures.FixtureLotsParite.ENTREPRENEURS;
import static com.tsix.immocrm.fixtures.FixtureLotsParite.FOURNISSEURS;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * E3, R5 — FEATURE-lots-liste-parite.md : chaque total = Σ du champ homonyme sur toutes les pages de E1 (taille 1) aux mêmes
 * filtres ; {@code nombreLots} = {@code totalElements}. Lots de montants distincts (règlements sur pièces du lot), un lot
 * invisible (projet non habilité) avec un règlement qui ne doit compter nulle part.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class LotsTotauxTest {

    private static final String NOMBRE_LOTS = "$.nombreLots";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureLotsParite fixture;
    @Autowired private FixtureProjetLots lots;

    private LotsPariteSupport appels;
    private String jeton;
    private RequestPostProcessor qui;
    private UUID entite;
    private UUID p1;
    private UUID s1;

    @BeforeEach
    void setUp() {
        appels = new LotsPariteSupport(mockMvc);
        jeton = FixtureProjetLots.jeton();
        fixture.genresDeLot();
        entite = fixture.entiteJuridique("ET-" + jeton);
        UUID autreEntite = fixture.entiteJuridique("EU-" + jeton);
        p1 = fixture.projet("TP1-" + jeton, entite);
        UUID p2 = fixture.projet("TP2-" + jeton, autreEntite);
        UUID invisible = fixture.projet("TPX-" + jeton, entite);
        s1 = fixture.statut(ENTREPRENEURS, "TS1-" + jeton);
        UUID s2 = fixture.statut(ENTREPRENEURS, "TS2-" + jeton);
        UUID sf = fixture.statut(FOURNISSEURS, "TSF-" + jeton);
        regle(fixture.lot(p1, "TA-" + jeton, ENTREPRENEURS, s1), "TA", "300.00", "200.00");
        regle(fixture.lot(p1, "TB-" + jeton, FOURNISSEURS, sf), "TB", "75.50", null);
        regle(fixture.lot(p2, "TC-" + jeton, ENTREPRENEURS, s1), "TC", "1000.00", "40.25");
        fixture.lot(p2, "TD-" + jeton, ENTREPRENEURS, s2);
        regle(fixture.lot(invisible, "TX-" + jeton, ENTREPRENEURS, s1), "TX", "999.00", null);
        qui = connecte(fixture.administrateur("lots.tot." + jeton, p1, p2), "Administrateur");
    }

    /** Pièce du lot réglée une fois (non annulé) et, si {@code annule} non nul, une seconde fois (CANCELED). */
    private void regle(UUID lot, String code, String montant, String annule) {
        UUID piece = fixture.pieceDuLot(lot, code + "-P-" + jeton, new BigDecimal("5000.00"));
        lots.reglement(piece, code + "-R-" + jeton, new BigDecimal(montant), null);
        if (annule != null) {
            lots.reglement(piece, code + "-RA-" + jeton, new BigDecimal(annule), "CANCELED");
        }
    }

    private void totauxEgauxALaSommeDesLignes(String... filtres) throws Exception {
        List<Map<String, Object>> lignes = appels.toutesLesLignes(qui, filtres);
        Reponse totalListe = appels.lire(qui, LotsPariteSupport.LOTS, filtres);
        Reponse totaux = appels.lire(qui, TOTAUX, filtres);

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(totaux.statut()).as(totaux.corps()).isEqualTo(200);
            s.assertThat(lignes).isNotEmpty();
            if (totaux.statut() != 200) {
                return;
            }
            s.assertThat(totaux.nombre(NOMBRE_LOTS)).as("nombreLots = totalElements")
                    .isEqualByComparingTo(totalListe.nombre(LotsPariteSupport.TOTAL))
                    .isEqualByComparingTo(BigDecimal.valueOf(lignes.size()));
            for (String champ : MONTANTS) {
                s.assertThat(totaux.nombre("$." + champ)).as(champ).isEqualByComparingTo(somme(lignes, champ));
            }
        });
    }

    @Test
    void r5_sansFiltre_totauxEgauxALaSommeDesLignes() throws Exception {
        totauxEgauxALaSommeDesLignes();
    }

    @Test
    void r5_filtreStatut_totauxEgauxALaSommeDesLignes() throws Exception {
        totauxEgauxALaSommeDesLignes(STATUT_IDS, s1.toString());
    }

    @Test
    void r5_filtreProjetEtEntite_totauxEgauxALaSommeDesLignes() throws Exception {
        totauxEgauxALaSommeDesLignes(PROJET_IDS, p1.toString(), ENTITE_ID, entite.toString());
    }

    @Test
    void r5_filtreRecherche_totauxEgauxALaSommeDesLignes() throws Exception {
        totauxEgauxALaSommeDesLignes(SEARCH, "TC-" + jeton);
    }

    @Test
    void e3_sansFiltre_compteLesSeulsLotsVisibles_reglesAnnulesCompris() throws Exception {
        Reponse totaux = appels.lire(qui, TOTAUX);

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(totaux.statut()).isEqualTo(200);
            s.assertThat(totaux.nombre(NOMBRE_LOTS)).as("lot invisible exclu").isEqualByComparingTo("4");
            s.assertThat(totaux.nombre("$.coutPrevisionnelRegle")).as("300+200+75,50+1000+40,25, sans les 999 du lot invisible")
                    .isEqualByComparingTo("1615.75");
        });
    }

    @Test
    void e3_filtreSansResultat_200EtZero() throws Exception {
        Reponse totaux = appels.lire(qui, TOTAUX, SEARCH, "AUCUN-" + jeton);

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(totaux.statut()).isEqualTo(200);
            s.assertThat(totaux.nombre(NOMBRE_LOTS)).isEqualByComparingTo("0");
            for (String champ : MONTANTS) {
                s.assertThat(totaux.nombre("$." + champ)).as(champ).isEqualByComparingTo("0");
            }
        });
    }
}
