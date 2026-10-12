package com.tsix.immocrm.config.audit;

import com.tsix.immocrm.domain.audit.ChangementAudit;
import com.tsix.immocrm.domain.audit.DetailsAudit;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.adm.AdmWebHistorics;
import com.tsix.immocrm.entities.cfg.CfgCompany;
import com.tsix.immocrm.entities.cfg.CfgProject;
import com.tsix.immocrm.fixtures.FixturePatrimoine;
import com.tsix.immocrm.repositories.adm.AdmWebHistoricsRepository;
import com.tsix.immocrm.repositories.cfg.CfgProjectRepository;
import com.tsix.immocrm.security.AuthenticatedUser;
import jakarta.persistence.EntityManager;
import org.hibernate.engine.spi.SessionImplementor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Revue FEATURE-patrimoine-audit-actions.md §2.2 r4 : plusieurs flushs, une ligne, ancien état = début de transaction. */
@SpringBootTest
@Transactional
class AuditOmranRevueTest {

    @Autowired private FixturePatrimoine fixture;
    @Autowired private EntityManager em;
    @Autowired private CfgProjectRepository projectRepository;
    @Autowired private AdmWebHistoricsRepository historiques;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private CfgProject preparer(String code) {
        CfgCompany entite = fixture.entite("ENT-" + code, true);
        CfgProject projet = fixture.projet(code, entite);
        AdmUser admin = fixture.utilisateur("ADM-" + code);
        em.flush();
        em.clear();
        EcouteurAuditOmran.viderTampons();
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new AuthenticatedUser(admin.getId(), admin.getCode(), null, false, true, 0), null, List.of()));
        return projectRepository.findById(projet.getId()).orElseThrow();
    }

    private void simulerCommit() {
        em.flush();
        em.unwrap(SessionImplementor.class).getActionQueue().beforeTransactionCompletion();
    }

    @Test
    void deuxModificationsFlusheesSeparement_uneLigneDepuisLEtatInitial() {
        CfgProject projet = preparer("PRJ-REV-2F");

        projet.setDescription("Intermédiaire");
        em.flush();
        projet.setDescription("Final");
        simulerCommit();

        assertThat(historiques.findByObjectId(projet.getId())).singleElement().satisfies(l ->
                assertThat(DetailsAudit.lire(l.getDetails())).containsExactly(
                        new ChangementAudit("Description", "Programme PRJ-REV-2F", "Final")));
    }

    @Test
    void modifieePuisRetablieDansLaTransaction_aucuneLigne() {
        CfgProject projet = preparer("PRJ-REV-AR");

        projet.setDescription("Temporaire");
        em.flush();
        projet.setDescription("Programme PRJ-REV-AR");
        simulerCommit();

        assertThat(historiques.findByObjectId(projet.getId())).extracting(AdmWebHistorics::getAction).isEmpty();
    }
}
