package com.tsix.immocrm.services.notification;

import com.tsix.immocrm.entities.cfg.CfgProject;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComProspection;
import com.tsix.immocrm.services.prospection.DiscriminantPreProspection;
import com.tsix.immocrm.services.prospection.ProspectionService;
import com.tsix.immocrm.utils.Champs;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.UUID;

/**
 * Évènement d'un lot d'écritures de commercial (création, import, répartir, distribuer, réaffecter), publié dans la
 * transaction et traité après commit. {@code aDistribuer} : leads importés restés sans commercial.
 */
public record LeadsAttribues(@Nullable UUID acteurId, List<Lead> attribues, List<Lead> aDistribuer) {

    /** Instantané d'une ligne : l'écouteur tourne hors de la session qui l'a écrite. */
    public record Lead(UUID id, String code, String nom, @Nullable UUID projetId, @Nullable String projetCode,
                       @Nullable UUID commercialId, boolean preProspection) {

        public static Lead de(ComProspection p) {
            CfgTranche tranche = p.getStkHierarchy();
            CfgProject projet = tranche != null ? tranche.getCfgProject() : null;
            return new Lead(p.getId(), p.getCode(), ProspectionService.nomProspect(p.getCfgProspect(), p),
                    projet != null ? projet.getId() : null, Champs.de(projet, CfgProject::getCode),
                    Champs.de(p.getCfgCommercial(), CfgTier::getId),
                    DiscriminantPreProspection.estPreProspection(p));
        }
    }

    public boolean estVide() {
        return attribues.isEmpty() && aDistribuer.isEmpty();
    }
}
