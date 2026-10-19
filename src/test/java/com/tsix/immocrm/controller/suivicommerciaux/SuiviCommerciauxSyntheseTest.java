package com.tsix.immocrm.controller.suivicommerciaux;

import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComActionMessage;
import com.tsix.immocrm.entities.com.ComActionMessageSubCategory;
import com.tsix.immocrm.entities.com.ComActionMessageType;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.com.ComProspection;
import com.tsix.immocrm.fixtures.FixtureSuiviCommerciaux;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static com.tsix.immocrm.fixtures.FixtureSuiviCommerciaux.JOUR;
import static com.tsix.immocrm.fixtures.FixtureSuiviCommerciaux.a;
import static com.tsix.immocrm.fixtures.FixtureSuiviCommerciaux.cle;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * {@code GET /api/suivi-commerciaux/synthese} — FEATURE-suivi-commerciaux.md R1–R7, R10. Chaque test bâtit l'activité
 * d'un commercial propre et filtre sur lui ({@code commercialIds}) : les autres lignes de la base n'interfèrent pas.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SuiviCommerciauxSyntheseTest {

    private static final String URL = "/api/suivi-commerciaux/synthese";
    private static final String COMMERCIAL = "Commercial";
    private static final String ADMINISTRATEUR = "Administrateur";
    private static final String COMMERCIAL_IDS = "commercialIds";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private FixtureSuiviCommerciaux fixture;

    private String prefixe;
    private CfgTranche tranche;
    private AdmUser commercial;
    private ComActionMessageType sortant;
    private ComActionMessageType entrant;
    private ComActionMessageType visite;
    private ComActionMessageSubCategory rdvPris;

    @BeforeEach
    void setUp() {
        prefixe = FixtureSuiviCommerciaux.prefixe("SCS");
        tranche = fixture.tranche("TR-" + prefixe, true);
        commercial = fixture.compte("scs.com." + prefixe, COMMERCIAL);
        sortant = fixture.appelSortant();
        entrant = fixture.appelEntrant();
        visite = fixture.visite();
        rdvPris = fixture.resultat(FixtureSuiviCommerciaux.RDV_PRIS);
    }

    private static String ligne(AdmUser compte, LocalDate jour) {
        return "$.lignes[?(@.commercialCle=='" + cle(compte) + "' && @.jour=='" + jour + "')]";
    }

    private ResultActions synthese(AdmUser compte, LocalDate du, LocalDate au) throws Exception {
        fixture.relire();
        return mockMvc.perform(get(URL).param("du", du.toString()).param("au", au.toString())
                        .param(COMMERCIAL_IDS, cle(compte))
                        .with(connecte(commercial, COMMERCIAL)))
                .andExpect(status().isOk());
    }

    private ResultActions syntheseDuJour() throws Exception {
        return synthese(commercial, JOUR, JOUR);
    }

    private ComProspection prospection(String suffixe) {
        return fixture.prospection(tranche, prefixe + "-" + suffixe);
    }

    private ComActionMessage appel(ComProspection p, ComActionMessageSubCategory resultat) {
        return fixture.interaction(commercial, p, sortant, resultat, a(JOUR, 10, 0));
    }

    // ------------------------------------------------------------------ R1 — appels passés / reçus, auteur

    @Test
    void r1_appelsPassesEtRecus_comptesParType() throws Exception {
        ComProspection p = prospection("P1");
        appel(p, null);
        appel(p, rdvPris);
        fixture.interaction(commercial, p, entrant, null, a(JOUR, 11, 0));

        syntheseDuJour()
                .andExpect(jsonPath(ligne(commercial, JOUR) + ".appelsPasses").value(2))
                .andExpect(jsonPath(ligne(commercial, JOUR) + ".appelsRecus").value(1));
    }

    /** L'auteur est le compte de l'interaction ({@code AdmUserId}), pas l'employé concerné ni le titulaire. */
    @Test
    void r1_appelSaisiParUnChef_estCompteChezLeChef() throws Exception {
        AdmUser chef = fixture.compte("scs.chef." + prefixe, "Commercial Chef");
        AdmUser admin = fixture.compte("scs.adm." + prefixe, ADMINISTRATEUR);
        ComProspection p = fixture.prospection(tranche, fixture.personne(prefixe + "-P1"), commercial, a(JOUR, 9, 0));
        ComActionMessage m = fixture.interaction(chef, p, sortant, rdvPris, a(JOUR, 10, 0));
        m.setCfgEmployee(commercial.getCfgEmployee());
        fixture.enregistrer(m);
        fixture.relire();

        mockMvc.perform(get(URL).param("du", JOUR.toString()).param("au", JOUR.toString())
                        .param(COMMERCIAL_IDS, cle(chef), cle(commercial))
                        .with(connecte(admin, ADMINISTRATEUR)))
                .andExpect(status().isOk())
                .andExpect(jsonPath(ligne(chef, JOUR) + ".appelsPasses").value(1))
                .andExpect(jsonPath(ligne(chef, JOUR) + ".personnesJointes").value(1))
                .andExpect(jsonPath(ligne(commercial, JOUR) + ".appelsPasses").value(0));
    }

    @Test
    void r1_actionSysteme_nestPasComptee() throws Exception {
        ComActionMessage m = appel(prospection("P1"), rdvPris);
        m.setUserName("Système");
        fixture.enregistrer(m);

        syntheseDuJour()
                .andExpect(jsonPath(ligne(commercial, JOUR) + ".appelsPasses").value(0))
                .andExpect(jsonPath(ligne(commercial, JOUR) + ".contactsJoints").value(0));
    }

    // ------------------------------------------------------------------ R2 — contact joint

    @ParameterizedTest
    @ValueSource(strings = {"Prospect ne répond pas", "Prospect injoignable ou Boite vocale",
            "Téléphone constamment occupé", FixtureSuiviCommerciaux.NUMERO_ERRONE,
            "Client injoignable ou Boite vocale", "Le client ne répond pas", FixtureSuiviCommerciaux.A_RAPPELER})
    void r2_resultatNonJoint_appelCompteMaisPasJoint(String code) throws Exception {
        appel(prospection("P1"), fixture.resultat(code));

        syntheseDuJour()
                .andExpect(jsonPath(ligne(commercial, JOUR) + ".appelsPasses").value(1))
                .andExpect(jsonPath(ligne(commercial, JOUR) + ".contactsJoints").value(0))
                .andExpect(jsonPath(ligne(commercial, JOUR) + ".personnesJointes").value(0));
    }

    @Test
    void r2_aRappelerParLeCommercial_nonJoint() throws Exception {
        appel(prospection("P1"), fixture.resultat(FixtureSuiviCommerciaux.A_RAPPELER));

        syntheseDuJour().andExpect(jsonPath(ligne(commercial, JOUR) + ".contactsJoints").value(0));
    }

    /** Le code de la base porte un espace final : « Numéro erroné  ». */
    @Test
    void r2_numeroErroneAvecEspaceFinal_nonJoint() throws Exception {
        appel(prospection("P1"), fixture.resultat(FixtureSuiviCommerciaux.NUMERO_ERRONE));

        syntheseDuJour().andExpect(jsonPath(ligne(commercial, JOUR) + ".contactsJoints").value(0));
    }

    @Test
    void r2_resultatAbsent_nonJoint() throws Exception {
        appel(prospection("P1"), null);

        syntheseDuJour()
                .andExpect(jsonPath(ligne(commercial, JOUR) + ".appelsPasses").value(1))
                .andExpect(jsonPath(ligne(commercial, JOUR) + ".contactsJoints").value(0));
    }

    @ParameterizedTest
    @ValueSource(strings = {FixtureSuiviCommerciaux.RDV_PRIS, "Prospect occupé, à rappeler plus tard",
            "Le prospect veut faire une visite du projet"})
    void r2_autreResultat_joint(String code) throws Exception {
        appel(prospection("P1"), fixture.resultat(code));

        syntheseDuJour()
                .andExpect(jsonPath(ligne(commercial, JOUR) + ".contactsJoints").value(1))
                .andExpect(jsonPath(ligne(commercial, JOUR) + ".personnesJointes").value(1));
    }

    @Test
    void r2_appelEntrantJoint_compte() throws Exception {
        fixture.interaction(commercial, prospection("P1"), entrant, rdvPris, a(JOUR, 10, 0));

        syntheseDuJour()
                .andExpect(jsonPath(ligne(commercial, JOUR) + ".appelsRecus").value(1))
                .andExpect(jsonPath(ligne(commercial, JOUR) + ".appelsPasses").value(0))
                .andExpect(jsonPath(ligne(commercial, JOUR) + ".contactsJoints").value(1))
                .andExpect(jsonPath(ligne(commercial, JOUR) + ".personnesJointes").value(1));
    }

    @Test
    void r2_visite_nestPasUnContactJoint() throws Exception {
        fixture.interaction(commercial, prospection("P1"), visite, rdvPris, a(JOUR, 10, 0));
        appel(prospection("P2"), null);

        syntheseDuJour()
                .andExpect(jsonPath(ligne(commercial, JOUR) + ".contactsJoints").value(0))
                .andExpect(jsonPath(ligne(commercial, JOUR) + ".personnesJointes").value(0));
    }

    // ------------------------------------------------------------------ R3 — personnes distinctes

    @Test
    void r3_troisAppelsJointsMemePersonneMemeJour_unePersonne() throws Exception {
        ComProspection p = prospection("P1");
        appel(p, rdvPris);
        appel(p, rdvPris);
        fixture.interaction(commercial, p, sortant, rdvPris, a(JOUR, 16, 0));

        syntheseDuJour()
                .andExpect(jsonPath(ligne(commercial, JOUR) + ".contactsJoints").value(3))
                .andExpect(jsonPath(ligne(commercial, JOUR) + ".personnesJointes").value(1));
    }

    @Test
    void r3_deuxPersonnes_deux() throws Exception {
        ComProspection p2 = prospection("P2");
        appel(prospection("P1"), rdvPris);
        appel(p2, rdvPris);
        appel(p2, rdvPris);

        syntheseDuJour()
                .andExpect(jsonPath(ligne(commercial, JOUR) + ".contactsJoints").value(3))
                .andExpect(jsonPath(ligne(commercial, JOUR) + ".personnesJointes").value(2));
    }

    /** Personne = {@code ISNULL(CfgTierId, ComProspectionId)} : sans tiers, la prospection fait la personne. */
    @Test
    void r3_sansTiers_laProspectionFaitLaPersonne() throws Exception {
        ComProspection p1 = prospection("P1");
        ComProspection p2 = prospection("P2");
        for (ComProspection p : new ComProspection[] {p1, p1, p2}) {
            ComActionMessage m = appel(p, rdvPris);
            m.setCfgTier(null);
            fixture.enregistrer(m);
        }

        syntheseDuJour().andExpect(jsonPath(ligne(commercial, JOUR) + ".personnesJointes").value(2));
    }

    @Test
    void r3_memePersonneDeuxJours_unePersonneChaqueJour() throws Exception {
        ComProspection p = prospection("P1");
        appel(p, rdvPris);
        fixture.interaction(commercial, p, sortant, rdvPris, a(JOUR.plusDays(1), 10, 0));

        synthese(commercial, JOUR, JOUR.plusDays(1))
                .andExpect(jsonPath(ligne(commercial, JOUR) + ".personnesJointes").value(1))
                .andExpect(jsonPath(ligne(commercial, JOUR.plusDays(1)) + ".personnesJointes").value(1));
    }

    // ------------------------------------------------------------------ R4 — jour

    @Test
    void r4_dimanche_compte() throws Exception {
        fixture.interaction(commercial, prospection("P1"), sortant, rdvPris,
                a(FixtureSuiviCommerciaux.DIMANCHE, 11, 0));

        synthese(commercial, FixtureSuiviCommerciaux.DIMANCHE, FixtureSuiviCommerciaux.DIMANCHE)
                .andExpect(jsonPath(ligne(commercial, FixtureSuiviCommerciaux.DIMANCHE) + ".personnesJointes").value(1));
    }

    /** {@code DocDate} est l'heure murale de Casablanca : 23 h 30 reste ce jour-là, 00 h 30 est le lendemain. */
    @Test
    void r4_jourDeLHeureMurale_sansDecalage() throws Exception {
        fixture.interaction(commercial, prospection("P1"), sortant, rdvPris, a(JOUR, 23, 30));
        fixture.interaction(commercial, prospection("P2"), sortant, rdvPris, a(JOUR.plusDays(1), 0, 30));

        synthese(commercial, JOUR, JOUR.plusDays(1))
                .andExpect(jsonPath(ligne(commercial, JOUR) + ".personnesJointes").value(1))
                .andExpect(jsonPath(ligne(commercial, JOUR.plusDays(1)) + ".personnesJointes").value(1));
    }

    @Test
    void r4_interactionHorsPeriode_nonComptee() throws Exception {
        fixture.interaction(commercial, prospection("P1"), sortant, rdvPris, a(JOUR.minusDays(1), 12, 0));
        appel(prospection("P2"), null);

        syntheseDuJour()
                .andExpect(jsonPath(ligne(commercial, JOUR) + ".appelsPasses").value(1))
                .andExpect(jsonPath(ligne(commercial, JOUR) + ".personnesJointes").value(0));
    }

    // ------------------------------------------------------------------ R5 — visites

    @Test
    void r5_visiteSansArticle_comptee_prospectionEtVenteSepares() throws Exception {
        fixture.interaction(commercial, prospection("P1"), visite, null, a(JOUR, 10, 0));
        ComFolder dossier = fixture.vente(prefixe + "-V1", commercial, tranche, null, null, a(JOUR, 9, 0),
                new BigDecimal("1000000"));
        fixture.visiteVente(commercial, dossier, visite, FixtureSuiviCommerciaux.CATEGORIE_VENTE_2222, a(JOUR, 14, 0));
        fixture.visiteVente(commercial, dossier, visite, FixtureSuiviCommerciaux.CATEGORIE_VENTE_3333, a(JOUR, 15, 0));

        syntheseDuJour()
                .andExpect(jsonPath(ligne(commercial, JOUR) + ".visitesProspection").value(1))
                .andExpect(jsonPath(ligne(commercial, JOUR) + ".visitesVente").value(2))
                .andExpect(jsonPath(ligne(commercial, JOUR) + ".appelsPasses").value(0));
    }

    // ------------------------------------------------------------------ R6 — visites prévues

    /**
     * Le jour prévu est {@code AdmEvent.StartHour}, à défaut {@code AlertValidityDate} ; seuls les résultats à date
     * planifiée comptent ; une Visite sur la même prospection ce jour-là la consigne.
     */
    @Test
    void r6_visitesPrevues_etNonConsignees() throws Exception {
        ComProspection parEvenement = prospection("P1");
        ComProspection parAlerte = prospection("P2");
        ComProspection autreResultat = prospection("P3");
        LocalDate veille = JOUR.minusDays(3);
        fixture.planifier(fixture.interaction(commercial, parEvenement, sortant, rdvPris, a(veille, 10, 0)),
                a(JOUR.plusDays(5), 10, 0), a(JOUR, 10, 0));
        fixture.planifier(fixture.interaction(commercial, parAlerte, sortant, rdvPris, a(veille, 11, 0)),
                a(JOUR, 15, 0), null);
        fixture.planifier(fixture.interaction(commercial, autreResultat, sortant,
                fixture.resultat("Le client ne répond pas"), a(veille, 12, 0)), a(JOUR, 15, 0), null);
        fixture.interaction(commercial, parAlerte, visite, null, a(JOUR, 15, 30));

        syntheseDuJour()
                .andExpect(jsonPath(ligne(commercial, JOUR) + ".visitesPrevues").value(2))
                .andExpect(jsonPath(ligne(commercial, JOUR) + ".visitesPrevuesNonConsignees").value(1));
    }

    @Test
    void r6_evenementPrimeSurLAlerte() throws Exception {
        LocalDate jourAlerte = JOUR.plusDays(5);
        fixture.planifier(fixture.interaction(commercial, prospection("P1"), sortant, rdvPris, a(JOUR, 10, 0)),
                a(jourAlerte, 10, 0), a(JOUR.plusDays(2), 10, 0));
        fixture.interaction(commercial, prospection("P2"), sortant, null, a(jourAlerte, 9, 0));

        synthese(commercial, jourAlerte, jourAlerte)
                .andExpect(jsonPath(ligne(commercial, jourAlerte) + ".appelsPasses").value(1))
                .andExpect(jsonPath(ligne(commercial, jourAlerte) + ".visitesPrevues").value(0));
    }

    // ------------------------------------------------------------------ R7 — objectif

    private void joindre(int personnes) {
        for (int i = 0; i < personnes; i++) {
            appel(prospection("N" + i), rdvPris);
        }
    }

    @Test
    void r7_soixantePersonnes_atteint() throws Exception {
        joindre(60);

        syntheseDuJour()
                .andExpect(jsonPath("$.objectif").value(60))
                .andExpect(jsonPath(ligne(commercial, JOUR) + ".personnesJointes").value(60))
                .andExpect(jsonPath(ligne(commercial, JOUR) + ".atteint").value(true));
    }

    @Test
    void r7_cinquanteNeufPersonnes_nonAtteint() throws Exception {
        joindre(59);

        syntheseDuJour()
                .andExpect(jsonPath(ligne(commercial, JOUR) + ".personnesJointes").value(59))
                .andExpect(jsonPath(ligne(commercial, JOUR) + ".atteint").value(false));
    }

    @Test
    void r7_objectifModifie_leSeuilSuit() throws Exception {
        AdmUser admin = fixture.compte("scs.adm." + prefixe, ADMINISTRATEUR);
        mockMvc.perform(put("/api/suivi-commerciaux/objectif").with(connecte(admin, ADMINISTRATEUR))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"objectifPersonnesJointesJour\":3}"))
                .andExpect(status().isOk());
        joindre(3);
        ComProspection autreJour = prospection("X");
        fixture.interaction(commercial, autreJour, sortant, rdvPris, a(JOUR.plusDays(1), 10, 0));
        fixture.interaction(commercial, prospection("Y"), sortant, rdvPris, a(JOUR.plusDays(1), 11, 0));

        synthese(commercial, JOUR, JOUR.plusDays(1))
                .andExpect(jsonPath("$.objectif").value(3))
                .andExpect(jsonPath(ligne(commercial, JOUR) + ".atteint").value(true))
                .andExpect(jsonPath(ligne(commercial, JOUR.plusDays(1)) + ".personnesJointes").value(2))
                .andExpect(jsonPath(ligne(commercial, JOUR.plusDays(1)) + ".atteint").value(false));
    }

    // ------------------------------------------------------------------ R10 — commercial sans activité

    @Test
    void r10_sansActivite_avecFiltre_ligneAZeros() throws Exception {
        syntheseDuJour()
                .andExpect(jsonPath(ligne(commercial, JOUR) + ".appelsPasses").value(0))
                .andExpect(jsonPath(ligne(commercial, JOUR) + ".appelsRecus").value(0))
                .andExpect(jsonPath(ligne(commercial, JOUR) + ".contactsJoints").value(0))
                .andExpect(jsonPath(ligne(commercial, JOUR) + ".personnesJointes").value(0))
                .andExpect(jsonPath(ligne(commercial, JOUR) + ".visitesProspection").value(0))
                .andExpect(jsonPath(ligne(commercial, JOUR) + ".visitesVente").value(0))
                .andExpect(jsonPath(ligne(commercial, JOUR) + ".atteint").value(false));
    }

    @Test
    void r10_sansActivite_sansFiltre_ligneOmise() throws Exception {
        fixture.relire();

        mockMvc.perform(get(URL).param("du", JOUR.toString()).param("au", JOUR.toString())
                        .with(connecte(commercial, COMMERCIAL)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lignes[?(@.commercialCle=='" + cle(commercial) + "')]").isEmpty());
    }

    // ------------------------------------------------------------------ §3 — période

    @Test
    void periodeDePlusDe92Jours_rend400() throws Exception {
        mockMvc.perform(get(URL).param("du", "2026-01-01").param("au", "2026-06-30")
                        .with(connecte(commercial, COMMERCIAL)))
                .andExpect(status().isBadRequest());
    }
}
