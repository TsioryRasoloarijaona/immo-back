package com.tsix.immocrm.controller.tresorerie;

import com.tsix.immocrm.controller.tresorerie.AppelsTresorerie.Reponse;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.fixtures.FixtureDecaissements;
import com.tsix.immocrm.fixtures.FixtureDecaissements.Drapeaux;
import com.tsix.immocrm.fixtures.FixtureDecaissements.Saisie;
import com.tsix.immocrm.fixtures.FixtureTresorerie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.tsix.immocrm.controller.tresorerie.AppelsSaisieDecaissement.corps;
import static com.tsix.immocrm.fixtures.FixtureDecaissements.texte;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * FEATURE-n3bis-saisie-reglements.md : {@code StlSettlementHistory} — une ligne à la création, une de plus à chaque
 * changement d'échéance, de type ou de référence, aucune pour les notes seules. L'ordre n'est jamais supposé (DocDate
 * peut être égal entre deux lignes).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class DecaissementHistoriqueTest {

    private static final String ECHEANCE = "echeance";
    private static final String REFERENCE = "reference";
    private static final String TYPE = "type";
    private static final String DEBUT_ECHEANCE = "2078-12-31";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureDecaissements fixture;

    private AppelsSaisieDecaissement appels;
    private Saisie s;
    private AdmUser compte;
    private RequestPostProcessor qui;
    private Map<String, Object> corps;
    private UUID id;

    @BeforeEach
    void setUp() throws Exception {
        appels = new AppelsSaisieDecaissement(mockMvc);
        s = fixture.saisie("DHI");
        compte = fixture.compte(s, "DHIA", FixtureDecaissements.DROIT_LIRE, FixtureDecaissements.DROIT_NOUVEAU,
                FixtureDecaissements.DROIT_SANS_AFFECTATION);
        qui = connecte(compte, FixtureTresorerie.ROLE);
        corps = corps(s, s.code("-R1"), "500", List.of());
        id = appels.creerOk(qui, corps);
    }

    private List<Map<String, Object>> apresModification(String champ, Object valeur) throws Exception {
        corps.put(champ, valeur);
        Reponse r = appels.modifier(qui, id, corps);
        assertThat(r.statut()).as(r.corps()).isEqualTo(200);
        return fixture.historique(id);
    }

    private static String echeance(Map<String, Object> ligne) {
        return String.valueOf(ligne.get(ECHEANCE));
    }

    @Test
    void creation_uneLigne_utilisateurTypeReferenceEcheance() {
        List<Map<String, Object>> historique = fixture.historique(id);

        assertThat(historique).hasSize(1);
        assertThat(historique.get(0))
                .containsEntry("utilisateur", texte(compte.getId()))
                .containsEntry(TYPE, texte(s.c().type()))
                .containsEntry(REFERENCE, s.code("-R1"));
        assertThat(echeance(historique.get(0))).startsWith(DEBUT_ECHEANCE);
    }

    @Test
    void changementDEcheance_deuxLignes() throws Exception {
        List<Map<String, Object>> historique = apresModification("dateEcheance", LocalDate.of(2079, 1, 15));

        assertThat(historique).hasSize(2)
                .anySatisfy(l -> assertThat(echeance(l)).startsWith(DEBUT_ECHEANCE))
                .anySatisfy(l -> assertThat(echeance(l)).startsWith("2079-01-15"));
    }

    @Test
    void changementDeType_deuxLignes() throws Exception {
        UUID autreType = fixture.type("T2-" + s.c().jeton(), new Drapeaux(false, false, false, false));

        List<Map<String, Object>> historique = apresModification("typeId", autreType);

        assertThat(historique).hasSize(2)
                .anySatisfy(l -> assertThat(l).containsEntry(TYPE, texte(s.c().type())))
                .anySatisfy(l -> assertThat(l).containsEntry(TYPE, texte(autreType)));
    }

    @Test
    void changementDeReference_deuxLignes() throws Exception {
        List<Map<String, Object>> historique = apresModification(REFERENCE, s.code("-R2"));

        assertThat(historique).hasSize(2)
                .anySatisfy(l -> assertThat(l).containsEntry(REFERENCE, s.code("-R1")))
                .anySatisfy(l -> assertThat(l).containsEntry(REFERENCE, s.code("-R2")));
    }

    @Test
    void notesSeules_resteUneLigne() throws Exception {
        List<Map<String, Object>> historique = apresModification("notes", "Note de test");

        assertThat(historique).hasSize(1);
    }
}
