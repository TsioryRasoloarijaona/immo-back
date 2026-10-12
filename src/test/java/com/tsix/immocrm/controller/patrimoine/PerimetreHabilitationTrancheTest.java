package com.tsix.immocrm.controller.patrimoine;

import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.adm.AdmUserHabilitation;
import com.tsix.immocrm.entities.cfg.CfgCompany;
import com.tsix.immocrm.entities.cfg.CfgProject;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.fixtures.FixturePatrimoine;
import com.tsix.immocrm.repositories.adm.AdmUserHabilitationRepository;
import com.tsix.immocrm.services.perimetre.PerimetreCommercialService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * La ligne {@code AdmUserHabilitation} « tranche seule » posée à la création d'une tranche (FEATURE-patrimoine-ecriture.md
 * §2.7) ne doit pas changer le périmètre commercial, qui ne lit que les lignes programme.
 */
@SpringBootTest
@Transactional
class PerimetreHabilitationTrancheTest {

    @Autowired private FixturePatrimoine fixture;
    @Autowired private AdmUserHabilitationRepository habilitationRepository;
    @Autowired private PerimetreCommercialService perimetreService;

    private AdmUser commercial;
    private CfgProject projetA;
    private CfgTranche trancheB;

    @BeforeEach
    void setUp() {
        commercial = fixture.utilisateur("COM-PERIM-TRA");
        CfgCompany entite = fixture.entite("ENT-PERIM-TRA", true);
        projetA = fixture.projet("PRJ-PERIM-TRA-A", entite);
        trancheB = fixture.tranche("TRA-PERIM-TRA-B", fixture.projet("PRJ-PERIM-TRA-B", entite));
    }

    private void habiliter(CfgProject projet, CfgTranche tranche) {
        AdmUserHabilitation h = new AdmUserHabilitation();
        h.setId(UUID.randomUUID());
        h.setAdmUser(commercial);
        h.setCfgProject(projet);
        h.setCfgTranche(tranche);
        habilitationRepository.save(h);
    }

    @Test
    void ligneTrancheSeule_neRestreintPasUnCompteSansProgramme() {
        habiliter(null, trancheB);

        assertThat(perimetreService.perimetreDe(commercial.getId())).isEmpty();
    }

    @Test
    void ligneTrancheSeule_nAjoutePasSonProgrammeAuPerimetre() {
        habiliter(projetA, null);
        habiliter(null, trancheB);

        assertThat(perimetreService.perimetreDe(commercial.getId()))
                .hasValueSatisfying(ids -> assertThat(ids).containsExactly(projetA.getId()));
    }
}
