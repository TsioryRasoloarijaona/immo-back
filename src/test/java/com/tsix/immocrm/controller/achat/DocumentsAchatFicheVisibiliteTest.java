package com.tsix.immocrm.controller.achat;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.fixtures.FixtureAchat;
import com.tsix.immocrm.fixtures.FixtureAchat.Piece;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static com.tsix.immocrm.controller.achat.AppelsAchat.codes;
import static com.tsix.immocrm.controller.achat.AppelsAchat.corps;
import static com.tsix.immocrm.controller.achat.AppelsAchat.liste;
import static com.tsix.immocrm.controller.achat.AppelsImpayes.NATURES;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * Lot A2a, E1 (règle 6) + réponse du manager Q2 (décision du user 12/11, « restreindre partout ») :
 * la fiche {@code GET /api/achat/documents/{id}} répond 200 ssi la pièce figure dans Documents Achats (liste sans
 * filtre de nature) ET dans la liste de sa nature ; sinon 404 « Pièce introuvable ». Restriction BC : déclencheur =
 * personnel du compte, sauf droit {@code WithNoRestriction} ; compte sans personnel = aucune restriction.
 * <pre>
 *   société S habilitée, projet P habilité : BC-A (déclencheur = personnel de A), BC-B (personnel de B), BC-SANS (aucun),
 *       FAC-B, DEV-B, BL-B (autres natures, déclencheur = personnel de B)
 *   hors périmètre (déclencheur = personnel de A) : BC-A-HS (société S2 non habilitée), BC-A-PNH (projet P2 de S non habilité)
 * </pre>
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class DocumentsAchatFicheVisibiliteTest {

    private static final String ROLE = "Commercial";
    private static final String TRIGGER = "CfgTierTriggerId";
    private static final String INTROUVABLE = "Pièce introuvable";

    private static final String BC_A = "-BC-A";
    private static final String BC_B = "-BC-B";
    private static final String BC_SANS = "-BC-SANS";
    private static final String FAC_B = "-FAC-B";
    private static final String DEV_B = "-DEV-B";
    private static final String BL_B = "-BL-B";
    private static final String BC_A_HS = "-BC-A-HS";
    private static final String BC_A_PNH = "-BC-A-PNH";

    /** Comptes du jeu : restreint A, A avec le droit, restreint B, sans personnel. */
    enum Compte { A_RESTREINT, A_AVEC_DROIT, B_RESTREINT, SANS_PERSONNEL }

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureAchat fixture;

    private String jeton;
    private final Map<String, UUID> pieces = new LinkedHashMap<>();
    private final Map<String, String> natures = new LinkedHashMap<>();
    private final Map<Compte, AdmUser> comptes = new LinkedHashMap<>();

    @BeforeEach
    void setUp() {
        jeton = FixtureAchat.jeton("AFV");
        UUID societe = fixture.societe("SOC-" + jeton);
        UUID autreSociete = fixture.societe("SOC2-" + jeton);
        UUID projet = fixture.projet("P-" + jeton, societe);
        UUID projetNonHabilite = fixture.projet("P2-" + jeton, societe);
        UUID projetAutreSociete = fixture.projet("P3-" + jeton, autreSociete);
        String nom = "Fournisseur " + jeton;
        UUID fournisseur = fixture.fournisseur("FOU-" + jeton, nom);

        for (Compte c : Compte.values()) {
            AdmUser compte = fixture.compte("ACH-" + c.name().substring(0, 3) + "-" + jeton, ROLE);
            fixture.habiliterSociete(compte, societe);
            fixture.habiliter(compte, projet);
            comptes.put(c, compte);
        }
        UUID personnelA = comptes.get(Compte.A_RESTREINT).getCfgEmployee().getId();
        UUID personnelB = comptes.get(Compte.B_RESTREINT).getCfgEmployee().getId();
        // A avec le droit porte le même personnel que A : seul le droit les distingue.
        fixture.poserSur("AdmUser", comptes.get(Compte.A_AVEC_DROIT).getId(), "CfgEmployeeId", personnelA);
        comptes.get(Compte.A_AVEC_DROIT).setCfgEmployee(comptes.get(Compte.A_RESTREINT).getCfgEmployee());
        fixture.droitOmran(comptes.get(Compte.A_AVEC_DROIT), FixtureAchat.DROIT_BC_SANS_RESTRICTION);
        fixture.sansPersonnel(comptes.get(Compte.SANS_PERSONNEL));

        piece(BC_A, FixtureAchat.BON_COMMANDE, fournisseur, nom, societe, projet, personnelA);
        piece(BC_B, FixtureAchat.BON_COMMANDE, fournisseur, nom, societe, projet, personnelB);
        piece(BC_SANS, FixtureAchat.BON_COMMANDE, fournisseur, nom, societe, projet, null);
        piece(FAC_B, FixtureAchat.FACTURE, fournisseur, nom, societe, projet, personnelB);
        piece(DEV_B, FixtureAchat.DEVIS, fournisseur, nom, societe, projet, personnelB);
        piece(BL_B, FixtureAchat.BON_LIVRAISON, fournisseur, nom, societe, projet, personnelB);
        piece(BC_A_HS, FixtureAchat.BON_COMMANDE, fournisseur, nom, autreSociete, projetAutreSociete, personnelA);
        piece(BC_A_PNH, FixtureAchat.BON_COMMANDE, fournisseur, nom, societe, projetNonHabilite, personnelA);
    }

    private void piece(String suffixe, String nature, UUID fournisseur, String nom, UUID societe, UUID projet,
                       UUID declencheur) {
        UUID id = fixture.piece(Piece.facture(jeton + suffixe, LocalDate.of(2026, 3, 10), fournisseur, nom, societe,
                projet, new BigDecimal("100")).nature(nature));
        if (declencheur != null) {
            fixture.poser(id, TRIGGER, declencheur);
        }
        pieces.put(suffixe, id);
        natures.put(suffixe, nature);
    }

    private RequestPostProcessor qui(Compte c) {
        return connecte(comptes.get(c), ROLE);
    }

    private String code(String suffixe) {
        return jeton + suffixe;
    }

    private MvcResult fiche(Compte c, String suffixe) throws Exception {
        return mockMvc.perform(get(AppelsAchat.DOCUMENT, pieces.get(suffixe)).with(qui(c))).andReturn();
    }

    private int statutFiche(Compte c, String suffixe) throws Exception {
        return fiche(c, suffixe).getResponse().getStatus();
    }

    /** Documents Achats : liste sans filtre de nature. */
    private List<String> documentsAchats(Compte c) throws Exception {
        return codes(corps(mockMvc, liste(qui(c))));
    }

    private List<String> listeDeNature(Compte c, String nature) throws Exception {
        return codes(corps(mockMvc, liste(qui(c), NATURES, nature)));
    }

    /** Fiche 404 « Pièce introuvable » (jamais 403 : la fiche ne confirme pas l'existence). */
    private void assertIntrouvable(Compte c, String suffixe) throws Exception {
        MvcResult r = fiche(c, suffixe);
        assertThat(r.getResponse().getStatus()).as("fiche %s pour %s", suffixe, c).isEqualTo(404);
        assertThat((Object) JsonPath.read(r.getResponse().getContentAsString(), "$.detail")).isEqualTo(INTROUVABLE);
    }

    // ------------------------------------------------------------------ compte restreint

    @Test
    void e1_compteRestreint_sonBc_visibleDansLesDeuxListes_etFiche200() throws Exception {
        assertAll(
                () -> assertThat(listeDeNature(Compte.A_RESTREINT, FixtureAchat.BON_COMMANDE)).contains(code(BC_A)),
                () -> assertThat(documentsAchats(Compte.A_RESTREINT)).contains(code(BC_A)),
                () -> assertThat(statutFiche(Compte.A_RESTREINT, BC_A)).isEqualTo(200));
    }

    @Test
    void e1_compteRestreint_bcDUnAutreDeclencheur_fiche404PieceIntrouvable_jamais403() throws Exception {
        assertIntrouvable(Compte.A_RESTREINT, BC_B);
    }

    @Test
    void e1_compteRestreint_bcSansDeclencheur_fiche404() throws Exception {
        assertIntrouvable(Compte.A_RESTREINT, BC_SANS);
    }

    @Test
    void e1_compteRestreintB_bcDeA_fiche404_sonBc200() throws Exception {
        assertIntrouvable(Compte.B_RESTREINT, BC_A);
        assertThat(statutFiche(Compte.B_RESTREINT, BC_B)).isEqualTo(200);
    }

    /** Q2 : la liste Documents Achats (toutes natures) restreint aussi les BC. */
    @Test
    void q2_documentsAchats_compteRestreint_bcDUnAutreAbsent_autresNaturesPresentes() throws Exception {
        assertThat(documentsAchats(Compte.A_RESTREINT))
                .containsExactlyInAnyOrder(code(BC_A), code(FAC_B), code(DEV_B), code(BL_B));
    }

    /** Q2 : Documents Achats avec plusieurs natures cochées dont ORDER : BC restreints, factures intactes. */
    @Test
    void q2_documentsAchats_naturesOrderEtInvoice_bcRestreints() throws Exception {
        String json = corps(mockMvc, liste(qui(Compte.A_RESTREINT), NATURES, FixtureAchat.BON_COMMANDE,
                NATURES, FixtureAchat.FACTURE));

        assertThat(codes(json)).containsExactlyInAnyOrder(code(BC_A), code(FAC_B));
    }

    @Test
    void e1_compteRestreint_listeDesBc_seulementLeSien() throws Exception {
        assertThat(listeDeNature(Compte.A_RESTREINT, FixtureAchat.BON_COMMANDE)).containsExactly(code(BC_A));
    }

    /** E1 « toute autre nature : inchangé » — devis, BL, facture d'un autre déclencheur restent visibles. */
    @Test
    void e1_compteRestreint_autresNatures_jamaisRestreintes() throws Exception {
        assertAll(
                () -> assertThat(statutFiche(Compte.A_RESTREINT, FAC_B)).isEqualTo(200),
                () -> assertThat(statutFiche(Compte.A_RESTREINT, DEV_B)).isEqualTo(200),
                () -> assertThat(statutFiche(Compte.A_RESTREINT, BL_B)).isEqualTo(200),
                () -> assertThat(listeDeNature(Compte.A_RESTREINT, FixtureAchat.FACTURE)).containsExactly(code(FAC_B)),
                () -> assertThat(listeDeNature(Compte.A_RESTREINT, FixtureAchat.DEVIS)).containsExactly(code(DEV_B)),
                () -> assertThat(listeDeNature(Compte.A_RESTREINT, FixtureAchat.BON_LIVRAISON))
                        .containsExactly(code(BL_B)));
    }

    // ------------------------------------------------------------------ comptes non restreints

    @Test
    void e1_droitWithNoRestriction_tousLesBc_fiche200_etDansLesDeuxListes() throws Exception {
        List<String> documents = documentsAchats(Compte.A_AVEC_DROIT);
        List<String> bc = listeDeNature(Compte.A_AVEC_DROIT, FixtureAchat.BON_COMMANDE);

        assertAll(
                () -> assertThat(bc).containsExactlyInAnyOrder(code(BC_A), code(BC_B), code(BC_SANS)),
                () -> assertThat(documents).contains(code(BC_A), code(BC_B), code(BC_SANS)),
                () -> assertThat(statutFiche(Compte.A_AVEC_DROIT, BC_B)).isEqualTo(200),
                () -> assertThat(statutFiche(Compte.A_AVEC_DROIT, BC_SANS)).isEqualTo(200));
    }

    @Test
    void e1_sansPersonnel_aucuneRestriction_tousLesBc_fiche200() throws Exception {
        List<String> documents = documentsAchats(Compte.SANS_PERSONNEL);

        assertAll(
                () -> assertThat(documents).containsExactlyInAnyOrder(code(BC_A), code(BC_B), code(BC_SANS),
                        code(FAC_B), code(DEV_B), code(BL_B)),
                () -> assertThat(statutFiche(Compte.SANS_PERSONNEL, BC_A)).isEqualTo(200),
                () -> assertThat(statutFiche(Compte.SANS_PERSONNEL, BC_B)).isEqualTo(200),
                () -> assertThat(statutFiche(Compte.SANS_PERSONNEL, BC_SANS)).isEqualTo(200));
    }

    // ------------------------------------------------------------------ périmètre société / projet (inchangé)

    @ParameterizedTest
    @EnumSource(Compte.class)
    void e1_horsSocieteHabilitee_404PourTousLesComptes_memeLeDeclencheur(Compte c) throws Exception {
        assertIntrouvable(c, BC_A_HS);
        assertThat(documentsAchats(c)).doesNotContain(code(BC_A_HS));
    }

    @ParameterizedTest
    @EnumSource(Compte.class)
    void e1_projetNonHabilite_404PourTousLesComptes_memeLeDeclencheur(Compte c) throws Exception {
        assertIntrouvable(c, BC_A_PNH);
        assertThat(documentsAchats(c)).doesNotContain(code(BC_A_PNH));
    }

    // ------------------------------------------------------------------ invariant (Vérification n° 1)

    /** Pour chaque pièce du jeu : fiche 200 ⇔ dans Documents Achats ⇔ dans la liste de sa nature ; sinon 404. */
    @ParameterizedTest
    @EnumSource(Compte.class)
    void e1_invariant_fiche200_ssiDansDocumentsAchatsEtDansLaListeDeSaNature(Compte c) throws Exception {
        List<String> documents = documentsAchats(c);
        Map<String, List<String>> parNature = new LinkedHashMap<>();
        for (String nature : Set.copyOf(natures.values())) {
            parNature.put(nature, listeDeNature(c, nature));
        }

        for (String suffixe : pieces.keySet()) {
            int statut = statutFiche(c, suffixe);
            boolean dansDocuments = documents.contains(code(suffixe));
            boolean dansSaNature = parNature.get(natures.get(suffixe)).contains(code(suffixe));
            assertAll(suffixe + " pour " + c,
                    () -> assertThat(statut).isIn(200, 404),
                    () -> assertThat(statut == 200).as("fiche 200 ⇔ Documents Achats").isEqualTo(dansDocuments),
                    () -> assertThat(statut == 200).as("fiche 200 ⇔ liste de sa nature").isEqualTo(dansSaNature));
        }
    }
}
