package com.tsix.immocrm.controller.achat;

import com.tsix.immocrm.controller.achat.AppelsSaisieAchat.NatureSaisie;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.fixtures.FixtureAchat;
import com.tsix.immocrm.fixtures.FixtureAchat.Cadre;
import com.tsix.immocrm.fixtures.FixtureAchat.LigneAchat;
import com.tsix.immocrm.fixtures.FixtureAchat.Saisie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static com.tsix.immocrm.controller.achat.AppelsEcrituresAchat.ROLE_JWT;
import static com.tsix.immocrm.controller.achat.AppelsEcrituresAchat.appeler;
import static com.tsix.immocrm.controller.achat.AppelsEcrituresAchat.corps;
import static com.tsix.immocrm.controller.achat.AppelsEcrituresAchat.detail;
import static com.tsix.immocrm.controller.achat.AppelsEcrituresAchat.statut;
import static com.tsix.immocrm.controller.achat.AppelsSaisieAchat.*;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static org.assertj.core.api.Assertions.assertThat;

/** Contre-revue A2b-2a (FEATURE-achat-fiche-creation.md) : cas limites non couverts par les tests verrouillés. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PieceAchatSaisieContreRevueTest {

    private static final BigDecimal PU = new BigDecimal("100");

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureAchat fixture;

    private Cadre c;
    private Saisie s;
    private AdmUser redacteur;

    @BeforeEach
    void setUp() {
        c = fixture.cadre("ACR");
        s = fixture.configurationSaisie(c.jeton());
        redacteur = fixture.compteHabilite(c, "ACR", ROLE_JWT, NOUVEAU_DEVIS, NOUVEAU_BC, NOUVEAU_BL, NOUVEAU_FACTURE);
        fixture.sansPersonnel(redacteur);
    }

    private void ligneEnBase(UUID piece, String code, BigDecimal quantite) {
        fixture.ligne(piece, new LigneAchat(1, code, "Article " + code, "U", quantite, PU, TVA_20, ZERO, ZERO,
                new BigDecimal("120")));
    }

    /**
     * OMRAN {@code Delivery.cs:Verif} l.52-77 rejoue le contrôle de quantités à chaque enregistrement d'un BL lié à un BC
     * (les 9 BL de prod le sont tous) ; le PUT doit refuser un BL qui dépasse la quantité commandée.
     */
    @Test
    void blLieAUnBc_quantiteDuCorpsDepasseLeBc_409() throws Exception {
        UUID bc = fixture.piece(c, "-BCL", FixtureAchat.BON_COMMANDE);
        ligneEnBase(bc, "ART-1", BigDecimal.TEN);
        UUID bl = fixture.piece(c, "-BL", FixtureAchat.BON_LIVRAISON);
        fixture.poser(bl, "StrEntityId", s.gestion(NatureSaisie.BL.gestion));
        fixture.poser(bl, "BuyDocumentStatusId", s.enCours());
        fixture.poser(bl, "BuyOrderId", bc);
        ligneEnBase(bl, "ART-1", BigDecimal.ONE);
        fixture.parametreBooleen(FixtureAchat.LIMITE_QUANTITES, true);

        MvcResult r = appeler(mockMvc, modifier(bl, corpsValide(c, s, FixtureAchat.BON_LIVRAISON)
                .avec("lignes", List.of(ligne("ART-1", "12", "100", s.categorie())))), connecte(redacteur, ROLE_JWT));

        assertThat(statut(r)).as(corps(r)).isEqualTo(409);
        assertThat(detail(r)).isEqualTo("La quantité de l'article 'ART-1' dépasse celle définie dans le bon de commande !");
    }

    /** Création : une ligne portant un id (copie d'une autre pièce, client API) ne doit pas tomber en 500. */
    @Test
    void creation_ligneAvecId_pas500() throws Exception {
        MvcResult r = appeler(mockMvc, creer(corpsValide(c, s, FixtureAchat.BON_COMMANDE)
                .avec("lignes", List.of(ligne("ART-1", "1", "100", s.categorie()).avec("id", UUID.randomUUID())))),
                connecte(redacteur, ROLE_JWT));

        assertThat(statut(r)).as(corps(r)).isIn(201, 400);
    }
}
