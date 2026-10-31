package com.tsix.immocrm.controller.administration;

import com.tsix.immocrm.controller.administration.ArbreHabilitations.Noeud;
import com.tsix.immocrm.fixtures.CorpsJson;
import com.tsix.immocrm.fixtures.FixtureUtilisateurs;
import com.tsix.immocrm.fixtures.FixtureUtilisateurs.Etat;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import static com.tsix.immocrm.controller.administration.AppelsUtilisateurs.acceptee;
import static com.tsix.immocrm.controller.administration.AppelsUtilisateurs.statut;
import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static com.tsix.immocrm.fixtures.CorpsJson.corps;
import static com.tsix.immocrm.fixtures.FixtureUtilisateurs.HABILITATIONS;
import static com.tsix.immocrm.fixtures.FixtureUtilisateurs.NOEUDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * R13 et R14. Une société active et une inactive, chacune avec projet, tranche et projet de travaux. Les {@code type} envoyés au PUT
 * sont ceux que l'arbre lu donne aux nœuds (voir {@link ArbreHabilitations}).
 */
@SpringBootTest
@AutoConfigureMockMvc
class UtilisateursHabilitationsTest {

    private static final String REFUS_TRANCHE = """
            CREATE OR ALTER TRIGGER dbo.trgTestUtilisateursRefusHabilitation ON dbo.AdmUserHabilitation AFTER INSERT AS
            IF EXISTS (SELECT 1 FROM inserted i JOIN CfgTranche t ON t.PKey = i.CfgTrancheId WHERE t.Code LIKE 'ECHEC-R14-%')
                THROW 50000, 'Refus de test R14', 1;""";
    private static final String RETRAIT_TRIGGER = "DROP TRIGGER IF EXISTS dbo.trgTestUtilisateursRefusHabilitation";
    private static final UUID INCONNU = UUID.fromString("5B6C7D8E-9FA0-4B1C-82D3-E4F506172839");

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureUtilisateurs fixture;
    @Autowired private JdbcTemplate jdbc;

    private String jeton;
    private AppelsUtilisateurs appels;
    private UUID cible;
    private UUID societe;
    private UUID projet;
    private UUID tranche;
    private UUID travaux;
    private UUID societeInactive;
    private UUID projetInactif;
    private UUID trancheInactive;
    private UUID travauxInactifs;

    @BeforeEach
    void setUp() {
        jeton = fixture.preparer();
        appels = new AppelsUtilisateurs(mockMvc, connecte(fixture.compte()));
        cible = fixture.compte().getId();
        societe = fixture.societe("SOC-" + jeton, true);
        projet = fixture.projet("PRJ-" + jeton, societe);
        tranche = fixture.tranche("TR-" + jeton, projet);
        travaux = fixture.projetTravaux("TRV-" + jeton, societe);
        societeInactive = fixture.societe("INA-" + jeton, false);
        projetInactif = fixture.projet("PRI-" + jeton, societeInactive);
        trancheInactive = fixture.tranche("TRI-" + jeton, projetInactif);
        travauxInactifs = fixture.projetTravaux("TVI-" + jeton, societeInactive);
    }

    @AfterEach
    void nettoyer() {
        jdbc.execute(RETRAIT_TRIGGER);
        fixture.nettoyer();
    }

    private Noeud noeud(UUID id) throws Exception {
        return appels.arbre(cible).noeud(id);
    }

    // ------------------------------------------------------------------ R13

    @Test
    void r13_hierarchie_societeProjetTranche_etSocieteProjetTravaux() throws Exception {
        ArbreHabilitations arbre = appels.arbre(cible);

        assertAll(
                () -> assertThat(arbre.noeud(projet).sous(societe)).isTrue(),
                () -> assertThat(arbre.noeud(tranche).sous(projet)).isTrue(),
                () -> assertThat(arbre.noeud(tranche).sous(societe)).isTrue(),
                () -> assertThat(arbre.noeud(travaux).sous(societe)).isTrue(),
                () -> assertThat(arbre.noeud(travaux).sous(projet)).isFalse());
    }

    @Test
    void r13_societeInactive_presenteAvecSesDescendants() throws Exception {
        ArbreHabilitations arbre = appels.arbre(cible);

        assertAll(
                () -> assertThat(arbre.contient(societeInactive)).isTrue(),
                () -> assertThat(arbre.noeud(projetInactif).sous(societeInactive)).isTrue(),
                () -> assertThat(arbre.noeud(trancheInactive).sous(projetInactif)).isTrue(),
                () -> assertThat(arbre.noeud(travauxInactifs).sous(societeInactive)).isTrue());
    }

    @Test
    void r13_ligneUneCible_cochee_nonFigee() throws Exception {
        fixture.habiliter(cible, null, projet, null, null);

        Noeud lu = noeud(projet);

        assertThat(lu.coche()).isTrue();
        assertThat(lu.figee()).isFalse();
    }

