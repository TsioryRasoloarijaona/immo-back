package com.tsix.immocrm.controller.rapport;

import com.tsix.immocrm.entities.adm.AdmUser;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import javax.sql.DataSource;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.LockSupport;

import static com.tsix.immocrm.fixtures.AppelsImportPatrimoine.ARTICLES;
import static com.tsix.immocrm.fixtures.AppelsImportPatrimoine.MODELE;
import static com.tsix.immocrm.fixtures.AppelsImportPatrimoine.requete;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Mesure des lenteurs sur la copie locale yesRemsProd (FEATURE-lenteurs-rapports-import.md §2), à la demande
 * ({@code -Dmesure.prod=true}) : temps de réponse, ordres SQL, temps JDBC, pile échantillonnée. Simulation annulée.
 */
@EnabledIfSystemProperty(named = "mesure.prod", matches = "true")
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:sqlserver://localhost:49687;databaseName=yesRemsProd;encrypt=true;trustServerCertificate=true",
        "spring.jpa.show-sql=false", "spring.jpa.properties.hibernate.format_sql=false"})
@AutoConfigureMockMvc
class MesureLenteursTest {

    private static final UUID ADMIN = UUID.fromString("CF6383FD-3FA6-4C4B-9E5E-72C5EA793506");
    private static final UUID ARS = UUID.fromString("5EEE12F0-03EA-4008-9808-4109B6E242BF");
    private static final Path SORTIE = Path.of("target", "mesure-lenteurs");
    private static final Map<String, long[]> JDBC = new ConcurrentHashMap<>();

    @Autowired private MockMvc mockMvc;
    @Autowired private EntityManager em;

    private final List<String> journal = new ArrayList<>();

    @TestConfiguration
    static class Chrono {
        @Bean
        static BeanPostProcessor chronoJdbc() {
            return new BeanPostProcessor() {
                @Override
                public Object postProcessAfterInitialization(Object bean, String nom) {
                    return bean instanceof DataSource ds ? proxy(DataSource.class, ds) : bean;
                }
            };
        }
    }

    @Test
    void mesurer() throws Exception {
        RequestPostProcessor qui = connecte(em.find(AdmUser.class, ADMIN), "Administrateur");
        Files.createDirectories(SORTIE);
        for (int passe = 1; passe <= 2; passe++) {
            mesurer("L1-" + passe, get("/api/rapports/vente/ComTdbProspectionState/resultat").with(qui));
            mesurer("L2-" + passe, get("/api/rapports/vente/ComTdbCommercialState/resultat").with(qui));
        }
        byte[] modele = mockMvc.perform(get(MODELE, ARTICLES).param("trancheId", ARS.toString()).with(qui))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray();
        for (int passe = 1; passe <= 2; passe++) {
            mesurer("L3-" + passe, requete(ARTICLES, ARS, true, modele, qui));
        }
        Files.write(SORTIE.resolve("journal.txt"), journal, StandardCharsets.UTF_8);
        assertThat(journal).isNotEmpty();
    }

    private void mesurer(String nom, RequestBuilder requete) throws Exception {
        JDBC.clear();
        Map<String, Integer> echantillons = new ConcurrentHashMap<>();
        Thread cible = Thread.currentThread();
        Thread sondeur = Thread.ofPlatform().daemon().start(() -> sonder(cible, echantillons));
        long debut = System.nanoTime();
        MvcResult r = mockMvc.perform(requete).andReturn();
        long ms = (System.nanoTime() - debut) / 1_000_000;
        sondeur.interrupt();
        sondeur.join();
        echantillons.entrySet().stream().sorted((a, b) -> Integer.compare(b.getValue(), a.getValue())).limit(25)
                .forEach(e -> journal.add("   [%d éch.] %s".formatted(e.getValue(), e.getKey())));
        long ordres = JDBC.values().stream().mapToLong(v -> v[0]).sum();
        long jdbcMs = JDBC.values().stream().mapToLong(v -> v[1]).sum() / 1_000_000;
        journal.add("%s : %d ms, HTTP %d, %d ordres SQL, %d ms dans JDBC".formatted(nom, ms,
                r.getResponse().getStatus(), ordres, jdbcMs));
        JDBC.entrySet().stream().sorted((a, b) -> Long.compare(b.getValue()[1], a.getValue()[1])).limit(10)
                .forEach(e -> journal.add("   %d x, %d ms : %s".formatted(e.getValue()[0], e.getValue()[1] / 1_000_000,
                        e.getKey())));
        String corps = r.getResponse().getContentAsString(StandardCharsets.UTF_8);
        Files.writeString(SORTIE.resolve(nom + ".json"), corps);
        Path avant = Path.of("target", "mesure-avant", nom + ".json");
        if (Files.exists(avant)) {
            String sansDate = "\"genereLe\":\"[^\"]*\"";
            journal.add("   réponse identique à « avant » (hors genereLe) : "
                    + Files.readString(avant).replaceAll(sansDate, "").equals(corps.replaceAll(sansDate, "")));
        }
    }

    /** Échantillonneur de pile toutes les 10 ms : clé = 1re méthode applicative + 1re méthode hors applicatif au-dessus. */
    private static void sonder(Thread cible, Map<String, Integer> echantillons) {
        while (!Thread.currentThread().isInterrupted()) {
            StackTraceElement[] pile = cible.getStackTrace();
            String haut = pile.length > 0 ? pile[0].getClassName() + "." + pile[0].getMethodName() : "?";
            StringBuilder app = new StringBuilder();
            int n = 0;
            for (StackTraceElement f : pile) {
                if (f.getClassName().startsWith("com.tsix.immocrm") && !f.getClassName().contains("Mesure")
                        && !f.getClassName().contains("$$") && n < 3) {
                    app.append(n++ == 0 ? "" : " < ").append(f.getClassName().replace("com.tsix.immocrm.", ""))
                            .append('.').append(f.getMethodName()).append(':').append(f.getLineNumber());
                }
            }
            echantillons.merge(app + "  ||  " + haut, 1, Integer::sum);
            LockSupport.parkNanos(10_000_000);
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> type, T cible) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] {type}, (p, m, args) -> {
            try {
                Object resultat = m.invoke(cible, args);
                if (resultat instanceof PreparedStatement ps && args != null && args[0] instanceof String sql) {
                    return chronometre(ps, sql);
                }
                return resultat instanceof Connection c ? proxy(Connection.class, c) : resultat;
            } catch (InvocationTargetException e) {
                throw e.getCause();
            }
        });
    }

    private static PreparedStatement chronometre(PreparedStatement ps, String sql) {
        String forme = forme(sql);
        return (PreparedStatement) Proxy.newProxyInstance(PreparedStatement.class.getClassLoader(),
                new Class<?>[] {PreparedStatement.class}, (p, m, args) -> {
                    long debut = System.nanoTime();
                    try {
                        return m.invoke(ps, args);
                    } catch (InvocationTargetException e) {
                        throw e.getCause();
                    } finally {
                        if (m.getName().startsWith("execute")) {
                            long[] v = JDBC.computeIfAbsent(forme, k -> new long[2]);
                            v[0]++;
                            v[1] += System.nanoTime() - debut;
                        }
                    }
                });
    }

    private static String forme(String sql) {
        String s = sql.replaceAll("\\s+", " ").trim();
        int from = s.toLowerCase().indexOf(" from ");
        String debut = s.length() > 60 ? s.substring(0, 60) : s;
        return from < 0 ? debut : debut.substring(0, Math.min(20, debut.length())) + " …"
                + s.substring(from, Math.min(s.length(), from + 120));
    }
}
