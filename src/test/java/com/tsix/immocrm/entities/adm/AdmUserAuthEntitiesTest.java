package com.tsix.immocrm.entities.adm;

import com.tsix.immocrm.entities.app.AppUserCredential;
import com.tsix.immocrm.repositories.adm.AdmRoleRepository;
import com.tsix.immocrm.repositories.adm.AdmUserRepository;
import com.tsix.immocrm.repositories.adm.AdmUserRoleRepository;
import com.tsix.immocrm.repositories.app.AppUserCredentialRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Phase 0 sanity check: the new Auth entities (AdmUser/AdmRole/AdmUserRole, mirroring the legacy
 * schema, and the new app-owned AppUserCredential) map correctly and round-trip.
 */
@SpringBootTest
@Transactional
class AdmUserAuthEntitiesTest {

    @Autowired
    private AdmUserRepository userRepository;

    @Autowired
    private AdmRoleRepository roleRepository;

    @Autowired
    private AdmUserRoleRepository userRoleRepository;

    @Autowired
    private AppUserCredentialRepository credentialRepository;

    @Test
    void savesAndLoadsUserRoleAndCredential() {
        AdmUser user = new AdmUser();
        user.setId(UUID.randomUUID());
        user.setCode("test.auth." + UUID.randomUUID());
        user.setDisabled(false);
        userRepository.save(user);

        AdmRole role = new AdmRole();
        role.setId(UUID.randomUUID());
        role.setCode("Commercial-TEST-" + UUID.randomUUID());
        roleRepository.save(role);

        AdmUserRole userRole = new AdmUserRole();
        userRole.setId(UUID.randomUUID());
        userRole.setAdmUser(user);
        userRole.setAdmRole(role);
        userRoleRepository.save(userRole);

        AppUserCredential credential = new AppUserCredential();
        credential.setId(UUID.randomUUID());
        credential.setAdmUser(user);
        credential.setPasswordHash("$2a$10$abcdefghijklmnopqrstuv");
        credential.setUserCreateDateTime(LocalDateTime.now());
        credentialRepository.save(credential);

        assertThat(userRoleRepository.findByAdmUserId(user.getId())).hasSize(1);
        assertThat(credentialRepository.findByAdmUserId(user.getId()))
                .isPresent()
                .get()
                .satisfies(c -> {
                    assertThat(c.getMustChangePassword()).isTrue();
                    assertThat(c.getProfileCompleted()).isFalse();
                    assertThat(c.getTokenVersion()).isZero();
                });
        assertThat(userRepository.findByCode(user.getCode())).isPresent();
    }
}
