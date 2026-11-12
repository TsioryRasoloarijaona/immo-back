package com.tsix.immocrm.config.audit;

import com.tsix.immocrm.config.Contributions;
import com.tsix.immocrm.config.audit.RegistreAuditOmran.Trace;
import com.tsix.immocrm.entities.buy.BuyDocument;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * FEATURE-achat-fiche-ecritures.md E-B7 : {@code TracesAchat} reprend {@code BuyDocument} (retiré de {@code TracesTerrains}) ;
 * {@code ObjectType} / libellé par {@code DocType} (mesurés dans {@code AdmWebHistorics} de prod), libellé d'objet = code ;
 * la facture de frais de terrain garde « Yes.Stl.Debit.Invoice » / « Facture ».
 */
class TracesAchatTest {

    private static BuyDocument piece(String nature, String code) {
        BuyDocument d = new BuyDocument();
        d.setDocType(nature);
        d.setCode(code);
        return d;
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource({
            "QUOTE, Yes.Buy.Quote, Devis",
            "ORDER, Yes.Buy.Order, Bon de Commande",
            "DELIVRY, Yes.Buy.Delivery, Bon de Livraison",
            "INVOICE, Yes.Stl.Debit.Invoice, Facture",
            "REFUND, Yes.Stl.Debit.Refund, Avoir"})
    void eb7_typeEtLibelleParNature_libelleObjetEgalCode(String nature, String type, String libelle) {
        Trace trace = RegistreAuditOmran.tracer(piece(nature, "26/00042")).orElseThrow();

        assertThat(trace).extracting(Trace::type, Trace::libelleType, Trace::libelleObjet)
                .containsExactly(type, libelle, "26/00042");
    }

    /** Équivalence : la facture de frais de terrain (ReferenceType LAND_EXPENSE) garde sa trace « Facture ». */
    @Test
    void eb7_fraisDeTerrain_equivalence_facture() {
        BuyDocument frais = piece("INVOICE", "26/03018");
        frais.setReferenceType("LAND_EXPENSE");

        Trace trace = RegistreAuditOmran.tracer(frais).orElseThrow();

        assertThat(trace).extracting(Trace::type, Trace::libelleType, Trace::libelleObjet, Trace::typeParent)
                .containsExactly("Yes.Stl.Debit.Invoice", "Facture", "26/03018", null);
    }

    /** Registre = premier {@code TracesAudit} non nul : {@code BuyDocument} revendiqué par un seul domaine, TracesAchat. */
    @Test
    void eb7_buyDocumentRevendiqueUneSeuleFois_parTracesAchat() {
        List<String> domaines = Contributions.trouver(TracesAudit.class).stream()
                .filter(d -> d.classes().contains(BuyDocument.class))
                .map(d -> d.getClass().getSimpleName()).toList();

        assertThat(domaines).containsExactly("TracesAchat");
    }

    @Test
    void eb7_tracesTerrains_neRevendiquePlusBuyDocument() {
        assertThat(new TracesTerrains().classes()).doesNotContain(BuyDocument.class);
    }
}
