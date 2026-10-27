package com.tsix.immocrm.fixtures;

import com.jayway.jsonpath.JsonPath;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.CorpsJson.corps;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

/** Routes et corps du fil de FEATURE-vente-commissions.md §4 (contrat du lot COM). */
public final class ApiCommissions {

    public static final String RACINE = "/api/commissions";
    public static final String TRANCHES = RACINE + "/tranches";
    public static final String TRANCHE = TRANCHES + "/{trancheId}";
    public static final String DOSSIERS = TRANCHE + "/dossiers";
    public static final String LIGNES = TRANCHE + "/lignes";
    public static final String FACTURES = TRANCHE + "/factures";
    public static final String GENERATION = TRANCHE + "/generation";
    public static final String MONTANT = RACINE + "/lignes/{ligneId}/montant";
    public static final String SUPPRESSION = RACINE + "/lignes/suppression";
    public static final String DOCUMENTS = RACINE + "/documents-a-payer";
    public static final String DOCUMENT_DE_LIGNE = RACINE + "/lignes/{ligneId}/document-a-payer";
    public static final String EXPORT_LIGNES = LIGNES + "/export";
    public static final String EXPORT_TRANCHES = TRANCHES + "/export";
    public static final String DROITS = RACINE + "/droits";

    // ------------------------------------------------------------------ enum « type » du fil
    public static final String T_CONCRETISATION = "CONCRETISATION";
    public static final String T_FINALISATION_1 = "FINALISATION_1";
    public static final String T_FINALISATION_2 = "FINALISATION_2";
    public static final String T_DESISTEMENT = "DESISTEMENT";

    public static final String DETAIL = "$.detail";

    private ApiCommissions() {
    }

    public static MockHttpServletRequestBuilder generation(UUID trancheId, String type, Collection<UUID> dossierIds) {
        return post(GENERATION, trancheId).contentType(MediaType.APPLICATION_JSON)
                .content(corps().avec("type", type).avec("dossierIds", dossierIds).json());
    }

    public static MockHttpServletRequestBuilder montant(UUID ligneId, BigDecimal montant) {
        return put(MONTANT, ligneId).contentType(MediaType.APPLICATION_JSON)
                .content(corps().avec("montant", montant).json());
    }

    public static MockHttpServletRequestBuilder suppression(Collection<UUID> ligneIds) {
        return post(SUPPRESSION).contentType(MediaType.APPLICATION_JSON)
                .content(corps().avec("ligneIds", ligneIds).json());
    }

    public static MockHttpServletRequestBuilder document(Collection<UUID> ligneIds) {
        return post(DOCUMENTS).contentType(MediaType.APPLICATION_JSON)
                .content(corps().avec("ligneIds", ligneIds).json());
    }

    public static MockHttpServletRequestBuilder tranche(UUID trancheId) {
        return get(TRANCHE, trancheId);
    }

    /** Valeur numérique d'un chemin JSON (30 ou 30.0 sur le fil : comparer par {@code isEqualByComparingTo}). */
    public static BigDecimal nombre(ResultActions resultat, String chemin) throws Exception {
        Object v = JsonPath.read(resultat.andReturn().getResponse().getContentAsString(), chemin);
        return v == null ? null : new BigDecimal(v.toString());
    }

    public static <T> T lire(ResultActions resultat, String chemin) throws Exception {
        return JsonPath.read(resultat.andReturn().getResponse().getContentAsString(), chemin);
    }

    /** Raisons des dossiers ignorés pour {@code dossierId} ({@code ResultatGenerationDto.ignores}). */
    public static List<String> raisons(ResultActions resultat, UUID dossierId) throws Exception {
        return lire(resultat, "$.ignores[?(@.dossierId == '" + dossierId + "')].raison");
    }

    /** {@code ResultatGenerationDto.creees[*].id}. */
    public static List<String> creees(ResultActions resultat) throws Exception {
        return lire(resultat, "$.creees[*].id");
    }
}