    @Test
    void r13_sansLigne_nonCoche() throws Exception {
        fixture.habiliter(cible, null, projet, null, null);

        assertThat(noeud(tranche).coche()).isFalse();
    }

    @Test
    void r13_ligneMultiCibles_cocheEtFigeChaqueCible() throws Exception {
        fixture.habiliter(cible, societeInactive, projetInactif, trancheInactive, travauxInactifs);

        ArbreHabilitations arbre = appels.arbre(cible);

        assertThat(List.of(societeInactive, projetInactif, trancheInactive, travauxInactifs))
                .allSatisfy(id -> assertThat(arbre.noeud(id)).matches(Noeud::coche).matches(Noeud::figee));
    }

    @Test
    void r13_autreCompte_neCochePas() throws Exception {
        fixture.habiliter(fixture.compte().getId(), null, projet, null, null);

        assertThat(noeud(projet).coche()).isFalse();
    }

    // ------------------------------------------------------------------ R14

    /** La ligne gardée conserve sa PKey : pas de vidage à la façon d'OMRAN. */
    @Test
    void r14_differenceSeule_ligneGardeeIntacte() throws Exception {
        UUID gardee = fixture.habiliter(cible, null, projet, null, null);
        fixture.habiliter(cible, null, null, trancheInactive, null);
        ArbreHabilitations arbre = appels.arbre(cible);

        acceptee(appels.habilitations(cible, arbre.noeud(projet), arbre.noeud(tranche)));

        assertAll(
                () -> assertThat(fixture.lignesHabilitation(cible, projet)).containsExactly(gardee),
                () -> assertThat(fixture.lignesHabilitation(cible, trancheInactive)).isEmpty(),
                () -> assertThat(fixture.lignesUneSeuleCible(cible, tranche)).isEqualTo(1));
    }

    @Test
    void r14_unNoeudNouveau_uneLigneUneCible_quelQueSoitSonType() throws Exception {
        ArbreHabilitations arbre = appels.arbre(cible);

        acceptee(appels.habilitations(cible, arbre.noeud(societe), arbre.noeud(projet), arbre.noeud(tranche), arbre.noeud(travaux)));

        assertAll(
                () -> assertThat(fixture.habilitations(cible)).hasSize(4),
                () -> assertThat(fixture.lignesUneSeuleCible(cible, societe)).isEqualTo(1),
                () -> assertThat(fixture.lignesUneSeuleCible(cible, projet)).isEqualTo(1),
                () -> assertThat(fixture.lignesUneSeuleCible(cible, tranche)).isEqualTo(1),
                () -> assertThat(fixture.lignesUneSeuleCible(cible, travaux)).isEqualTo(1));
    }

    /** Les parents d'un nœud coché, sociétés inactives comprises, ne s'ajoutent pas seuls : le front envoie ce qui est coché. */
    @Test
    void r14_noeudDeSocieteInactive_accepte() throws Exception {
        acceptee(appels.habilitations(cible, noeud(trancheInactive)));

        assertThat(fixture.lignesUneSeuleCible(cible, trancheInactive)).isEqualTo(1);
        assertThat(fixture.habilitations(cible)).hasSize(1);
    }

    @Test
    void r14_ligneMultiCibles_jamaisTouchee() throws Exception {
        fixture.habiliter(cible, societeInactive, projetInactif, null, null);
        fixture.habiliter(cible, null, projet, null, null);
        var multi = fixture.lignesHabilitation(cible, societeInactive);
        var colonnes = fixture.habilitations(cible).stream()
                .filter(l -> multi.getFirst().toString().equalsIgnoreCase(l.get("PKey").toString())).toList();

        acceptee(appels.habilitations(cible));

        assertThat(fixture.habilitations(cible)).isEqualTo(colonnes);
    }

    @Test
    void r14_noeudSansEffet_rienNestEcrit() throws Exception {
        fixture.habiliter(cible, null, projet, null, null);
        Noeud lu = noeud(projet);
        Etat avant = fixture.etat(cible);

        acceptee(appels.habilitations(cible, lu));

        assertThat(fixture.etat(cible)).isEqualTo(avant);
    }

    @Test
    void r14_noeudEnDouble_uneSeuleLigne() throws Exception {
        Noeud lu = noeud(projet);

        acceptee(appels.habilitations(cible, lu, lu));

        assertThat(fixture.lignesHabilitation(cible, projet)).hasSize(1);
    }

    @Test
    void r14_idInconnu_400_sansEcriture() throws Exception {
        UUID retiree = fixture.habiliter(cible, null, projet, null, null);
        ArbreHabilitations arbre = appels.arbre(cible);
        Etat avant = fixture.etat(cible);

        appels.habilitations(cible, arbre.noeud(tranche), new Noeud(INCONNU.toString(), arbre.noeud(tranche).type(), false, false, List.of()))
                .andExpect(status().isBadRequest());

        assertThat(fixture.etat(cible)).isEqualTo(avant);
        assertThat(fixture.lignesHabilitation(cible, projet)).containsExactly(retiree);
    }

