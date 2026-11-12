package com.tsix.immocrm.controller.achat;

import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.fixtures.FixtureAchat;
import com.tsix.immocrm.fixtures.FixtureAchat.Cadre;
import com.tsix.immocrm.fixtures.FixtureConfigurationAchat;
import com.tsix.immocrm.fixtures.FixtureConfigurationAchat.Statut;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;

import static com.tsix.immocrm.controller.achat.AppelsEcrituresAchat.BC_INTROUVABLE;
import static com.tsix.immocrm.controller.achat.AppelsEcrituresAchat.INTROUVABLE;
import static com.tsix.immocrm.controller.achat.AppelsEcrituresAchat.MODIF_FACTURE;
import static com.tsix.immocrm.controller.achat.AppelsEcrituresAchat.ROLE_JWT;
import static com.tsix.immocrm.controller.achat.AppelsEcrituresAchat.SUPPR_BC;
import static com.tsix.immocrm.controller.achat.AppelsEcrituresAchat.affecter;
import static com.tsix.immocrm.controller.achat.AppelsEcrituresAchat.appeler;
import static com.tsix.immocrm.controller.achat.AppelsEcrituresAchat.corps;
import static com.tsix.immocrm.controller.achat.AppelsEcrituresAchat.detail;
import static com.tsix.immocrm.controller.achat.AppelsEcrituresAchat.statut;
import static com.tsix.immocrm.controller.achat.AppelsEcrituresAchat.supprimer;
import static com.tsix.immocrm.controller.achat.AppelsEcrituresAchat.transition;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

