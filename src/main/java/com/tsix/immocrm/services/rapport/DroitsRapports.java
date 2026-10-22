package com.tsix.immocrm.services.rapport;

import com.tsix.immocrm.repositories.adm.AdmReportDataRepository;
import com.tsix.immocrm.services.perimetre.PerimetreCommercialService;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * Droits de lecture des rapports (FEATURE-tdb-vente-rapports-socle.md §2 RD3–RD4) : rôles lus dans {@code AdmUserRole}
 * à chaque appel. Libre si un rôle a {@code WithReportRestriction = 0}, sinon les seuls rapports liés à ses rôles.
 */
@Component
public class DroitsRapports {

    private final AdmReportDataRepository repository;

    public DroitsRapports(AdmReportDataRepository repository) {
        this.repository = repository;
    }

    /** Ids {@code AdmReportData} visibles par l'appelant ; sans compte identifiable, aucun. */
    public Predicate<UUID> visibles() {
        UUID admUserId = PerimetreCommercialService.admUserIdCourant().orElse(null);
        if (admUserId == null) {
            return id -> false;
        }
        if (repository.compterRolesLibres(admUserId) > 0) {
            return id -> true;
        }
        return Set.copyOf(repository.rapportsLies(admUserId))::contains;
    }
}
