package com.tsix.immocrm.controller.projet;

import com.tsix.immocrm.controller.projet.AppelsTravauxChantier.Reponse;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.fixtures.FixtureDossierVente;
import com.tsix.immocrm.fixtures.FixtureProjetLots;
import com.tsix.immocrm.fixtures.FixtureTravauxChantier;
import com.tsix.immocrm.fixtures.FixtureTravauxChantier.Compte;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.FixtureTravauxChantier.DROIT_MODIFS_LIRE;
import static com.tsix.immocrm.fixtures.FixtureTravauxChantier.DROIT_MODIFS_MODIFIER;
import static com.tsix.immocrm.fixtures.FixtureTravauxChantier.MODIFICATIONS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

/**
 * Modifications du nœud Projet › Chantier (FEATURE-n2-travaux-chantier.md D4, D5, D6, R6, R7 et réponse 4 du manager) :
 * liste par tranches habilitées, PUT délégué à {@code ModificationsService.modifier} (recalcul du dossier, périmètre du dossier).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ModificationsProjetTest {

    private static final LocalDateTime LE_10_MARS = LocalDateTime.of(2077, 3, 10, 0, 0);
    private static final String MARS = "du=2077-03-01&au=2077-03-31";
    private static final String STATUT = "SELECT Status FROM ComModification WHERE PKey = ?";
    private static final String MONTANT_DOSSIER = "SELECT ModificationsAmount FROM ComFolder WHERE PKey = ?";
    private static final String ENCOURS = "ENCOURS";
    private static final String ACCEPTEE = "ACCEPTEE";

    @Autowired private MockMvc mockMvc;
    @Autowired private JsonMapper json;
    @Autowired private FixtureTravauxChantier fixture;
    @Autowired private FixtureDossierVente vente;

    private AppelsTravauxChantier appels;
    private String jeton;
    private CfgTranche trancheHabilitee;
    private CfgTranche trancheNonHabilitee;
    private ComFolder dossier;
    private UUID enCours;
    private UUID refusee;
    private UUID horsTranche;
    private UUID sousArticle;

    @BeforeEach
    void setUp() {
        fixture.preparer();
        appels = new AppelsTravauxChantier(mockMvc, json, fixture);
        jeton = FixtureProjetLots.jeton();
        trancheHabilitee = vente.tranche("TMH-" + jeton, true);
        trancheNonHabilitee = vente.tranche("TMN-" + jeton, true);
        dossier = fixture.dossier("DMH-" + jeton, trancheHabilitee, false);
        enCours = fixture.modification(dossier, ENCOURS, "Cloison " + jeton, LE_10_MARS);
        refusee = fixture.modification(dossier, "REFUSE_PAR_LE_CLIENT", "Carrelage " + jeton, LE_10_MARS);
        horsTranche = fixture.modification(fixture.dossier("DMN-" + jeton, trancheNonHabilitee, false), ENCOURS,
                "Hors tranche " + jeton, LE_10_MARS);
        sousArticle = fixture.modification(fixture.dossier("DMS-" + jeton, trancheHabilitee, true), ENCOURS,
                "Sous-article " + jeton, LE_10_MARS);
    }

    /** Compte non administrateur portant ces droits, habilité sur la seule tranche habilitée. */
    private Compte compte(String suffixe, String... droits) {
        Compte c = fixture.compte(jeton + suffixe, false, List.of(), droits);
        fixture.habiliterTranche(c.user(), trancheHabilitee.getId());
        return c;
    }

    private RequestPostProcessor lecteur(String suffixe) {
        return compte(suffixe, DROIT_MODIFS_LIRE).connexion();
    }

    private RequestPostProcessor editeur(String suffixe) {
        return compte(suffixe, DROIT_MODIFS_LIRE, DROIT_MODIFS_MODIFIER).connexion();
    }

    private List<String> ids(String filtres, RequestPostProcessor qui) throws Exception {
        Reponse r = appels.appeler(get(MODIFICATIONS + "?" + filtres), qui);
        assertThat(r.statut()).as(r.corps()).isEqualTo(200);
        return r.lire("$[*].id");
    }

    private static Map<String, Object> saisie(String etat) {
        Map<String, Object> corps = new LinkedHashMap<>();
        corps.put("description", "Cloison");
        corps.put("dateDemande", "2077-03-10T00:00:00");
        corps.put("cout", 8000);
        corps.put("prixHT", 10000);
        corps.put("prixTTC", 12000);
        corps.put("etat", etat);
        corps.put("notes", "");
        return corps;
    }

    private Reponse modifier(UUID modification, String etat, RequestPostProcessor qui) throws Exception {
        return appels.envoyer(put(MODIFICATIONS + "/" + modification), saisie(etat), qui);
    }

    // ------------------------------------------------------------------ liste, filtres d'état

    @Test
    void liste_etatsRienCoche_tousLesEtats() throws Exception {
        List<String> liste = ids(MARS, lecteur("a"));

        assertThat(liste).contains(enCours.toString(), refusee.toString());
    }

    @Test
    void liste_etatsCoches_restreignent() throws Exception {
        List<String> liste = ids(MARS + "&etats=REFUSE_PAR_LE_CLIENT", lecteur("b"));

        assertThat(liste).contains(refusee.toString()).doesNotContain(enCours.toString());
    }

    @Test
    void liste_ligne_dossierEtEtat() throws Exception {
        Reponse r = appels.appeler(get(MODIFICATIONS + "?" + MARS), lecteur("c"));

        assertThat(r.statut()).as(r.corps()).isEqualTo(200);
        String ligne = "$[?(@.id == '" + enCours + "')]";
        assertThat(r.<List<String>>lire(ligne + ".dossierId")).containsExactly(dossier.getId().toString());
        assertThat(r.<List<String>>lire(ligne + ".etat.code")).containsExactly(ENCOURS);
    }

    @Test
    void liste_vide_200_tableauVide() throws Exception {
        Reponse r = appels.appeler(get(MODIFICATIONS + "?" + MARS + "&recherche=AUCUNE-" + jeton), lecteur("d"));

        assertThat(r.statut()).as(r.corps()).isEqualTo(200);
        assertThat(r.<List<Object>>lire("$")).isEmpty();
    }

    // ------------------------------------------------------------------ R7 périmètre (tranche habilitée, article racine)

    @Test
    void r7_liste_trancheNonHabilitee_absente() throws Exception {
        List<String> liste = ids(MARS, lecteur("e"));

        assertThat(liste).contains(enCours.toString()).doesNotContain(horsTranche.toString());
    }

    @Test
    void r7_liste_trancheNonHabiliteeDemandee_absente() throws Exception {
        RequestPostProcessor qui = lecteur("f");

        List<String> temoin = ids(MARS + "&trancheId=" + trancheHabilitee.getId(), qui);
        List<String> liste = ids(MARS + "&trancheId=" + trancheNonHabilitee.getId(), qui);

        assertThat(temoin).contains(enCours.toString());
        assertThat(liste).doesNotContain(horsTranche.toString());
    }

    @Test
    void r7_liste_sousArticle_absent() throws Exception {
        List<String> liste = ids(MARS, lecteur("g"));

        assertThat(liste).contains(enCours.toString()).doesNotContain(sousArticle.toString());
    }

    @Test
    void r7_fiche_trancheNonHabilitee_404() throws Exception {
        RequestPostProcessor qui = lecteur("h");

        Reponse temoin = appels.appeler(get(MODIFICATIONS + "/" + enCours), qui);
        Reponse r = appels.appeler(get(MODIFICATIONS + "/" + horsTranche), qui);

        assertThat(temoin.statut()).as("témoin : " + temoin.corps()).isEqualTo(200);
        assertThat(r.statut()).as(r.corps()).isEqualTo(404);
    }

    @Test
    void r7_put_trancheNonHabilitee_404_inchange() throws Exception {
        RequestPostProcessor qui = editeur("i");

        Reponse temoin = modifier(enCours, ENCOURS, qui);
        Reponse r = modifier(horsTranche, ACCEPTEE, qui);

        assertThat(temoin.statut()).as("témoin : " + temoin.corps()).isEqualTo(200);
        assertThat(r.statut()).as(r.corps()).isEqualTo(404);
        assertThat(fixture.lire(STATUT, horsTranche)).isEqualTo(ENCOURS);
    }

    // ------------------------------------------------------------------ R6 / D6 recalcul du dossier

    @Test
    void r6_put_etatCompte_recalculeLeDossier() throws Exception {
        Reponse r = modifier(enCours, ACCEPTEE, editeur("j"));

        assertThat(r.statut()).as(r.corps()).isEqualTo(200);
        assertAll(
                () -> assertThat(fixture.lire(STATUT, enCours)).isEqualTo(ACCEPTEE),
                () -> assertThat(fixture.montant(MONTANT_DOSSIER, dossier.getId())).isEqualByComparingTo("12000"),
                () -> assertThat(fixture.montant("SELECT ModificationsAmountHT FROM ComFolder WHERE PKey = ?",
                        dossier.getId())).isEqualByComparingTo("10000"));
    }

    @Test
    void r6_put_etatNonCompte_nAjouteRienAuDossier() throws Exception {
        Reponse r = modifier(enCours, "REFUSE_PAR_LE_SERVICE_TECHNIQUE", editeur("k"));

        assertThat(r.statut()).as(r.corps()).isEqualTo(200);
        assertAll(
                () -> assertThat(fixture.lire(STATUT, enCours)).isEqualTo("REFUSE_PAR_LE_SERVICE_TECHNIQUE"),
                () -> assertThat(fixture.montant(MONTANT_DOSSIER, dossier.getId())).isEqualByComparingTo(BigDecimal.ZERO));
    }

    /** Réponse 4 : le PUT garde les contrôles du chemin dossier, dont le périmètre commercial du dossier (404 comme là-bas). */
    @Test
    void reponse4_put_dossierHorsPerimetreCommercial_refuse_inchange() throws Exception {
        CfgTranche autreProjet = vente.tranche("TMP-" + jeton, true);
        UUID horsPerimetre = fixture.modification(fixture.dossier("DMP-" + jeton, autreProjet, false), ENCOURS,
                "Hors périmètre " + jeton, LE_10_MARS);
        Compte c = compte("l", DROIT_MODIFS_LIRE, DROIT_MODIFS_MODIFIER);
        fixture.habiliterTranche(c.user(), autreProjet.getId());
        fixture.habiliterProjetCommercial(c.user(), trancheHabilitee.getCfgProject().getId());

        Reponse temoin = modifier(enCours, ENCOURS, c.connexion());
        Reponse r = modifier(horsPerimetre, ACCEPTEE, c.connexion());

        assertThat(temoin.statut()).as("témoin : " + temoin.corps()).isEqualTo(200);
        assertThat(r.statut()).as(r.corps()).isEqualTo(404);
        assertThat(fixture.lire(STATUT, horsPerimetre)).isEqualTo(ENCOURS);
    }

    // ------------------------------------------------------------------ D4 droits

    @Test
    void d4_liste_sansModificationList_403() throws Exception {
        Reponse temoin = appels.appeler(get(MODIFICATIONS + "?" + MARS), lecteur("m"));
        Reponse r = appels.appeler(get(MODIFICATIONS + "?" + MARS), compte("n", DROIT_MODIFS_MODIFIER).connexion());

        assertThat(temoin.statut()).as("témoin : " + temoin.corps()).isEqualTo(200);
        assertThat(r.statut()).as(r.corps()).isEqualTo(403);
    }

    @Test
    void d4_fiche_sansModificationList_403() throws Exception {
        Reponse temoin = appels.appeler(get(MODIFICATIONS + "/" + enCours), lecteur("o"));
        Reponse r = appels.appeler(get(MODIFICATIONS + "/" + enCours), compte("p", DROIT_MODIFS_MODIFIER).connexion());

        assertThat(temoin.statut()).as("témoin : " + temoin.corps()).isEqualTo(200);
        assertThat(r.statut()).as(r.corps()).isEqualTo(403);
    }

    @Test
    void d4_put_sansToolNew_403_inchange() throws Exception {
        Reponse temoin = modifier(refusee, "REFUSE_PAR_LE_CLIENT", editeur("q"));
        Reponse r = modifier(enCours, ACCEPTEE, lecteur("r"));

        assertThat(temoin.statut()).as("témoin : " + temoin.corps()).isEqualTo(200);
        assertThat(r.statut()).as(r.corps()).isEqualTo(403);
        assertThat(fixture.lire(STATUT, enCours)).isEqualTo(ENCOURS);
    }

    // ------------------------------------------------------------------ D5 ni création ni suppression

    @Test
    void d5_niPostNiDelete() throws Exception {
        RequestPostProcessor qui = editeur("s");

        Reponse temoin = appels.appeler(get(MODIFICATIONS + "?" + MARS), qui);
        Reponse creation = appels.envoyer(post(MODIFICATIONS), saisie(ENCOURS), qui);
        Reponse suppression = appels.appeler(delete(MODIFICATIONS + "/" + enCours), qui);

        assertThat(temoin.statut()).as("témoin : " + temoin.corps()).isEqualTo(200);
        assertThat(creation.statut()).as(creation.corps()).isIn(404, 405);
        assertThat(suppression.statut()).as(suppression.corps()).isIn(404, 405);
        assertThat(fixture.compter("SELECT COUNT(*) FROM ComModification WHERE PKey = ?", enCours)).isEqualTo(1);
    }
}
