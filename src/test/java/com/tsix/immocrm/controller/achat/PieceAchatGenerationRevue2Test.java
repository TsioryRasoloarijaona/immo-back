package com.tsix.immocrm.controller.achat;

import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.fixtures.FixtureAchat;
import com.tsix.immocrm.fixtures.FixtureAchat.Cadre;
import com.tsix.immocrm.fixtures.FixtureAchat.Saisie;
import com.tsix.immocrm.fixtures.FixtureGenerationAchat;
import com.tsix.immocrm.fixtures.FixtureGenerationAchat.LigneSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static com.tsix.immocrm.controller.achat.AppelsEcrituresAchat.ROLE_JWT;
import static com.tsix.immocrm.controller.achat.AppelsEcrituresAchat.appeler;
import static com.tsix.immocrm.controller.achat.AppelsEcrituresAchat.corps;
import static com.tsix.immocrm.controller.achat.AppelsEcrituresAchat.detail;
import static com.tsix.immocrm.controller.achat.AppelsEcrituresAchat.id;
import static com.tsix.immocrm.controller.achat.AppelsEcrituresAchat.statut;
import static com.tsix.immocrm.controller.achat.AppelsGenerationAchat.BL_DEJA_FACTURE;
import static com.tsix.immocrm.controller.achat.AppelsGenerationAchat.avecSources;
import static com.tsix.immocrm.controller.achat.AppelsGenerationAchat.depasse;
import static com.tsix.immocrm.controller.achat.AppelsSaisieAchat.NOUVEAU_BC;
import static com.tsix.immocrm.controller.achat.AppelsSaisieAchat.NOUVEAU_BL;
import static com.tsix.immocrm.controller.achat.AppelsSaisieAchat.NOUVEAU_DEVIS;
import static com.tsix.immocrm.controller.achat.AppelsSaisieAchat.NOUVEAU_FACTURE;
import static com.tsix.immocrm.controller.achat.AppelsSaisieAchat.corpsValide;
import static com.tsix.immocrm.controller.achat.AppelsSaisieAchat.creer;
import static com.tsix.immocrm.controller.achat.AppelsSaisieAchat.idCree;
import static com.tsix.immocrm.controller.achat.AppelsSaisieAchat.ligne;
import static com.tsix.immocrm.fixtures.FixtureAchat.BON_COMMANDE;
import static com.tsix.immocrm.fixtures.FixtureAchat.BON_LIVRAISON;
import static com.tsix.immocrm.fixtures.FixtureAchat.FACTURE;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

/**
 * Seconde revue règle 6 du lot A2b-2b (FEATURE-achat-generation.md G6) : contrôle de quantités d'une facture générée
 * depuis un BL rattaché à un BC, et double génération depuis le même BL (préconditions relues en base).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PieceAchatGenerationRevue2Test {

    private static final String ART = "ART-1";
    private static final String BUY_ORDER_ID = "BuyOrderId";
    private static final String BUY_INVOICE_ID = "BuyInvoiceId";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureAchat fixture;
    @Autowired private FixtureGenerationAchat generation;

    private Cadre c;
    private Saisie s;
    private AdmUser redacteur;

    @BeforeEach
    void setUp() {
        c = fixture.cadre("AR2");
        s = fixture.configurationSaisie(c.jeton());
        redacteur = fixture.compteHabilite(c, "AR2", ROLE_JWT, NOUVEAU_DEVIS, NOUVEAU_BC, NOUVEAU_BL, NOUVEAU_FACTURE);
        fixture.sansPersonnel(redacteur);
        generation.parametres(false, false, false);
        generation.tableBuyOrderInvoice();
    }

    private UUID bcValide() {
        UUID bc = generation.source(c, "-BC", BON_COMMANDE, s.gestion(FixtureAchat.GESTION_BC), s.valideDg());
        generation.ligne(bc, LigneSource.simple(1, ART, "10", s.categorie()));
        return bc;
    }

    private UUID blValide(String suffixe, UUID bc, String quantite) {
        UUID bl = generation.source(c, suffixe, BON_LIVRAISON, s.gestion(FixtureAchat.GESTION_BL), s.valideDg());
        generation.ligne(bl, LigneSource.simple(1, ART, quantite, s.categorie()));
        fixture.poser(bl, BUY_ORDER_ID, bc);
        return bl;
    }

    private MvcResult genererFacture(UUID bl, String quantite) throws Exception {
        return appeler(mockMvc, creer(avecSources(corpsValide(c, s, FACTURE)
                .avec("lignes", List.of(ligne(ART, quantite, "100", s.categorie()))), bl)), connecte(redacteur, ROLE_JWT));
    }

    /**
     * G6 « facture à BC : contrôle existant » : paramètre à 1, BC 10, une facture du BC porte déjà 10 ; la facture générée
     * depuis un BL du BC (2) dépasse → 409 au message de QuantitesBonCommande, aucun numéro pris, BL non facturé.
     */
    @Test
    void g6_factureGeneree_depasseLeDejaFactureDuBc_409_rienEcrit() throws Exception {
        generation.parametre(FixtureAchat.LIMITE_QUANTITES, true);
        UUID bc = bcValide();
        UUID ancienne = generation.source(c, "-FAX", FACTURE, s.gestion(FixtureAchat.GESTION_FACTURE), s.enCours());
        fixture.poser(ancienne, BUY_ORDER_ID, bc);
        generation.ligne(ancienne, LigneSource.simple(1, ART, "10", s.categorie()));
        UUID bl = blValide("-BL1", bc, "2");

        MvcResult r = genererFacture(bl, "2");

        assertThat(statut(r)).as(corps(r)).isEqualTo(409);
        assertThat(detail(r)).isEqualTo(depasse(ART));
        fixture.oublier();
        FixtureAchat.SequenceNature seq = FixtureAchat.SEQUENCES_SAISIE.get(FACTURE);
        assertAll(
                () -> assertThat(fixture.valeurSequence(seq.code())).as("numéro non consommé").isEqualTo(seq.valeur()),
                () -> assertThat(fixture.lienDe(bl, BUY_INVOICE_ID)).as("BL non facturé").isNull());
    }

    /** G2 / G6 : deux générations successives depuis le même BL → la seconde 409 « déjà facturé », le BL garde la 1re. */
    @Test
    void g2_doubleGenerationDuMemeBl_secondeRefusee() throws Exception {
        UUID bc = bcValide();
        UUID bl = blValide("-BL2", bc, "2");

        MvcResult premiere = genererFacture(bl, "2");
        assertThat(statut(premiere)).as(corps(premiere)).isEqualTo(201);
        UUID facture = idCree(premiere);

        MvcResult seconde = genererFacture(bl, "2");

        assertThat(statut(seconde)).as(corps(seconde)).isEqualTo(409);
        assertThat(detail(seconde)).isEqualTo(BL_DEJA_FACTURE);
        assertThat(fixture.lienDe(bl, BUY_INVOICE_ID)).isEqualTo(id(facture));
    }
}
