package com.tsix.immocrm.controller.projet;

import com.tsix.immocrm.controller.projet.AppelsProjet.Reponse;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.fixtures.FixtureProjetLots;
import com.tsix.immocrm.fixtures.FixtureVisibiliteOmran;
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
import java.util.Map;
import java.util.UUID;

import static com.tsix.immocrm.controller.projet.AppelsProjet.DECOMPTES;
import static com.tsix.immocrm.controller.projet.AppelsProjet.LOTS;
import static com.tsix.immocrm.controller.projet.AppelsProjet.REFERENTIELS;
import static com.tsix.immocrm.controller.projet.AppelsProjet.TAILLE;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Lots visibles par statut, comme OMRAN (FEATURE-visibilite-omran.md R1 (b), R2, R7 ; réponses du manager Q1, Q5).
 * <pre>
 *   entité E (propre au test) : S1 ← rôle RU, S2 ← rôle RX (porté par personne), S3 ← compte W (nominatif),
 *                               S4 sans aucun intervenant ; statut de projet SP ← rôles RU et RW
 *   projet A (SP) : L1 (S1, F1), L2 (S2, F2), L3 (S3, F1), L4 (S4, F1), L5 (sans statut, F1)   projet B (SP) : LB (S1)
 *   U (rôle RU), W (rôle RW), ADM (rôle « Administrateur ») restreints ; LIB (rôle à WithSequenceFreeRestriction = 0)
 *   tous habilités sur A seulement ; décomptes D1 sur L1, D2 sur L2, DB sur LB
 * </pre>
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class LotsVisibiliteStatutTest {

    private static final String CONTENU_IDS = "$.content[*].id";
    private static final String SIZE = "size";
    private static final String LOT_INTROUVABLE = "Lot introuvable";
    private static final String ADMINISTRATEUR = "Administrateur";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureProjetLots fixture;
    @Autowired private FixtureVisibiliteOmran visibilite;

    private AppelsProjet appels;
    private String jeton;
    private UUID entite;
    private UUID projetA;
    private UUID fournisseur1;
    private UUID fournisseur2;
    private UUID lot1;
    private UUID lot2;
    private UUID lot3;
    private UUID lot4;
    private UUID lot5;
    private UUID lotB;
    private UUID decompte1;
    private UUID decompte2;
    private AdmUser compteU;
    private RequestPostProcessor u;
    private RequestPostProcessor w;

    @BeforeEach
    void setUp() {
        appels = new AppelsProjet(mockMvc);
        jeton = FixtureProjetLots.jeton();
        fournisseur1 = fixture.fournisseur("F1-" + jeton);
        fournisseur2 = fixture.fournisseur("F2-" + jeton);
        projetA = fixture.projet("PA-" + jeton);
        UUID projetB = fixture.projet("PB-" + jeton);

        String roleU = "RU-" + jeton;
        String roleW = "RW-" + jeton;
        compteU = visibilite.compte("lot.u." + jeton, roleU, true);
        AdmUser compteW = visibilite.compte("lot.w." + jeton, roleW, true);
        UUID roleX = visibilite.role("RX-" + jeton);

        entite = visibilite.entite("LOTV-" + jeton);
        UUID s1 = visibilite.statut(entite, "S1-" + jeton);
        UUID s2 = visibilite.statut(entite, "S2-" + jeton);
        UUID s3 = visibilite.statut(entite, "S3-" + jeton);
        UUID s4 = visibilite.statut(entite, "S4-" + jeton);
        visibilite.intervenantRole(entite, s1, visibilite.idRole(roleU));
        visibilite.intervenantRole(entite, s2, roleX);
        visibilite.intervenantCompte(entite, s3, compteW.getId());

        UUID entiteProjet = visibilite.entite("PRJV-" + jeton);
        UUID sp = visibilite.statut(entiteProjet, "SP-" + jeton);
        visibilite.intervenantRole(entiteProjet, sp, visibilite.idRole(roleU));
        visibilite.intervenantRole(entiteProjet, sp, visibilite.idRole(roleW));
        visibilite.statutProjet(projetA, entiteProjet, sp);
        visibilite.statutProjet(projetB, entiteProjet, sp);

        lot1 = lot(projetA, "L1-" + jeton, fournisseur1, s1);
        lot2 = lot(projetA, "L2-" + jeton, fournisseur2, s2);
        lot3 = lot(projetA, "L3-" + jeton, fournisseur1, s3);
        lot4 = lot(projetA, "L4-" + jeton, fournisseur1, s4);
        lot5 = fixture.lot(projetA, "L5-" + jeton, fournisseur1);
        lotB = lot(projetB, "LB-" + jeton, fournisseur1, s1);
        decompte1 = fixture.decompte(lot1, "D1-" + jeton, 1, montants());
        decompte2 = fixture.decompte(lot2, "D2-" + jeton, 2, montants());
        fixture.decompte(lotB, "DB-" + jeton, 3, montants());

        fixture.habiliter(compteU, projetA);
        fixture.habiliter(compteW, projetA);
        u = connecte(compteU, roleU);
        w = connecte(compteW, roleW);
    }

    private UUID lot(UUID projet, String code, UUID fournisseur, UUID statut) {
        UUID id = fixture.lot(projet, code, fournisseur);
        visibilite.statutLot(id, entite, statut);
        return id;
    }

    private static Map<String, BigDecimal> montants() {
        return Map.of(FixtureProjetLots.TTC, new BigDecimal("1200.00"), FixtureProjetLots.A_PAYER,
                new BigDecimal("1200.00"));
    }

    // ------------------------------------------------------------------ R1 (b) : qui voit, qui ne voit pas

    @Test
    void r1b_compteParRole_neVoitQueLesLotsDesStatutsDeSonRole() throws Exception {
        Reponse r = appels.lire(u, LOTS, SIZE, TAILLE);

        assertThat(r.statut()).isEqualTo(200);
        assertThat(r.ids(CONTENU_IDS)).containsExactly(lot1.toString());
    }

    @Test
    void r1b_intervenantNominatif_compteVoitLeLotDeSonStatut() throws Exception {
        Reponse r = appels.lire(w, LOTS, SIZE, TAILLE);

        assertThat(r.statut()).isEqualTo(200);
        assertThat(r.ids(CONTENU_IDS)).containsExactly(lot3.toString());
    }

    @Test
    void r1b_statutSansAucunIntervenant_etLotSansStatut_invisibles() throws Exception {
        Reponse liste = appels.lire(u, LOTS, SIZE, TAILLE);
        Reponse ficheSansIntervenant = appels.lire(u, LOTS + "/" + lot4);
        Reponse ficheSansStatut = appels.lire(u, LOTS + "/" + lot5);

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(liste.ids(CONTENU_IDS)).doesNotContain(lot4.toString(), lot5.toString());
            s.assertThat(ficheSansIntervenant.statut()).isEqualTo(404);
            s.assertThat(ficheSansStatut.statut()).isEqualTo(404);
        });
    }

    @Test
    void r1b_ficheLotAuStatutInvisible_404CommeIdInconnu() throws Exception {
        Reponse visible = appels.lire(u, LOTS + "/" + lot1);
        Reponse statutInvisible = appels.lire(u, LOTS + "/" + lot2);
        Reponse nominatifDAutrui = appels.lire(u, LOTS + "/" + lot3);

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(visible.statut()).as("témoin").isEqualTo(200);
            s.assertThat(statutInvisible.statut()).isEqualTo(404);
            s.assertThat(statutInvisible.corps()).contains(LOT_INTROUVABLE);
            s.assertThat(nominatifDAutrui.statut()).isEqualTo(404);
            s.assertThat(nominatifDAutrui.corps()).contains(LOT_INTROUVABLE);
        });
    }

    @Test
    void r1b_totalElements_egalAuNombreDeLotsVisibles() throws Exception {
        Reponse r = appels.lire(u, LOTS, SIZE, TAILLE);

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(r.statut()).isEqualTo(200);
            s.assertThat(r.nombre("$.totalElements")).isEqualByComparingTo("1");
            s.assertThat(r.ids(CONTENU_IDS)).hasSize(1);
        });
    }

    // ------------------------------------------------------------------ R1 : exemption = drapeau OMRAN seul (Q1)

    @Test
    void r1_roleSansRestriction_voitTousLesStatutsDeSesProjets() throws Exception {
        String role = "LIB-" + jeton;
        AdmUser libre = visibilite.compte("lot.lib." + jeton, role, false);
        fixture.habiliter(libre, projetA);

        Reponse r = appels.lire(connecte(libre, role), LOTS, SIZE, TAILLE);

        assertThat(r.statut()).isEqualTo(200);
        assertThat(r.ids(CONTENU_IDS)).containsExactlyInAnyOrder(lot1.toString(), lot2.toString(), lot3.toString(),
                lot4.toString(), lot5.toString());
    }

    @Test
    void r1_administrateurRestreint_pasDExemptionParLeCodeDeRole() throws Exception {
        AdmUser admin = visibilite.compte("lot.adm." + jeton, "ADM-" + jeton, true);
        visibilite.restreindre(ADMINISTRATEUR);
        fixture.habiliter(admin, projetA);
        RequestPostProcessor commeAdmin = connecte(admin, ADMINISTRATEUR, "ADM-" + jeton);

        Reponse liste = appels.lire(commeAdmin, LOTS, SIZE, TAILLE);
        Reponse fiche = appels.lire(commeAdmin, LOTS + "/" + lot1);

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(liste.statut()).isEqualTo(200);
            s.assertThat(liste.nombre("$.totalElements")).isEqualByComparingTo("0");
            s.assertThat(fiche.statut()).isEqualTo(404);
        });
    }

    // ------------------------------------------------------------------ R1 (a) : le statut ne suffit pas sans le projet

    @Test
    void r1a_statutVisibleMaisProjetNonHabilite_lotInvisible() throws Exception {
        Reponse liste = appels.lire(u, LOTS, SIZE, TAILLE);
        Reponse fiche = appels.lire(u, LOTS + "/" + lotB);

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(liste.ids(CONTENU_IDS)).doesNotContain(lotB.toString());
            s.assertThat(fiche.statut()).isEqualTo(404);
        });
    }

    // ------------------------------------------------------------------ R1 : référentiel (nombre de lots, fournisseurs)

    @Test
    void r1_referentiel_nbLotsDuProjet_neCompteQueLesLotsVisibles() throws Exception {
        Reponse r = appels.lire(u, REFERENTIELS);

        assertThat(r.statut()).isEqualTo(200);
        assertThat(String.valueOf(r.valeur("$.projets[?(@.id == '" + projetA + "')].nbLots"))).isEqualTo("[1]");
    }

    @Test
    void r1_referentiel_fournisseurs_seulementCeuxDesLotsVisibles() throws Exception {
        Reponse r = appels.lire(u, REFERENTIELS);

        assertThat(r.ids("$.fournisseurs[*].id")).contains(fournisseur1.toString())
                .doesNotContain(fournisseur2.toString());
    }

    // ------------------------------------------------------------------ R2 : décomptes inchangés (projets seuls)

    @Test
    void r2_listeDecomptes_projetsSeuls_memeSurUnLotAuStatutInvisible() throws Exception {
        Reponse r = appels.lire(u, DECOMPTES, SIZE, TAILLE);

        assertThat(r.statut()).isEqualTo(200);
        assertThat(r.ids(CONTENU_IDS)).containsExactlyInAnyOrder(decompte1.toString(), decompte2.toString());
    }

    @Test
    void r2_ficheDecompte_lotAuStatutInvisible_200() throws Exception {
        Reponse r = appels.lire(u, DECOMPTES + "/" + decompte2);

        assertThat(r.statut()).isEqualTo(200);
        assertThat(r.valeur("$.id")).isEqualTo(decompte2.toString());
    }

    // ------------------------------------------------------------------ R7 : liste des projets du module Projet

    @Test
    void r7_referentielProjets_projetAuStatutInvisible_absent() throws Exception {
        UUID entiteProjet = visibilite.entite("PRJX-" + jeton);
        UUID statutCache = visibilite.statut(entiteProjet, "SPX-" + jeton);
        visibilite.intervenantRole(entiteProjet, statutCache, visibilite.role("RPX-" + jeton));
        UUID projetCache = fixture.projet("PX-" + jeton);
        visibilite.statutProjet(projetCache, entiteProjet, statutCache);
        UUID projetSansStatut = fixture.projet("PN-" + jeton);
        fixture.habiliter(compteU, projetCache);
        fixture.habiliter(compteU, projetSansStatut);

        Reponse r = appels.lire(u, REFERENTIELS);

        assertThat(r.statut()).isEqualTo(200);
        assertThat(r.ids("$.projets[*].id")).containsExactly(projetA.toString());
    }

    @Test
    void r7_referentielProjets_roleSansRestriction_voitTousSesProjets() throws Exception {
        UUID entiteProjet = visibilite.entite("PRJX-" + jeton);
        UUID statutCache = visibilite.statut(entiteProjet, "SPX-" + jeton);
        visibilite.intervenantRole(entiteProjet, statutCache, visibilite.role("RPX-" + jeton));
        UUID projetCache = fixture.projet("PX-" + jeton);
        visibilite.statutProjet(projetCache, entiteProjet, statutCache);
        String role = "LIB-" + jeton;
        AdmUser libre = visibilite.compte("prj.lib." + jeton, role, false);
        fixture.habiliter(libre, projetA);
        fixture.habiliter(libre, projetCache);

        Reponse r = appels.lire(connecte(libre, role), REFERENTIELS);

        assertThat(r.statut()).isEqualTo(200);
        assertThat(r.ids("$.projets[*].id")).containsExactlyInAnyOrder(projetA.toString(), projetCache.toString());
    }
}
