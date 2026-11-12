package com.tsix.immocrm.controller.projet;

import com.tsix.immocrm.controller.projet.AppelsProjet.Reponse;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.fixtures.FixtureLotFiche;
import com.tsix.immocrm.fixtures.FixtureLotsParite;
import com.tsix.immocrm.fixtures.FixtureProjetLots;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static com.tsix.immocrm.fixtures.FixtureLotsParite.ENTREPRENEURS;
import static com.tsix.immocrm.fixtures.FixtureLotsParite.GENRES;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * E7, R13 (règle 6) — FEATURE-lot-fiche-parite.md : l'Administrateur suit OMRAN ({@code StrStatus.cs:GetNextWorkFlowList}). Statuts
 * A, B, C du genre Entrepreneurs ; T1 = A→B, T2 = A→C ; lot sur A. Le compte porte le rôle « Administrateur » (jeton et
 * {@code AdmUserRole}) mais restreint ({@code WithSequenceFreeRestriction} 1 ou NULL) ; il voit le statut A par son intervenant.
 * Aucun rôle exempté n'est créé dans la transaction hors des tests qui le demandent : le jeton « Administrateur » relit tous les
 * rôles de ce code ({@code IntervenantCourant.courant}).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class LotsTransitionAdministrateurTest {

    private static final String IDS = "$[*].id";
    private static final String ADMINISTRATEUR = "Administrateur";
    private static final String STATUT_DU_LOT = "SELECT COUNT(*) FROM PrjMarket WHERE PKey = ? AND StrStatusId = ?";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureLotsParite parite;
    @Autowired private FixtureLotFiche fiche;

    private LotsPariteSupport appels;
    private String jeton;
    private UUID projet;
    private UUID statutA;
    private UUID t1;
    private UUID t2;
    private UUID lot;

    @BeforeEach
    void setUp() {
        appels = new LotsPariteSupport(mockMvc);
        jeton = FixtureProjetLots.jeton();
        parite.genresDeLot();
        projet = parite.projet("TAP-" + jeton, parite.entiteJuridique("TAE-" + jeton));
        statutA = parite.statut(ENTREPRENEURS, "TAA-" + jeton);
        UUID statutB = parite.statut(ENTREPRENEURS, "TAB-" + jeton);
        UUID statutC = parite.statut(ENTREPRENEURS, "TAC-" + jeton);
        t1 = parite.transition(ENTREPRENEURS, statutA, statutB, "T1 " + jeton);
        t2 = parite.transition(ENTREPRENEURS, statutA, statutC, "T2 " + jeton);
        parite.transition(ENTREPRENEURS, statutB, statutC, "B>C " + jeton);
        lot = parite.lot(projet, "TAL-" + jeton, ENTREPRENEURS, statutA);
    }

    /** Administrateur restreint, intervenant de son rôle sur le statut A et, si non nulle, sur cette transition. */
    private AdmUser administrateur(String suffixe, boolean drapeauNul, UUID transition) {
        AdmUser compte = fiche.administrateurRestreint("lots.tra." + suffixe + "." + jeton, drapeauNul, projet);
        parite.intervenant(ENTREPRENEURS, parite.roleDe(compte), statutA, transition);
        return compte;
    }

    private static RequestPostProcessor enAdministrateur(AdmUser compte) {
        return connecte(compte, ADMINISTRATEUR);
    }

    private boolean surStatutA() {
        return parite.compter(STATUT_DU_LOT, lot.toString(), statutA.toString()) == 1;
    }

    // ------------------------------------------------------------------ GET : seulement ses transitions

    @ParameterizedTest(name = "drapeau NULL = {0}")
    @ValueSource(booleans = {false, true})
    void e7_administrateurIntervenantDeT1_getEgalT1(boolean drapeauNul) throws Exception {
        RequestPostProcessor qui = enAdministrateur(administrateur("t1", drapeauNul, t1));

        Reponse r = appels.transitions(qui, lot);

        assertThat(r.statut()).as(r.corps()).isEqualTo(200);
        assertThat(r.ids(IDS)).containsExactly(t1.toString());
    }

    @Test
    void e7_administrateurIntervenantDAucuneTransition_getVide() throws Exception {
        RequestPostProcessor qui = enAdministrateur(administrateur("aucune", false, null));

        Reponse r = appels.transitions(qui, lot);

        assertThat(r.statut()).as(r.corps()).isEqualTo(200);
        assertThat(r.ids(IDS)).isEmpty();
    }

    // ------------------------------------------------------------------ POST : refus de ce qui n'est pas proposé

    @Test
    void e7_administrateurIntervenantDeT1_postT2_409_statutInchange() throws Exception {
        RequestPostProcessor qui = enAdministrateur(administrateur("p2", false, t1));

        Reponse r = appels.executer(qui, lot, t2);

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(r.statut()).as(r.corps()).isEqualTo(409);
            s.assertThat(surStatutA()).as("lot resté sur A").isTrue();
        });
    }

    @Test
    void e7_administrateurIntervenantDAucuneTransition_postT1_409() throws Exception {
        RequestPostProcessor qui = enAdministrateur(administrateur("p0", false, null));

        Reponse r = appels.executer(qui, lot, t1);

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(r.statut()).as(r.corps()).isEqualTo(409);
            s.assertThat(surStatutA()).as("lot resté sur A").isTrue();
        });
    }

    @Test
    void e7_administrateurIntervenantDeT1_postT1_200() throws Exception {
        RequestPostProcessor qui = enAdministrateur(administrateur("p1", false, t1));

        Reponse r = appels.executer(qui, lot, t1);

        assertThat(r.statut()).as(r.corps()).isEqualTo(200);
    }

    // ------------------------------------------------------------------ R13 : proposé = exécutable, dans les deux sens

    @Test
    void r13_administrateurRestreint_proposeesExecutables_nonProposeesRefusees() throws Exception {
        RequestPostProcessor qui = enAdministrateur(administrateur("r13", false, t1));
        List<String> proposees = appels.transitions(qui, lot).ids(IDS);

        SoftAssertions s = new SoftAssertions();
        for (UUID t : List.of(t1, t2)) {
            parite.statutDuLot(lot, statutA);
            int attendu = proposees.contains(t.toString()) ? 200 : 409;
            Reponse r = appels.executer(qui, lot, t);
            s.assertThat(r.statut()).as(t + " proposée=" + proposees.contains(t.toString()) + " : " + r.corps())
                    .isEqualTo(attendu);
        }
        s.assertThat(proposees).as("proposées").containsExactly(t1.toString());
        s.assertAll();
    }

    @Test
    void r13_intervenantParLeCompteLuiMeme_proposeeEtExecutable() throws Exception {
        AdmUser compte = administrateur("moi", false, null);
        fiche.intervenantCompte(GENRES.get(ENTREPRENEURS), compte, t2);
        RequestPostProcessor qui = enAdministrateur(compte);

        Reponse get = appels.transitions(qui, lot);
        Reponse refus = appels.executer(qui, lot, t1);
        Reponse post = appels.executer(qui, lot, t2);

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(get.ids(IDS)).containsExactly(t2.toString());
            s.assertThat(refus.statut()).as(refus.corps()).isEqualTo(409);
            s.assertThat(post.statut()).as(post.corps()).isEqualTo(200);
        });
    }

    // ------------------------------------------------------------------ rôle exempté : toutes les sortantes

    @Test
    void e7_administrateurAvecUnRoleWithSequenceFreeRestriction0_toutesLesSortantes() throws Exception {
        AdmUser compte = administrateur("libre", false, t1);
        fiche.roleLibreEnPlus(compte, "LIBRE-TRA-" + jeton);
        RequestPostProcessor qui = enAdministrateur(compte);

        Reponse get = appels.transitions(qui, lot);
        Reponse post = appels.executer(qui, lot, t2);

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(get.statut()).as(get.corps()).isEqualTo(200);
            s.assertThat(get.ids(IDS)).containsExactlyInAnyOrder(t1.toString(), t2.toString());
            s.assertThat(post.statut()).as(post.corps()).isEqualTo(200);
        });
    }

    // ------------------------------------------------------------------ le drapeau Administrateur n'entre plus dans le calcul

    @Test
    void e7_administrateurEtCompteNonAdministrateur_memeConfiguration_memesTransitions() throws Exception {
        RequestPostProcessor admin = enAdministrateur(administrateur("cmpa", false, t1));
        AdmUser commercial = parite.restreint("lots.tra.cmpc." + jeton, projet);
        parite.intervenant(ENTREPRENEURS, parite.roleDe(commercial), statutA, t1);
        RequestPostProcessor qCommercial = connecte(commercial, "Commercial");

        List<String> deLAdministrateur = appels.transitions(admin, lot).ids(IDS);
        List<String> duCommercial = appels.transitions(qCommercial, lot).ids(IDS);

        assertThat(deLAdministrateur).isNotEmpty().containsExactlyInAnyOrderElementsOf(duCommercial);
    }
}
