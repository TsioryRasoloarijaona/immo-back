package com.tsix.immocrm.controller.projet;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.fixtures.CorpsJson;
import com.tsix.immocrm.fixtures.FixtureDossierVente;
import com.tsix.immocrm.fixtures.FixturePatrimoine;
import com.tsix.immocrm.fixtures.FixtureProjetConfig;
import com.tsix.immocrm.fixtures.FixtureReferentielsAdmin;
import jakarta.persistence.Tuple;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static com.tsix.immocrm.fixtures.CorpsJson.corps;
import static com.tsix.immocrm.fixtures.FixturePatrimoine.guid;
import static com.tsix.immocrm.fixtures.FixtureProjetConfig.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Configuration Projet — FEATURE-projet-config.md §2 (règles 1 à 6), §3, §7 (tests 1 à 6), corrigés par §8 et les
 * « Réponses du manager » (statut et entité posés par le service, 13 statuts de lot, 22 tables bloquantes).
 * Rollback de la création : {@link ProjetConfigTransactionTest} ; types de lot (moteur) : {@code ReferentielTypesLotTest}.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ProjetConfigTest {

    private static final String CODE_DEJA_UTILISE = "Code déjà utilisé";
    private static final String UTILISE_PAR = "Utilisé par";
    private static final String COMPTE_PROJET = "SELECT COUNT(*) FROM PrjProject WHERE PKey = :id";
    private static final String COMPTE_HABILITATIONS = "SELECT COUNT(*) FROM AdmUserHabilitation WHERE PrjProjectId = :id";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixturePatrimoine patrimoine;
    @Autowired private FixtureProjetConfig fixture;

    private AdmUser admin;
    private AdmUser commercial;
    private String jeton;
    private UUID societe;

    @BeforeEach
    void setUp() {
        admin = patrimoine.administrateur();
        commercial = patrimoine.administrateur();
        jeton = FixtureReferentielsAdmin.jeton();
        societe = patrimoine.entite("SOC-" + jeton, true).getId();
        fixture.parametrage();
    }

    // ------------------------------------------------------------------ appels

    private static RequestPostProcessor enCommercial(AdmUser compte) {
        return FixtureDossierVente.connecte(compte, "Commercial");
    }

    private ResultActions appeler(MockHttpServletRequestBuilder requete, RequestPostProcessor qui) throws Exception {
        return mockMvc.perform(requete.with(qui));
    }

    private ResultActions ecrire(MockHttpServletRequestBuilder requete, CorpsJson corps, RequestPostProcessor qui)
            throws Exception {
        return mockMvc.perform(requete.with(qui).contentType(MediaType.APPLICATION_JSON).content(corps.json()));
    }

    private CorpsJson corpsProjet(String code) {
        return corps().avec(CODE, code).avec(DESCRIPTION, "Projet " + code).avec(SOCIETE_ID, societe);
    }

    private UUID creerProjet(String code) throws Exception {
        String json = ecrire(post(PROJETS), corpsProjet(code), connecte(admin))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return UUID.fromString(JsonPath.read(json, "$.id"));
    }

    private UUID projetEnBase(String code) {
        return fixture.projet(code, societe);
    }

    // ------------------------------------------------------------------ règle 1 : habilitation du créateur (§7.1)

    @Test
    void r1_creation_201_habiliteLeCreateurSurLeNouveauProjet() throws Exception {
        UUID id = creerProjet("PC-" + jeton);

        List<Tuple> habilitations = patrimoine.habilitations(admin.getId());
        assertThat(habilitations).extracting(t -> guid(t.get("PrjProjectId"))).containsExactly(guid(id));
    }

    @Test
    void r1_creation_habilitationSansAutrePortee() throws Exception {
        UUID id = creerProjet("PH-" + jeton);

        assertThat(fixture.compter(COMPTE_HABILITATIONS + " AND CfgCompanyId IS NULL AND CfgProjectId IS NULL"
                + " AND CfgTrancheId IS NULL", id)).isEqualTo(1);
    }

    // ------------------------------------------------------------------ règle 6 + §8.1 : ce que pose le service

    @Test
    void r6_creation_ligneEcrite_societeSaisie_entiteProjetEtStatutNouveauPoses() throws Exception {
        UUID id = creerProjet("PE-" + jeton);

        assertThat(fixture.compter(COMPTE_PROJET + " AND CfgCompanyId = '" + societe + "' AND StrEntityId = '"
                + ENTITE_PROJET + "' AND StrStatusId = '" + STATUT_NOUVEAU + "'", id)).isEqualTo(1);
    }

    @Test
    void r6_creation_defautsVentilationEtPartiesCommunes() throws Exception {
        UUID id = creerProjet("PD-" + jeton);

        assertThat(fixture.compter(COMPTE_PROJET
                + " AND IsVentilated = 0 AND defaultVentilationRate = 0 AND IsACommon = 0", id)).isEqualTo(1);
    }

    @Test
    void r6_creation_auditRempliAuNomDuCreateur() throws Exception {
        UUID id = creerProjet("PA-" + jeton);

        assertThat(fixture.compter(COMPTE_PROJET + " AND UserCreate = N'" + FixturePatrimoine.AUTEUR
                + "' AND UserCreateDateTime IS NOT NULL", id)).isEqualTo(1);
    }

    // ------------------------------------------------------------------ règles 2 / 3 : accès (§7.2)

    enum Refus { LISTE_PROJETS, LIRE_PROJET, CREER_PROJET, MODIFIER_PROJET, SUPPRIMER_PROJET, REFERENTIELS }

    private MockHttpServletRequestBuilder requete(Refus refus, UUID projet) {
        return switch (refus) {
            case LISTE_PROJETS -> get(PROJETS);
            case LIRE_PROJET -> get(PROJET, projet);
            case CREER_PROJET -> avecCorps(post(PROJETS), corpsProjet("RF-" + jeton));
            case MODIFIER_PROJET -> avecCorps(put(PROJET, projet), corpsProjet("RF-" + jeton));
            case SUPPRIMER_PROJET -> delete(PROJET, projet);
            case REFERENTIELS -> get(REFERENTIELS);
        };
    }

    private static MockHttpServletRequestBuilder avecCorps(MockHttpServletRequestBuilder requete, CorpsJson corps) {
        return requete.contentType(MediaType.APPLICATION_JSON).content(corps.json());
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(Refus.class)
    void r2_r3_nonAdministrateur_403(Refus refus) throws Exception {
        UUID projet = projetEnBase("PR-" + jeton);

        appeler(requete(refus, projet), enCommercial(commercial)).andExpect(status().isForbidden());
    }

    @Test
    void r2_suppressionRefuseeAuNonAdministrateur_projetEtHabilitationIntacts() throws Exception {
        UUID projet = projetEnBase("PI-" + jeton);
        fixture.habilitation(admin.getId(), projet);

        appeler(delete(PROJET, projet), enCommercial(commercial)).andExpect(status().isForbidden());

        assertThat(new int[]{fixture.compter(COMPTE_PROJET, projet), fixture.compter(COMPTE_HABILITATIONS, projet)})
                .containsExactly(1, 1);
    }

    @Test
    void r2_creationRefuseeAuNonAdministrateur_aucuneHabilitation() throws Exception {
        ecrire(post(PROJETS), corpsProjet("PN-" + jeton), enCommercial(commercial)).andExpect(status().isForbidden());

        assertThat(patrimoine.habilitations(commercial.getId())).isEmpty();
    }

    @Test
    void r3_statutsLot_200_pourToutAuthentifie_genresDeLotSeulement() throws Exception {
        UUID entrepreneurs = fixture.statut(LOT_ENTREPRENEURS, "STE-" + jeton);
        UUID maitresOuvrage = fixture.statut(LOT_MAITRES_OUVRAGE, "STM-" + jeton);

        appeler(get(STATUTS_LOT), enCommercial(commercial))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", hasItems(entrepreneurs.toString(), maitresOuvrage.toString())))
                .andExpect(jsonPath("$[*].id", not(hasItem(STATUT_NOUVEAU.toString().toLowerCase()))));
    }

    @Test
    void r3_statutsLot_champsDuStatut() throws Exception {
        UUID statut = fixture.statut(LOT_ENTREPRENEURS, "STC-" + jeton);
        String filtre = "$[?(@.id == '" + statut + "')]";

        appeler(get(STATUTS_LOT), enCommercial(commercial))
                .andExpect(status().isOk())
                .andExpect(jsonPath(filtre + ".code", hasItem("STC-" + jeton)))
                .andExpect(jsonPath(filtre + ".ordre", hasItem(1)))
                .andExpect(jsonPath(filtre + ".couleur", hasItem("LimeGreen")))
                .andExpect(jsonPath(filtre + ".genre", not(hasItem(nullValue()))));
    }

    @Test
    void r3_listeProjets_administrateur_projetsSansHabilitationTriesParCode() throws Exception {
        UUID second = projetEnBase("ZB-" + jeton);
        UUID premier = projetEnBase("ZA-" + jeton);

        String json = appeler(get(PROJETS), connecte(admin)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        List<String> ids = JsonPath.read(json, "$[*].id");
        assertThat(ids).containsSubsequence(premier.toString(), second.toString());
    }

    @Test
    void r3_referentielsDuFormulaire_administrateur_sansEntites() throws Exception {
        appeler(get(REFERENTIELS), connecte(admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.societes").isArray())
                .andExpect(jsonPath("$.statutsProjet").isArray())
                .andExpect(jsonPath("$.gestionnaires").isArray())
                .andExpect(jsonPath("$.entites").doesNotExist());
    }

    // ------------------------------------------------------------------ règle 4 : unicité du code (§7.3)

    @Test
    void r4_creationProjet_codeEnDoubleCasseEtEspaces_409() throws Exception {
        projetEnBase("DBL-" + jeton);

        ecrire(post(PROJETS), corpsProjet("  dbl-" + jeton.toLowerCase() + "  "), connecte(admin))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail", containsString(CODE_DEJA_UTILISE)));

        assertThat(patrimoine.habilitations(admin.getId())).isEmpty();
    }

    @Test
    void r4_modificationProjet_codeDUnAutreProjet_409() throws Exception {
        projetEnBase("PRIS-" + jeton);
        UUID id = projetEnBase("LIBRE-" + jeton);

        ecrire(put(PROJET, id), corpsProjet(" pris-" + jeton.toLowerCase()), connecte(admin))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail", containsString(CODE_DEJA_UTILISE)));

        assertThat(fixture.compter(COMPTE_PROJET + " AND Code = N'LIBRE-" + jeton + "'", id)).isEqualTo(1);
    }

    @Test
    void r4_modificationProjet_sonPropreCode_200() throws Exception {
        UUID id = projetEnBase("SIEN-" + jeton);

        ecrire(put(PROJET, id), corpsProjet("SIEN-" + jeton).avec(DESCRIPTION, "Renomme " + jeton), connecte(admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description").value("Renomme " + jeton));
    }

    // ------------------------------------------------------------------ règle 5 : suppression (§7.4, §8.4)

    /** Tables du §8.4 présentes dans yesRemsTest et en CI ; {@code :id}, {@code :code}, {@code :projet} liés. */
    enum Reference {
        LOT("INSERT INTO PrjMarket (PKey, Code, PrjProjectId) VALUES (:id, :code, :projet)",
                "SELECT COUNT(*) FROM PrjMarket WHERE PKey = :id"),
        PIECE_ACHAT("INSERT INTO BuyDocument (PKey, Code, PrjProjectId) VALUES (:id, :code, :projet)",
                "SELECT COUNT(*) FROM BuyDocument WHERE PKey = :id"),
        TRANCHE("INSERT INTO CfgTranche (PKey, Code, PrjProjectId) VALUES (:id, :code, :projet)",
                "SELECT COUNT(*) FROM CfgTranche WHERE PKey = :id"),
        TACHE("INSERT INTO AdmTask (PKey, PrjProjectId) SELECT :id, :projet WHERE :code IS NOT NULL",
                "SELECT COUNT(*) FROM AdmTask WHERE PKey = :id"),
        VENTILATION_DEPENSE("INSERT INTO BuyVentilationExpense (Pkey, PrjProjectId) SELECT :id, :projet WHERE :code IS NOT NULL",
                "SELECT COUNT(*) FROM BuyVentilationExpense WHERE Pkey = :id"),
        PROJET_REFERENCE("INSERT INTO PrjProject (PKey, Code, ReferenceId) VALUES (:id, :code, :projet)",
                "SELECT COUNT(*) FROM PrjProject WHERE PKey = :id");

        private final String insertion;
        private final String comptage;

        Reference(String insertion, String comptage) {
            this.insertion = insertion;
            this.comptage = comptage;
        }
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(Reference.class)
    void r5_suppressionProjetReference_409_rienSupprime(Reference reference) throws Exception {
        UUID projet = projetEnBase("PM-" + jeton);
        fixture.habilitation(admin.getId(), projet);
        UUID ligne = fixture.reference(reference.insertion, "REF-" + jeton, projet);

        appeler(delete(PROJET, projet), connecte(admin))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail", containsString(UTILISE_PAR)));

        assertThat(new int[]{fixture.compter(COMPTE_PROJET, projet), fixture.compter(COMPTE_HABILITATIONS, projet),
                fixture.compter(reference.comptage, ligne)})
                .containsExactly(1, 1, 1);
    }

    @Test
    void r5_suppressionProjetSansReference_204_projetEtToutesSesHabilitationsSupprimes() throws Exception {
        UUID projet = projetEnBase("PS-" + jeton);
        fixture.habilitation(admin.getId(), projet);
        fixture.habilitation(commercial.getId(), projet);

        appeler(delete(PROJET, projet), connecte(admin)).andExpect(status().isNoContent());

        assertThat(new int[]{fixture.compter(COMPTE_PROJET, projet), fixture.compter(COMPTE_HABILITATIONS, projet)})
                .containsExactly(0, 0);
    }

    // ------------------------------------------------------------------ §7.5 : 400 et 404

    @ParameterizedTest(name = "sans {0}")
    @ValueSource(strings = {CODE, DESCRIPTION, SOCIETE_ID})
    void r6_creationProjet_champObligatoireManquant_400_rienEcrit(String champ) throws Exception {
        ecrire(post(PROJETS), corpsProjet("PO-" + jeton).sans(champ), connecte(admin))
                .andExpect(status().isBadRequest());

        assertThat(patrimoine.habilitations(admin.getId())).isEmpty();
    }

    @Test
    void r6_creationProjet_sansStatutNiEntite_201() throws Exception {
        ecrire(post(PROJETS), corpsProjet("PW-" + jeton), connecte(admin))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.statutId").value(STATUT_NOUVEAU.toString().toLowerCase()));
    }

    @Test
    void r6_modificationProjet_codeManquant_400() throws Exception {
        UUID id = projetEnBase("PQ-" + jeton);

        ecrire(put(PROJET, id), corpsProjet("PQ-" + jeton).sans(CODE), connecte(admin))
                .andExpect(status().isBadRequest());
    }

    @Test
    void lireProjet_200_puisIdInconnu_404() throws Exception {
        UUID id = projetEnBase("PL-" + jeton);

        appeler(get(PROJET, id), connecte(admin)).andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("PL-" + jeton));
        appeler(get(PROJET, UUID.randomUUID()), connecte(admin)).andExpect(status().isNotFound());
    }

    @Test
    void modifierProjet_200_puisIdInconnu_404() throws Exception {
        UUID id = projetEnBase("PU-" + jeton);

        ecrire(put(PROJET, id), corpsProjet("PU-" + jeton), connecte(admin)).andExpect(status().isOk());
        ecrire(put(PROJET, UUID.randomUUID()), corpsProjet("PV-" + jeton), connecte(admin))
                .andExpect(status().isNotFound());
    }

    @Test
    void supprimerProjet_204_puisIdInconnu_404() throws Exception {
        UUID id = projetEnBase("PX-" + jeton);

        appeler(delete(PROJET, id), connecte(admin)).andExpect(status().isNoContent());
        appeler(delete(PROJET, UUID.randomUUID()), connecte(admin)).andExpect(status().isNotFound());
    }

    // ------------------------------------------------------------------ §3 : compteurs de la fiche

    @Test
    void lireProjet_compteLotsEtPiecesAchat() throws Exception {
        UUID projet = projetEnBase("PK-" + jeton);
        fixture.reference(Reference.LOT.insertion, "LOT-" + jeton, projet);
        fixture.reference(Reference.PIECE_ACHAT.insertion, "BD1-" + jeton, projet);
        fixture.reference(Reference.PIECE_ACHAT.insertion, "BD2-" + jeton, projet);

        appeler(get(PROJET, projet), connecte(admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nbLots").value(1))
                .andExpect(jsonPath("$.nbPiecesAchat").value(2));
    }

    // ------------------------------------------------------------------ §7.6 : projets « année-société »

    @Test
    void anneeSociete_vraiPour2024Admod() throws Exception {
        UUID id = projetEnBase("2024-ADMOD");

        appeler(get(PROJET, id), connecte(admin)).andExpect(status().isOk())
                .andExpect(jsonPath("$.anneeSociete").value(true));
    }

    @Test
    void anneeSociete_fauxPourArsPrj() throws Exception {
        UUID id = projetEnBase("ARS-PRJ");

        appeler(get(PROJET, id), connecte(admin)).andExpect(status().isOk())
                .andExpect(jsonPath("$.anneeSociete").value(false));
    }
}