/**
 * A2b-1, Vérification n° 2 (règle 6, visibilité des écritures) : pièce hors société / hors projet habilité, BC d'un autre
 * déclencheur pour un compte restreint → 404 (jamais 403 / 409) sur {@code PATCH statut}, {@code DELETE},
 * {@code PUT bon-commande} ; avec {@code WithNoRestriction} ou sans personnel → l'écriture passe.
 * <pre>
 *   gestion G : S0 → S1, liée au rôle de chaque compte « à droits » ; droits : suppression BC, modification facture
 *   A_RESTREINT (personnel A) · A_AVEC_DROIT (personnel A + WithNoRestriction) · SANS_PERSONNEL · SANS_DROIT (personnel C, rien)
 *   BC_B : déclencheur = personnel de B (autre compte) ; FAC : facture de S / P
 * </pre>
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PieceAchatVisibiliteEcrituresTest {

    private static final String TRIGGER = "CfgTierTriggerId";

    enum Compte { A_RESTREINT, A_AVEC_DROIT, SANS_PERSONNEL, SANS_DROIT }

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureAchat fixture;
    @Autowired private FixtureConfigurationAchat config;

    private Cadre c;
    private Cadre horsSociete;
    private Cadre projetNonHabilite;
    private UUID gestion;
    private UUID s0;
    private UUID s1;
    private UUID personnelA;
    private UUID personnelB;
    private final Map<Compte, AdmUser> comptes = new EnumMap<>(Compte.class);
    private int rang;

    @BeforeEach
    void setUp() {
        c = fixture.cadre("AVE");
        UUID autreSociete = fixture.societe("SOC2-" + c.jeton());
        horsSociete = new Cadre(c.jeton(), autreSociete, fixture.projet("P3-" + c.jeton(), autreSociete), c.fournisseur(),
                c.fournisseurNom());
        projetNonHabilite = new Cadre(c.jeton(), c.societe(), fixture.projet("P2-" + c.jeton(), c.societe()),
                c.fournisseur(), c.fournisseurNom());
        fixture.tablesComptables();
        fixture.parametreBooleen(FixtureAchat.LIMITE_QUANTITES, true);
        gestion = config.gestion("G-" + c.jeton(), "Gestion " + c.jeton());
        s0 = config.statut(Statut.simple("S0-" + c.jeton(), 1));
        s1 = config.statut(Statut.simple("S1-" + c.jeton(), 2));
        UUID t = config.transition(gestion, 1, "Valider", s0, s1);

        for (Compte k : Compte.values()) {
            AdmUser compte = k == Compte.SANS_DROIT
                    ? fixture.compteHabilite(c, "AVE-" + k.ordinal(), ROLE_JWT)
                    : fixture.compteHabilite(c, "AVE-" + k.ordinal(), ROLE_JWT, SUPPR_BC, MODIF_FACTURE);
            if (k != Compte.SANS_DROIT) {
                config.lier(t, fixture.roleDe(compte));
            }
            comptes.put(k, compte);
        }
        personnelA = comptes.get(Compte.A_RESTREINT).getCfgEmployee().getId();
        personnelB = fixture.compteHabilite(c, "AVE-B", ROLE_JWT).getCfgEmployee().getId();
        fixture.poserSur("AdmUser", comptes.get(Compte.A_AVEC_DROIT).getId(), "CfgEmployeeId", personnelA);
        comptes.get(Compte.A_AVEC_DROIT).setCfgEmployee(comptes.get(Compte.A_RESTREINT).getCfgEmployee());
        fixture.droitOmran(comptes.get(Compte.A_AVEC_DROIT), FixtureAchat.DROIT_BC_SANS_RESTRICTION);
        fixture.sansPersonnel(comptes.get(Compte.SANS_PERSONNEL));
    }

    /** BC de la gestion G au statut S0. */
    private UUID bc(Cadre cadre, UUID declencheur) {
        UUID id = fixture.piece(cadre, "-BC" + (rang++), FixtureAchat.BON_COMMANDE);
        fixture.poser(id, "StrEntityId", gestion);
        fixture.poser(id, "BuyDocumentStatusId", s0);
        fixture.poser(id, TRIGGER, declencheur);
        return id;
    }

    private UUID facture(Cadre cadre) {
        return fixture.piece(cadre, "-FAC" + (rang++), FixtureAchat.FACTURE);
    }

    private MvcResult par(Compte k, MockHttpServletRequestBuilder requete) throws Exception {
        return appeler(mockMvc, requete, connecte(comptes.get(k), ROLE_JWT));
    }

    private void assert404(MvcResult r, String message) throws Exception {
        assertThat(statut(r)).as(corps(r)).isEqualTo(404);
        assertThat(detail(r)).isEqualTo(message);
    }

    // ------------------------------------------------------------------ BC d'un autre déclencheur, compte restreint

    @Test
    void v2_compteRestreint_bcDUnAutreDeclencheur_patch404() throws Exception {
        UUID bcB = bc(c, personnelB);

        assert404(par(Compte.A_RESTREINT, transition(bcB, s1, null)), INTROUVABLE);
        fixture.oublier();
        assertThat(fixture.lienDe(bcB, "BuyDocumentStatusId")).isEqualTo(AppelsEcrituresAchat.id(s0));
    }

    @Test
    void v2_compteRestreint_bcDUnAutreDeclencheur_delete404_pieceIntacte() throws Exception {
        UUID bcB = bc(c, personnelB);

        assert404(par(Compte.A_RESTREINT, supprimer(bcB)), INTROUVABLE);
        fixture.oublier();
        assertThat(fixture.nombre("BuyDocument", "PKey", bcB)).isEqualTo(1);
    }

    @Test
    void v2_compteRestreint_affecterAUnBcDUnAutreDeclencheur_404BonDeCommandeIntrouvable() throws Exception {
        UUID bcB = bc(c, personnelB);

        assert404(par(Compte.A_RESTREINT, affecter(facture(c), bcB)), BC_INTROUVABLE);
    }

    /** Témoin : le compte restreint écrit sur son propre BC. */
    @Test
    void v2_compteRestreint_sonBc_patch200_delete204() {
        UUID bcA = bc(c, personnelA);
        UUID autreBcA = bc(c, personnelA);

        assertAll(
                () -> assertThat(statut(par(Compte.A_RESTREINT, transition(bcA, s1, null)))).isEqualTo(200),
                () -> assertThat(statut(par(Compte.A_RESTREINT, supprimer(autreBcA)))).isEqualTo(204));
    }

    // ------------------------------------------------------------------ comptes non restreints : l'écriture passe

    @ParameterizedTest
    @EnumSource(value = Compte.class, names = {"A_AVEC_DROIT", "SANS_PERSONNEL"})
    void v2_nonRestreint_bcDUnAutreDeclencheur_patch200(Compte k) throws Exception {
        MvcResult r = par(k, transition(bc(c, personnelB), s1, null));

        assertThat(statut(r)).as(corps(r)).isEqualTo(200);
    }

    @ParameterizedTest
    @EnumSource(value = Compte.class, names = {"A_AVEC_DROIT", "SANS_PERSONNEL"})
    void v2_nonRestreint_bcDUnAutreDeclencheur_delete204(Compte k) throws Exception {
        MvcResult r = par(k, supprimer(bc(c, personnelB)));

        assertThat(statut(r)).as(corps(r)).isEqualTo(204);
    }

    @ParameterizedTest
    @EnumSource(value = Compte.class, names = {"A_AVEC_DROIT", "SANS_PERSONNEL"})
    void v2_nonRestreint_affecterAUnBcDUnAutreDeclencheur_200(Compte k) throws Exception {
        MvcResult r = par(k, affecter(facture(c), bc(c, personnelB)));

        assertThat(statut(r)).as(corps(r)).isEqualTo(200);
    }

    // ------------------------------------------------------------------ hors société / projet non habilité : 404 pour tous

    @ParameterizedTest
    @EnumSource(Compte.class)
    void v2_horsSociete_patchEtDelete404_memeDeclencheur(Compte k) {
        UUID bcHs = bc(horsSociete, personnelA);

        assertAll(
                () -> assert404(par(k, transition(bcHs, s1, null)), INTROUVABLE),
                () -> assert404(par(k, supprimer(bcHs)), INTROUVABLE));
    }

    @ParameterizedTest
    @EnumSource(Compte.class)
    void v2_projetNonHabilite_patchEtDelete404_memeDeclencheur(Compte k) {
        UUID bcPnh = bc(projetNonHabilite, personnelA);

        assertAll(
                () -> assert404(par(k, transition(bcPnh, s1, null)), INTROUVABLE),
                () -> assert404(par(k, supprimer(bcPnh)), INTROUVABLE));
    }

    @ParameterizedTest
    @EnumSource(Compte.class)
    void v2_factureHorsPerimetre_affecter404PieceIntrouvable(Compte k) {
        UUID bcA = bc(c, personnelA);

        assertAll(
                () -> assert404(par(k, affecter(facture(horsSociete), bcA)), INTROUVABLE),
                () -> assert404(par(k, affecter(facture(projetNonHabilite), bcA)), INTROUVABLE));
    }

    @ParameterizedTest
    @EnumSource(value = Compte.class, names = {"A_RESTREINT", "A_AVEC_DROIT", "SANS_PERSONNEL"})
    void v2_bcHorsPerimetre_affecter404BonDeCommandeIntrouvable(Compte k) {
        UUID facture = facture(c);

        assertAll(
                () -> assert404(par(k, affecter(facture, bc(horsSociete, personnelA))), BC_INTROUVABLE),
                () -> assert404(par(k, affecter(facture, bc(projetNonHabilite, personnelA))), BC_INTROUVABLE));
    }

    // ------------------------------------------------------------------ 404 avant 403 / 409

    /** Sans droit ni rôle : une pièce invisible répond 404, jamais 403 (droit) ni 409 (transition, nature). */
    @Test
    void v2_compteSansDroit_pieceInvisible_404JamaisRefusMetier() {
        UUID bcB = bc(c, personnelB);
        UUID bcHs = bc(horsSociete, personnelA);

        assertAll(
                () -> assert404(par(Compte.SANS_DROIT, supprimer(bcB)), INTROUVABLE),
                () -> assert404(par(Compte.SANS_DROIT, transition(bcB, s1, null)), INTROUVABLE),
                () -> assert404(par(Compte.SANS_DROIT, transition(bcB, s0, null)), INTROUVABLE),
                () -> assert404(par(Compte.SANS_DROIT, affecter(bcB, bcB)), INTROUVABLE),
                () -> assert404(par(Compte.SANS_DROIT, supprimer(bcHs)), INTROUVABLE));
    }

    /** Témoins du compte sans droit sur une pièce visible : 403 (suppression), 403 (transition sans rôle). */
    @Test
    void v2_compteSansDroit_pieceVisible_refus403() {
        UUID facture = facture(c);
        fixture.poser(facture, "StrEntityId", gestion);
        fixture.poser(facture, "BuyDocumentStatusId", s0);

        assertAll(
                () -> assertThat(statut(par(Compte.SANS_DROIT, supprimer(facture)))).isEqualTo(403),
                () -> assertThat(statut(par(Compte.SANS_DROIT, transition(facture, s1, null)))).isEqualTo(403));
    }
}
