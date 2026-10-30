package com.tsix.immocrm.services.auth;

import com.tsix.immocrm.domain.TypeHabilitation;
import com.tsix.immocrm.dto.auth.HabilitationsEcritureDto;
import com.tsix.immocrm.dto.auth.HabilitationsEcritureDto.Cible;
import com.tsix.immocrm.dto.auth.NoeudHabilitationDto;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.adm.AdmUserHabilitation;
import com.tsix.immocrm.entities.cfg.CfgCompany;
import com.tsix.immocrm.entities.cfg.CfgProject;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.exceptions.InvalidRequestException;
import com.tsix.immocrm.exceptions.ResourceNotFoundException;
import com.tsix.immocrm.repositories.adm.AdmUserHabilitationRepository;
import com.tsix.immocrm.repositories.adm.AdmUserRepository;
import com.tsix.immocrm.utils.Champs;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** R13, R14 : les lignes à plusieurs cibles sont gérées dans OMRAN, une ligne hors de l'arbre n'est jamais retirée. */
@Service
public class HabilitationsUtilisateurService {

    private record Noeud(TypeHabilitation type, UUID id, String libelle, UUID parent) {

        Cible cible() {
            return new Cible(type, id);
        }
    }

    private record Arbre(List<Noeud> societes, List<Noeud> projets, List<Noeud> tranches, List<Noeud> travaux) {

        Set<Cible> connus() {
            return Stream.of(societes, projets, tranches, travaux).flatMap(List::stream).map(Noeud::cible)
                    .collect(Collectors.toSet());
        }
    }

    /** Cibles portées par les lignes d'un compte ; une ligne à plusieurs cibles les fige toutes. */
    private record Portee(Set<Cible> simples, Set<Cible> figees) {

        static Portee de(List<AdmUserHabilitation> lignes) {
            Set<Cible> simples = new HashSet<>();
            Set<Cible> figees = new HashSet<>();
            lignes.stream().map(HabilitationsUtilisateurService::cibles).forEach(c -> (c.size() == 1 ? simples : figees).addAll(c));
            return new Portee(simples, figees);
        }
    }

    private final AdmUserHabilitationRepository habilitations;
    private final AdmUserRepository comptes;
    private final EcritureUtilisateur ecriture;
    private final EntityManager em;

    public HabilitationsUtilisateurService(AdmUserHabilitationRepository habilitations, AdmUserRepository comptes,
                                           EcritureUtilisateur ecriture, EntityManager em) {
        this.habilitations = habilitations;
        this.comptes = comptes;
        this.ecriture = ecriture;
        this.em = em;
    }

    @Transactional
    public List<NoeudHabilitationDto> arbre(UUID id) {
        if (!comptes.existsById(id)) {
            throw new ResourceNotFoundException("Utilisateur introuvable: " + id);
        }
        return dto(lire(), id);
    }

    @Transactional
    public List<NoeudHabilitationDto> remplacer(UUID id, HabilitationsEcritureDto corps) {
        if (corps.noeuds() == null) {
            throw new InvalidRequestException("noeuds est obligatoire : envoyez [] pour tout retirer.");
        }
        AdmUser compte = ecriture.compte(id);
        Arbre arbre = lire();
        Set<Cible> connus = arbre.connus();
        Set<Cible> voulus = new HashSet<>(corps.noeuds());
        if (!connus.containsAll(voulus)) {
            throw new InvalidRequestException("Nœud d'habilitation inconnu.");
        }
        List<AdmUserHabilitation> lignes = habilitations.findByAdmUserId(id);
        Portee portee = Portee.de(lignes);
        voulus.removeAll(portee.figees());
        lignes.stream().filter(l -> retirable(cibles(l), connus, voulus, portee.figees())).forEach(em::remove);
        voulus.removeAll(portee.simples());
        voulus.forEach(c -> ajouter(compte, c));
        return dto(arbre, id);
    }

