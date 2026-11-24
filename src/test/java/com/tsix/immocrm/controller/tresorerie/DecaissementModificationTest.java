package com.tsix.immocrm.controller.tresorerie;

import com.tsix.immocrm.controller.tresorerie.AppelsTresorerie.Reponse;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.fixtures.FixtureAchat;
import com.tsix.immocrm.fixtures.FixtureDecaissements;
import com.tsix.immocrm.fixtures.FixtureDecaissements.Drapeaux;
import com.tsix.immocrm.fixtures.FixtureDecaissements.Saisie;
import com.tsix.immocrm.fixtures.FixtureTresorerie;
import com.tsix.immocrm.fixtures.FixtureTresorerie.Reglement;
import org.assertj.core.api.SoftAssertions;
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
import static com.tsix.immocrm.controller.tresorerie.AppelsSaisieDecaissement.ligne;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * FEATURE-n3bis-saisie-reglements.md : modification d'un décaissement ({@code PUT /decaissements/{id}}) — effet de bord
 * {@code IsAccounted} (parité {@code BeforeUpdate}), lignes masquées conservées (D7), {@code CanModifyCompany},
 * règlement de virement interne non modifiable (Q4).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class DecaissementModificationTest {

    private static final String COMPTABILISE = "comptabilise";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureDecaissements fixture;
    @Autowired private FixtureTresorerie tresorerie;
    @Autowired private FixtureAchat achat;

    private AppelsSaisieDecaissement appels;
    private Saisie s;
    private AdmUser utilisateur;
    private RequestPostProcessor qui;
    private UUID pa;
    private String reference;
    private UUID id;

    @BeforeEach
    void setUp() throws Exception {
        appels = new AppelsSaisieDecaissement(mockMvc);
        s = fixture.saisie("DMO");
        utilisateur = fixture.compte(s, "DMOA", FixtureDecaissements.DROIT_LIRE, FixtureDecaissements.DROIT_NOUVEAU);
        qui = connecte(utilisateur, FixtureTresorerie.ROLE);
        pa = fixture.piece(s, "-FA", s.c().projet(), "1000");
        reference = s.code("-R");
        id = appels.creerOk(qui, origine());
        fixture.marquerComptabilise(id);
    }

    /** Le corps de création, renvoyé tel quel au PUT. */
    private Map<String, Object> origine() {
        return corps(s, reference, "500", List.of(ligne(pa, "300")));
    }

    private Reponse modifierOk(Map<String, Object> corps) throws Exception {
        Reponse r = appels.modifier(qui, id, corps);
        assertThat(r.statut()).as(r.corps()).isEqualTo(200);
        return r;
    }

    private Object comptabilise() {
        return fixture.reglementEnBase(id).get(0).get(COMPTABILISE);
    }

    // ------------------------------------------------------------------ IsAccounted (effets de bord)

    @Test
    void isAccounted_notesSeulesModifiees_resteA1() throws Exception {
        Map<String, Object> corps = origine();
        corps.put("notes", "Simple note");

        modifierOk(corps);

        assertThat(comptabilise()).isEqualTo(true);
    }

    @Test
    void isAccounted_resteAAffecterChange_repasseA0() throws Exception {
        modifierOk(corps(s, reference, "500", List.of(ligne(pa, "200"))));

        assertThat(comptabilise()).isEqualTo(false);
    }

    @Test
    void isAccounted_typeChange_repasseA0() throws Exception {
        Map<String, Object> corps = origine();
        corps.put("typeId", fixture.type("T2-" + s.c().jeton(), new Drapeaux(false, false, false, false)));

        modifierOk(corps);

        assertThat(comptabilise()).isEqualTo(false);
    }

    @Test
    void isAccounted_compteChange_repasseA0() throws Exception {
        Map<String, Object> corps = origine();
        corps.put("compteId", fixture.compteBancaire("CPT3-" + s.c().jeton(), s.c().societe(), s.devise()));

        modifierOk(corps);

        assertThat(comptabilise()).isEqualTo(false);
    }

    @Test
    void isAccounted_pointageChange_repasseA0() throws Exception {
        Map<String, Object> corps = origine();
        corps.put("statut", POINTED);
        corps.put("dateDecaissement", LocalDate.of(2026, 7, 15));

        modifierOk(corps);

        assertThat(comptabilise()).isEqualTo(false);
    }

    // ------------------------------------------------------------------ D7 lignes masquées conservées

    @Test
    void d7_ligneHorsHabilitation_conserveeEtCompteeDansLeReste() throws Exception {
        UUID masque = tresorerie.reglement(Reglement.debit(s.code("-RM"), s.c(), "500"));
        tresorerie.ligne(masque, pa, "100");
        UUID pInvisible = fixture.piece(s, "-FINV", s.c().projetNonHabilite(), "1000");
        tresorerie.ligne(masque, pInvisible, "50");

        Reponse r = appels.modifier(qui, masque, corps(s, s.code("-RM"), "500", List.of(ligne(pa, "200"))));

        assertThat(r.statut()).as(r.corps()).isEqualTo(200);
        Reponse fiche = appels.fiche(qui, masque);
        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(fixture.lignesEnBase(masque)).as("ligne masquée gardée").hasSize(2);
            soft.assertThat(fiche.nombre("$.lignesMasquees")).isEqualByComparingTo("1");
            soft.assertThat(fiche.nombre("$.decaissement.montantAffecte")).as("200 + 50").isEqualByComparingTo("250");
            soft.assertThat(fiche.nombre("$.decaissement.resteAAffecter")).as("500 − 250").isEqualByComparingTo("250");
        });
    }

    @Test
    void d7_rB_ligneMasqueeCompteeDansLeControleDuReste_400() throws Exception {
        UUID masque = tresorerie.reglement(Reglement.debit(s.code("-RN"), s.c(), "500"));
        tresorerie.ligne(masque, pa, "100");
        UUID pInvisible = fixture.piece(s, "-FINW", s.c().projetNonHabilite(), "1000");
        tresorerie.ligne(masque, pInvisible, "50");

        Reponse r = appels.modifier(qui, masque, corps(s, s.code("-RN"), "230", List.of(ligne(pa, "200"))));

        assertThat(r.statut()).as("200 + 50 masqués > 230 : " + r.corps()).isEqualTo(400);
    }

    // ------------------------------------------------------------------ CanModifyCompany

    /** Corps passé à l'autre société : son compte, une pièce de cette société sur un projet habilité. */
    private Map<String, Object> versAutreSociete(AdmUser... comptes) {
        UUID projet = achat.projet(s.c().jeton() + "-PC", s.autreSociete());
        for (AdmUser compte : comptes) {
            achat.habiliterSociete(compte, s.autreSociete());
            achat.habiliter(compte, projet);
        }
        UUID piece = tresorerie.piece(s.code("-FS2"), s.autreSociete(), projet, s.c().fournisseur(),
                s.c().fournisseurNom(), s.c().modulePrj(), "1000");
        achat.poser(piece, "StlCurrencyId", s.devise());
        achat.poser(piece, "BuyDocumentStatusId", achat.statut("ST" + s.code("-FS2"), true, false, 1));
        Map<String, Object> corps = corps(s, reference, "500", List.of(ligne(piece, "300")));
        corps.put("societeId", s.autreSociete());
        corps.put("compteId", s.compteAutreSociete());
        return corps;
    }

    @Test
    void canModifyCompany_sansLeDroit_changerDeSociete_403() throws Exception {
        Map<String, Object> corps = versAutreSociete(utilisateur);

        Reponse r = appels.modifier(qui, id, corps);

        assertThat(r.statut()).as(r.corps()).isEqualTo(403);
    }

    @Test
    void canModifyCompany_avecLeDroit_changerDeSociete_200() throws Exception {
        AdmUser autorise = fixture.compte(s, "DMOS", FixtureDecaissements.DROIT_LIRE, FixtureDecaissements.DROIT_NOUVEAU,
                FixtureDecaissements.DROIT_CHANGER_SOCIETE);
        Map<String, Object> corps = versAutreSociete(autorise);

        Reponse r = appels.modifier(connecte(autorise, FixtureTresorerie.ROLE), id, corps);

        assertThat(r.statut()).as(r.corps()).isEqualTo(200);
    }

    // ------------------------------------------------------------------ Q4 virement interne

    @Test
    void q4_reglementDeVirementInterne_nonModifiable_409() throws Exception {
        Map<String, Object> corps = origine();
        corps.put("notes", "Avant le virement");
        modifierOk(corps);
        fixture.lierVirementInterne(id);

        Reponse r = appels.modifier(qui, id, origine());

        assertThat(r.statut()).as(r.corps()).isEqualTo(409);
    }
}
