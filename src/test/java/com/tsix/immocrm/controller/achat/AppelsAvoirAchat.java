package com.tsix.immocrm.controller.achat;

import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.fixtures.CorpsJson;
import com.tsix.immocrm.fixtures.FixtureAchat;
import com.tsix.immocrm.fixtures.FixtureAchat.Cadre;
import com.tsix.immocrm.fixtures.FixtureAchat.Saisie;
import com.tsix.immocrm.fixtures.FixtureAchat.SequenceNature;
import com.tsix.immocrm.fixtures.FixtureGenerationAchat;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static com.tsix.immocrm.controller.achat.AppelsEcrituresAchat.ROLE_JWT;
import static com.tsix.immocrm.controller.achat.AppelsEcrituresAchat.SUPPR_AVOIR;
import static com.tsix.immocrm.controller.achat.AppelsSaisieAchat.NOUVEL_AVOIR;
import static com.tsix.immocrm.controller.achat.AppelsSaisieAchat.corpsValide;

/** Lot Avoirs (FEATURE-achat-avoirs.md) : messages exacts, gestions et séquence de prod, contexte commun des tests. */
final class AppelsAvoirAchat {

    static final String FACTURE_INTROUVABLE = "La facture sélectionnée n'est pas présente dans notre référentiel !";
    static final String IMPOSSIBLE = "Impossible de générer Avoir depuis ces pièces.";
    static final String GENERER_AVOIR = "Générer Avoir";
    static final String NOUVEL_AVOIR_LOT = "Yes.Prj.Front.Market.RefundList.toolNew";
    static final String DEFAUT_FINANCIER = "DefaultRefundIsFinancial";

    static final String GESTION_FACTURE_PROJET = "PrjInvoiceDebit";
    static final String GESTION_AVOIR = "BuyRefundDebit";
    static final String GESTION_DECOMPTE = "PrjInvoiceDebitWorkingOut";
    static final String GESTION_FACTURE_STL = "StlInvoiceDebit";
    static final String GESTION_FACTURE_COM = "ComInvoiceDebit";

    static final UUID PKEY_FACTURE_PROJET = UUID.fromString("1692B407-170E-48D6-A438-5D3368B9C376");
    static final UUID PKEY_AVOIR = UUID.fromString("E7999F70-7507-40D9-AA78-A2825C42B620");
    static final UUID PKEY_DECOMPTE = UUID.fromString("E7999F70-7507-40D9-AA78-A2825C42B604");
    static final UUID PKEY_FACTURE_STL = UUID.fromString("B8E787D2-23C0-4D03-AD5C-7AC346DC6944");
    static final UUID PKEY_FACTURE_COM = UUID.fromString("92336FCB-A7F6-4A43-8B51-296D2ACCA484");

    /** Séquence « Avoir fournisseur » (R13, réponse manager 17/11) à sa PKey de prod ; code attendu 21/00901. */
    static final SequenceNature SEQUENCE_AVOIR =
            new SequenceNature("Avoir fournisseur", UUID.fromString("50C4F768-1478-5897-1245-F784D28F2001"), 900);

    static final String A_PAYER = "AmountToPay";
    static final String FINANCIER = "IsFinancialRefund";
    static final String SOURCE = "InvoiceSourceId";

    /** Cadre, saisie, gestions facture projet / avoir, statut de départ de l'avoir, rédacteur (créer + supprimer un avoir). */
    record Contexte(Cadre c, Saisie s, UUID gestionFacture, UUID gestionAvoir, UUID departAvoir, AdmUser redacteur) {
    }

    private AppelsAvoirAchat() {
    }

    static Contexte preparer(FixtureAchat fixture, FixtureGenerationAchat generation, String prefixe) {
        Cadre c = fixture.cadre(prefixe);
        Saisie s = fixture.configurationSaisie(c.jeton());
        UUID gestionFacture = fixture.gestionVide(GESTION_FACTURE_PROJET, PKEY_FACTURE_PROJET);
        UUID gestionAvoir = fixture.gestionVide(GESTION_AVOIR, PKEY_AVOIR);
        UUID departAvoir = fixture.statutSaisie("DEPART-AV-" + c.jeton(), 5, FixtureAchat.ETAT_NOUVEAU, false, false);
        fixture.transitionDe(gestionAvoir, 10, departAvoir, s.valideDg());
        fixture.sequenceSaisie(SEQUENCE_AVOIR);
        generation.parametres(false, false, false);
        generation.parametre(DEFAUT_FINANCIER, true);
        generation.tableBuyOrderInvoice();
        fixture.tablesComptables();
        AdmUser redacteur = fixture.compteHabilite(c, prefixe, ROLE_JWT, NOUVEL_AVOIR, SUPPR_AVOIR);
        fixture.sansPersonnel(redacteur);
        return new Contexte(c, s, gestionFacture, gestionAvoir, departAvoir, redacteur);
    }

    /** Facture projet EN COURS, HT 1 000, TTC 1 200, à payer 1 200, sans lot ni ligne. */
    static UUID facture(FixtureAchat fixture, Contexte x, String suffixe) {
        UUID f = fixture.piece(x.c(), suffixe, FixtureAchat.FACTURE);
        fixture.poser(f, "StrEntityId", x.gestionFacture());
        fixture.poser(f, "BuyDocumentStatusId", x.s().enCours());
        fixture.montants(f, "1000", "1200", "1200");
        return f;
    }

    /** Avoir déjà en base sur {@code facture} (gestion BuyRefundDebit, statut de départ), HT/TTC/à payer donnés. */
    static UUID avoirExistant(FixtureAchat fixture, Contexte x, UUID facture, boolean financier, String ht, String ttc) {
        UUID a = fixture.piece(x.c(), "-AV" + UUID.randomUUID().toString().substring(0, 4), FixtureAchat.AVOIR);
        fixture.poser(a, "StrEntityId", x.gestionAvoir());
        fixture.poser(a, "BuyDocumentStatusId", x.departAvoir());
        fixture.poser(a, SOURCE, facture);
        fixture.poser(a, FINANCIER, financier);
        fixture.montants(a, ht, ttc, ttc);
        return a;
    }

    /** Corps d'avoir sans ligne : {@code montantHT} / {@code montantTVA} fixent les totaux (D9). */
    static CorpsJson corpsSansLigne(Contexte x, String ht, String tva, boolean financier) {
        return corpsValide(x.c(), x.s(), FixtureAchat.AVOIR).avec("lignes", List.of())
                .avec("montantHT", new BigDecimal(ht)).avec("montantTVA", new BigDecimal(tva))
                .avec("avoirFinancier", financier);
    }
}
