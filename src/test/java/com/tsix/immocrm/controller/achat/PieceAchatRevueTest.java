package com.tsix.immocrm.controller.achat;

import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.fixtures.FixtureAchat;
import com.tsix.immocrm.fixtures.FixtureAchat.Cadre;
import com.tsix.immocrm.fixtures.FixtureConfigurationAchat;
import com.tsix.immocrm.fixtures.FixtureConfigurationAchat.Statut;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static com.tsix.immocrm.controller.achat.AppelsEcrituresAchat.ROLE_JWT;
import static com.tsix.immocrm.controller.achat.AppelsEcrituresAchat.appeler;
import static com.tsix.immocrm.controller.achat.AppelsEcrituresAchat.corps;
import static com.tsix.immocrm.controller.achat.AppelsEcrituresAchat.id;
import static com.tsix.immocrm.controller.achat.AppelsEcrituresAchat.statut;
import static com.tsix.immocrm.controller.achat.AppelsEcrituresAchat.transition;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static org.assertj.core.api.Assertions.assertThat;

/** Seconde revue (règle 6) du lot A2b-1 : cas limites non couverts par les tests verrouillés. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PieceAchatRevueTest {

    private static final String STATUT_PIECE = "BuyDocumentStatusId";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureAchat fixture;
    @Autowired private FixtureConfigurationAchat config;
    @Autowired private EntityManager em;

    private Cadre c;
    private UUID g;
    private UUID s0;
    private UUID s1;
    private AdmUser a;

    @BeforeEach
    void setUp() {
        c = fixture.cadre("ARV");
        g = config.gestion("G-" + c.jeton(), "Gestion " + c.jeton());
        s0 = nouveauStatut("S0");
        s1 = nouveauStatut("S1");
        a = fixture.compteHabilite(c, "ARV-A", ROLE_JWT);
        config.lier(config.transition(g, 1, "Valider", s0, s1, false, false), fixture.roleDe(a));
    }

    private UUID nouveauStatut(String code) {
        return config.statut(new Statut(code + "-" + c.jeton(), "Statut " + code, 10, null, null, null, false, null, null));
    }

    /**
     * BuyDocumentStatusHistory.Notes est un nvarchar(1000) : une note plus longue, saisie dans le champ multiligne du
     * dialogue, doit être refusée en 400 (champ invalide) sans rien écrire, pas tomber en 500 au flush.
     */
    @Test
    void revue_noteDePlusDe1000Caracteres_400_rienEcrit() throws Exception {
        UUID piece = fixture.piece(c, "-F0", FixtureAchat.FACTURE);
        fixture.poser(piece, "StrEntityId", g);
        fixture.poser(piece, STATUT_PIECE, s0);

        MvcResult r = appeler(mockMvc, transition(piece, s1, "x".repeat(1001)), connecte(a, ROLE_JWT));

        assertThat(statut(r)).as(corps(r)).isEqualTo(400);
        fixture.oublier();
        assertThat(fixture.lienDe(piece, STATUT_PIECE)).isEqualTo(id(s0));
        Number lignes = (Number) em.createNativeQuery("SELECT COUNT(*) FROM BuyDocumentStatusHistory WHERE BuyDocumentId = :id")
                .setParameter("id", piece).getSingleResult();
        assertThat(lignes.intValue()).isZero();
    }
}
