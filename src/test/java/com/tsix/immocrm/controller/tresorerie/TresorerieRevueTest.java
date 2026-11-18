package com.tsix.immocrm.controller.tresorerie;

import com.tsix.immocrm.controller.tresorerie.AppelsTresorerie.Reponse;
import com.tsix.immocrm.domain.tresorerie.VueRecette;
import com.tsix.immocrm.fixtures.FixtureAchat;
import com.tsix.immocrm.fixtures.FixtureTresorerie;
import com.tsix.immocrm.fixtures.FixtureTresorerie.Cadre;
import com.tsix.immocrm.fixtures.FixtureTresorerie.Reglement;
import jakarta.persistence.EntityManager;
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
import java.util.UUID;

import static com.tsix.immocrm.controller.tresorerie.AppelsTresorerie.AFFECTATIONS;
import static com.tsix.immocrm.controller.tresorerie.AppelsTresorerie.DEBIT;
import static com.tsix.immocrm.controller.tresorerie.AppelsTresorerie.DECAISSEMENTS;
import static com.tsix.immocrm.controller.tresorerie.AppelsTresorerie.MODULE;
import static com.tsix.immocrm.controller.tresorerie.AppelsTresorerie.PRJ;
import static com.tsix.immocrm.controller.tresorerie.AppelsTresorerie.SENS;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Revue adversariale N3 (FEATURE-n3-recettes-depenses.md). Jeu : société S habilitée, projet A habilité ← FA (ventilée
 * sur une catégorie), projet B non habilité ← FB ; société T non habilitée ← FT. RAB : 150 → FA + 250 → FB ; RT → FT.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class TresorerieRevueTest {

    private static final String PROJET_IDS = "projetIds";
    private static final String SOCIETE_ID = "societeId";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureTresorerie fixture;
    @Autowired private FixtureAchat achat;
    @Autowired private EntityManager em;

    private AppelsTresorerie appels;
    private RequestPostProcessor a;
    private Cadre c;
    private UUID societeT;
    private UUID rab;
    private UUID rt;
    private UUID labA;
    private String categorie;

    @BeforeEach
    void setUp() {
        appels = new AppelsTresorerie(mockMvc);
        c = fixture.cadre("TRR");
        societeT = achat.societe("SOCT-" + c.jeton());
        UUID projetT = achat.projet(c.code("-PT"), societeT);
        UUID fa = fixture.piece(c.code("-FA"), c.societe(), c.projet(), c.fournisseur(), c.fournisseurNom(),
                c.modulePrj(), "1000");
        UUID fb = fixture.piece(c.code("-FB"), c.societe(), c.projetNonHabilite(), c.fournisseur(), c.fournisseurNom(),
                c.modulePrj(), "1000");
        UUID ft = fixture.piece(c.code("-FT"), societeT, projetT, c.fournisseur(), c.fournisseurNom(), c.modulePrj(),
                "1000");
        String codeCategorie = "CAT-" + c.jeton();
        achat.ventilation(fa, achat.categorie(codeCategorie), BigDecimal.ONE);
        categorie = "Categorie " + codeCategorie;

        rab = fixture.reglement(Reglement.debit(c.code("-RAB"), c, "400"));
        labA = fixture.ligne(rab, fa, "150");
        fixture.ligne(rab, fb, "250");
        rt = fixture.reglement(Reglement.debit(c.code("-RT"), c, "900").societe(societeT));
        fixture.ligne(rt, ft, "900");

        a = connecte(fixture.compte(c, "TRRA", FixtureTresorerie.DROIT_DECAISSEMENTS,
                FixtureTresorerie.DROIT_AFFECTATIONS), FixtureTresorerie.ROLE);
    }

    /** Filtres projet / société hors périmètre : jamais d'élargissement (contrôle, attendu vert). */
    @Test
    void filtresHorsPerimetre_nElargissentJamais() throws Exception {
        List<String> projetNonHabilite = appels.ok(a, DECAISSEMENTS, MODULE, PRJ,
                PROJET_IDS, c.projetNonHabilite().toString()).ids();
        List<String> societeNonHabilitee = appels.ok(a, DECAISSEMENTS, MODULE, PRJ,
                SOCIETE_ID, societeT.toString()).ids();
        List<String> lesDeux = appels.ok(a, DECAISSEMENTS, MODULE, PRJ, PROJET_IDS, c.projetNonHabilite().toString(),
                "sansProjet", "true").ids();

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(projetNonHabilite).isEmpty();
            s.assertThat(societeNonHabilitee).isEmpty();
            s.assertThat(lesDeux).doesNotContain(rab.toString(), rt.toString());
        });
    }

    /** Fiche à cheval : une ligne visible, une masquée, montants sur toutes les lignes (contrôle, attendu vert). */
    @Test
    void ficheACheval_uneLigneVisibleUneMasquee() throws Exception {
        Reponse r = appels.lire(a, DECAISSEMENTS + "/" + rab);

        assertThat(r.statut()).as(r.corps()).isEqualTo(200);
        SoftAssertions.assertSoftly(s -> {
            s.assertThat((List<?>) r.valeur("$.lignes")).hasSize(1);
            s.assertThat(r.nombre("$.lignesMasquees")).isEqualByComparingTo("1");
            s.assertThat(r.nombre("$.decaissement.montantAffecte")).isEqualByComparingTo("400");
            s.assertThat(r.nombre("$.decaissement.resteAAffecter")).isEqualByComparingTo("0");
        });
    }

    /**
     * DÉFAUT : {@code categorie} lit {@code StlSettlement.StlCategoryId} (0 / 2 518 règlements PRJ/BUY en prod) ; OMRAN
     * ({@code StlSettlementRelationView}) la lit sur la ventilation de la pièce ({@code BuyVentilationCategory}, 2 532 / 2 532).
     */
    @Test
    void affectation_categorie_estCelleDeLaVentilationDeLaPiece() throws Exception {
        Reponse r = appels.ok(a, AFFECTATIONS, MODULE, PRJ, SENS, DEBIT);

        assertThat(r.ligne(labA)).containsEntry("categorie", categorie);
    }

    /** DÉFAUT (contrat) : le @Parameter de {@code recherche} annonce le code reçu, la requête ne le cherche pas. */
    @Test
    void recherche_parCodeRecu_trouveLeReglement() throws Exception {
        em.flush();
        em.createNativeQuery("UPDATE StlSettlement SET ReceiptCode = :recu WHERE PKey = :id")
                .setParameter("recu", "RECU-" + c.jeton()).setParameter("id", rab).executeUpdate();

        List<String> ids = appels.ok(a, DECAISSEMENTS, MODULE, PRJ, "recherche", "RECU-" + c.jeton()).ids();

        assertThat(ids).contains(rab.toString());
    }

    /**
     * DÉFAUT (D3 / Q3 parité complète) : la grille OMRAN des factures ({@code InvoiceList.cs}, {@code RefundList.cs})
     * compte 45 colonnes de données ; le port en a 39 (manquent tranche, article code / libellé, avoir, document lié,
     * catégorie) et les avoirs n'ont pas la facture d'origine ({@code StlInvoiceCode}).
     */
    @Test
    void recettes_colonnesDesPieces_pariteOmran() {
        SoftAssertions.assertSoftly(s -> {
            s.assertThat(VueRecette.FACTURES.colonnes()).hasSizeGreaterThanOrEqualTo(45);
            s.assertThat(VueRecette.AVOIRS.colonnes()).hasSizeGreaterThan(VueRecette.FACTURES.colonnes().size());
        });
    }
}
