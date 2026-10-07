package com.tsix.immocrm.services.prospection;

import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.com.ComProspection;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link ProspectionService#nomProspect} — voir {@code FEATURE-nom-pre-prospection-relances.md}.
 * La fiche tiers gagne ; à défaut, le nom saisi au premier contact ({@code TempDescription}).
 */
class NomProspectTest {

    private static CfgTier tier(String prenom, String nom) {
        CfgTier t = new CfgTier();
        t.setFirstName(prenom);
        t.setLastName(nom);
        return t;
    }

    private static ComProspection prospection(String tempDescription) {
        ComProspection p = new ComProspection();
        p.setTempDescription(tempDescription);
        return p;
    }

    @Test
    void ficheAvecNom_gagneSurTempDescription() {
        assertThat(ProspectionService.nomProspect(tier("Sara", "ALAMI"), prospection("Autre nom")))
                .isEqualTo("Sara ALAMI");
    }

    @Test
    void sansFiche_replieSurTempDescriptionNettoye() {
        assertThat(ProspectionService.nomProspect(null, prospection("  Karim Lead  ")))
                .isEqualTo("Karim Lead");
    }

    @Test
    void ficheSansAucunNom_replieSurTempDescription() {
        assertThat(ProspectionService.nomProspect(tier(null, "  "), prospection("Karim Lead")))
                .isEqualTo("Karim Lead");
    }

    @Test
    void toutVide_rendNull() {
        assertThat(ProspectionService.nomProspect(null, prospection("   "))).isNull();
        assertThat(ProspectionService.nomProspect(tier(null, null), prospection(null))).isNull();
    }

    @Test
    void prospectionNulle_seComporteCommeSansTempDescription() {
        assertThat(ProspectionService.nomProspect(null, null)).isNull();
        assertThat(ProspectionService.nomProspect(tier("Sara", "ALAMI"), null)).isEqualTo("Sara ALAMI");
    }
}