    private static boolean retirable(List<Cible> cibles, Set<Cible> connus, Set<Cible> voulus, Set<Cible> figees) {
        if (cibles.size() != 1) {
            return false;
        }
        Cible cible = cibles.getFirst();
        return connus.contains(cible) && !voulus.contains(cible) && !figees.contains(cible);
    }

    private void ajouter(AdmUser compte, Cible cible) {
        AdmUserHabilitation ligne = new AdmUserHabilitation();
        ligne.setId(UUID.randomUUID());
        ligne.setAdmUser(compte);
        switch (cible.type()) {
            case SOCIETE -> ligne.setCfgCompany(em.getReference(CfgCompany.class, cible.id()));
            case PROJET -> ligne.setCfgProject(em.getReference(CfgProject.class, cible.id()));
            case TRANCHE -> ligne.setCfgTranche(em.getReference(CfgTranche.class, cible.id()));
            case PROJET_TRAVAUX -> ligne.setPrjProjectId(cible.id());
        }
        em.persist(ligne);
    }

    private static List<Cible> cibles(AdmUserHabilitation h) {
        return Stream.of(new Cible(TypeHabilitation.SOCIETE, Champs.de(h.getCfgCompany(), CfgCompany::getId)),
                        new Cible(TypeHabilitation.PROJET, Champs.de(h.getCfgProject(), CfgProject::getId)),
                        new Cible(TypeHabilitation.TRANCHE, Champs.de(h.getCfgTranche(), CfgTranche::getId)),
                        new Cible(TypeHabilitation.PROJET_TRAVAUX, h.getPrjProjectId()))
                .filter(c -> c.id() != null).toList();
    }

    private Arbre lire() {
        return new Arbre(noeuds(TypeHabilitation.SOCIETE, habilitations.noeudsSocietes()),
                noeuds(TypeHabilitation.PROJET, habilitations.noeudsProjets()),
                noeuds(TypeHabilitation.TRANCHE, habilitations.noeudsTranches()),
                noeuds(TypeHabilitation.PROJET_TRAVAUX, habilitations.noeudsProjetsTravaux()));
    }

    private static List<Noeud> noeuds(TypeHabilitation type, List<Object[]> lignes) {
        return lignes.stream().map(l -> new Noeud(type, uuid(l[0]), String.valueOf(l[1]), uuid(l[2]))).toList();
    }

    private static UUID uuid(Object valeur) {
        return UUID.fromString(String.valueOf(valeur));
    }

    private List<NoeudHabilitationDto> dto(Arbre a, UUID compte) {
        Portee portee = Portee.de(habilitations.findByAdmUserId(compte));
        Map<UUID, List<Noeud>> projets = parParent(a.projets());
        Map<UUID, List<Noeud>> tranches = parParent(a.tranches());
        Map<UUID, List<Noeud>> travaux = parParent(a.travaux());
        return a.societes().stream().map(s -> {
            List<NoeudHabilitationDto> enfants = new ArrayList<>();
            projets.getOrDefault(s.id(), List.of()).forEach(p -> enfants.add(
                    dto(p, portee, feuilles(tranches.getOrDefault(p.id(), List.of()), portee))));
            enfants.addAll(feuilles(travaux.getOrDefault(s.id(), List.of()), portee));
            return dto(s, portee, enfants);
        }).toList();
    }

    private static Map<UUID, List<Noeud>> parParent(List<Noeud> noeuds) {
        return noeuds.stream().collect(Collectors.groupingBy(Noeud::parent));
    }

    private static List<NoeudHabilitationDto> feuilles(List<Noeud> noeuds, Portee portee) {
        return noeuds.stream().map(n -> dto(n, portee, List.of())).toList();
    }

    private static NoeudHabilitationDto dto(Noeud n, Portee portee, List<NoeudHabilitationDto> enfants) {
        boolean figee = portee.figees().contains(n.cible());
        return new NoeudHabilitationDto(n.type(), n.id(), n.libelle(), figee || portee.simples().contains(n.cible()), figee,
                enfants);
    }
}
