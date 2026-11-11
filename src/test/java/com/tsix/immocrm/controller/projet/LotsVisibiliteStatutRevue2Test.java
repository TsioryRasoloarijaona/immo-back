package com.tsix.immocrm.controller.projet;

import com.tsix.immocrm.controller.projet.AppelsProjet.Reponse;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.fixtures.FixtureProjetLots;
import com.tsix.immocrm.fixtures.FixtureVisibiliteOmran;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static com.tsix.immocrm.controller.projet.AppelsProjet.LOTS;
import static com.tsix.immocrm.controller.projet.AppelsProjet.TAILLE;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * 2ᵉ revue V1 (FEATURE-visibilite-omran.md R1, « Après les tests ») : drapeau {@code WithSequenceFreeRestriction} NULL =
 * restreint ; plusieurs rôles dont un exempté = exempté (OMRAN {@code AdmUser.LoadUserRights}).
 * Projet A : L1 (statut S1 ← rôle R), L2 (statut S2, aucun intervenant).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class LotsVisibiliteStatutRevue2Test {

    private static final String CONTENU_IDS = "$.content[*].id";
    private static final String SIZE = "size";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureProjetLots fixture;
    @Autowired private FixtureVisibiliteOmran visibilite;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private EntityManager em;

    private AppelsProjet appels;
    private String jeton;
    private UUID projetA;
    private UUID lot1;
    private UUID lot2;

    @BeforeEach
    void setUp() {
        appels = new AppelsProjet(mockMvc);
        jeton = FixtureProjetLots.jeton();
        UUID fournisseur = fixture.fournisseur("F-" + jeton);
        projetA = fixture.projet("PA-" + jeton);
        UUID entite = visibilite.entite("LOTR2-" + jeton);
        UUID s1 = visibilite.statut(entite, "S1-" + jeton);
        UUID s2 = visibilite.statut(entite, "S2-" + jeton);
        visibilite.intervenantRole(entite, s1, visibilite.role("R-" + jeton));
        lot1 = fixture.lot(projetA, "L1-" + jeton, fournisseur);
        visibilite.statutLot(lot1, entite, s1);
        lot2 = fixture.lot(projetA, "L2-" + jeton, fournisseur);
        visibilite.statutLot(lot2, entite, s2);
    }

    @Test
    void drapeauNull_restreint_aucunLot() throws Exception {
        String role = "RNUL-" + jeton;
        AdmUser compte = visibilite.compte("lot.nul." + jeton, role, true);
        em.flush();
        jdbc.update("UPDATE AdmRole SET WithSequenceFreeRestriction = NULL WHERE Code = ?", role);
        fixture.habiliter(compte, projetA);

        Reponse r = appels.lire(connecte(compte, role), LOTS, SIZE, TAILLE);

        assertThat(r.statut()).isEqualTo(200);
        assertThat(r.nombre("$.totalElements")).isEqualByComparingTo("0");
    }

    @Test
    void deuxRoles_unExempte_voitTousLesLots() throws Exception {
        String restreint = "RRES-" + jeton;
        String libre = "RLIB-" + jeton;
        AdmUser compte = visibilite.compte("lot.deux." + jeton, restreint, true);
        visibilite.compte("lot.porteur." + jeton, libre, false);
        fixture.habiliter(compte, projetA);

        Reponse r = appels.lire(connecte(compte, restreint, libre), LOTS, SIZE, TAILLE);

        assertThat(r.statut()).isEqualTo(200);
        assertThat(r.ids(CONTENU_IDS)).containsExactlyInAnyOrder(lot1.toString(), lot2.toString());
    }
}
