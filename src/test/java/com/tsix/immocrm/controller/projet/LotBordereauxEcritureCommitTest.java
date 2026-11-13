package com.tsix.immocrm.controller.projet;

import com.tsix.immocrm.config.CollecteurLignesEcrites;
import com.tsix.immocrm.config.EcrituresValidees;
import com.tsix.immocrm.config.LigneEcrite;
import com.tsix.immocrm.controller.projet.AppelsProjet.Reponse;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.fixtures.FixtureBordereauxEcriture;
import com.tsix.immocrm.fixtures.FixtureProjetLots;
import jakarta.persistence.EntityManagerFactory;
import org.assertj.core.api.SoftAssertions;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.EventType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.tsix.immocrm.controller.projet.BordereauxEcritureSupport.DEFINITIF;
import static com.tsix.immocrm.controller.projet.BordereauxEcritureSupport.ESTIMATIF;
import static com.tsix.immocrm.controller.projet.BordereauxEcritureSupport.bordereau;
import static com.tsix.immocrm.controller.projet.BordereauxEcritureSupport.groupe;
import static com.tsix.immocrm.controller.projet.BordereauxEcritureSupport.ligne;
import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static com.tsix.immocrm.fixtures.FixtureLotsParite.ENTREPRENEURS;
import static com.tsix.immocrm.fixtures.FixtureLotsParite.GENRES;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Écritures des bordereaux validées pour de bon (FEATURE-lot-ecritures.md §3 Q2, R9, R14, §9) : un refus 409 n'écrit rien
 * (transaction unique), un enregistrement accepté laisse une ligne d'historique {@code AdmWebHistorics}. Sans
 * {@code @Transactional} : tout ce qui est validé est effacé en {@code @AfterEach}.
 */
@SpringBootTest
@AutoConfigureMockMvc
@RecordApplicationEvents
class LotBordereauxEcritureCommitTest {

    private static final Logger log = LoggerFactory.getLogger(LotBordereauxEcritureCommitTest.class);
    private static final String QUANTITE_LIGNE = "SELECT Quantity FROM PrjMarketLine WHERE PKey = ?";
    private static final String PAR_PKEY = " WHERE PKey = ?";

    @Autowired private MockMvc mockMvc;
    @Autowired private DataSource dataSource;
    @Autowired private ApplicationEvents evenements;

    /** Le collecteur de la remontée n'est branché qu'en deux bases : le brancher ici, contexte propre à cette classe. */
    @TestConfiguration
    static class AvecCollecteur {
        AvecCollecteur(EntityManagerFactory fabrique, ApplicationEventPublisher publication) {
            CollecteurLignesEcrites collecteur = new CollecteurLignesEcrites(publication);
            EventListenerRegistry registre = fabrique.unwrap(SessionFactoryImplementor.class).getEventListenerRegistry();
            registre.appendListeners(EventType.POST_INSERT, collecteur);
            registre.appendListeners(EventType.POST_DELETE, collecteur);
        }
    }

    private final String jeton = FixtureProjetLots.jeton();
    private final List<Runnable> nettoyages = new ArrayList<>();
    private final List<UUID> idsEcrits = new ArrayList<>();

    private JdbcTemplate jdbc;
    private BordereauxEcritureSupport appels;
    private RequestPostProcessor admin;
    private UUID projet;
    private UUID lot;
    private UUID categorie;