    /** Un id existant annoncé sous le type d'un autre niveau reste un nœud inconnu. */
    @Test
    void r14_idDUnAutreType_400_sansEcriture() throws Exception {
        ArbreHabilitations arbre = appels.arbre(cible);
        Etat avant = fixture.etat(cible);
        Noeud croise = new Noeud(arbre.noeud(projet).id(), arbre.noeud(societe).type(), false, false, List.of());

        appels.habilitations(cible, croise).andExpect(status().isBadRequest());

        assertThat(fixture.etat(cible)).isEqualTo(avant);
    }

    @Test
    void r14_typeInconnu_400_sansEcriture() throws Exception {
        Etat avant = fixture.etat(cible);
        CorpsJson corps = corps().avec(NOEUDS, List.of(corps().avec("type", "INCONNU").avec("id", projet)));

        appels.habilitations(cible, corps).andExpect(status().isBadRequest());

        assertThat(fixture.etat(cible)).isEqualTo(avant);
    }

    /** Réponse 11 : un corps tronqué ne doit pas retirer d'habilitation, donc élargir un périmètre. */
    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void reponse11_noeudsAbsentsOuNull_400_etatInchange(boolean explicite) throws Exception {
        fixture.habiliter(cible, null, projet, null, null);
        Etat avant = fixture.etat(cible);
        CorpsJson tronque = explicite ? corps().avec(NOEUDS, null) : corps();

        appels.habilitations(cible, tronque).andExpect(status().isBadRequest());

        assertThat(fixture.etat(cible)).isEqualTo(avant);
    }

    /** Réponse 10 : envoyé, le nœud figé ne crée pas de ligne à une cible et ne retire pas la ligne d'origine. */
    @Test
    void reponse10_noeudFigeEnvoye_sansEffet() throws Exception {
        fixture.habiliter(cible, societeInactive, projetInactif, null, null);
        Noeud fige = noeud(projetInactif);
        Etat avant = fixture.etat(cible);

        acceptee(appels.habilitations(cible, fige));

        assertThat(fixture.etat(cible)).isEqualTo(avant);
    }

    /** Réponse 10 : tous les cochés renvoyés, figés compris ; {@code etat} compte aussi les traces. */
    @Test
    void reponse10_allerRetourArbreInchange_aucuneEcriture_aucuneTrace() throws Exception {
        fixture.habiliter(cible, null, projet, null, null);
        fixture.habiliter(cible, null, null, tranche, null);
        fixture.habiliter(cible, societeInactive, projetInactif, trancheInactive, travauxInactifs);
        Noeud[] coches = appels.arbre(cible).coches();
        Etat avant = fixture.etat(cible);

        acceptee(appels.habilitations(cible, coches));

        assertThat(coches).hasSize(6);
        assertThat(fixture.etat(cible)).isEqualTo(avant);
    }

    @Test
    void reponse9_typesDuFil() throws Exception {
        ArbreHabilitations arbre = appels.arbre(cible);

        assertAll(
                () -> assertThat(arbre.noeud(societe).type()).isEqualTo("SOCIETE"),
                () -> assertThat(arbre.noeud(projet).type()).isEqualTo("PROJET"),
                () -> assertThat(arbre.noeud(tranche).type()).isEqualTo("TRANCHE"),
                () -> assertThat(arbre.noeud(travaux).type()).isEqualTo("PROJET_TRAVAUX"));
    }

    @Test
    void reponse9_clesDuNoeud() throws Exception {
        String corps = appels.corpsLu(HABILITATIONS.replace("{id}", cible.toString()));

        assertThat(corps).contains("\"libelle\"", "\"coche\"", "\"figee\"", "\"enfants\"");
    }

    @Test
    void reponse13_putHabilitations_200_avecLArbre() throws Exception {
        Noeud lu = noeud(projet);

        String corps = appels.habilitations(cible, lu).andExpect(status().isOk()).andReturn().getResponse()
                .getContentAsString(StandardCharsets.UTF_8);

        assertThat(ArbreHabilitations.lu(corps).noeud(projet).coche()).isTrue();
    }

    /** Déclencheur de test : le dernier ajout échoue en base, le retrait et le premier ajout sont annulés avec lui. */
    @Test
    void r14_echecEnBase_toutEstAnnule() throws Exception {
        UUID echec = fixture.tranche("ECHEC-R14-" + jeton, projet);
        fixture.habiliter(cible, null, projet, null, null);
        ArbreHabilitations arbre = appels.arbre(cible);
        Etat avant = fixture.etat(cible);
        jdbc.execute(REFUS_TRANCHE);

        int statut = statut(appels.habilitations(cible, arbre.noeud(tranche), arbre.noeud(echec)));

        assertThat(statut).isGreaterThanOrEqualTo(400).isNotEqualTo(404).isNotEqualTo(405);
        assertThat(fixture.etat(cible)).isEqualTo(avant);
    }
}
