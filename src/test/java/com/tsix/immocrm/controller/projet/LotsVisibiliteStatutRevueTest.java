package com.tsix.immocrm.controller.projet;

import com.tsix.immocrm.controller.projet.AppelsProjet.Reponse;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.fixtures.FixtureProjetLots;
import com.tsix.immocrm.fixtures.FixtureVisibiliteOmran;
import jakarta.persistence.EntityManager;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static com.tsix.immocrm.controller.projet.AppelsProjet.LOTS;
import static com.tsix.immocrm.controller.projet.AppelsProjet.REFERENTIELS;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Revue du lot V1 (FEATURE-visibilite-omran.md R1, R7, « Après les tests ») : chemins non couverts par
 * {@code LotsVisibiliteStatutTest}. Projet A : L1 (S1 ← rôle RU, catégorie C1), L2 (S2 ← rôle RX, catégorie C2).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class LotsVisibiliteStatutRevueTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureProjetLots fixture;
    @Autowired private FixtureVisibiliteOmran visibilite;
    @Autowired private EntityManager em;
    @Autowired private JdbcTemplate jdbc;

    private AppelsProjet appels;
    private String jeton;
    private UUID projetA;
    private UUID lot1;
    private UUID lot2;
    private UUID categorie1;
    private UUID categorie2;
    private RequestPostProcessor u;

    @BeforeEach
    void setUp() {
        appels = new AppelsProjet(mockMvc);
        jeton = FixtureProjetLots.jeton();
        UUID fournisseur = fixture.fournisseur("F-" + jeton);
        projetA = fixture.projet("PA-" + jeton);
        String roleU = "RU-" + jeton;
        AdmUser compteU = visibilite.compte("rev.u." + jeton, roleU, true);
        UUID entite = visibilite.entite("LOTR-" + jeton);
        UUID s1 = visibilite.statut(entite, "S1-" + jeton);
        UUID s2 = visibilite.statut(entite, "S2-" + jeton);
        visibilite.intervenantRole(entite, s1, visibilite.idRole(roleU));
        visibilite.intervenantRole(entite, s2, visibilite.role("RX-" + jeton));
        lot1 = fixture.lot(projetA, "L1-" + jeton, fournisseur);
        lot2 = fixture.lot(projetA, "L2-" + jeton, fournisseur);
        visibilite.statutLot(lot1, entite, s1);
        visibilite.statutLot(lot2, entite, s2);
        categorie1 = categorie(lot1, "C1-" + jeton);
        categorie2 = categorie(lot2, "C2-" + jeton);
        fixture.habiliter(compteU, projetA);
        u = connecte(compteU, roleU);
    }

    private UUID categorie(UUID lot, String code) {
        UUID id = UUID.randomUUID();
        em.flush();
        jdbc.update("INSERT INTO StlCategory (PKey, Code, Description, IsCredit, Sorting) VALUES (?, ?, ?, 0, 1)",
                id.toString(), code, code);
        jdbc.update("UPDATE PrjMarket SET StlCategoryId = ? WHERE PKey = ?", id.toString(), lot.toString());
        return id;
    }

    @Test
    void sousRoutesDUnLotAuStatutInvisible_404CommeLaFiche() throws Exception {
        Reponse bordereauVisible = appels.lire(u, LOTS + "/" + lot1 + "/bordereau");
        Reponse decomptesVisible = appels.lire(u, LOTS + "/" + lot1 + "/decomptes");
        Reponse bordereauCache = appels.lire(u, LOTS + "/" + lot2 + "/bordereau");
        Reponse decomptesCache = appels.lire(u, LOTS + "/" + lot2 + "/decomptes");

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(bordereauVisible.statut()).as("témoin bordereau").isEqualTo(200);
            s.assertThat(decomptesVisible.statut()).as("témoin décomptes").isEqualTo(200);
            s.assertThat(bordereauCache.statut()).isEqualTo(404);
            s.assertThat(decomptesCache.statut()).isEqualTo(404);
        });
    }

    @Test
    void referentielCategories_seulementCellesDesLotsVisibles() throws Exception {
        Reponse r = appels.lire(u, REFERENTIELS);

        assertThat(r.ids("$.categories[*].id")).contains(categorie1.toString()).doesNotContain(categorie2.toString());
    }

    @Test
    void referentielProjets_compteExempte_projetSansStatutPresentEtTousSesLotsComptes() throws Exception {
        UUID projetSansStatut = fixture.projet("PN-" + jeton);
        String role = "LIB-" + jeton;
        AdmUser libre = visibilite.compte("rev.lib." + jeton, role, false);
        fixture.habiliter(libre, projetA);
        fixture.habiliter(libre, projetSansStatut);

        Reponse r = appels.lire(connecte(libre, role), REFERENTIELS);

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(r.ids("$.projets[*].id")).containsExactlyInAnyOrder(projetA.toString(),
                    projetSansStatut.toString());
            s.assertThat(String.valueOf(r.valeur("$.projets[?(@.id == '" + projetA + "')].nbLots"))).isEqualTo("[2]");
        });
    }
}