    @BeforeEach
    void setUp() {
        jdbc = new JdbcTemplate(dataSource);
        appels = new BordereauxEcritureSupport(mockMvc);
        jdbc.execute(FixtureBordereauxEcriture.TABLE_VENTILATION);
        UUID genre = GENRES.get(ENTREPRENEURS);
        siAbsent("StrEntity", genre, "INSERT INTO StrEntity (PKey, Code, Description) VALUES (?, N'PrjMarket', N'Lot Entrepreneurs')");
        UUID statut = inserer("StrStatus", """
                INSERT INTO StrStatus (PKey, Code, Description, Sorting, StrConfigEntityId, BoolField004)
                VALUES (?, ?, ?, 1, ?, 1)""", "BCS-" + jeton, "BCS-" + jeton, genre.toString());
        projet = inserer("PrjProject", "INSERT INTO PrjProject (PKey, Code, Description) VALUES (?, ?, ?)",
                "BCP-" + jeton, "BCP-" + jeton);
        categorie = inserer("StlCategory", "INSERT INTO StlCategory (PKey, Code, Description, IsCredit) VALUES (?, ?, ?, 0)",
                "BCC-" + jeton, "BCC-" + jeton);
        lot = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO PrjMarket (PKey, Code, Description, PrjProjectId, StrEntityId, StrStatusId, WarrantyRatio,
                    EstimatedPriceHTManual, EstimatedVatAmountManual, EstimatedPriceTTCManual, IsMultiSupplier)
                VALUES (?, ?, ?, ?, ?, ?, 0.1, 0, 0, 0, 0)""",
                lot.toString(), "BCL-" + jeton, "Lot bordereaux " + jeton, projet.toString(), genre.toString(), statut.toString());
        admin = connecte(administrateur());
    }

    private UUID inserer(String table, String sql, Object... autres) {
        UUID id = UUID.randomUUID();
        Object[] params = new Object[autres.length + 1];
        params[0] = id.toString();
        System.arraycopy(autres, 0, params, 1, autres.length);
        jdbc.update(sql, params);
        nettoyages.add(0, () -> effacer("DELETE FROM " + table + PAR_PKEY, id));
        return id;
    }

    private void siAbsent(String table, UUID id, String insertion) {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM " + table + PAR_PKEY, Integer.class, id.toString());
        if (n != null && n == 0) {
            jdbc.update(insertion, id.toString());
            nettoyages.add(0, () -> effacer("DELETE FROM " + table + PAR_PKEY, id));
        }
    }

    private AdmUser administrateur() {
        AdmUser compte = new AdmUser();
        compte.setId(UUID.randomUUID());
        compte.setCode("lots.bc." + jeton);
        jdbc.update("INSERT INTO AdmUser (PKey, Code, Description, Disabled) VALUES (?, ?, ?, 0)",
                compte.getId().toString(), compte.getCode(), "Compte bordereaux");
        nettoyages.add(0, () -> effacer("DELETE FROM AdmUser WHERE PKey = ?", compte.getId()));
        UUID role = inserer("AdmRole", """
                INSERT INTO AdmRole (PKey, Code, Description, WithSequenceFreeRestriction) VALUES (?, N'Administrateur', ?, 0)""",
                "Administrateur bordereaux " + jeton);
        inserer("AdmUserRole", "INSERT INTO AdmUserRole (PKey, AdmUserId, AdmRoleId) VALUES (?, ?, ?)",
                compte.getId().toString(), role.toString());
        inserer("AdmUserHabilitation", "INSERT INTO AdmUserHabilitation (PKey, AdmUserId, PrjProjectId) VALUES (?, ?, ?)",
                compte.getId().toString(), projet.toString());
        return compte;
    }

    private UUID ligneDefinitive(UUID groupe, int ordre, String code, String quantite) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO PrjMarketLine (PKey, Code, Description, Sorting, Unity, Quantity, UnitPriceHT, VatRatio, UnitPriceTTC,
                    FODECRatio, IsStock, PrjMarketId, PrjMarketLineGroupingId, StlCategoryId)
                VALUES (?, ?, ?, ?, N'U', ?, 100, 0.2, 120, 0, 0, ?, ?, ?)""",
                id.toString(), code, "Ligne " + code, ordre, new BigDecimal(quantite), lot.toString(),
                groupe == null ? null : groupe.toString(), categorie.toString());
        idsEcrits.add(id);
        return id;
    }

    private UUID decompter(UUID ligne) {
        UUID decompte = UUID.randomUUID();
        jdbc.update("INSERT INTO PrjWorkingOut (PKey, Code, Sorting, PrjMarketId) VALUES (?, ?, 1, ?)",
                decompte.toString(), "BCW-" + jeton, lot.toString());
        jdbc.update("""
                INSERT INTO PrjWorkingOutLine (Pkey, Sorting, Code, Quantity, UnitPriceHT, PrjWorkingOutId, PrjMarketLineId)
                VALUES (?, 1, N'1', 1, 100, ?, ?)""", UUID.randomUUID().toString(), decompte.toString(), ligne.toString());
        return decompte;
    }

    @AfterEach
    void nettoyer() {
        String l = lot.toString();
        idsEcrits.forEach(id -> effacer("DELETE FROM AdmWebHistorics WHERE ObjectId = ? OR ParentObjectId = ?", id, id));
        effacer("DELETE FROM AdmWebHistorics WHERE ObjectId = ? OR ParentObjectId = ?", l, l);
        effacer("DELETE FROM PrjWorkingOutLine WHERE PrjWorkingOutId IN (SELECT PKey FROM PrjWorkingOut WHERE PrjMarketId = ?)", l);
        effacer("DELETE FROM PrjWorkingOut WHERE PrjMarketId = ?", l);
        effacer("DELETE FROM PrjMarketVentilationCategory WHERE PrjMarketId = ?", l);
        effacer("DELETE FROM PrjMarketLine WHERE PrjMarketId = ?", l);
        effacer("DELETE FROM PrjMarketLineEstimation WHERE PrjMarketId = ?", l);
        effacer("DELETE FROM PrjMarketLineGrouping WHERE PrjMarketId = ?", l);
        effacer("DELETE FROM PrjMarketLineGroupingEstimation WHERE PrjMarketId = ?", l);
        effacer("DELETE FROM StrHistory WHERE ObjectId = ?", l);
        effacer("DELETE FROM PrjMarket WHERE PKey = ?", l);
        nettoyages.forEach(Runnable::run);
    }

    /** Nettoyage au mieux : une table annexe (FK en CI) ne doit pas faire échouer un test déjà jugé. */
    private void effacer(String sql, Object... parametres) {
        Object[] textes = new Object[parametres.length];
        for (int i = 0; i < parametres.length; i++) {
            textes[i] = parametres[i].toString();
        }
        try {
            jdbc.update(sql, textes);
        } catch (DataAccessException e) {
            log.warn("Nettoyage incomplet ({}) : {}", sql, e.getMessage());
        }
    }

    private int compter(String sql, Object... parametres) {
        Integer n = jdbc.queryForObject(sql, Integer.class, parametres);
        return n == null ? 0 : n;
    }

    // R9 + Q2 : 409 sur la ligne décomptée supprimée ; la modification et l'insertion du même envoi sont annulées.
    @Test
    void r9_suppressionDecomptee_409_rienNEstEcrit_relectureIdentique() throws Exception {
        UUID g = UUID.randomUUID();
        jdbc.update("INSERT INTO PrjMarketLineGrouping (PKey, Code, Description, Sorting, PrjMarketId, IsStock) VALUES (?, N'1', N'G', 1, ?, 0)",
                g.toString(), lot.toString());
        UUID decomptee = ligneDefinitive(g, 1, "1.1", "2");
        UUID modifiee = ligneDefinitive(g, 2, "1.2", "4");
        decompter(decomptee);
        UUID nouvelle = UUID.randomUUID();
        idsEcrits.add(nouvelle);
        Reponse avant = appels.lire(admin, lot, DEFINITIF);

        Reponse r = appels.ecrire(admin, lot, DEFINITIF, bordereau(List.of(groupe(g, null, 1, "1")), List.of(
                ligne(modifiee, g, 2, "1.2", "9", "100", categorie),
                ligne(nouvelle, g, 3, "1.3", "1", "100", categorie))));

        Reponse apres = appels.lire(admin, lot, DEFINITIF);
        SoftAssertions.assertSoftly(s -> {
            s.assertThat(r.statut()).as(r.corps()).isEqualTo(409);
            s.assertThat(jdbc.queryForObject(QUANTITE_LIGNE, BigDecimal.class, modifiee.toString()))
                    .as("modification annulée").isEqualByComparingTo("4");
            s.assertThat(compter("SELECT COUNT(*) FROM PrjMarketLine WHERE PKey = ?", nouvelle.toString()))
                    .as("insertion annulée").isZero();
            s.assertThat(compter("SELECT COUNT(*) FROM PrjMarketLine WHERE PKey = ?", decomptee.toString())).isEqualTo(1);
            s.assertThat(compter("SELECT COUNT(*) FROM PrjMarketVentilationCategory WHERE PrjMarketId = ?", lot.toString()))
                    .as("ventilation non écrite").isZero();
            s.assertThat(apres.corps()).as("relecture identique").isEqualTo(avant.corps());
        });
    }

    // R14 : une ligne d'historique Arpent par enregistrement accepté.
    @Test
    void r14_enregistrementAccepte_ligneDHistoriqueDuLot() throws Exception {
        UUID nouvelle = UUID.randomUUID();
        idsEcrits.add(nouvelle);

        Reponse r = appels.ecrire(admin, lot, ESTIMATIF,
                bordereau(List.of(), List.of(ligne(nouvelle, null, 1, "1", "1", "100", categorie))));

        assertThat(r.statut()).as(r.corps()).isEqualTo(200);
        assertThat(compter("SELECT COUNT(*) FROM AdmWebHistorics WHERE ObjectId = ? OR ParentObjectId = ?",
                lot.toString(), lot.toString())).isPositive();
    }

    // R12 + remontée : chaque ligne de ventilation remplacée est publiée, sa relecture absente l'efface de la copie.
    @Test
    void r12_ventilationReecrite_ancienneLignePublieePourEtreEffaceeDeLaCopie() throws Exception {
        UUID ancienne = UUID.randomUUID();
        jdbc.update("INSERT INTO PrjMarketVentilationCategory (PKey, StlCategoryId, PrjMarketId, VentilationRate) VALUES (?, ?, ?, 1)",
                ancienne.toString(), categorie.toString(), lot.toString());
        UUID nouvelle = UUID.randomUUID();
        idsEcrits.add(nouvelle);

        Reponse r = appels.ecrire(admin, lot, ESTIMATIF,
                bordereau(List.of(), List.of(ligne(nouvelle, null, 1, "1", "1", "100", categorie))));

        List<UUID> enBase = jdbc.queryForList("SELECT PKey FROM PrjMarketVentilationCategory WHERE PrjMarketId = ?",
                String.class, lot.toString()).stream().map(UUID::fromString).toList();
        List<UUID> publiees = evenements.stream(EcrituresValidees.class).flatMap(e -> e.lignes().stream())
                .filter(l -> "PrjMarketVentilationCategory".equals(l.table())).map(LigneEcrite::pkey).toList();
        SoftAssertions.assertSoftly(s -> {
            s.assertThat(r.statut()).as(r.corps()).isEqualTo(200);
            s.assertThat(enBase).hasSize(1).doesNotContain(ancienne);
            s.assertThat(publiees).as("lignes remontées dans la copie").contains(ancienne).containsAll(enBase);
        });
    }
}
