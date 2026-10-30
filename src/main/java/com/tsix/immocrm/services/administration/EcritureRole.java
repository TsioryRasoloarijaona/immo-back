package com.tsix.immocrm.services.administration;

import com.tsix.immocrm.dto.administration.RestrictionsRoleDto;
import com.tsix.immocrm.dto.administration.RoleEcritureDto;
import com.tsix.immocrm.entities.adm.AdmRole;
import com.tsix.immocrm.services.patrimoine.SaisiePatrimoine;
import com.tsix.immocrm.services.patrimoine.TracePatrimoine;
import org.springframework.stereotype.Component;

import java.util.Objects;

import static com.tsix.immocrm.services.patrimoine.SaisiePatrimoine.ou;

/** Saisie d'un rôle : les contrôles passent avant le premier setter, un refus ne laisse rien à flusher. */
@Component
record EcritureRole(TracePatrimoine trace) {

    private static final RestrictionsRoleDto TOUTES = new RestrictionsRoleDto(true, true, true, true, true, true, true, true,
            true, true);

    /** NULL en base = restreint, comme l'accès aux rapports. */
    static RestrictionsRoleDto lues(AdmRole r) {
        return fusion(enBase(r), TOUTES);
    }

    void creer(AdmRole r, String code, RoleEcritureDto d) {
        r.setCode(code);
        r.setDescription(description(d, null));
        ecrire(r, d.restrictions() == null ? TOUTES : fusion(d.restrictions(), TOUTES));
        TracePatrimoine.Trace maintenant = trace.maintenant();
        r.setUserCreate(maintenant.auteur());
        r.setUserCreateDateTime(maintenant.le());
    }

    /** Sans changement effectif, la ligne n'est pas touchée : rien à tracer. */
    void modifier(AdmRole r, RoleEcritureDto d) {
        String description = description(d, r.getDescription());
        RestrictionsRoleDto actuelles = enBase(r);
        RestrictionsRoleDto restrictions = d.restrictions() == null ? actuelles : fusion(d.restrictions(), actuelles);
        if (Objects.equals(description, r.getDescription()) && restrictions.equals(actuelles)) {
            return;
        }
        r.setDescription(description);
        ecrire(r, restrictions);
        marquerModifie(r);
    }

    void marquerModifie(AdmRole r) {
        TracePatrimoine.Trace maintenant = trace.maintenant();
        r.setUserUpdate(maintenant.auteur());
        r.setUserUpdateDateTime(maintenant.le());
    }

    private static String description(RoleEcritureDto d, String actuelle) {
        return d.description() == null ? actuelle
                : SaisiePatrimoine.texte(d.description(), null, SaisiePatrimoine.COURT, "description");
    }

    private static RestrictionsRoleDto enBase(AdmRole r) {
        return new RestrictionsRoleDto(r.getWithAddAttachementRestriction(), r.getWithEditAttachementRestriction(),
                r.getWithDeleteAttachementRestriction(), r.getWithAddActionMessageRestriction(),
                r.getWithEditActionMessageRestriction(), r.getWithDeleteActionMessageRestriction(),
                r.getWithReportRestriction(), r.getWithAuditRestriction(), r.getWithSpecificFieldsRestriction(),
                r.getWithSequenceFreeRestriction());
    }

    private static RestrictionsRoleDto fusion(RestrictionsRoleDto s, RestrictionsRoleDto base) {
        return new RestrictionsRoleDto(ou(s.ajoutPiecesJointes(), base.ajoutPiecesJointes()),
                ou(s.modificationPiecesJointes(), base.modificationPiecesJointes()),
                ou(s.suppressionPiecesJointes(), base.suppressionPiecesJointes()),
                ou(s.ajoutActions(), base.ajoutActions()), ou(s.modificationActions(), base.modificationActions()),
                ou(s.suppressionActions(), base.suppressionActions()), ou(s.rapports(), base.rapports()),
                ou(s.audits(), base.audits()), ou(s.champsSpecifiques(), base.champsSpecifiques()),
                ou(s.sequences(), base.sequences()));
    }

    private static void ecrire(AdmRole r, RestrictionsRoleDto s) {
        r.setWithAddAttachementRestriction(s.ajoutPiecesJointes());
        r.setWithEditAttachementRestriction(s.modificationPiecesJointes());
        r.setWithDeleteAttachementRestriction(s.suppressionPiecesJointes());
        r.setWithAddActionMessageRestriction(s.ajoutActions());
        r.setWithEditActionMessageRestriction(s.modificationActions());
        r.setWithDeleteActionMessageRestriction(s.suppressionActions());
        r.setWithReportRestriction(s.rapports());
        r.setWithAuditRestriction(s.audits());
        r.setWithSpecificFieldsRestriction(s.champsSpecifiques());
        r.setWithSequenceFreeRestriction(s.sequences());
    }
}
