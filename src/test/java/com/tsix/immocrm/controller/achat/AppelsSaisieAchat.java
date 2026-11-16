package com.tsix.immocrm.controller.achat;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.fixtures.CorpsJson;
import com.tsix.immocrm.fixtures.FixtureAchat;
import com.tsix.immocrm.fixtures.FixtureAchat.Cadre;
import com.tsix.immocrm.fixtures.FixtureAchat.Saisie;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

/** Routes, corps et textes du lot A2b-2a « créer, modifier » (FEATURE-achat-fiche-creation.md), noms du fil du spec. */
final class AppelsSaisieAchat {

    static final String DOCUMENTS = "/api/achat/documents";
    static final String MODELE = "/api/achat/documents/modele";
    static final String REFERENTIELS = "/api/achat/referentiels";

    // Droits de création / modification (E-C1) : code Achat, code Projet › Lot (pièce à lot seulement).
    static final String NOUVEAU_DEVIS = "Yes.Buy.Front.Quote.QuoteList.toolNew";
    static final String NOUVEAU_DEVIS_LOT = "Yes.Prj.Front.Market.QuoteList.toolNew";
    static final String NOUVEAU_BC = "Yes.Buy.Front.Order.OrderList.toolNew";
    static final String NOUVEAU_BC_LOT = "Yes.Prj.Front.Market.OrderList.toolNew";
    static final String NOUVEAU_BL = "Yes.Buy.Front.Delivery.DeliveryList.toolNew";
    static final String NOUVEAU_BL_LOT = "Yes.Prj.Front.Market.DeliveryList.toolNew";
    static final String NOUVEAU_FACTURE = "Yes.Stl.Front.Debit.InvoiceList.toolNew";
    static final String NOUVEAU_FACTURE_LOT_IGNORE = "Yes.Prj.Front.Market.InvoiceList.toolNew";
    static final String NOUVEL_AVOIR = "Yes.Stl.Front.Debit.RefundList.toolNew";

    static final String NATURE_NON_PRISE = "Nature non prise en charge";
    static final String SOCIETE_INTROUVABLE = "Société introuvable";
    static final String PROJET_INTROUVABLE = "Projet introuvable";
    static final String GEREE_AILLEURS = "Cette pièce est gérée ailleurs (décompte, avance, frais de terrain, document à régler) : "
            + "elle ne se modifie pas depuis sa fiche.";
    static final String VENTILATION_CATEGORIES = "La ventilation des catégories est incorrecte !";
    static final String VENTILATION_PROJETS = "La ventilation des projets est incorrecte !";
    static final String VENTILATION_PROJETS_100 = "La ventilation des projets est incorrecte (elle doit être égale à 100%) !";
    static final String LECTURE_SEULE = "Cette pièce est en lecture seule";

    static final LocalDate DATE = LocalDate.of(2026, 5, 4);
    static final BigDecimal TVA_20 = new BigDecimal("0.2");
    static final BigDecimal ZERO = BigDecimal.ZERO;

    /** Nature du fil → code Achat {@code toolNew}, code Projet › Lot (null pour la facture), gestion de création. */
    enum NatureSaisie {
        DEVIS(FixtureAchat.DEVIS, NOUVEAU_DEVIS, NOUVEAU_DEVIS_LOT, FixtureAchat.GESTION_DEVIS),
        BC(FixtureAchat.BON_COMMANDE, NOUVEAU_BC, NOUVEAU_BC_LOT, FixtureAchat.GESTION_BC),
        BL(FixtureAchat.BON_LIVRAISON, NOUVEAU_BL, NOUVEAU_BL_LOT, FixtureAchat.GESTION_BL),
        FACTURE(FixtureAchat.FACTURE, NOUVEAU_FACTURE, null, FixtureAchat.GESTION_FACTURE);

        final String code;
        final String droit;
        final String droitLot;
        final String gestion;

        NatureSaisie(String code, String droit, String droitLot, String gestion) {
            this.code = code;
            this.droit = droit;
            this.droitLot = droitLot;
            this.gestion = gestion;
        }

        FixtureAchat.SequenceNature sequence() {
            return FixtureAchat.SEQUENCES_SAISIE.get(code);
        }
    }

    private AppelsSaisieAchat() {
    }

    static MockHttpServletRequestBuilder creer(CorpsJson corps) {
        return post(DOCUMENTS).contentType(MediaType.APPLICATION_JSON).content(corps.json());
    }

    static MockHttpServletRequestBuilder modifier(UUID piece, CorpsJson corps) {
        return put(AppelsEcrituresAchat.DOCUMENT, piece).contentType(MediaType.APPLICATION_JSON).content(corps.json());
    }

    static MockHttpServletRequestBuilder modele(String nature) {
        return get(MODELE).param("nature", nature);
    }

    static MockHttpServletRequestBuilder referentiels() {
        return get(REFERENTIELS);
    }

    /** Ligne de saisie ; taux en fraction. */
    static CorpsJson ligne(String code, String quantite, String prixHT, String remise, String fodec, String tva,
                           UUID categorie) {
        return CorpsJson.corps().avec("id", null).avec("code", code).avec("description", "Article " + code)
                .avec("unite", "U").avec("quantite", new BigDecimal(quantite)).avec("prixUnitaireHT", new BigDecimal(prixHT))
                .avec("remise", new BigDecimal(remise)).avec("fodec", new BigDecimal(fodec)).avec("tva", new BigDecimal(tva))
                .avec("categorieId", categorie).avec("notes", "");
    }

    /** Ligne simple : sans remise ni FODEC, TVA 20 %. */
    static CorpsJson ligne(String code, String quantite, String prixHT, UUID categorie) {
        return ligne(code, quantite, prixHT, "0", "0", "0.2", categorie);
    }

    /**
     * Corps valide minimal : société, projet (donc ventilation projet facultative) et fournisseur du cadre, module Achat,
     * type de marché par défaut, une ligne ART-1 10 × 100 HT à 20 %.
     */
    static CorpsJson corpsValide(Cadre c, Saisie s, String nature) {
        return CorpsJson.corps().avec("nature", nature).avec("dateDocument", DATE).avec("dateEcheance", null)
                .avec("dateLivraisonSouhaitee", null).avec("societeId", c.societe()).avec("moduleId", s.moduleAchat())
                .avec("fournisseurId", c.fournisseur()).avec("projetId", c.projet()).avec("lotId", null)
                .avec("typeMarcheId", s.typeMarche()).avec("declencheurId", null).avec("personneAContacter", "")
                .avec("referenceInterne", "").avec("referenceBureauOrdre", "").avec("notes", "").avec("notes2", "")
                .avec("montantRegul", ZERO).avec("timbreFiscal", ZERO).avec("categorieId", s.categorie())
                .avec("lignes", List.of(ligne("ART-1", "10", "100", s.categorie())))
                .avec("ventilationProjets", List.of());
    }

    static CorpsJson part(UUID cle, String taux) {
        return CorpsJson.corps().avec("cleId", cle).avec("taux", new BigDecimal(taux));
    }

    static UUID idCree(MvcResult r) throws Exception {
        return UUID.fromString(JsonPath.read(AppelsEcrituresAchat.corps(r), "$.id"));
    }

    static BigDecimal decimalDe(Map<String, Object> ligne, String colonne) {
        Object v = ligne.get(colonne);
        return v == null ? null : new BigDecimal(v.toString());
    }

    static String guidDe(Map<String, Object> ligne, String colonne) {
        Object v = ligne.get(colonne);
        return v == null ? null : v.toString().toUpperCase();
    }
}
