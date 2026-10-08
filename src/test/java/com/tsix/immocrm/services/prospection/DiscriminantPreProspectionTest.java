package com.tsix.immocrm.services.prospection;

import com.tsix.immocrm.entities.com.ComProspection;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** FIX-sonar-fiabilite.md, correction 1 : test en mémoire null-safe. */
class DiscriminantPreProspectionTest {

    @Test
    void estPreProspection_nullOuSansType_false() {
        assertThat(DiscriminantPreProspection.estPreProspection(null)).isFalse();
        assertThat(DiscriminantPreProspection.estPreProspection(new ComProspection())).isFalse();
    }

    @Test
    void estPreProspection_selonStrEntityId() {
        ComProspection pre = new ComProspection();
        pre.setStrEntityId(DiscriminantPreProspection.ID_PRE_PROSPECTION);
        ComProspection autre = new ComProspection();
        autre.setStrEntityId(UUID.randomUUID());
        assertThat(DiscriminantPreProspection.estPreProspection(pre)).isTrue();
        assertThat(DiscriminantPreProspection.estPreProspection(autre)).isFalse();
    }
}
