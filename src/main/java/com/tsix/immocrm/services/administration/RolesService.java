package com.tsix.immocrm.services.administration;

import com.tsix.immocrm.dto.administration.DroitsRoleEcritureDto;
import com.tsix.immocrm.dto.administration.FiltresRoles;
import com.tsix.immocrm.dto.administration.NoeudDroitDto;
import com.tsix.immocrm.dto.administration.RoleDto;
import com.tsix.immocrm.dto.administration.RoleEcritureDto;
import com.tsix.immocrm.dto.administration.RoleLigneDto;
import com.tsix.immocrm.dto.common.PageDto;
import com.tsix.immocrm.entities.adm.AdmRole;
import com.tsix.immocrm.exceptions.ConflictException;
import com.tsix.immocrm.exceptions.ResourceNotFoundException;
import com.tsix.immocrm.repositories.adm.AdmRoleRepository;
import com.tsix.immocrm.services.patrimoine.ReferentielAdminService;
import com.tsix.immocrm.services.patrimoine.SaisiePatrimoine;
import com.tsix.immocrm.web.export.LignesAplaties;
import com.tsix.immocrm.web.export.LignesExport;
import jakarta.persistence.EntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/** Rôles d'OMRAN. Toute écriture verrouille la ligne et passe le verrou Administrateur avant de lire son corps. */
@Service
public class RolesService {

    private static final String ADMINISTRATEUR = "Administrateur";
    private static final String LIBELLE = "Rôle";
    private static final Sort TRI = Sort.by("code");

    private final AdmRoleRepository roles;
    private final DroitsRoleService droits;
    private final EcritureRole ecriture;
    private final EntityManager em;

    public RolesService(AdmRoleRepository roles, DroitsRoleService droits, EcritureRole ecriture, EntityManager em) {
        this.roles = roles;
        this.droits = droits;
        this.ecriture = ecriture;
        this.em = em;
    }

    @Transactional(readOnly = true)
    public PageDto<RoleLigneDto> lister(FiltresRoles filtres, Pageable pageable) {
        return PageDto.of(chercher(filtres, PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), TRI)));
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> lignesExport(FiltresRoles filtres) {
        return chercher(filtres, PageRequest.of(0, LignesExport.LIGNES_EXPORT_MAX, TRI)).stream().map(LignesAplaties::de).toList();
    }

    @Transactional(readOnly = true)
    public List<NoeudDroitDto> arbreDroits() {
        return droits.arbre();
    }

    // Sans readOnly : lu sur la source, AdmRight n'est pas sous Change Tracking (FEATURE-admin-roles.md, décisions du manager).
    @Transactional
    public RoleDto detail(UUID id) {
        return dto(roles.findById(id).orElseThrow(() -> introuvable(id)));
    }

    @Transactional
    public RoleDto creer(RoleEcritureDto corps) {
        String code = SaisiePatrimoine.obligatoire(sansBlancsAuxBords(corps.code()), null, "code");
        ReferentielAdminService.verrouiller(em, AdmRole.class, code, LIBELLE);
        if (roles.existsByCodeIgnoreCase(code)) {
            throw new ConflictException("Le code de rôle %s existe déjà.".formatted(code));
        }
        AdmRole role = new AdmRole();
        role.setId(UUID.randomUUID());
        ecriture.creer(role, code, corps);
        em.persist(role);
        return dto(role);
    }

    // trim() ignore U+00A0, U+2003, U+202F : un sosie visuel passerait la garde d'unicité.
    private static String sansBlancsAuxBords(String valeur) {
        if (valeur == null) {
            return null;
        }
        int debut = 0;
        int fin = valeur.length();
        while (debut < fin && estBlanc(valeur.charAt(debut))) {
            debut++;
        }
        while (fin > debut && estBlanc(valeur.charAt(fin - 1))) {
            fin--;
        }
        return valeur.substring(debut, fin);
    }

    private static boolean estBlanc(char c) {
        return Character.isWhitespace(c) || Character.isSpaceChar(c);
    }

    @Transactional
    public RoleDto modifier(UUID id, RoleEcritureDto corps) {
        AdmRole role = modifiable(id);
        ecriture.modifier(role, corps);
        return dto(role);
    }

    @Transactional
    public RoleDto remplacerDroits(UUID id, DroitsRoleEcritureDto corps) {
        AdmRole role = modifiable(id);
        droits.remplacer(role, corps.codes(), ecriture);
        return dto(role);
    }

    @Transactional
    public void supprimer(UUID id) {
        AdmRole role = modifiable(id);
        List<String> liens = roles.liens(id);
        if (!liens.isEmpty()) {
            throw new ConflictException("Rôle lié à %s : suppression refusée.".formatted(String.join(", ", liens)));
        }
        droits.supprimerTous(id);
        em.remove(role);
    }

    private AdmRole modifiable(UUID id) {
        AdmRole role = roles.findPourEcriture(id).orElseThrow(() -> introuvable(id));
        if (verrouille(role)) {
            throw new ConflictException("Rôle Administrateur : non modifiable, il protège l'accès à l'administration.");
        }
        return role;
    }

    private static boolean verrouille(AdmRole role) {
        return ADMINISTRATEUR.equalsIgnoreCase(role.getCode());
    }

    private static ResourceNotFoundException introuvable(UUID id) {
        return new ResourceNotFoundException("Rôle introuvable: " + id);
    }

    private Page<RoleLigneDto> chercher(FiltresRoles filtres, Pageable pageable) {
        String texte = filtres.texte() == null ? "" : filtres.texte().strip().toLowerCase(Locale.ROOT);
        return roles.chercher(texte, pageable);
    }

    private RoleDto dto(AdmRole role) {
        DroitsRoleService.Repartition repartition = droits.repartition(role.getId());
        return new RoleDto(role.getId(), role.getCode(), role.getDescription(), EcritureRole.lues(role), repartition.codes(),
                repartition.horsArbre(), verrouille(role));
    }
}
