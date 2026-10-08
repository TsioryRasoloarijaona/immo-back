package com.tsix.immocrm.controller.synchronisation;

import com.tsix.immocrm.config.SynchronisationDescendante;
import com.tsix.immocrm.config.SynchronisationDescendante.ResultatSync;
import com.tsix.immocrm.dto.synchronisation.SynchronisationResultatDto;
import com.tsix.immocrm.exceptions.ConflictException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * {@link SynchronisationDescendante} n'existe qu'en mode deux-bases : injecté par
 * {@link ObjectProvider} pour que la route reste publiée en mono-base, où elle répond 409.
 */
@Tag(name = "Synchronisation")
@RestController
@RequestMapping("/api/synchronisation")
public class SynchronisationController {

    private final ObjectProvider<SynchronisationDescendante> synchronisation;

    public SynchronisationController(ObjectProvider<SynchronisationDescendante> synchronisation) {
        this.synchronisation = synchronisation;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('Administrateur', 'Commercial', 'Commercial Chef', 'Commercial Aide')")
    @Operation(summary = "Déclenche immédiatement un cycle de synchronisation descendante")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cycle exécuté ; le corps décrit ce "
                    + "qui a été rapatrié."),
            @ApiResponse(responseCode = "403", description = "L'appelant n'est ni Administrateur "
                    + "ni commercial."),
            @ApiResponse(responseCode = "409")
    })
    public SynchronisationResultatDto declencher() {
        SynchronisationDescendante service = synchronisation.getIfAvailable();
        if (service == null) {
            throw new ConflictException("Synchronisation indisponible : l'application tourne en "
                    + "mode mono-base (aucune base de référence distincte à rapatrier).");
        }
        ResultatSync resultat = service.executer();
        if (!resultat.declenchee()) {
            throw new ConflictException("Un cycle de synchronisation est déjà en cours ; "
                    + "réessayez dans un instant.");
        }
        return SynchronisationResultatDto.de(resultat);
    }
}
