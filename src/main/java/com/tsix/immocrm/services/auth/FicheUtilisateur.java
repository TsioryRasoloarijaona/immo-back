package com.tsix.immocrm.services.auth;

import com.tsix.immocrm.dto.auth.UtilisateurFicheDto;
import com.tsix.immocrm.entities.adm.AdmRole;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.adm.AdmUserRole;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.stl.StlDepositUser;
import com.tsix.immocrm.repositories.adm.AdmUserRoleRepository;
import com.tsix.immocrm.repositories.app.AppUserCredentialRepository;
import com.tsix.immocrm.repositories.stl.StlDepositUserRepository;
import com.tsix.immocrm.services.administration.RolesService;
import com.tsix.immocrm.utils.Champs;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

import static com.tsix.immocrm.services.auth.EcritureUtilisateur.vide;

@Component
record FicheUtilisateur(AdmUserRoleRepository userRoles, StlDepositUserRepository depots,
                        AppUserCredentialRepository identifiants) {

    UtilisateurFicheDto dto(AdmUser u) {
        List<AdmRole> roles = userRoles.findByAdmUserId(u.getId()).stream().map(AdmUserRole::getAdmRole)
                .filter(Objects::nonNull).toList();
        CfgTier personnel = u.getCfgEmployee();
        return new UtilisateurFicheDto(u.getId(), u.getCode(), u.getDescription(), vide(u.getEmail()), vide(u.getPhone()),
                vide(u.getMobile()), Champs.de(personnel, CfgTier::getId), Champs.de(personnel, CfgTier::getDescription),
                Boolean.TRUE.equals(u.getDisabled()), Boolean.TRUE.equals(u.getVisibleForChat()),
                Boolean.TRUE.equals(u.getCanDisapearNotification()), Boolean.TRUE.equals(u.getMobileUser()),
                u.getEmailSmtpServer(), u.getEmailPortNumber(), Boolean.TRUE.equals(u.getEmailSslActive()),
                u.getWebCultureId(), u.getWebRoleId(), roles.stream().map(AdmRole::getId).toList(),
                depots.findByAdmUserId(u.getId()).stream().map(StlDepositUser::getStlDepositId).toList(),
                RolesService.restrictionsCumulees(roles), identifiants.findByAdmUserId(u.getId()).isPresent(),
                u.getUserCreate(), u.getUserCreateDateTime(), u.getUserUpdate(), u.getUserUpdateDateTime());
    }
}
