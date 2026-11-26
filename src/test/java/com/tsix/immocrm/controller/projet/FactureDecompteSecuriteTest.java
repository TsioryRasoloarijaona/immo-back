package com.tsix.immocrm.controller.projet;

import com.tsix.immocrm.controller.projet.AppelsProjet.Reponse;
import com.tsix.immocrm.fixtures.FixtureFactureDecompte;
import com.tsix.immocrm.fixtures.FixturePiecesLot;
import com.tsix.immocrm.fixtures.FixturePiecesLot.Cadre;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static com.tsix.immocrm.controller.projet.FactureDecompteSupport.AVOIR;
import static com.tsix.immocrm.controller.projet.FactureDecompteSupport.DROIT_AVOIR;
import static com.tsix.immocrm.controller.projet.FactureDecompteSupport.DROIT_FACTURE;
import static com.tsix.immocrm.controller.projet.FactureDecompteSupport.FACTURE;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Droits et ordre des refus de FEATURE-lc8-facture-decompte.md R4 / D4 : facture = {@code InvoiceList.toolNew},
 * avoir = {@code RefundList.toolNew} ; ordre 404 → 403 → 409/400. Comptes non Administrateur habilités sur le projet.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class FactureDecompteSecuriteTest {

    private static final String ROLE = "Commercial";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixturePiecesLot pf;
    @Autowired private FixtureFactureDecompte fd;

    private FactureDecompteSupport appels;
    private Cadre c;
    private int rang;

    @BeforeEach
    void setUp() {
        c = fd.cadre("FS");
        appels = new FactureDecompteSupport(mockMvc);
    }

    private UUID decompte() {
        rang++;
        return fd.decompteSimple(c, pf.lot(c, "FS" + rang, c.statutLibre()), rang);
    }

    private RequestPostProcessor compte(String code, String... droits) {
        return connecte(pf.compte(c, code, List.of(droits)), ROLE);
    }

    private Reponse generer(RequestPostProcessor qui, UUID decompte, String route) throws Exception {
        fd.flush();
        return appels.generer(qui, decompte, route);
    }

    // R4 / D4 : facture sans le droit InvoiceList.toolNew → 403, décompte non facturé.
    @Test
    void r4_facture_sansDroit_403() throws Exception {
        UUID d = decompte();

        Reponse r = generer(compte("fs.sans", DROIT_AVOIR), d, FACTURE);

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(r.statut()).as(r.corps()).isEqualTo(403);
            s.assertThat(fd.factureDuDecompte(d)).isNull();
        });
    }

    // R4 / D4 témoin : même compte avec le droit InvoiceList.toolNew → 201.
    @Test
    void r4_facture_avecDroit_201() throws Exception {
        UUID d = decompte();

        Reponse r = generer(compte("fs.avec", DROIT_FACTURE), d, FACTURE);

        assertThat(r.statut()).as(r.corps()).isEqualTo(201);
    }

    // R6 / D4 : avoir sans le droit RefundList.toolNew → 403.
    @Test
    void r6_avoir_sansDroit_403() throws Exception {
        UUID d = decompte();
        fd.dejaFacture(c, d);

        Reponse r = generer(compte("fs.av.sans", DROIT_FACTURE), d, AVOIR);

        assertThat(r.statut()).as(r.corps()).isEqualTo(403);
    }

    // R6 / D4 témoin : même compte avec le droit RefundList.toolNew → 201 (facture générée par l'écrivain d'abord).
    @Test
    void r6_avoir_avecDroit_201() throws Exception {
        UUID d = decompte();
        Reponse facture = generer(compte("fs.f", DROIT_FACTURE), d, FACTURE);
        assertThat(facture.statut()).as(facture.corps()).isEqualTo(201);

        Reponse r = generer(compte("fs.av.avec", DROIT_AVOIR), d, AVOIR);

        assertThat(r.statut()).as(r.corps()).isEqualTo(201);
    }

    // R4 ordre : 404 avant 403 (hors périmètre, compte sans droit).
    @Test
    void r4_ordre_horsPerimetreSansDroit_404() throws Exception {
        UUID d = fd.decompteSimple(c, pf.lotHorsPerimetre(c, "FSX"), 20);

        Reponse r = generer(compte("fs.o1"), d, FACTURE);

        assertThat(r.statut()).as(r.corps()).isEqualTo(404);
    }

    // R4 ordre : 403 avant 409 (déjà facturé, compte sans droit).
    @Test
    void r4_ordre_dejaFactureSansDroit_403() throws Exception {
        UUID d = decompte();
        fd.dejaFacture(c, d);

        Reponse r = generer(compte("fs.o2"), d, FACTURE);

        assertThat(r.statut()).as(r.corps()).isEqualTo(403);
    }

    // R4 ordre : 403 avant 400 (lot sans ventilation, compte sans droit).
    @Test
    void r4_ordre_sansVentilationSansDroit_403() throws Exception {
        UUID d = fd.decompteSimple(c, pf.lotSansVentilation(c, "FSV", c.statutLibre()), 21);

        Reponse r = generer(compte("fs.o3"), d, FACTURE);

        assertThat(r.statut()).as(r.corps()).isEqualTo(403);
    }

    // R6 ordre : 403 avant 409 (avoir sur décompte non facturé, compte sans droit).
    @Test
    void r6_ordre_sansFactureSansDroit_403() throws Exception {
        UUID d = decompte();

        Reponse r = generer(compte("fs.o4"), d, AVOIR);

        assertThat(r.statut()).as(r.corps()).isEqualTo(403);
    }
}
