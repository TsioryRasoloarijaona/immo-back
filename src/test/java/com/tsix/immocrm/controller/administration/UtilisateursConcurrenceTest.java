package com.tsix.immocrm.controller.administration;

import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.fixtures.FixtureUtilisateurs;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static com.tsix.immocrm.controller.administration.AppelsUtilisateurs.statut;
import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static com.tsix.immocrm.fixtures.CorpsJson.corps;
import static com.tsix.immocrm.fixtures.FixtureUtilisateurs.CODE;
import static com.tsix.immocrm.fixtures.FixtureUtilisateurs.DESCRIPTION;
import static com.tsix.immocrm.fixtures.FixtureUtilisateurs.INACTIF;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * R3, R8 et R10 sous concurrence, avec de vrais commits et de vraies connexions. Un test qui rejoue une course compte exactement
 * un succès : sans cela il serait vert quand la route n'existe pas.
 */
@SpringBootTest
@AutoConfigureMockMvc
class UtilisateursConcurrenceTest {

    private static final int TOURS = 5;
    private static final int REPONSE_MAX_SECONDES = 30;
    private static final long ATTENTE_DU_VERROU_MS = 1500;

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureUtilisateurs fixture;
    @Autowired private DataSource dataSource;

    private String jeton;
    private AppelsUtilisateurs appels;

    @BeforeEach
    void setUp() {
        jeton = fixture.preparer();
        appels = new AppelsUtilisateurs(mockMvc, connecte(fixture.compte()));
    }

    @AfterEach
    void nettoyer() {
        fixture.nettoyer();
    }

    /** Lance les appels au même instant, rend leurs statuts. */
    private static List<Integer> enParallele(List<Callable<Integer>> taches) throws Exception {
        CyclicBarrier depart = new CyclicBarrier(taches.size());
        try (ExecutorService executeur = Executors.newFixedThreadPool(taches.size())) {
            List<Future<Integer>> resultats = new ArrayList<>();
            for (Callable<Integer> tache : taches) {
                resultats.add(executeur.submit(() -> {
                    depart.await();
                    return tache.call();
                }));
            }
            List<Integer> statuts = new ArrayList<>();
            for (Future<Integer> resultat : resultats) {
                statuts.add(resultat.get(REPONSE_MAX_SECONDES, TimeUnit.SECONDS));
            }
            return statuts;
        }
    }

    private static long succes(List<Integer> statuts) {
        return statuts.stream().filter(s -> s >= 200 && s < 300).count();
    }

    private static long conflits(List<Integer> statuts) {
        return statuts.stream().filter(s -> s == 409).count();
    }

    // ------------------------------------------------------------------ R3

    @Test
    void r3_creationsConcurrentes_unSeulCompteNait() throws Exception {
        String code = "Course." + jeton;
        List<String> variantes = List.of(code, code.toLowerCase(Locale.ROOT), code.toUpperCase(Locale.ROOT), code, "COURSE." + jeton,
                code.toLowerCase(Locale.ROOT));
        List<Callable<Integer>> taches = new ArrayList<>();
        variantes.forEach(v -> taches.add(() -> statut(appels.creer(corps().avec(CODE, v).avec(DESCRIPTION, "Course")))));

        List<Integer> statuts = enParallele(taches);

        assertThat(statuts).containsOnlyOnce(201);
        assertThat(conflits(statuts)).isEqualTo(variantes.size() - 1L);
        assertThat(fixture.nombreComptes(code)).isEqualTo(1);
    }

    // ------------------------------------------------------------------ R8

    private void uneCourse(int tour, Callable<Integer> premier, Callable<Integer> second, AdmUser a, AdmUser b) throws Exception {
        List<Integer> statuts = enParallele(List.of(premier, second));

        assertThat(succes(statuts)).as("tour %d : %s", tour, statuts).isEqualTo(1);
        assertThat(conflits(statuts)).as("tour %d : %s", tour, statuts).isEqualTo(1);
        assertThat(fixture.administrateursActifs()).as("tour %d", tour).isEqualTo(1);

        fixture.desactiver(a.getId());
        fixture.desactiver(b.getId());
    }

    @Test
    void r8_deuxRetraitsSimultanes_unAdministrateurReste() throws Exception {
        fixture.suspendreLesAdministrateurs();

        for (int tour = 0; tour < TOURS; tour++) {
            AdmUser a = fixture.administrateurActif();
            AdmUser b = fixture.administrateurActif();

            uneCourse(tour, () -> statut(appels.roles(a.getId())), () -> statut(appels.roles(b.getId())), a, b);
        }
    }

