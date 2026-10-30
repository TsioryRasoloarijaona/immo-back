package com.tsix.immocrm.services.administration;

import com.tsix.immocrm.config.audit.EcouteurAuditOmran;
import com.tsix.immocrm.dto.administration.NoeudDroitDto;
import com.tsix.immocrm.entities.adm.AdmRight;
import com.tsix.immocrm.entities.adm.AdmRole;
import com.tsix.immocrm.exceptions.InvalidRequestException;
import com.tsix.immocrm.repositories.adm.AdmRightEntityRepository;
import com.tsix.immocrm.repositories.adm.AdmRightRepository;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Droits d'un rôle face à l'arbre, comparés en Java : la collation SQL Server ignorerait la casse. FEATURE-admin-roles.md R8-R11. */
@Component
class DroitsRoleService {

    /** Droits d'un rôle : ceux de l'arbre, et le nombre de ceux qui n'y sont pas. */
    record Repartition(List<String> codes, long horsArbre) {
    }

    private final AdmRightRepository droits;
    private final AdmRightEntityRepository arbre;
    private final EcouteurAuditOmran audit;
    private final EntityManager em;

    DroitsRoleService(AdmRightRepository droits, AdmRightEntityRepository arbre, EcouteurAuditOmran audit, EntityManager em) {
        this.droits = droits;
        this.arbre = arbre;
        this.audit = audit;
        this.em = em;
    }

    List<NoeudDroitDto> arbre() {
        return arbre.findByCodeIsNotNullOrderBySortingAscCodeAsc().stream()
                .map(n -> new NoeudDroitDto(n.getCode(), n.getDescription(), n.getDepth(), n.getKind())).toList();
    }

    Repartition repartition(UUID role) {
        Map<String, String> noeuds = libellesParCode();
        List<AdmRight> lignes = droits.findByAdmRoleId(role);
        List<String> dansArbre = lignes.stream().map(AdmRight::getCode).filter(noeuds::containsKey).toList();
        return new Repartition(dansArbre, (long) lignes.size() - dansArbre.size());
    }

    void remplacer(AdmRole role, List<String> codes, EcritureRole ecriture) {
        if (codes == null) {
            throw new InvalidRequestException("codes est obligatoire.");
        }
        Map<String, String> noeuds = libellesParCode();
        Set<String> coches = new LinkedHashSet<>(codes);
        if (!noeuds.keySet().containsAll(coches)) {
            throw new InvalidRequestException("codes : code absent de l'arbre des droits.");
        }
        List<AdmRight> lignes = droits.findByAdmRoleId(role.getId());
        List<AdmRight> retirees = lignes.stream()
                .filter(l -> noeuds.containsKey(l.getCode()) && !coches.contains(l.getCode())).toList();
        lignes.forEach(l -> coches.remove(l.getCode()));
        if (retirees.isEmpty() && coches.isEmpty()) {
            return;
        }
        retirees.forEach(em::remove);
        coches.forEach(code -> em.persist(ligne(role.getId(), code, noeuds.get(code))));
        ecriture.marquerModifie(role);
        em.persist(audit.resume(role, role.getId(), "%d droits ajoutés, %d retirés".formatted(coches.size(), retirees.size())));
    }

    void supprimerTous(UUID role) {
        droits.findByAdmRoleId(role).forEach(em::remove);
    }

    private Map<String, String> libellesParCode() {
        Map<String, String> libelles = new LinkedHashMap<>();
        arbre.findByCodeIsNotNullOrderBySortingAscCodeAsc().forEach(n -> libelles.put(n.getCode(), n.getDescription()));
        return libelles;
    }

    private static AdmRight ligne(UUID role, String code, String libelle) {
        AdmRight droit = new AdmRight();
        droit.setId(UUID.randomUUID());
        droit.setCode(code);
        droit.setDescription(libelle);
        droit.setAdmRoleId(role);
        return droit;
    }
}
