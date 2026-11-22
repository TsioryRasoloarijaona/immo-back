package com.tsix.immocrm.controller.projet;

import com.tsix.immocrm.controller.projet.AppelsProjet.Reponse;
import com.tsix.immocrm.fixtures.FixtureBilansProjet;
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
import java.util.UUID;

import static com.tsix.immocrm.controller.projet.AppelsBilans.PROJETS;
import static com.tsix.immocrm.controller.projet.AppelsBilans.valeur;
import static com.tsix.immocrm.fixtures.FixtureBilansProjet.ESTIMATION_BASE;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static org.assertj.core.api.Assertions.within;

/** N5a R15 Bilans — droit {@code Yes.Prj.Front.Tdb.ProjectReview}, sélection obligatoire, projets demandés ∩ habilités. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class BilansProjetVisibiliteTest {

    static final String MESSAGE_SANS_PROJET = "Veuillez sélectionner au moins un projet !";
    private static final String COMMERCIAL = "Commercial";
    private static final BigDecimal PRECISION = new BigDecimal("0.01");

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureBilansProjet f;

    private AppelsBilans appels;
    private UUID projet;
    private UUID categorie;

    @BeforeEach
    void setUp() {
        appels = new AppelsBilans(mockMvc);
        f.preparer();
        projet = f.projet("VP", false);
        categorie = f.categorie("Visibilite", null, 1);
        lotEstime("LV", projet, "1000");
    }

    private void lotEstime(String code, UUID p, String ht) {
        UUID lot = f.lot(code, p, false);
        f.ventiler(lot, categorie, "1");
        f.estimationBase(lot, ht);
    }

    private RequestPostProcessor qui(String code, boolean droit, UUID... projets) {
        return connecte(f.compte(code, droit, projets), COMMERCIAL);
    }

    @Test
    void r15_sansDroitProjectReview_403_temoinAvecDroit200() throws Exception {
        Reponse temoin = appels.lire(qui("avec", true, projet), PROJETS, projet.toString());
        Reponse r = appels.lire(qui("sans", false, projet), PROJETS, projet.toString());

        SoftAssertions s = new SoftAssertions();
        s.assertThat(temoin.statut()).as(temoin.corps()).isEqualTo(200);
        s.assertThat(r.statut()).as(r.corps()).isEqualTo(403);
        s.assertAll();
    }

    @Test
    void r15_aucunProjet_400_messageOmran_temoin200() throws Exception {
        RequestPostProcessor qui = qui("vide", true, projet);

        Reponse temoin = appels.lire(qui, PROJETS, projet.toString());
        Reponse r = appels.lire(qui);

        SoftAssertions s = new SoftAssertions();
        s.assertThat(temoin.statut()).as(temoin.corps()).isEqualTo(200);
        s.assertThat(r.statut()).as(r.corps()).isEqualTo(400);
        s.assertThat(r.corps()).contains(MESSAGE_SANS_PROJET);
        s.assertAll();
    }

    /** Projet d'une société non habilitée, sans habilitation projet : ses 5000 n'entrent pas, et la réponse reste 200. */
    @Test
    void r15_projetHorsPerimetre_ignore_jamais403() throws Exception {
        UUID cache = f.projetHorsPerimetre("VH");
        lotEstime("LH", cache, "5000");

        Reponse r = appels.lire(qui("hors", true, projet), PROJETS, projet.toString(), PROJETS, cache.toString());

        SoftAssertions s = new SoftAssertions();
        s.assertThat(r.statut()).as(r.corps()).isEqualTo(200);
        s.assertThat(valeur(r, categorie, ESTIMATION_BASE)).isCloseTo(new BigDecimal("1000"), within(PRECISION));
        s.assertAll();
    }

    /** Seuls des projets hors périmètre demandés : intersection vide → même 400 que sans projet ; témoin habilité 200. */
    @Test
    void r15_seulementProjetsHorsPerimetre_400_messageOmran() throws Exception {
        UUID cache = f.projetHorsPerimetre("VX");
        lotEstime("LX", cache, "5000");
        RequestPostProcessor qui = qui("seulhors", true, projet);

        Reponse temoin = appels.lire(qui, PROJETS, projet.toString());
        Reponse r = appels.lire(qui, PROJETS, cache.toString());

        SoftAssertions s = new SoftAssertions();
        s.assertThat(temoin.statut()).as(temoin.corps()).isEqualTo(200);
        s.assertThat(r.statut()).as(r.corps()).isEqualTo(400);
        s.assertThat(r.corps()).contains(MESSAGE_SANS_PROJET);
        s.assertAll();
    }

    /** Deux projets habilités demandés : leurs lignes se cumulent par catégorie (1000 + 300) ; témoin un seul projet = 1000. */
    @Test
    void r15_plusieursProjetsHabilites_cumulesParCategorie() throws Exception {
        UUID second = f.projet("VS", false);
        lotEstime("LS", second, "300");
        RequestPostProcessor qui = qui("deux", true, projet, second);

        Reponse seul = appels.lire(qui, PROJETS, projet.toString());
        Reponse deux = appels.lire(qui, PROJETS, projet.toString(), PROJETS, second.toString());

        SoftAssertions s = new SoftAssertions();
        s.assertThat(valeur(seul, categorie, ESTIMATION_BASE)).isCloseTo(new BigDecimal("1000"), within(PRECISION));
        s.assertThat(valeur(deux, categorie, ESTIMATION_BASE)).isCloseTo(new BigDecimal("1300"), within(PRECISION));
        s.assertAll();
    }
}
