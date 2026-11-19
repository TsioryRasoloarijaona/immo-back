package com.tsix.immocrm.controller.achat;

import com.tsix.immocrm.controller.achat.DemandesAchatAppels.Reponse;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.fixtures.CorpsJson;
import com.tsix.immocrm.fixtures.FixtureDemandesAchat;
import com.tsix.immocrm.fixtures.FixtureDemandesAchat.Cadre;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.tsix.immocrm.controller.achat.DemandesAchatAppels.DETAIL;
import static com.tsix.immocrm.fixtures.CorpsJson.corps;
import static com.tsix.immocrm.fixtures.FixtureDemandesAchat.DEMANDE;
import static com.tsix.immocrm.fixtures.FixtureDemandesAchat.DROIT_LECTURE;
import static com.tsix.immocrm.fixtures.FixtureDemandesAchat.DROIT_NOUVEAU;
import static com.tsix.immocrm.fixtures.FixtureDemandesAchat.DROIT_SUPPRIMER;
import static com.tsix.immocrm.fixtures.FixtureDemandesAchat.TOUS_LES_DROITS;
import static com.tsix.immocrm.fixtures.FixtureDemandesAchat.TYPE_NOUVEAU;
import static com.tsix.immocrm.fixtures.FixtureDemandesAchat.qui;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * N6a R1-R3, R5-R7, R14, D4, D7 (écritures) et invariant 2 (FEATURE-n6-demandes-achat.md). Cadre : entité de test à 4 statuts,
 * procédure « sans vérification » ; compte libre (tous droits) et compte restreint voyant les 4 statuts.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class DemandesAchatEcritureTest {

    private static final String PROCEDURE_ID = "procedureId";
    private static final String SOCIETE_ID = "societeId";
    private static final String PROJET_ID = "projetId";
    private static final String DESCRIPTION = "description";
    private static final String PRIX_TTC = "prixEstimeTTC";
    private static final String PAR_DESCRIPTION = "SELECT * FROM BuySelection WHERE Description = ?";
    private static final String NOMBRE_PAR_ID = "SELECT COUNT(*) FROM BuySelection WHERE PKey = ?";
    private static final String COL_STATUT = "StrStatusId";
    private static final String COL_PROCEDURE = "BuySelectionKindId";
    private static final String MONTANT_REFUSE =
            "La procédure d'achat selectionnée ne gère pas le montant estimé pour cette demande d'achat";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureDemandesAchat fixture;

    private DemandesAchatAppels appels;
    private Cadre c;
    private AdmUser libre;
    private RequestPostProcessor qLibre;
    private RequestPostProcessor qRestreint;

    @BeforeEach
    void setUp() {
        appels = new DemandesAchatAppels(mockMvc);
        c = fixture.cadre("EC");
        libre = fixture.libre(c, "n6.ec.libre." + c.jeton(), TOUS_LES_DROITS);
        qLibre = qui(libre);
        qRestreint = qui(fixture.restreint(c, "n6.ec.restr." + c.jeton(), TOUS_LES_DROITS));
    }

    private CorpsJson saisie(String description, UUID procedure) {
        return corps().avec(PROCEDURE_ID, procedure).avec(SOCIETE_ID, c.societe()).avec(PROJET_ID, c.projet())
                .avec(DESCRIPTION, description);
    }

    private String description(String suffixe) {
        return "DIA " + suffixe + " " + c.jeton();
    }

    private List<Map<String, Object>> creees(String description) {
        return fixture.lignes(PAR_DESCRIPTION, description);
    }

    private Map<String, Object> enBase(UUID dia) {
        return fixture.demandeEnBase(dia);
    }

    private int nombre(UUID dia) {
        return fixture.compter(NOMBRE_PAR_ID, dia.toString());
    }

    private static String maj(UUID id) {
        return id == null ? "null" : id.toString().toUpperCase();
    }

    private static String texte(Object valeur) {
        return String.valueOf(valeur).toUpperCase();
    }

    // ------------------------------------------------------------------ R1 champs obligatoires

    @ParameterizedTest
    @ValueSource(strings = {PROCEDURE_ID, SOCIETE_ID, DESCRIPTION})
    void r1_champObligatoireAbsent_400_rienNEstCree(String champ) throws Exception {
        String description = description("R1-" + champ);

        Reponse r = appels.creer(qLibre, saisie(description, c.procedure()).sans(champ));
        Reponse temoin = appels.creer(qLibre, saisie(description("R1T-" + champ), c.procedure()));

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(r.statut()).as(r.corps()).isEqualTo(400);
            s.assertThat(creees(description)).isEmpty();
            s.assertThat(temoin.statut()).as(temoin.corps()).isEqualTo(201);
        });
    }

    // ------------------------------------------------------------------ R2 création

    @Test
    void r2_creation_ligneEcriteParLeServeur() throws Exception {
        String description = description("R2");

        Reponse r = appels.creer(qLibre, saisie(description, c.procedure()));

        assertThat(r.statut()).as(r.corps()).isEqualTo(201);
        List<Map<String, Object>> lignes = creees(description);
        assertThat(lignes).hasSize(1);
        Map<String, Object> ligne = lignes.get(0);
        SoftAssertions.assertSoftly(s -> {
            s.assertThat(texte(ligne.get("StrEntityId"))).isEqualTo(maj(c.entite()));
            s.assertThat(texte(ligne.get(COL_STATUT))).isEqualTo(maj(c.prep()));
            s.assertThat(texte(ligne.get(COL_PROCEDURE))).isEqualTo(maj(c.procedure()));
            s.assertThat(texte(ligne.get("CfgCompanyId"))).isEqualTo(maj(c.societe()));
            s.assertThat(texte(ligne.get("PrjProjectId"))).isEqualTo(maj(c.projet()));
            s.assertThat(ligne.get("IsTheLast")).isEqualTo(true);
            s.assertThat(texte(ligne.get("GroupId"))).isEqualTo(texte(ligne.get("PKey")));
            s.assertThat(texte(ligne.get("CfgTierTriggerId"))).isEqualTo(maj(libre.getCfgEmployee().getId()));
            s.assertThat(texte(ligne.get("AdmModuleId"))).isEqualTo(maj(c.moduleAchat()));
            s.assertThat(r.corps()).containsIgnoringCase(String.valueOf(ligne.get("PKey")));
        });
    }

    @Test
    void r2_creation_historiqueCreation() throws Exception {
        String description = description("R2H");

        Reponse r = appels.creer(qLibre, saisie(description, c.procedure()));

        assertThat(r.statut()).as(r.corps()).isEqualTo(201);
        UUID id = UUID.fromString(String.valueOf(creees(description).get(0).get("PKey")));
        assertThat(fixture.historique(id)).extracting(l -> l.get("Description")).containsExactly("Création");
    }

    @Test
    void d4_code_anneeEtPlusGrandNumeroPlusUn() throws Exception {
        String annee = "%02d/".formatted(LocalDate.now(ZoneId.of("Africa/Casablanca")).getYear() % 100);
        fixture.demande(c, annee + "99990", c.prep());
        String description = description("D4");

        Reponse r = appels.creer(qLibre, saisie(description, c.procedure()));

        assertThat(r.statut()).as(r.corps()).isEqualTo(201);
        assertThat(creees(description)).extracting(l -> l.get("Code")).containsExactly(annee + "99991");
    }

    @Test
    void d4_decision13_compteurContinueDUneAnneeALAutre() throws Exception {
        String annee = "%02d/".formatted(LocalDate.now(ZoneId.of("Africa/Casablanca")).getYear() % 100);
        fixture.demande(c, "25/00172", c.prep());
        String description = description("D4A");

        Reponse r = appels.creer(qLibre, saisie(description, c.procedure()));

        assertThat(r.statut()).as(r.corps()).isEqualTo(201);
        assertThat(creees(description)).extracting(l -> l.get("Code")).containsExactly(annee + "00173");
    }

    @Test
    void r2_statutDeDepart_premierNouveauVisibleDuCompte() throws Exception {
        UUID prep2 = fixture.statut(c.entite(), "PREP2-" + c.jeton(), TYPE_NOUVEAU, 5, true);
        AdmUser restreint = fixture.restreintSansStatut(c, "n6.ec.dep." + c.jeton(), TOUS_LES_DROITS);
        fixture.voitStatuts(c.entite(), fixture.roleDe(restreint), List.of(prep2, c.depouil()));
        String description = description("R2V");

        Reponse r = appels.creer(qui(restreint), saisie(description, c.procedure()));

        assertThat(r.statut()).as(r.corps()).isEqualTo(201);
        assertThat(creees(description)).extracting(l -> texte(l.get(COL_STATUT))).containsExactly(maj(prep2));
    }

    @Test
    void r2_aucunStatutDeDepartVisible_403_rienNEstCree() throws Exception {
        AdmUser restreint = fixture.restreintSansStatut(c, "n6.ec.nodep." + c.jeton(), TOUS_LES_DROITS);
        fixture.voitStatuts(c.entite(), fixture.roleDe(restreint), List.of(c.depouil()));
        String description = description("R2X");

        Reponse r = appels.creer(qui(restreint), saisie(description, c.procedure()));
        Reponse temoin = appels.creer(qRestreint, saisie(description("R2XT"), c.procedure()));

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(r.statut()).as(r.corps()).isEqualTo(403);
            s.assertThat(String.valueOf(r.valeur(DETAIL)))
                    .startsWith("Vous n'avez pas les droits sur les états de création de l'entité");
            s.assertThat(creees(description)).isEmpty();
            s.assertThat(temoin.statut()).as(temoin.corps()).isEqualTo(201);
        });
    }

    @Test
    void r3_procedureSansEntite_400() throws Exception {
        UUID sansEntite = fixture.procedureSansEntite("PSE-" + c.jeton());
        String description = description("R3");

        Reponse r = appels.creer(qLibre, saisie(description, sansEntite));

        assertThat(r.statut()).as(r.corps()).isEqualTo(400);
        SoftAssertions.assertSoftly(s -> {
            s.assertThat(r.valeur(DETAIL)).isEqualTo("Aucun Statut n'est sélectionné ou vous n'avez pas les droits sur les états "
                    + "de création de l'entité en cours !");
            s.assertThat(creees(description)).isEmpty();
        });
    }

    // ------------------------------------------------------------------ D7 droits d'écriture

    @Test
    void d7_creationSansToolNew_403() throws Exception {
        AdmUser lecteur = fixture.libre(c, "n6.ec.lect." + c.jeton(), List.of(DROIT_LECTURE));
        String description = description("D7C");

        Reponse r = appels.creer(qui(lecteur), saisie(description, c.procedure()));

        assertThat(r.statut()).as(r.corps()).isEqualTo(403);
        assertThat(creees(description)).isEmpty();
    }

    @Test
    void d7_modificationSansToolNew_403() throws Exception {
        AdmUser lecteur = fixture.libre(c, "n6.ec.lectm." + c.jeton(), List.of(DROIT_LECTURE));
        UUID dia = fixture.demande(c, c.code("D7M"), c.prep());

        Reponse r = appels.modifier(qui(lecteur), dia, saisie(description("D7M"), c.procedure()));

        assertThat(r.statut()).as(r.corps()).isEqualTo(403);
        assertThat(enBase(dia).get("Description")).isEqualTo("DIA " + c.code("D7M"));
    }

    @Test
    void d7_suppressionSansToolDelete_403() throws Exception {
        AdmUser lecteur = fixture.libre(c, "n6.ec.lectd." + c.jeton(), List.of(DROIT_LECTURE, DROIT_NOUVEAU));
        UUID dia = fixture.demande(c, c.code("D7D"), c.prep());

        Reponse r = appels.supprimer(qui(lecteur), dia);

        assertThat(r.statut()).as(r.corps()).isEqualTo(403);
        assertThat(nombre(dia)).isEqualTo(1);
    }

    @Test
    void inv3_suppression_horsPerimetreSansDroit_404AvantLe403() throws Exception {
        Cadre ailleurs = fixture.cadre("ECX");
        UUID invisible = fixture.demande(ailleurs, ailleurs.code("X"), ailleurs.prep());
        AdmUser lecteur = fixture.libre(c, "n6.ec.lectx." + c.jeton(), List.of(DROIT_LECTURE));

        Reponse r = appels.supprimer(qui(lecteur), invisible);
        Reponse temoin = appels.supprimer(qui(lecteur), fixture.demande(c, c.code("XT"), c.prep()));

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(r.statut()).as(r.corps()).isEqualTo(404);
            s.assertThat(temoin.statut()).as(temoin.corps()).isEqualTo(403);
            s.assertThat(nombre(invisible)).isEqualTo(1);
        });
    }

    // ------------------------------------------------------------------ R5 changement de procédure

    @Test
    void r5_changerDeProcedure_horsNouveau_409() throws Exception {
        UUID autre = fixture.procedure("PAUTRE-" + c.jeton(), c.entite());
        UUID dia = fixture.demande(c, c.code("R5A"), c.depouil());

        Reponse r = appels.modifier(qLibre, dia, saisie(description("R5A"), autre));
        Reponse temoin = appels.modifier(qLibre, dia, saisie(description("R5A"), c.procedure()));

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(r.statut()).as(r.corps()).isEqualTo(409);
            s.assertThat(temoin.statut()).as(temoin.corps()).isEqualTo(200);
            s.assertThat(texte(enBase(dia).get(COL_PROCEDURE))).isEqualTo(maj(c.procedure()));
        });
    }

    @Test
    void r5_changerDeProcedure_enNouveau_autreEntite_statutRemisAuDepart_sansHistorique() throws Exception {
        UUID entite2 = fixture.entite("BuyCallOfTenderT2-" + c.jeton());
        UUID depart2 = fixture.statut(entite2, "N2PREP-" + c.jeton(), TYPE_NOUVEAU, 1, true);
        UUID procedure2 = fixture.procedure("P2-" + c.jeton(), entite2);
        UUID dia = fixture.demande(c, c.code("R5B"), c.prep());

        Reponse r = appels.modifier(qLibre, dia, saisie(description("R5B"), procedure2));

        assertThat(r.statut()).as(r.corps()).isEqualTo(200);
        Map<String, Object> ligne = enBase(dia);
        SoftAssertions.assertSoftly(s -> {
            s.assertThat(texte(ligne.get(COL_PROCEDURE))).isEqualTo(maj(procedure2));
            s.assertThat(texte(ligne.get("StrEntityId"))).isEqualTo(maj(entite2));
            s.assertThat(texte(ligne.get(COL_STATUT))).isEqualTo(maj(depart2));
            s.assertThat(fixture.historique(dia)).isEmpty();
        });
    }

    // ------------------------------------------------------------------ R6 montant par procédure

    @Test
    void r6_montantAuDelaDuMaxDeLaProcedure_400_procedureSuperieure_201() throws Exception {
        UUID p10 = fixture.procedureVerifiee("P10K-" + c.jeton(), c.entite(), new BigDecimal("10000"));
        UUID p100 = fixture.procedureVerifiee("P100K-" + c.jeton(), c.entite(), new BigDecimal("100000"));
        String refusee = description("R6A");

        Reponse refus = appels.creer(qLibre, saisie(refusee, p10).avec(PRIX_TTC, new BigDecimal("50000")));
        Reponse acceptee = appels.creer(qLibre, saisie(description("R6B"), p100).avec(PRIX_TTC, new BigDecimal("50000")));

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(refus.statut()).as(refus.corps()).isEqualTo(400);
            s.assertThat(refus.valeur(DETAIL)).isEqualTo(MONTANT_REFUSE);
            s.assertThat(creees(refusee)).isEmpty();
            s.assertThat(acceptee.statut()).as(acceptee.corps()).isEqualTo(201);
        });
    }

    @Test
    void r6_procedureSansVerification_montantAccepte() throws Exception {
        fixture.procedureVerifiee("P100K-" + c.jeton(), c.entite(), new BigDecimal("100000"));
        UUID sansVerif = fixture.procedure("PSV-" + c.jeton(), c.entite());
        fixture.poser(FixtureDemandesAchat.PROCEDURE, sansVerif, "MaxAmount", new BigDecimal("10000"));

        Reponse r = appels.creer(qLibre, saisie(description("R6C"), sansVerif).avec(PRIX_TTC, new BigDecimal("50000")));

        assertThat(r.statut()).as(r.corps()).isEqualTo(201);
    }

    @Test
    void r6_modification_montantAuDelaDuMax_400() throws Exception {
        UUID p10 = fixture.procedureVerifiee("P10K-" + c.jeton(), c.entite(), new BigDecimal("10000"));
        fixture.procedureVerifiee("P100K-" + c.jeton(), c.entite(), new BigDecimal("100000"));
        UUID dia = fixture.demande(c, c.code("R6D"), c.prep(), p10, LocalDate.of(2026, 3, 10));

        Reponse r = appels.modifier(qLibre, dia, saisie(description("R6D"), p10).avec(PRIX_TTC, new BigDecimal("50000")));

        assertThat(r.statut()).as(r.corps()).isEqualTo(400);
        assertThat(r.valeur(DETAIL)).isEqualTo(MONTANT_REFUSE);
    }

    // ------------------------------------------------------------------ R7 DIA non dernière

    @Test
    void r7_diaNonDerniere_touteEcriture409() throws Exception {
        UUID lancer = fixture.transition(c.entite(), c.prep(), c.depouil(), "Lancer " + c.jeton(), 10);
        UUID dia = fixture.demande(c, c.code("R7"), c.prep());
        fixture.poser(DEMANDE, dia, "IsTheLast", false);
        UUID temoin = fixture.demande(c, c.code("R7T"), c.prep());

        Reponse modification = appels.modifier(qLibre, dia, saisie(description("R7"), c.procedure()));
        Reponse transition = appels.executer(qLibre, dia, lancer);
        Reponse suppression = appels.supprimer(qLibre, dia);
        Reponse modificationTemoin = appels.modifier(qLibre, temoin, saisie(description("R7T"), c.procedure()));

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(modification.statut()).as(modification.corps()).isEqualTo(409);
            s.assertThat(transition.statut()).as(transition.corps()).isEqualTo(409);
            s.assertThat(suppression.statut()).as(suppression.corps()).isEqualTo(409);
            s.assertThat(nombre(dia)).isEqualTo(1);
            s.assertThat(texte(enBase(dia).get(COL_STATUT))).isEqualTo(maj(c.prep()));
            s.assertThat(enBase(dia).get("Description")).isEqualTo("DIA " + c.code("R7"));
            s.assertThat(modificationTemoin.statut()).as(modificationTemoin.corps()).isEqualTo(200);
        });
    }

    // ------------------------------------------------------------------ R14 suppression

    @Test
    void r14_restreint_statutNonSupprimable_409() throws Exception {
        UUID dia = fixture.demande(c, c.code("R14A"), c.clot());

        Reponse r = appels.supprimer(qRestreint, dia);

        assertThat(r.statut()).as(r.corps()).isEqualTo(409);
        SoftAssertions.assertSoftly(s -> {
            s.assertThat(r.valeur(DETAIL))
                    .isEqualTo("Vous ne pouvez pas supprimer cette entité dans le statut : [CLO-" + c.jeton() + "] !");
            s.assertThat(nombre(dia)).isEqualTo(1);
        });
    }

    @Test
    void r14_libre_statutNonSupprimable_supprime() throws Exception {
        UUID dia = fixture.demande(c, c.code("R14B"), c.clot());

        Reponse r = appels.supprimer(qLibre, dia);

        assertThat(r.statut()).as(r.corps()).isBetween(200, 204);
        assertThat(nombre(dia)).isZero();
    }

    @Test
    void r14_restreint_statutSupprimable_supprime() throws Exception {
        UUID dia = fixture.demande(c, c.code("R14C"), c.prep());

        Reponse r = appels.supprimer(qRestreint, dia);

        assertThat(r.statut()).as(r.corps()).isBetween(200, 204);
        assertThat(nombre(dia)).isZero();
    }

    @Test
    void r14_pieceLiee_409_avantLaBase() throws Exception {
        UUID dia = fixture.demande(c, c.code("R14D"), c.prep());
        fixture.pieceLiee(dia, "DEV-" + c.jeton());

        Reponse r = appels.supprimer(qLibre, dia);

        assertThat(r.statut()).as(r.corps()).isEqualTo(409);
        assertThat(nombre(dia)).isEqualTo(1);
    }

    // ------------------------------------------------------------------ invariant 2 : le corps ment

    @Test
    void inv2_modification_statutEntiteEtDerniereDuCorps_ignores() throws Exception {
        UUID autreEntite = fixture.entite("AUTRE-" + c.jeton());
        UUID dia = fixture.demande(c, c.code("INV2"), c.prep());

        Reponse r = appels.modifier(qLibre, dia, saisie(description("INV2"), c.procedure())
                .avec("statutId", c.clot()).avec("statut", corps().avec("id", c.clot())).avec("entiteId", autreEntite)
                .avec("estDerniere", false).avec("code", "99/99999"));

        assertThat(r.statut()).as(r.corps()).isEqualTo(200);
        Map<String, Object> ligne = enBase(dia);
        SoftAssertions.assertSoftly(s -> {
            s.assertThat(ligne.get("Description")).isEqualTo(description("INV2"));
            s.assertThat(texte(ligne.get(COL_STATUT))).isEqualTo(maj(c.prep()));
            s.assertThat(texte(ligne.get("StrEntityId"))).isEqualTo(maj(c.entite()));
            s.assertThat(ligne.get("IsTheLast")).isEqualTo(true);
            s.assertThat(ligne.get("Code")).isEqualTo(c.code("INV2"));
        });
    }

    @Test
    void inv2_modification_diaHorsPerimetre_corpsAvecProjetVisible_404() throws Exception {
        Cadre ailleurs = fixture.cadre("ECY");
        UUID invisible = fixture.demande(ailleurs, ailleurs.code("Y"), ailleurs.prep());

        Reponse r = appels.modifier(qLibre, invisible, saisie(description("INV2Y"), c.procedure()));
        Reponse temoin = appels.modifier(qLibre, fixture.demande(c, c.code("YT"), c.prep()),
                saisie(description("INV2YT"), c.procedure()));

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(r.statut()).as(r.corps()).isEqualTo(404);
            s.assertThat(enBase(invisible).get("Description")).isEqualTo("DIA " + ailleurs.code("Y"));
            s.assertThat(temoin.statut()).as(temoin.corps()).isEqualTo(200);
        });
    }
}
