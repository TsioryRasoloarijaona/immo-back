package com.tsix.immocrm.services.auth;

import com.tsix.immocrm.dto.auth.PersonnelReferenceDto;
import com.tsix.immocrm.dto.auth.UtilisateurEcritureDto;
import com.tsix.immocrm.dto.auth.UtilisateurFicheDto;
import com.tsix.immocrm.dto.auth.UtilisateurReferencesDto;
import com.tsix.immocrm.dto.referentiel.ReferentielItemDto;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.exceptions.ConflictException;
import com.tsix.immocrm.exceptions.InvalidRequestException;
import com.tsix.immocrm.exceptions.ResourceNotFoundException;
import com.tsix.immocrm.repositories.adm.AdmRoleRepository;
import com.tsix.immocrm.repositories.adm.AdmUserRepository;
import com.tsix.immocrm.repositories.adm.AdmWebCulturesRepository;
import com.tsix.immocrm.repositories.cfg.CfgTierRepository;
import com.tsix.immocrm.repositories.stl.StlDepositRepository;
import com.tsix.immocrm.services.patrimoine.ReferentielAdminService;
import com.tsix.immocrm.services.patrimoine.SaisiePatrimoine;
import jakarta.persistence.EntityManager;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.Charset;
import java.util.UUID;

/** Sans readOnly, lectures comprises : la fiche se relit sur la source, {@code StlDepositUser} n'étant pas sur la copie. */
@Service
public class UtilisateursService {

    private static final int CODE_MAX = 100;
    private static final Sort PAR_CODE = Sort.by("code");
    private static final String CODE = "code";
    private static final Charset CP1252 = Charset.forName("windows-1252");

    /** Listes de choix : un record les regroupe pour rester sous S107. */
    @Component
    record Choix(AdmRoleRepository roles, AdmWebCulturesRepository cultures, CfgTierRepository tiers,
                 StlDepositRepository depots) {
    }

    private final AdmUserRepository comptes;
    private final Choix choix;
    private final EcritureUtilisateur ecriture;
    private final FicheUtilisateur fiche;
    private final GardeAdministrateurs garde;
    private final EntityManager em;

    public UtilisateursService(AdmUserRepository comptes, Choix choix, EcritureUtilisateur ecriture, FicheUtilisateur fiche,
                               GardeAdministrateurs garde, EntityManager em) {
        this.comptes = comptes;
        this.choix = choix;
        this.ecriture = ecriture;
        this.fiche = fiche;
        this.garde = garde;
        this.em = em;
    }

    @Transactional
    public UtilisateurReferencesDto references() {
        return new UtilisateurReferencesDto(
                choix.roles().findAll(PAR_CODE).stream()
                        .map(r -> new ReferentielItemDto(r.getId(), r.getCode(), r.getDescription(), null)).toList(),
                choix.cultures().findAll(PAR_CODE).stream()
                        .map(c -> new ReferentielItemDto(c.getId(), c.getCode(), c.getCode(), null)).toList(),
                choix.tiers().findEmployes().stream()
                        .map(t -> new PersonnelReferenceDto(t.getId(), t.getCode(), t.getDescription(),
                                EcritureUtilisateur.vide(t.getEMail()), EcritureUtilisateur.vide(t.getMobile()),
                                EcritureUtilisateur.vide(t.getPhone()))).toList(),
                choix.depots().findAllByOrderByDescriptionAsc().stream()
                        .map(d -> new ReferentielItemDto(d.getId(), d.getCode(), d.getDescription(), null)).toList());
    }

    @Transactional
    public UtilisateurFicheDto detail(UUID id) {
        return fiche.dto(comptes.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable: " + id)));
    }

    @Transactional
    public UtilisateurFicheDto creer(UtilisateurEcritureDto corps) {
        String code = SaisiePatrimoine.texte(
                SaisiePatrimoine.obligatoire(SaisiePatrimoine.sansBlancsAuxBords(corps.code()), null, CODE),
                null, CODE_MAX, CODE);
        // Code est un varchar CP1252 : un caractère hors page serait remplacé à l'écriture, après le contrôle d'unicité.
        if (!CP1252.newEncoder().canEncode(code)) {
            throw new InvalidRequestException("Le code contient un caractère non pris en charge.");
        }
        ReferentielAdminService.verrouiller(em, AdmUser.class, code, "Utilisateur");
        if (comptes.compterParCode(code) > 0) {
            throw new ConflictException("Le code utilisateur %s existe déjà.".formatted(code));
        }
        AdmUser compte = new AdmUser();
        compte.setId(UUID.randomUUID());
        ecriture.creer(compte, code, corps);
        em.persist(compte);
        comptes.flush();
        comptes.retirerDefautPass(compte.getId());
        return fiche.dto(compte);
    }

    @Transactional
    public UtilisateurFicheDto modifier(UUID id, UtilisateurEcritureDto corps) {
        boolean inactifDemande = Boolean.TRUE.equals(corps.inactif());
        if (inactifDemande) {
            garde.verrouiller();
        }
        AdmUser compte = ecriture.compte(id);
        boolean desactivation = inactifDemande && !Boolean.TRUE.equals(compte.getDisabled());
        if (desactivation) {
            garde.avantDesactivation(compte);
        }
        ecriture.modifier(compte, corps);
        if (desactivation) {
            ecriture.couperSession(id);
        }
        return fiche.dto(compte);
    }
}
