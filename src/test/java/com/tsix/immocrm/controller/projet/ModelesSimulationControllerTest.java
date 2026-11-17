package com.tsix.immocrm.controller.projet;

import com.tsix.immocrm.controller.projet.AppelsProjet.Reponse;
import com.tsix.immocrm.fixtures.CorpsJson;
import com.tsix.immocrm.fixtures.FixtureLotsParite;
import com.tsix.immocrm.fixtures.FixtureModelesSimulation;
import com.tsix.immocrm.fixtures.FixtureModelesSimulation.Ligne;
import com.tsix.immocrm.fixtures.FixtureProjetLots;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.tsix.immocrm.controller.projet.PrevisionsReelSupport.CATEGORIE_ID;
import static com.tsix.immocrm.controller.projet.PrevisionsReelSupport.detail;
import static com.tsix.immocrm.controller.projet.PrevisionsReelSupport.saisieModele;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static com.tsix.immocrm.fixtures.FixtureLotPrevisions.DROIT_PREVISIONS;
import static com.tsix.immocrm.fixtures.FixtureLotFiche.DROIT_ESTIMATION;
import static com.tsix.immocrm.fixtures.FixtureModelesSimulation.DROIT_MODELES;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * FEATURE-lot-previsions-reel.md §3 et §4 M1-M4 : CRUD {@code /api/projet/modeles-simulation}, validation OMRAN (M2), unicité du
 * code, cascade des lignes, 409 si une prévision de faisabilité cite le modèle, filtre catégorie, droits.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ModelesSimulationControllerTest {

    private static final String COMMERCIAL = "Commercial";
    private static final String ID = "$.id";
    private static final String MESSAGE_LIGNES = "Vous devez saisir au moins deux ligne de prévision avec obligatoirement un "
            + "pourcentage d'avancement du temps égale à 0% et 100%";
    private static final String MESSAGE_PAIEMENT = "le total des % de paiement doit être égale à 100%";
    private static final Ligne ZERO = new Ligne("0", "0", "0");
    private static final Ligne MOITIE = new Ligne("0.5", "0.4", "0.5");
    private static final Ligne FIN = new Ligne("1", "1", "0.5");

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureLotsParite parite;
    @Autowired private FixtureModelesSimulation modeles;

    private PrevisionsReelSupport appels;
    private String jeton;
    private RequestPostProcessor admin;
    private int rang;

    @BeforeEach
    void setUp() {
        modeles.tables();
        appels = new PrevisionsReelSupport(mockMvc);
        jeton = FixtureProjetLots.jeton();
        admin = connecte(parite.administrateur("mod.sim." + jeton), "Administrateur");
    }

    private String code() {
        rang++;
        return "MSM" + rang + "-" + jeton;
    }

    private RequestPostProcessor avec(String... droits) {
        rang++;
        return connecte(parite.avecDroits("mod.sim" + rang + "." + jeton, List.of(droits)), COMMERCIAL);
    }

    private static CorpsJson valide(String code, UUID categorie) {
        return saisieModele(code, "Modèle " + code, categorie, ZERO, MOITIE, FIN);
    }

    private static UUID id(Reponse r) {
        return UUID.fromString(String.valueOf(r.valeur(ID)));
    }

    private static List<Double> nombres(Reponse r, String chemin) {
        Object v = r.valeur(chemin);
        List<?> l = v instanceof List<?> liste ? liste : List.of();
        return l.stream().map(x -> Double.valueOf(String.valueOf(x))).toList();
    }

    private static List<Double> colonne(List<Map<String, Object>> lignes, String nom) {
        return lignes.stream().map(l -> new BigDecimal(String.valueOf(l.get(nom))).doubleValue()).toList();
    }

    // ------------------------------------------------------------------ M1 / M3 : création, lecture, modification

    @Test
    void m1_creer_201_puisDetail_codeDescriptionCategorieLignes() throws Exception {
        UUID categorie = modeles.categorie("MSK-" + jeton);
        String code = code();

        Reponse cree = appels.creer(admin, valide(code, categorie));

        assertThat(cree.statut()).as(cree.corps()).isEqualTo(201);
        Reponse lu = appels.modele(admin, id(cree));
        assertThat(lu.statut()).as(lu.corps()).isEqualTo(200);
        SoftAssertions s = new SoftAssertions();
        s.assertThat(lu.valeur("$.code")).isEqualTo(code);
        s.assertThat(lu.valeur("$.description")).isEqualTo("Modèle " + code);
        s.assertThat(String.valueOf(lu.valeur("$.categorieId"))).isEqualToIgnoringCase(categorie.toString());
        s.assertThat(lu.valeur("$.categorie")).isEqualTo("MSK-" + jeton);
        s.assertThat(nombres(lu, "$.lignes[*].temps")).containsExactly(0.0, 0.5, 1.0);
        s.assertThat(nombres(lu, "$.lignes[*].avancement")).containsExactly(0.0, 0.4, 1.0);
        s.assertThat(nombres(lu, "$.lignes[*].paiement")).containsExactly(0.0, 0.5, 0.5);
        s.assertAll();
    }

    @Test
    void m1_creer_ecritEnteteEtLignesEnBase() throws Exception {
        String code = code();

        Reponse cree = appels.creer(admin, valide(code, null));

        assertThat(cree.statut()).as(cree.corps()).isEqualTo(201);
        UUID id = id(cree);
        SoftAssertions s = new SoftAssertions();
        s.assertThat(modeles.entete(id)).singleElement().satisfies(e -> assertThat(e).containsEntry("Code", code));
        s.assertThat(colonne(modeles.lignes(id), "PointTime")).containsExactly(0.0, 0.5, 1.0);
        s.assertThat(colonne(modeles.lignes(id), "PointPayment")).containsExactly(0.0, 0.5, 0.5);
        s.assertAll();
    }

    @Test
    void m3_modifier_remplaceLesLignes() throws Exception {
        String code = code();
        UUID id = modeles.modele(code, "Avant", null, ZERO, MOITIE, FIN);

        Reponse r = appels.modifier(admin, id, saisieModele(code, "Après", null, ZERO, new Ligne("1", "1", "1")));

        assertThat(r.statut()).as(r.corps()).isEqualTo(200);
        SoftAssertions s = new SoftAssertions();
        s.assertThat(modeles.entete(id)).singleElement().satisfies(e -> assertThat(e).containsEntry("Description", "Après"));
        s.assertThat(colonne(modeles.lignes(id), "PointTime")).containsExactly(0.0, 1.0);
        s.assertAll();
    }

    @Test
    void m1_detail_modeleInconnu_404() throws Exception {
        Reponse r = appels.modele(admin, UUID.randomUUID());
        Reponse temoin = appels.modele(admin, modeles.modele(code(), "Témoin", null, ZERO, FIN));

        SoftAssertions s = new SoftAssertions();
        s.assertThat(r.statut()).as(r.corps()).isEqualTo(404);
        s.assertThat(temoin.statut()).as(temoin.corps()).isEqualTo(200);
        s.assertAll();
    }

    // ------------------------------------------------------------------ M2 : validation

    @Test
    void m2_uneSeuleLigne_400_messageOmran() throws Exception {
        Reponse r = appels.creer(admin, saisieModele(code(), "Une ligne", null, new Ligne("1", "1", "1")));

        assertThat(r.statut()).as(r.corps()).isEqualTo(400);
        assertThat(detail(r)).contains(MESSAGE_LIGNES);
    }

    @Test
    void m2_tempsMaxDifferentDeUn_400_messageOmran() throws Exception {
        Reponse r = appels.creer(admin, saisieModele(code(), "Temps 0,9", null, ZERO, new Ligne("0.9", "1", "1")));

        assertThat(r.statut()).as(r.corps()).isEqualTo(400);
        assertThat(detail(r)).contains(MESSAGE_LIGNES);
    }

    @Test
    void m2_sommePaiementsSousLaBorne_400_messageOmran() throws Exception {
        Reponse r = appels.creer(admin, saisieModele(code(), "Σ 0,98", null, ZERO, new Ligne("1", "1", "0.98")));

        assertThat(r.statut()).as(r.corps()).isEqualTo(400);
        assertThat(detail(r)).contains(MESSAGE_PAIEMENT);
    }

    @Test
    void m2_sommePaiementsAuDessusDeLaBorne_400_messageOmran() throws Exception {
        Reponse r = appels.creer(admin, saisieModele(code(), "Σ 1,02", null, ZERO, new Ligne("1", "1", "1.02")));

        assertThat(r.statut()).as(r.corps()).isEqualTo(400);
        assertThat(detail(r)).contains(MESSAGE_PAIEMENT);
    }

    // Bornes incluses [0,99 ; 1,01] : 1,009 accepté.
    @Test
    void m2_sommePaiementsDansLaTolerance_201() throws Exception {
        Reponse r = appels.creer(admin, saisieModele(code(), "Σ 1,009", null, ZERO, new Ligne("1", "1", "1.009")));

        assertThat(r.statut()).as(r.corps()).isEqualTo(201);
    }

    @Test
    void m2_codeVide_400() throws Exception {
        Reponse r = appels.creer(admin, saisieModele("", "Sans code", null, ZERO, FIN));

        assertThat(r.statut()).as(r.corps()).isEqualTo(400);
    }

    @Test
    void m2_descriptionVide_400() throws Exception {
        Reponse r = appels.creer(admin, saisieModele(code(), "", null, ZERO, FIN));

        assertThat(r.statut()).as(r.corps()).isEqualTo(400);
    }

    @Test
    void m2_modifier_lignesInvalides_400_lignesInchangees() throws Exception {
        String code = code();
        UUID id = modeles.modele(code, "Valide", null, ZERO, MOITIE, FIN);

        Reponse r = appels.modifier(admin, id, saisieModele(code, "Invalide", null, new Ligne("1", "1", "1")));

        assertThat(r.statut()).as(r.corps()).isEqualTo(400);
        assertThat(colonne(modeles.lignes(id), "PointTime")).containsExactly(0.0, 0.5, 1.0);
    }

    // ------------------------------------------------------------------ M1 : unicité du code

    @Test
    void m1_creer_codeDejaPris_409() throws Exception {
        String code = code();
        modeles.modele(code, "Premier", null, ZERO, FIN);

        Reponse r = appels.creer(admin, valide(code, null));

        assertThat(r.statut()).as(r.corps()).isEqualTo(409);
        assertThat(modeles.modelesDeCode(code)).isEqualTo(1);
    }

    @Test
    void m1_modifier_codeDUnAutreModele_409() throws Exception {
        String pris = code();
        modeles.modele(pris, "Autre", null, ZERO, FIN);
        UUID id = modeles.modele(code(), "Moi", null, ZERO, FIN);

        Reponse r = appels.modifier(admin, id, valide(pris, null));

        assertThat(r.statut()).as(r.corps()).isEqualTo(409);
    }

    @Test
    void m1_modifier_gardeSonPropreCode_200() throws Exception {
        String code = code();
        UUID id = modeles.modele(code, "Moi", null, ZERO, FIN);

        Reponse r = appels.modifier(admin, id, valide(code, null));

        assertThat(r.statut()).as(r.corps()).isEqualTo(200);
    }

    // ------------------------------------------------------------------ M4 : suppression

    @Test
    void m4_supprimer_204_lignesEnCascade() throws Exception {
        UUID id = modeles.modele(code(), "À supprimer", null, ZERO, MOITIE, FIN);

        Reponse r = appels.supprimer(admin, id);

        assertThat(r.statut()).as(r.corps()).isEqualTo(204);
        SoftAssertions s = new SoftAssertions();
        s.assertThat(modeles.entete(id)).isEmpty();
        s.assertThat(modeles.lignes(id)).isEmpty();
        s.assertAll();
    }

    @Test
    void m4_supprimer_modeleCiteParUnePrevisionDeFaisabilite_409_modeleConserve() throws Exception {
        UUID id = modeles.modele(code(), "Cité", null, ZERO, FIN);
        modeles.previsionFaisabilite(id);

        Reponse r = appels.supprimer(admin, id);

        assertThat(r.statut()).as(r.corps()).isEqualTo(409);
        SoftAssertions s = new SoftAssertions();
        s.assertThat(modeles.entete(id)).hasSize(1);
        s.assertThat(modeles.lignes(id)).hasSize(2);
        s.assertAll();
    }

    @Test
    void m4_supprimer_modeleInconnu_404() throws Exception {
        Reponse r = appels.supprimer(admin, UUID.randomUUID());

        assertThat(r.statut()).as(r.corps()).isEqualTo(404);
    }

    // ------------------------------------------------------------------ M4 / M5 : liste et filtre catégorie

    @Test
    void m4_liste_filtreCategorie_seulsLesModelesDeLaCategorie() throws Exception {
        UUID a = modeles.categorie("MSA-" + jeton);
        UUID b = modeles.categorie("MSB-" + jeton);
        UUID deA = modeles.modele(code(), "De A", a, ZERO, FIN);
        UUID deB = modeles.modele(code(), "De B", b, ZERO, FIN);

        Reponse filtree = appels.modeles(admin, CATEGORIE_ID, a.toString());
        Reponse toute = appels.modeles(admin);

        assertThat(filtree.statut()).as(filtree.corps()).isEqualTo(200);
        assertThat(toute.statut()).as(toute.corps()).isEqualTo(200);
        SoftAssertions s = new SoftAssertions();
        s.assertThat(minuscules(filtree.ids("$[*].id"))).contains(deA.toString()).doesNotContain(deB.toString());
        s.assertThat(minuscules(toute.ids("$[*].id"))).contains(deA.toString(), deB.toString());
        s.assertAll();
    }

    private static List<String> minuscules(List<String> ids) {
        return ids.stream().map(i -> i.toLowerCase(java.util.Locale.ROOT)).toList();
    }

    // ------------------------------------------------------------------ droits (§3 Droits)

    @Test
    void droits_creer_sansDroitModeles_403_avecDroit_201() throws Exception {
        Reponse refus = appels.creer(avec(DROIT_PREVISIONS), valide(code(), null));
        Reponse accord = appels.creer(avec(DROIT_MODELES), valide(code(), null));

        SoftAssertions s = new SoftAssertions();
        s.assertThat(refus.statut()).as(refus.corps()).isEqualTo(403);
        s.assertThat(accord.statut()).as(accord.corps()).isEqualTo(201);
        s.assertAll();
    }

    @Test
    void droits_modifier_sansDroitModeles_403_avecDroit_200() throws Exception {
        String code = code();
        UUID id = modeles.modele(code, "Droits", null, ZERO, FIN);

        Reponse refus = appels.modifier(avec(DROIT_PREVISIONS), id, valide(code, null));
        Reponse accord = appels.modifier(avec(DROIT_MODELES), id, valide(code, null));

        SoftAssertions s = new SoftAssertions();
        s.assertThat(refus.statut()).as(refus.corps()).isEqualTo(403);
        s.assertThat(accord.statut()).as(accord.corps()).isEqualTo(200);
        s.assertAll();
    }

    @Test
    void droits_supprimer_sansDroitModeles_403_modeleConserve() throws Exception {
        UUID id = modeles.modele(code(), "Droits", null, ZERO, FIN);

        Reponse refus = appels.supprimer(avec(DROIT_PREVISIONS), id);
        int restant = modeles.entete(id).size();
        Reponse accord = appels.supprimer(avec(DROIT_MODELES), id);

        SoftAssertions s = new SoftAssertions();
        s.assertThat(refus.statut()).as(refus.corps()).isEqualTo(403);
        s.assertThat(restant).isEqualTo(1);
        s.assertThat(accord.statut()).as(accord.corps()).isEqualTo(204);
        s.assertAll();
    }

    // GET liste et détail : droit des modèles OU droit d'édition des prévisions du lot.
    @Test
    void droits_lecture_droitPrevisionsSuffit_200() throws Exception {
        UUID id = modeles.modele(code(), "Lecture", null, ZERO, FIN);
        RequestPostProcessor lecteur = avec(DROIT_PREVISIONS);

        Reponse liste = appels.modeles(lecteur);
        Reponse fiche = appels.modele(lecteur, id);

        SoftAssertions s = new SoftAssertions();
        s.assertThat(liste.statut()).as(liste.corps()).isEqualTo(200);
        s.assertThat(fiche.statut()).as(fiche.corps()).isEqualTo(200);
        s.assertAll();
    }

    @Test
    void droits_lecture_droitModelesSuffit_200() throws Exception {
        Reponse liste = appels.modeles(avec(DROIT_MODELES));

        assertThat(liste.statut()).as(liste.corps()).isEqualTo(200);
    }

    @Test
    void droits_lecture_sansAucunDesDeuxDroits_403() throws Exception {
        UUID id = modeles.modele(code(), "Lecture", null, ZERO, FIN);
        RequestPostProcessor sansDroit = avec(DROIT_ESTIMATION);

        Reponse liste = appels.modeles(sansDroit);
        Reponse fiche = appels.modele(sansDroit, id);
        Reponse temoin = appels.modeles(admin);

        SoftAssertions s = new SoftAssertions();
        s.assertThat(liste.statut()).as(liste.corps()).isEqualTo(403);
        s.assertThat(fiche.statut()).as(fiche.corps()).isEqualTo(403);
        s.assertThat(temoin.statut()).as(temoin.corps()).isEqualTo(200);
        s.assertAll();
    }

    // Ordre : 403 avant 400 (corps invalide, compte sans droit) ; témoin : même corps, admin → 400.
    @Test
    void ordre_sansDroit_corpsInvalide_403AvantLe400() throws Exception {
        CorpsJson invalide = saisieModele(code(), "Invalide", null, new Ligne("1", "1", "1"));

        Reponse refus = appels.creer(avec(DROIT_PREVISIONS), invalide);
        Reponse temoin = appels.creer(admin, invalide);

        SoftAssertions s = new SoftAssertions();
        s.assertThat(refus.statut()).as(refus.corps()).isEqualTo(403);
        s.assertThat(temoin.statut()).as(temoin.corps()).isEqualTo(400);
        s.assertAll();
    }
}
