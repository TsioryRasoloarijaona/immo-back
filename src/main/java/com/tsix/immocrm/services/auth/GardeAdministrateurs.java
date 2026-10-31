package com.tsix.immocrm.services.auth;

import com.tsix.immocrm.entities.adm.AdmRole;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.exceptions.ConflictException;
import com.tsix.immocrm.repositories.adm.AdmUserRoleRepository;
import com.tsix.immocrm.services.patrimoine.ReferentielAdminService;
import com.tsix.immocrm.services.perimetre.PerimetreCommercialService;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;

/** R8 : le verrou se prend avant de lire le compte, sinon deux retraits simultanés décomptent le même état. */
@Component
class GardeAdministrateurs {

    static final String ADMINISTRATEUR = "Administrateur";
    private static final String DERNIER = "Il doit rester au moins un Administrateur actif.";

    private final AdmUserRoleRepository userRoles;
    private final EntityManager em;

    GardeAdministrateurs(AdmUserRoleRepository userRoles, EntityManager em) {
        this.userRoles = userRoles;
        this.em = em;
    }

    void verrouiller() {
        ReferentielAdminService.verrouiller(em, AdmRole.class, "dernier-administrateur", "Administrateurs");
    }

    void avantDesactivation(AdmUser compte) {
        if (estSoiMeme(compte)) {
            throw new ConflictException("Vous ne pouvez pas désactiver votre propre compte.");
        }
        if (userRoles.existsByAdmUserIdAndAdmRole_Code(compte.getId(), ADMINISTRATEUR)) {
            exigerUnAutre(compte);
        }
    }

    void avantRetraitRole(AdmUser compte) {
        if (estSoiMeme(compte)) {
            throw new ConflictException("Vous ne pouvez pas retirer votre propre rôle Administrateur.");
        }
        if (!Boolean.TRUE.equals(compte.getDisabled())) {
            exigerUnAutre(compte);
        }
    }

    private void exigerUnAutre(AdmUser compte) {
        if (userRoles.compterAutresActifs(ADMINISTRATEUR, compte.getId()) == 0) {
            throw new ConflictException(DERNIER);
        }
    }

    private static boolean estSoiMeme(AdmUser compte) {
        return PerimetreCommercialService.admUserIdCourant().filter(compte.getId()::equals).isPresent();
    }
}