    @Test
    void r8_deuxDesactivationsSimultanees_unAdministrateurReste() throws Exception {
        fixture.suspendreLesAdministrateurs();

        for (int tour = 0; tour < TOURS; tour++) {
            AdmUser a = fixture.administrateurActif();
            AdmUser b = fixture.administrateurActif();

            uneCourse(tour, () -> statut(appels.modifier(a.getId(), corps().avec(INACTIF, true))),
                    () -> statut(appels.modifier(b.getId(), corps().avec(INACTIF, true))), a, b);
        }
    }

    @Test
    void r8_retraitEtDesactivationSimultanes_unAdministrateurReste() throws Exception {
        fixture.suspendreLesAdministrateurs();

        for (int tour = 0; tour < TOURS; tour++) {
            AdmUser a = fixture.administrateurActif();
            AdmUser b = fixture.administrateurActif();

            uneCourse(tour, () -> statut(appels.roles(a.getId())),
                    () -> statut(appels.modifier(b.getId(), corps().avec(INACTIF, true))), a, b);
        }
    }

    // ------------------------------------------------------------------ R10

    /** La connexion joue le rôle de {@code RolesService.supprimer} : verrou d'écriture tenu sur la ligne du rôle. */
    private Connection verrouiller(UUID role) throws SQLException {
        Connection connexion = dataSource.getConnection();
        connexion.setAutoCommit(false);
        try (PreparedStatement verrou = connexion.prepareStatement("SELECT PKey FROM AdmRole WITH (UPDLOCK, ROWLOCK) WHERE PKey = ?")) {
            verrou.setString(1, role.toString());
            verrou.executeQuery().close();
        }
        return connexion;
    }

    private static void supprimer(Connection connexion, UUID role) throws SQLException {
        try (PreparedStatement suppression = connexion.prepareStatement("DELETE FROM AdmRole WHERE PKey = ?")) {
            suppression.setString(1, role.toString());
            suppression.executeUpdate();
        }
    }

    @Test
    void r10_put_attendLaSuppressionEnCours_puis400_sansLigne() throws Exception {
        UUID cible = fixture.compte().getId();
        UUID role = fixture.role("SUP-" + jeton);

        try (ExecutorService executeur = Executors.newSingleThreadExecutor(); Connection suppression = verrouiller(role)) {
            Future<Integer> put = executeur.submit(() -> statut(appels.roles(cible, role)));

            assertThatThrownBy(() -> put.get(ATTENTE_DU_VERROU_MS, TimeUnit.MILLISECONDS)).isInstanceOf(TimeoutException.class);

            supprimer(suppression, role);
            suppression.commit();

            assertThat(put.get(REPONSE_MAX_SECONDES, TimeUnit.SECONDS)).isEqualTo(400);
        }
        assertThat(fixture.rolesDe(cible)).isEmpty();
    }

    /** Témoin : la suppression annulée, le PUT reprend et écrit. */
    @Test
    void r10_suppressionAnnulee_putAboutit() throws Exception {
        UUID cible = fixture.compte().getId();
        UUID role = fixture.role("SUP-" + jeton);

        try (ExecutorService executeur = Executors.newSingleThreadExecutor(); Connection suppression = verrouiller(role)) {
            Future<Integer> put = executeur.submit(() -> statut(appels.roles(cible, role)));

            assertThatThrownBy(() -> put.get(ATTENTE_DU_VERROU_MS, TimeUnit.MILLISECONDS)).isInstanceOf(TimeoutException.class);

            suppression.rollback();

            assertThat(put.get(REPONSE_MAX_SECONDES, TimeUnit.SECONDS)).isBetween(200, 299);
        }
        assertThat(fixture.rolesDe(cible)).containsExactly(role);
    }

    /** Verrous pris en ordre d'id : deux PUT aux listes inversées ne s'interbloquent pas (une victime répondrait 500). */
    @Test
    void r10_listesInversees_pasDInterblocage() throws Exception {
        UUID premier = fixture.role("ORD1-" + jeton);
        UUID second = fixture.role("ORD2-" + jeton);

        for (int tour = 0; tour < TOURS * 2; tour++) {
            UUID a = fixture.compte().getId();
            UUID b = fixture.compte().getId();

            List<Integer> statuts = enParallele(List.of(() -> statut(appels.roles(a, premier, second)),
                    () -> statut(appels.roles(b, second, premier))));

            assertThat(succes(statuts)).as("tour %d : %s", tour, statuts).isEqualTo(2);
        }
    }
}
