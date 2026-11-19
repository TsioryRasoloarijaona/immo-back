package com.tsix.immocrm.services.projet;

import com.tsix.immocrm.dto.projet.NoeudNivellementDto;
import com.tsix.immocrm.entities.cfg.CfgTrancheLevelingDefinition;
import com.tsix.immocrm.entities.prj.PrjProject;
import com.tsix.immocrm.exceptions.InvalidRequestException;
import com.tsix.immocrm.exceptions.ResourceNotFoundException;
import com.tsix.immocrm.services.patrimoine.SuppressionProjet;
import com.tsix.immocrm.services.patrimoine.TracePatrimoine;
import com.tsix.immocrm.services.perimetre.PerimetreProjetService;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/** Définition de nivellement d'un projet, arbre bâti sur les lignes du {@code StkLeveling} du projet (FEATURE-n9-planification.md R4). */
@Service
@Transactional(readOnly = true)
public class NivellementProjetService {

    static final String AUCUN_NIVEAU = "Aucun niveau n'est défini pour ce projet : renseigner le nivellement du projet.";
    static final String NIVEAU_INFERIEUR = "Vous ne pouvez pas ajouter un niveau inférieur";
    private static final int LONGUEUR_CODE = 255;
    // Tables qui citent un nœud ; yesRemsTest n'a pas les FK de production : refus explicite (D7).
    private static final List<Map.Entry<String, String>> CITATIONS = List.of(
            Map.entry("SELECT COUNT(*) FROM PrjMarketHierarchy WHERE StkHierarchyId IN (:ids)", "lot(s) (PrjMarketHierarchy)"),
            Map.entry("SELECT COUNT(*) FROM GrhWorkedDay WHERE CfgTrancheLevelingDefinitionId IN (:ids)",
                    "journée(s) de travail (GrhWorkedDay)"),
            Map.entry("SELECT COUNT(*) FROM BulMeetingPrjProjectProgressCriteriaLine WHERE CfgTrancheLevelingdefinitionId IN (:ids)",
                    "avancement(s) de réunion (BulMeetingPrjProjectProgressCriteriaLine)"));

    private final EntityManager em;
    private final PerimetreProjetService perimetre;
    private final TracePatrimoine trace;

    public NivellementProjetService(EntityManager em, PerimetreProjetService perimetre, TracePatrimoine trace) {
        this.em = em;
        this.perimetre = perimetre;
        this.trace = trace;
    }

    public List<NoeudNivellementDto> lister(UUID projetId) {
        projetVisible(projetId);
        return noeudsDu(projetId).stream().map(NivellementProjetService::dto).toList();
    }

    /** Code = « code de la ligne de niveau de même profondeur » + rang parmi les frères (OMRAN {@code AddRoot/AddChild}). */
    @Transactional
    public NoeudNivellementDto creer(UUID projetId, UUID parentId) {
        PrjProject projet = projetVisible(projetId);
        if (parentId != null) {
            noeud(projetId, parentId);
        }
        Map<Integer, String> niveaux = niveaux(projet.getStkLevelingId());
        if (niveaux.isEmpty()) {
            throw new InvalidRequestException(AUCUN_NIVEAU);
        }
        List<CfgTrancheLevelingDefinition> noeuds = noeudsDu(projetId);
        String libelle = niveaux.get(profondeur(noeuds, parentId, Collections.max(niveaux.keySet())));
        if (libelle == null) {
            throw new InvalidRequestException(NIVEAU_INFERIEUR);
        }
        int ordre = (int) noeuds.stream().filter(n -> Objects.equals(n.getParentId(), parentId)).count() + 1;
        CfgTrancheLevelingDefinition n = new CfgTrancheLevelingDefinition();
        n.setId(UUID.randomUUID());
        n.setParentId(parentId);
        n.setCfgTrancheId(projetId);
        n.setSorting(ordre);
        n.setCode(libelle + " " + ordre);
        trace.horodater(n);
        em.persist(n);
        return dto(n);
    }

    @Transactional
    public NoeudNivellementDto modifier(UUID projetId, UUID noeudId, String code) {
        projetVisible(projetId);
        CfgTrancheLevelingDefinition n = noeud(projetId, noeudId);
        if (code == null || code.isBlank() || code.length() > LONGUEUR_CODE) {
            throw new InvalidRequestException("Le code est obligatoire (255 caractères au plus).");
        }
        n.setCode(code);
        trace.horodater(n);
        return dto(n);
    }

