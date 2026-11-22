package com.tsix.immocrm.controller.tresorerie;

import com.tsix.immocrm.controller.tresorerie.AppelsTresorerie.Reponse;
import com.tsix.immocrm.fixtures.FixtureAchat;
import com.tsix.immocrm.fixtures.FixtureDecaissements;
import com.tsix.immocrm.fixtures.FixtureDecaissements.Drapeaux;
import com.tsix.immocrm.fixtures.FixtureDecaissements.Saisie;
import com.tsix.immocrm.fixtures.FixtureTresorerie;
import com.tsix.immocrm.fixtures.FixtureTresorerie.Reglement;
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

import static com.tsix.immocrm.controller.tresorerie.AppelsSaisieDecaissement.POINTED;
import static com.tsix.immocrm.controller.tresorerie.AppelsSaisieDecaissement.corps;
import static com.tsix.immocrm.controller.tresorerie.AppelsSaisieDecaissement.detail;
import static com.tsix.immocrm.controller.tresorerie.AppelsSaisieDecaissement.ligne;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * FEATURE-n3bis-saisie-reglements.md §5 : chaque refus R-a…R-l et chaque invariant ajouté, avec son appel témoin 2xx
 * (la même saisie corrigée passe : le refus vient bien de la règle).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class DecaissementRefusTest {

    private static final String MSG_SANS_LIGNE =
            "Vous ne pouvez pas enregistrer la pièce de règlement sans aucune lignes affectés !";
    private static final String MSG_RESTE = "Le reste à affecter doit être >= 0 !";
    private static final String MSG_LIGNE_ZERO =
            "Vous ne pouvez pas enregistrer la pièce de règlement avec des lignes qui ont un montant affecté=0 !";
    private static final String MSG_DEVISE = "Toutes les affectations doivent se faire avec la même devise de référence !";
    private static final String MSG_POINTAGE_SANS_COMPTE =
            "Si votre pièce possède une date de pointage, il est obligatoire de lui affecter un compte !";
    private static final String MSG_ECHEANCE = "La date échéance doit être supérieure ou égale à la date de la pièce !";
    private static final String MSG_POINTAGE_STATUT =
            "Si la pièce possède une date de pointage il faut que le statut soit égal à pointé !";
    private static final String MSG_DATE_POINTAGE = "La date de pointage est obligatoire !";
    private static final String MSG_RAPPROCHEMENT = "Vous ne pouvez pas pointer une pièce à une date inférieure à la date"
            + " du dernier rapprochement du compte affecté";
    private static final String MSG_REFERENCE = "Le champ référence est obligatoire !";
    private static final String MSG_NOTAIRE = "Le champ Notaire/Avocat est obligatoire !";
    private static final String MSG_COMPTE = "Le champ compte est obligatoire !";

    private static final String DATE_DECAISSEMENT = "dateDecaissement";
    private static final String STATUT = "statut";
    private static final String COMPTE_ID = "compteId";
    private static final String TYPE_ID = "typeId";
    private static final String REFERENCE = "reference";
    private static final LocalDate APRES_RAPPROCHEMENT = LocalDate.of(2026, 7, 15);

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureDecaissements fixture;
    @Autowired private FixtureTresorerie tresorerie;
    @Autowired private FixtureAchat achat;

    private AppelsSaisieDecaissement appels;
    private Saisie s;
    private RequestPostProcessor qui;
    private UUID pa;

    @BeforeEach
    void setUp() {
        appels = new AppelsSaisieDecaissement(mockMvc);
        s = fixture.saisie("DRF");
        qui = connecte(fixture.compte(s, "DRFA", FixtureDecaissements.DROIT_LIRE, FixtureDecaissements.DROIT_NOUVEAU),
                FixtureTresorerie.ROLE);
        pa = fixture.piece(s, "-FA", s.c().projet(), "1000");
    }

    /** Saisie valide de 100 affectés à {@code pa} ; référence propre au test. */
    private Map<String, Object> cent(String suffixe) {
        return corps(s, s.code(suffixe), "100", List.of(ligne(pa, "100")));
    }

    private void refuse(Map<String, Object> corps, int statut, String message) throws Exception {
        Reponse r = appels.creer(qui, corps);
        assertThat(r.statut()).as(r.corps()).isEqualTo(statut);
        assertThat(detail(r)).as(r.corps()).contains(message);
    }

    private void refuse400(Map<String, Object> corps) throws Exception {
        Reponse r = appels.creer(qui, corps);
        assertThat(r.statut()).as(r.corps()).isEqualTo(400);
    }

    // ------------------------------------------------------------------ R-a … R-h

    @Test
    void rA_sansLigne_400() throws Exception {
        refuse(corps(s, s.code("-RA"), "100", List.of()), 400, MSG_SANS_LIGNE);

        appels.creerOk(qui, cent("-RA2"));
    }

    @Test
    void rB_lignesSuperieuresAuMontant_400() throws Exception {
        UUID pb = fixture.piece(s, "-FB", s.c().projet(), "1000");

        refuse(corps(s, s.code("-RB"), "500", List.of(ligne(pa, "300"), ligne(pb, "300"))), 400, MSG_RESTE);

        appels.creerOk(qui, corps(s, s.code("-RB2"), "600", List.of(ligne(pa, "300"), ligne(pb, "300"))));
    }

    @Test
    void rC_ligneAZero_400() throws Exception {
        refuse(corps(s, s.code("-RC"), "100", List.of(ligne(pa, "0"))), 400, MSG_LIGNE_ZERO);

        appels.creerOk(qui, cent("-RC2"));
    }

    @Test
    void rD_pieceDansUneAutreDevise_400() throws Exception {
        UUID pDevise = fixture.piece(s, "-FD", s.c().projet(), "1000");
        achat.poser(pDevise, "StlCurrencyId", achat.devise("E" + s.c().jeton()));

        refuse(corps(s, s.code("-RD"), "100", List.of(ligne(pDevise, "100"))), 400, MSG_DEVISE);

        appels.creerOk(qui, cent("-RD2"));
    }

    @Test
    void rE_pointageSansCompte_400() throws Exception {
        Map<String, Object> corps = pointe(cent("-RE"), APRES_RAPPROCHEMENT);
        corps.remove(COMPTE_ID);

        refuse(corps, 400, MSG_POINTAGE_SANS_COMPTE);

        appels.creerOk(qui, pointe(cent("-RE2"), APRES_RAPPROCHEMENT));
    }

    @Test
    void rF_echeanceAvantDatePiece_400() throws Exception {
        Map<String, Object> corps = cent("-RF");
        corps.put("dateDocument", LocalDate.of(2026, 4, 10));
        corps.put("dateEcheance", LocalDate.of(2026, 4, 1));

        refuse(corps, 400, MSG_ECHEANCE);

        Map<String, Object> temoin = cent("-RF2");
        temoin.put("dateDocument", LocalDate.of(2026, 4, 10));
        temoin.put("dateEcheance", LocalDate.of(2026, 4, 10));
        appels.creerOk(qui, temoin);
    }

    @Test
    void rG_datePointageAvecStatutEnCours_400() throws Exception {
        Map<String, Object> corps = cent("-RG");
        corps.put(DATE_DECAISSEMENT, APRES_RAPPROCHEMENT);

        refuse(corps, 400, MSG_POINTAGE_STATUT);

        appels.creerOk(qui, pointe(cent("-RG2"), APRES_RAPPROCHEMENT));
    }

    @Test
    void rH_pointeSansDatePointage_400() throws Exception {
        Map<String, Object> corps = cent("-RH");
        corps.put(STATUT, POINTED);

        refuse(corps, 400, MSG_DATE_POINTAGE);

        appels.creerOk(qui, cent("-RH2"));
    }

    // ------------------------------------------------------------------ R-i rapprochement (D5)

    @Test
    void rI_pointageALaDateDuDernierRapprochement_409() throws Exception {
        fixture.rapprochement(s.compte(), LocalDate.of(2026, 6, 30));

        Reponse r = appels.creer(qui, pointe(cent("-RI"), LocalDate.of(2026, 6, 30)));

        assertThat(r.statut()).as(r.corps()).isEqualTo(409);
        assertThat(detail(r)).as(r.corps()).startsWith(MSG_RAPPROCHEMENT);
        appels.creerOk(qui, pointe(cent("-RI2"), APRES_RAPPROCHEMENT));
    }

    // ------------------------------------------------------------------ R-k champs obligatoires du type

    @Test
    void rK_referenceObligatoireParLeType_400() throws Exception {
        UUID type = fixture.type("REF-" + s.c().jeton(), new Drapeaux(true, false, false, false));
        Map<String, Object> corps = cent("-RK");
        corps.put(TYPE_ID, type);
        corps.remove(REFERENCE);

        refuse(corps, 400, MSG_REFERENCE);

        Map<String, Object> temoin = cent("-RK2");
        temoin.put(TYPE_ID, type);
        appels.creerOk(qui, temoin);
    }

    @Test
    void rK_notaireObligatoireParLeType_400() throws Exception {
        UUID type = fixture.type("NOT-" + s.c().jeton(), new Drapeaux(false, false, false, true));
        Map<String, Object> corps = cent("-RKN");
        corps.put(TYPE_ID, type);

        refuse(corps, 400, MSG_NOTAIRE);

        Map<String, Object> temoin = cent("-RKN2");
        temoin.put(TYPE_ID, type);
        temoin.put("notaireId", achat.fournisseur("NOT-" + s.c().jeton(), "Notaire " + s.c().jeton()));
        appels.creerOk(qui, temoin);
    }

    @Test
    void rK_compteObligatoireParLeType_400() throws Exception {
        UUID type = fixture.type("BQ-" + s.c().jeton(), new Drapeaux(false, false, true, false));
        Map<String, Object> corps = cent("-RKC");
        corps.put(TYPE_ID, type);
        corps.remove(COMPTE_ID);

        refuse(corps, 400, MSG_COMPTE);

        Map<String, Object> temoin = cent("-RKC2");
        temoin.put(TYPE_ID, type);
        appels.creerOk(qui, temoin);
    }

    // ------------------------------------------------------------------ R-l unicité de la référence

    @Test
    void rL_referenceUnique_memeCompte_409_autreCompte_201() throws Exception {
        UUID type = fixture.type("UNI-" + s.c().jeton(), new Drapeaux(false, true, false, false));
        String reference = s.code("-RLU");
        Map<String, Object> premier = corps(s, reference, "100", List.of(ligne(pa, "100")));
        premier.put(TYPE_ID, type);
        appels.creerOk(qui, premier);

        Map<String, Object> doublon = corps(s, reference, "100", List.of(ligne(pa, "100")));
        doublon.put(TYPE_ID, type);
        refuse(doublon, 409, reference);

        Map<String, Object> autreCompte = corps(s, reference, "100", List.of(ligne(pa, "100")));
        autreCompte.put(TYPE_ID, type);
        autreCompte.put(COMPTE_ID, fixture.compteBancaire("CPT3-" + s.c().jeton(), s.c().societe(), s.devise()));
        appels.creerOk(qui, autreCompte);
    }

    // ------------------------------------------------------------------ Q « statut CANCELED → 400 »

    @Test
    void statutAnnuleALaCreation_400() throws Exception {
        Map<String, Object> corps = cent("-RCA");
        corps.put(STATUT, "CANCELED");

        refuse400(corps);

        appels.creerOk(qui, cent("-RCA2"));
    }

    // ------------------------------------------------------------------ invariants ajoutés (400)

    @Test
    void invariant_montantNul_400() throws Exception {
        refuse400(corps(s, s.code("-IM"), "0", List.of(ligne(pa, "100"))));

        appels.creerOk(qui, cent("-IM2"));
    }

    @Test
    void invariant_pieceDUnAutreFournisseur_400() throws Exception {
        UUID autre = achat.fournisseur("FOU2-" + s.c().jeton(), "Autre " + s.c().jeton());
        UUID pAutre = fixture.piece(s, "-FAUT", s.c().projet(), autre, s.c().modulePrj(), "1000");

        refuse400(corps(s, s.code("-IF"), "100", List.of(ligne(pAutre, "100"))));

        appels.creerOk(qui, cent("-IF2"));
    }

    @Test
    void invariant_memePieceDeuxFois_400() throws Exception {
        refuse400(corps(s, s.code("-ID"), "100", List.of(ligne(pa, "50"), ligne(pa, "50"))));

        appels.creerOk(qui, cent("-ID2"));
    }

    @Test
    void invariant_compteDUneAutreSociete_400() throws Exception {
        Map<String, Object> corps = cent("-IC");
        corps.put(COMPTE_ID, s.compteAutreSociete());

        refuse400(corps);

        appels.creerOk(qui, cent("-IC2"));
    }

    @Test
    void invariant_ligneSuperieureAuResteDeLaPiece_400() throws Exception {
        UUID existant = tresorerie.reglement(Reglement.debit(s.code("-RX"), s.c(), "900"));
        tresorerie.ligne(existant, pa, "900");

        refuse400(corps(s, s.code("-IR"), "150", List.of(ligne(pa, "150"))));

        appels.creerOk(qui, cent("-IR2"));
    }

    @Test
    void invariant_resteDeLaPiece_ignoreLesReglementsAnnules() throws Exception {
        UUID annule = tresorerie.reglement(Reglement.debit(s.code("-RXA"), s.c(), "900").statut(FixtureTresorerie.ANNULE));
        tresorerie.ligne(annule, pa, "900");

        appels.creerOk(qui, corps(s, s.code("-IRA"), "150", List.of(ligne(pa, "150"))));
    }

    @Test
    void invariant_auPut_lAncienneLigneDuReglementNestPasComptee() throws Exception {
        UUID p500 = fixture.piece(s, "-F500", s.c().projet(), "500");
        String reference = s.code("-IP");
        UUID id = appels.creerOk(qui, corps(s, reference, "500", List.of(ligne(p500, "500"))));

        Map<String, Object> identique = corps(s, reference, "500", List.of(ligne(p500, "500")));
        identique.put("notes", "Note de modification");
        Reponse ok = appels.modifier(qui, id, identique);
        assertThat(ok.statut()).as(ok.corps()).isEqualTo(200);

        Reponse trop = appels.modifier(qui, id, corps(s, reference, "501", List.of(ligne(p500, "501"))));
        assertThat(trop.statut()).as(trop.corps()).isEqualTo(400);
    }

    private static Map<String, Object> pointe(Map<String, Object> corps, LocalDate date) {
        corps.put(STATUT, POINTED);
        corps.put(DATE_DECAISSEMENT, date);
        return corps;
    }
}
