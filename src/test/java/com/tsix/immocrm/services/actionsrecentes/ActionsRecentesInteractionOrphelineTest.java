package com.tsix.immocrm.services.actionsrecentes;

import com.tsix.immocrm.entities.com.ComActionMessage;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** R8 : une interaction dont la fiche liée a disparu est ignorée, jamais une 500. */
class ActionsRecentesInteractionOrphelineTest {

    @Test
    void interactionSansProspectionNiDossier_ignoree() {
        ComActionMessage m = new ComActionMessage();
        m.setId(UUID.randomUUID());
        m.setDocDate(LocalDateTime.of(2026, 10, 8, 10, 0));

        assertThat(ActionsRecentesService.depuisInteraction(m)).isNull();
    }
}
