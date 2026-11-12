package com.tsix.immocrm.controller.achat;

import com.tsix.immocrm.fixtures.FixtureConfigurationAchat;
import com.tsix.immocrm.fixtures.FixtureConfigurationAchat.Statut;
import com.tsix.immocrm.fixtures.FixturePatrimoine;
import com.tsix.immocrm.fixtures.FixtureStatutsAchat;
import jakarta.persistence.EntityManager;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static com.tsix.immocrm.controller.achat.LectureConfigurationAchat.STATUTS;
import static com.tsix.immocrm.controller.achat.LectureConfigurationAchat.TRANSITIONS;
import static com.tsix.immocrm.controller.achat.LectureConfigurationAchat.ligne;
import static com.tsix.immocrm.controller.achat.LectureConfigurationAchat.lire;
import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

/**
 * §4.1 « 3 requêtes au plus par appel […] jamais une par ligne » : le nombre d'ordres SQL de GET /api/achat/config/transitions
 * est le même pour 2 et pour 50 transitions (rôles et passages compris). Ordres comptés sur les connexions du DataSource.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ConfigurationAchatRequetesTest {

    private static final AtomicInteger ORDRES = new AtomicInteger();
    private static final Set<String> CREATION_ORDRE = Set.of("prepareStatement", "prepareCall", "createStatement");

    @TestConfiguration
    static class Compteur {
        @Bean
        static BeanPostProcessor compteurOrdresSql() {
            return new BeanPostProcessor() {
                @Override
                public Object postProcessAfterInitialization(Object bean, String nom) {
                    return bean instanceof DataSource ds ? proxy(DataSource.class, ds) : bean;
                }
            };
        }
    }

    @Autowired private MockMvc mockMvc;
    @Autowired private EntityManager em;
    @Autowired private FixtureConfigurationAchat fixture;
    @Autowired private FixturePatrimoine patrimoine;
    @Autowired private FixtureStatutsAchat statutsAchat;

    @Test
    void transitions_memeNombreDOrdresSql_pour2Et50Transitions() throws Exception {
        String jeton = FixtureConfigurationAchat.jeton("A4Q");
        RequestPostProcessor qui = connecte(patrimoine.administrateur());
        UUID depart = fixture.statut(Statut.simple("D-" + jeton, 1));
        UUID arrivee = fixture.statut(Statut.simple("A-" + jeton, 2));
        UUID gestion = fixture.gestion("G-" + jeton, "Gestion " + jeton);
        UUID piece = fixture.piece(arrivee, gestion);
        List<UUID> ids = new ArrayList<>();
        ajouter(ids, 2, jeton, gestion, depart, arrivee, piece);
        lire(mockMvc, TRANSITIONS, qui);

        int pour2 = compter(qui, ids);
        ajouter(ids, 48, jeton, gestion, depart, arrivee, piece);
        int pour50 = compter(qui, ids);

        assertAll(
                () -> assertThat(ids).hasSize(50),
                () -> assertThat(pour2).isPositive(),
                () -> assertThat(pour50).isEqualTo(pour2));
    }

    /**
     * FEATURE-statuts-achat-parite.md règle 13 : GET /statuts (types, image, documents à joindre, comptes de pièces,
     * transitions, historiques) émet autant d'ordres SQL pour 2 que pour 30 statuts.
     */
    @Test
    void r13_statuts_memeNombreDOrdresSql_pour2Et30Statuts() throws Exception {
        String jeton = FixtureConfigurationAchat.jeton("K2Q");
        RequestPostProcessor qui = connecte(patrimoine.administrateur());
        statutsAchat.tables();
        UUID typeEtat = fixture.typeEtat("Intermédiaire " + jeton);
        UUID image = statutsAchat.image("IMG-" + jeton, FixtureStatutsAchat.PNG);
        UUID typeDocument = statutsAchat.typeDocument("Contrat " + jeton);
        UUID cible = fixture.statut(Statut.simple("CIB-" + jeton, 0));
        List<UUID> ids = new ArrayList<>();
        ajouterStatuts(ids, 2, jeton, typeEtat, image, typeDocument, cible);
        lire(mockMvc, STATUTS, qui);

        int pour2 = compterStatuts(qui, ids);
        ajouterStatuts(ids, 28, jeton, typeEtat, image, typeDocument, cible);
        int pour30 = compterStatuts(qui, ids);

        assertAll(
                () -> assertThat(ids).hasSize(30),
                () -> assertThat(pour2).isPositive(),
                () -> assertThat(pour30).isEqualTo(pour2));
    }

    /** {@code n} statuts, chacun avec image, type, un document à joindre, une pièce, une transition et un passage. */
    private void ajouterStatuts(List<UUID> ids, int n, String jeton, UUID typeEtat, UUID image, UUID typeDocument,
                                UUID cible) {
        for (int i = 0; i < n; i++) {
            int rang = ids.size();
            UUID s = fixture.statut(new Statut("S" + rang + "-" + jeton, "Statut " + rang, rang, typeEtat, true, false,
                    false, "Pink", 0));
            em.flush();
            em.createNativeQuery("UPDATE BuyDocumentStatus SET CfgImageId = :image WHERE PKey = :id")
                    .setParameter("image", image).setParameter("id", s).executeUpdate();
            statutsAchat.documentAJoindre(s, typeDocument, true, false);
            UUID piece = fixture.piece(s, null);
            fixture.transition(null, rang, "T" + rang + " " + jeton, s, cible);
            fixture.passage(piece, null, s, LocalDateTime.of(2022, 1, 1, 8, 0).plusDays(rang));
            ids.add(s);
        }
    }

    /** Ordres SQL d'un appel ; chaque statut créé est rendu avec son document à joindre. */
    private int compterStatuts(RequestPostProcessor qui, List<UUID> ids) throws Exception {
        em.flush();
        ORDRES.set(0);
        List<Map<String, Object>> lignes = lire(mockMvc, STATUTS, qui);
        int n = ORDRES.get();
        assertThat(ids).isNotEmpty().allSatisfy(id -> assertThat(ligne(lignes, id)).isNotNull()
                .extractingByKey("documentsAJoindre").asInstanceOf(InstanceOfAssertFactories.LIST).hasSize(1));
        return n;
    }

    /** {@code n} transitions, chacune avec deux rôles et un passage. */
    private void ajouter(List<UUID> ids, int n, String jeton, UUID gestion, UUID depart, UUID arrivee, UUID piece) {
        for (int i = 0; i < n; i++) {
            int rang = ids.size();
            UUID t = fixture.transition(gestion, rang, "T" + rang + " " + jeton, depart, arrivee);
            fixture.lier(t, fixture.role("R" + rang + "a-" + jeton));
            fixture.lier(t, fixture.role("R" + rang + "b-" + jeton));
            fixture.passage(piece, t, arrivee, LocalDateTime.of(2022, 1, 1, 8, 0).plusDays(rang));
            ids.add(t);
        }
    }

    /** Ordres SQL d'un appel ; la réponse contient bien toutes les transitions créées. */
    private int compter(RequestPostProcessor qui, List<UUID> ids) throws Exception {
        em.flush();
        ORDRES.set(0);
        List<Map<String, Object>> lignes = lire(mockMvc, TRANSITIONS, qui);
        int n = ORDRES.get();
        assertThat(ids).isNotEmpty().allSatisfy(id -> assertThat(ligne(lignes, id)).isNotNull());
        return n;
    }

    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> type, T cible) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] {type}, (p, m, args) -> {
            if (CREATION_ORDRE.contains(m.getName())) {
                ORDRES.incrementAndGet();
            }
            try {
                Object resultat = m.invoke(cible, args);
                return resultat instanceof Connection c ? proxy(Connection.class, c) : resultat;
            } catch (InvocationTargetException e) {
                throw e.getCause();
            }
        });
    }
}