    /** Le nœud et ses descendants, refusé si l'un d'eux est cité ; enfants supprimés avant leur parent. */
    @Transactional
    public void supprimer(UUID projetId, UUID noeudId) {
        projetVisible(projetId);
        CfgTrancheLevelingDefinition racine = noeud(projetId, noeudId);
        List<CfgTrancheLevelingDefinition> branche = branche(noeudsDu(projetId), racine);
        List<UUID> ids = branche.stream().map(CfgTrancheLevelingDefinition::getId).toList();
        List<String> presents = new ArrayList<>();
        CITATIONS.forEach(c -> {
            long n = ((Number) em.createNativeQuery(c.getKey()).setParameter("ids", ids).getSingleResult()).longValue();
            if (n > 0) {
                presents.add(n + " " + c.getValue());
            }
        });
        if (!presents.isEmpty()) {
            throw SuppressionProjet.utilise("le niveau " + racine.getCode(), presents);
        }
        branche.reversed().forEach(em::remove);
    }

    private PrjProject projetVisible(UUID projetId) {
        PrjProject projet = perimetre.estVisible(projetId) ? em.find(PrjProject.class, projetId) : null;
        if (projet == null) {
            throw new ResourceNotFoundException("Projet introuvable: " + projetId);
        }
        return projet;
    }

    /** Projet lu sur la ligne stockée : un nœud d'un autre projet, adressé sous cette URL, est introuvable (D6). */
    private CfgTrancheLevelingDefinition noeud(UUID projetId, UUID noeudId) {
        CfgTrancheLevelingDefinition n = em.find(CfgTrancheLevelingDefinition.class, noeudId);
        if (n == null || !projetId.equals(n.getCfgTrancheId())) {
            throw new ResourceNotFoundException("Nœud de nivellement introuvable: " + noeudId);
        }
        return n;
    }

    private List<CfgTrancheLevelingDefinition> noeudsDu(UUID projetId) {
        return em.createQuery("select n from CfgTrancheLevelingDefinition n where n.cfgTrancheId = :projet"
                        + " order by n.sorting asc, n.code asc", CfgTrancheLevelingDefinition.class)
                .setParameter("projet", projetId)
                .getResultList();
    }

    /** Rang (Sorting) → code des lignes du nivellement ; vide si le projet n'en a pas. */
    private Map<Integer, String> niveaux(UUID stkLevelingId) {
        Map<Integer, String> niveaux = new HashMap<>();
        if (stkLevelingId == null) {
            return niveaux;
        }
        List<?> lignes = em.createNativeQuery("SELECT Sorting, Code FROM StkLevelingLine WHERE StkLevelId = :id"
                        + " AND Sorting IS NOT NULL ORDER BY Sorting ASC")
                .setParameter("id", stkLevelingId)
                .getResultList();
        lignes.forEach(l -> {
            Object[] ligne = (Object[]) l;
            niveaux.putIfAbsent(((Number) ligne[0]).intValue(), (String) ligne[1]);
        });
        return niveaux;
    }

    /** 1 pour une racine ; s'arrête au-delà de {@code max} (aussi contre un cycle de ParentId). */
    private static int profondeur(List<CfgTrancheLevelingDefinition> noeuds, UUID parentId, int max) {
        Map<UUID, UUID> parents = new HashMap<>();
        noeuds.forEach(n -> parents.put(n.getId(), n.getParentId()));
        int profondeur = 1;
        for (UUID courant = parentId; courant != null && profondeur <= max; courant = parents.get(courant)) {
            profondeur++;
        }
        return profondeur;
    }

    /** Le nœud puis ses descendants, en largeur. */
    private static List<CfgTrancheLevelingDefinition> branche(List<CfgTrancheLevelingDefinition> noeuds,
                                                              CfgTrancheLevelingDefinition racine) {
        List<CfgTrancheLevelingDefinition> branche = new ArrayList<>(List.of(racine));
        for (int i = 0; i < branche.size(); i++) {
            UUID parent = branche.get(i).getId();
            noeuds.stream().filter(n -> parent.equals(n.getParentId()) && !branche.contains(n)).forEach(branche::add);
        }
        return branche;
    }

    private static NoeudNivellementDto dto(CfgTrancheLevelingDefinition n) {
        return new NoeudNivellementDto(n.getId(), n.getParentId(), n.getCode(), n.getSorting());
    }
}
