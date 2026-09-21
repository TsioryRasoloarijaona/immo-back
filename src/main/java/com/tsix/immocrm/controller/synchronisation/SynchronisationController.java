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
 * Déclenchement manuel de la synchronisation descendante — « synchroniser maintenant » plutôt
 * que d'attendre le prochain cycle automatique.
 *
 * <p>Le service {@link SynchronisationDescendante} n'existe qu'en mode deux-bases ; il est donc
 * injecté par {@link ObjectProvider} pour que ce contrôleur reste présent (et documenté) même
 * en mono-base, où il répond alors 409.
 */
@Tag(name = "Synchronisation", description = "Rapatriement, à la demande, de ce que le logiciel "
        + "de bureau écrit sur la base de référence vers la copie de lecture du CRM. En temps "
        + "normal un cycle automatique s'en charge toutes les deux minutes ; cet endpoint permet "
        + "de le déclencher immédiatement. Ouvert à l'Administrateur et aux commerciaux "
        + "(Commercial, Commercial Chef, Commercial Aide) — l'opération est idempotente, sans "
        + "risque de doublon, et un seul cycle tourne à la fois.")
@RestController
@RequestMapping("/api/synchronisation")
public class SynchronisationController {

    private final ObjectProvider<SynchronisationDescendante> synchronisation;

    public SynchronisationController(ObjectProvider<SynchronisationDescendante> synchronisation) {
        this.synchronisation = synchronisation;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('Administrateur', 'Commercial', 'Commercial Chef', 'Commercial Aide')")
    @Operation(
            summary = "Déclenche immédiatement un cycle de synchronisation descendante",
            description = "Rapatrie sur-le-champ, dans la copie de lecture, ce que le logiciel de "
                    + "bureau a écrit sur la base de référence depuis le dernier cycle — au lieu "
                    + "d'attendre le cycle automatique (toutes les deux minutes). Renvoie le bilan "
                    + "du cycle : lignes fusionnées, supprimées, tables traitées.\n\n"
                    + "**Synchrone** : la réponse n'arrive qu'une fois le cycle terminé (quelques "
                    + "secondes en régime normal ; plus long si une table doit repartir d'une "
                    + "reconstruction complète). **Un seul cycle à la fois** : si un cycle "
                    + "automatique ou manuel tourne déjà, l'appel renvoie 409 sans rien relancer. "
                    + "L'opération est idempotente et sans risque de doublon. Ouvert à "
                    + "l'Administrateur et aux commerciaux (Commercial, Commercial Chef, "
                    + "Commercial Aide)."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cycle exécuté ; le corps décrit ce "
                    + "qui a été rapatrié."),
            @ApiResponse(responseCode = "403", description = "L'appelant n'est ni Administrateur "
                    + "ni commercial."),
            @ApiResponse(responseCode = "409", description = "Soit un cycle est déjà en cours, "
                    + "soit l'application tourne en mono-base (aucune base de référence "
                    + "distincte) : rien n'est déclenché. Le message précise le cas.")
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
