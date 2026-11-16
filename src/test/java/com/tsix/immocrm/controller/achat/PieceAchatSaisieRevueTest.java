package com.tsix.immocrm.controller.achat;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.controller.achat.AppelsSaisieAchat.NatureSaisie;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.fixtures.CorpsJson;
import com.tsix.immocrm.fixtures.FixtureAchat;
import com.tsix.immocrm.fixtures.FixtureAchat.Cadre;
import com.tsix.immocrm.fixtures.FixtureAchat.Saisie;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.tsix.immocrm.controller.achat.AppelsEcrituresAchat.ADMINISTRATEUR;
import static com.tsix.immocrm.controller.achat.AppelsEcrituresAchat.ROLE_JWT;
import static com.tsix.immocrm.controller.achat.AppelsEcrituresAchat.appeler;
import static com.tsix.immocrm.controller.achat.AppelsEcrituresAchat.corps;
import static com.tsix.immocrm.controller.achat.AppelsEcrituresAchat.fiche;
import static com.tsix.immocrm.controller.achat.AppelsEcrituresAchat.statut;
import static com.tsix.immocrm.controller.achat.AppelsSaisieAchat.*;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * A2b-2a E-C6 et Vérification n° 6 (FEATURE-achat-fiche-creation.md) : {@code actions.peutModifier} vrai ⇔ {@code PUT} ∉ {403, 409}
 * et {@code naturesCreables} ∋ N ⇔ {@code POST} de N ≠ 403, sur un jeu de comptes (droits variés) × pièces (natures, lot,
 * lecture seule, gérées ailleurs).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PieceAchatSaisieRevueTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureAchat fixture;

    private Cadre c;
    private Saisie s;
    private UUID lot;
    private int rang;

    private record PieceJeu(String libelle, UUID id, String nature, UUID lot) {
    }

    @BeforeEach
    void setUp() {
        c = fixture.cadre("ARV");
        s = fixture.configurationSaisie(c.jeton());
        lot = fixture.lot("L-" + c.jeton(), c.projet());
    }

    private MvcResult par(AdmUser compte, MockHttpServletRequestBuilder requete) throws Exception {
        return appeler(mockMvc, requete, connecte(compte, ROLE_JWT));
    }

    private List<AdmUser> comptes() {
        List<AdmUser> comptes = new ArrayList<>(List.of(
                fixture.compteHabilite(c, "ARV-TOUS", ROLE_JWT, NOUVEAU_DEVIS, NOUVEAU_BC, NOUVEAU_BL, NOUVEAU_FACTURE),
                fixture.compteHabilite(c, "ARV-BC", ROLE_JWT, NOUVEAU_BC),
                fixture.compteHabilite(c, "ARV-LOT", ROLE_JWT, NOUVEAU_BC_LOT, NOUVEAU_FACTURE_LOT_IGNORE),
                fixture.compteHabilite(c, "ARV-RIEN", ROLE_JWT),
                fixture.compteHabilite(c, "ARV-ADM", ADMINISTRATEUR)));
        comptes.forEach(fixture::sansPersonnel);
        return comptes;
    }

    private UUID piece(NatureSaisie n, UUID statut, UUID lotPiece) {
        UUID id = fixture.piece(c, "-" + n.name() + (rang++), n.code);
        fixture.poser(id, "StrEntityId", s.gestion(n.gestion));
        if (statut != null) {
            fixture.poser(id, "BuyDocumentStatusId", statut);
        }
        if (lotPiece != null) {
            fixture.poser(id, "PrjMarketId", lotPiece);
        }
        return id;
    }

    private List<PieceJeu> pieces() {
        List<PieceJeu> pieces = new ArrayList<>();
        pieces.add(new PieceJeu("devis", piece(NatureSaisie.DEVIS, null, null), FixtureAchat.DEVIS, null));
        for (NatureSaisie n : List.of(NatureSaisie.BC, NatureSaisie.BL, NatureSaisie.FACTURE)) {
            pieces.add(new PieceJeu(n.name() + " en cours", piece(n, s.enCours(), null), n.code, null));
            pieces.add(new PieceJeu(n.name() + " lecture seule", piece(n, s.valideDt(), null), n.code, null));
        }
        pieces.add(new PieceJeu("BC à lot", piece(NatureSaisie.BC, s.enCours(), lot), FixtureAchat.BON_COMMANDE, lot));
        pieces.add(new PieceJeu("facture à lot", piece(NatureSaisie.FACTURE, s.enCours(), lot), FixtureAchat.FACTURE, lot));
        UUID decompte = piece(NatureSaisie.FACTURE, s.enCours(), null);
        fixture.decompte(decompte);
        pieces.add(new PieceJeu("facture de décompte", decompte, FixtureAchat.FACTURE, null));
        UUID frais = piece(NatureSaisie.FACTURE, s.enCours(), null);
        fixture.poser(frais, "ReferenceType", "LAND_EXPENSE");
        pieces.add(new PieceJeu("frais de terrain", frais, FixtureAchat.FACTURE, null));
        pieces.add(new PieceJeu("document à régler", fixture.piece(c, "-DAR", FixtureAchat.DOCUMENT_A_REGLER),
                FixtureAchat.FACTURE, null));
        return pieces;
    }

    /** Vérification 6 / E-C6 : {@code peutModifier} lu sur la fiche ⇔ le {@code PUT} n'est ni 403 ni 409. */
    @Test
    void v6_peutModifier_equivaut_putNiRefuseNiVerrouille() throws Exception {
        List<AdmUser> comptes = comptes();
        List<PieceJeu> pieces = pieces();
        SoftAssertions soft = new SoftAssertions();
        int paires = 0;

        for (AdmUser compte : comptes) {
            for (PieceJeu p : pieces) {
                MvcResult f = par(compte, fiche(p.id()));
                assertThat(statut(f)).as(compte.getCode() + " / " + p.libelle() + " : " + corps(f)).isEqualTo(200);
                boolean peutModifier = JsonPath.read(corps(f), "$.actions.peutModifier");
                CorpsJson corps = corpsValide(c, s, p.nature()).avec("lotId", p.lot());
                int put = statut(par(compte, modifier(p.id(), corps)));
                soft.assertThat(put != 403 && put != 409).as("%s / %s : peutModifier=%s, PUT=%d", compte.getCode(), p.libelle(),
                        peutModifier, put).isEqualTo(peutModifier);
                paires++;
            }
        }

        assertThat(paires).isEqualTo(comptes.size() * pieces.size());
        soft.assertAll();
    }

    /** Vérification 6 / E-C6 : {@code naturesCreables} ∋ N ⇔ le {@code POST} de N n'est pas 403. */
    @Test
    void v6_naturesCreables_equivaut_postNonInterdit() throws Exception {
        SoftAssertions soft = new SoftAssertions();
        int paires = 0;

        for (AdmUser compte : comptes()) {
            List<String> creables = JsonPath.read(corps(par(compte, referentiels())), "$.naturesCreables");
            for (NatureSaisie n : NatureSaisie.values()) {
                int post = statut(par(compte, creer(corpsValide(c, s, n.code))));
                soft.assertThat(post != 403).as("%s / %s : naturesCreables=%s, POST=%d", compte.getCode(), n, creables, post)
                        .isEqualTo(creables.contains(n.code));
                paires++;
            }
        }

        assertThat(paires).isEqualTo(5 * NatureSaisie.values().length);
        soft.assertAll();
    }
}
