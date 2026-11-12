package com.tsix.immocrm.controller.achat;

import com.jayway.jsonpath.JsonPath;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

/** Corps {@code StatutAchatEcritureDto} et requêtes d'écriture de FEATURE-statuts-achat-parite.md (noms du fil). */
final class EcritureStatutsAchat {

    static final String STATUTS = LectureConfigurationAchat.STATUTS;
    static final String STATUT = STATUTS + "/{id}";
    static final String TYPES = STATUTS + "/types";
    static final String CHAMPS = STATUTS + "/champs-obligatoires";
    static final String TYPES_DOCUMENTS = STATUTS + "/types-documents";
    static final String IMAGES = "/api/achat/config/images";

    static final String ORDRE = "ordre";
    static final String CODE = "code";
    static final String DESCRIPTION = "description";
    static final String TYPE_ID = "typeId";
    static final String DUREE = "dureeVieJours";
    static final String COULEUR = "couleur";
    static final String OK_PAIEMENT = "okPourPaiement";
    static final String VALIDE_COMPTA = "valideComptabilite";
    static final String LECTURE_SEULE = "lectureSeule";
    static final String IMAGE_ID = "imageId";
    static final String NOTES = "notes";
    static final String CHAMPS_PIECE = "champsObligatoires";
    static final String CHAMPS_FOURNISSEUR = "champsObligatoiresFournisseur";
    static final String DOCUMENTS = "documentsAJoindre";
    static final String TYPE_DOCUMENT_ID = "typeDocumentId";
    static final String OBLIGATOIRE = "obligatoire";
    static final String AVEC_TITRE = "avecTitre";

    private EcritureStatutsAchat() {
    }

    /** Corps minimal valide (les 4 obligatoires), modifiable. */
    static Map<String, Object> corps(String code, UUID typeId) {
        Map<String, Object> c = new LinkedHashMap<>();
        c.put(ORDRE, 3);
        c.put(CODE, code);
        c.put(DESCRIPTION, "Statut " + code);
        c.put(TYPE_ID, typeId.toString());
        return c;
    }

    static Map<String, Object> document(UUID type, boolean obligatoire, boolean avecTitre) {
        Map<String, Object> d = new LinkedHashMap<>();
        d.put(TYPE_DOCUMENT_ID, type.toString());
        d.put(OBLIGATOIRE, obligatoire);
        d.put(AVEC_TITRE, avecTitre);
        return d;
    }

    static MockHttpServletRequestBuilder creation(JsonMapper json, Map<String, Object> corps) {
        return post(STATUTS).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(corps));
    }

    static MockHttpServletRequestBuilder modification(JsonMapper json, Object id, Map<String, Object> corps) {
        return put(STATUT, id).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(corps));
    }

    static MockHttpServletRequestBuilder suppression(Object id) {
        return delete(STATUT, id);
    }

    static UUID id(MvcResult reponse) throws Exception {
        String corps = reponse.getResponse().getContentAsString(StandardCharsets.UTF_8);
        return UUID.fromString(String.valueOf((Object) JsonPath.read(corps, "$.id")));
    }

    static String detail(MvcResult reponse) throws Exception {
        String corps = reponse.getResponse().getContentAsString(StandardCharsets.UTF_8);
        return String.valueOf((Object) JsonPath.read(corps, "$.detail"));
    }

    @SuppressWarnings("unchecked")
    static List<Map<String, Object>> liste(Map<String, Object> ligne, String champ) {
        return (List<Map<String, Object>>) ligne.get(champ);
    }

    @SuppressWarnings("unchecked")
    static List<String> noms(Map<String, Object> ligne, String champ) {
        return (List<String>) ligne.get(champ);
    }
}
