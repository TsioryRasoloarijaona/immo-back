package com.tsix.immocrm.services.patrimoine;

import com.tsix.immocrm.dto.referentiel.ContenanceDto;
import com.tsix.immocrm.dto.referentiel.ReferentielItemDto;
import com.tsix.immocrm.entities.CodeLibelleOmran;
import com.tsix.immocrm.entities.stk.StkContenance;
import com.tsix.immocrm.entities.stk.StkCostCategory;
import com.tsix.immocrm.entities.stk.StkFacade;
import com.tsix.immocrm.entities.stk.StkOrientation;
import com.tsix.immocrm.entities.stk.StkPresentationType;
import com.tsix.immocrm.entities.stk.StkVue;
import com.tsix.immocrm.exceptions.ResourceNotFoundException;
import com.tsix.immocrm.repositories.cfg.TrancheVisibleRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Référentiels de la fiche article (FEATURE-patrimoine-article-fiche.md §1, §2, §9.9) et résolution d'une clé saisie. */
@Component
@Transactional(readOnly = true)
public class ReferencesFicheArticle {

    private static final String CODE = "code";

    private final EntityManager em;
    private final TrancheVisibleRepository tranches;

    public ReferencesFicheArticle(EntityManager em, TrancheVisibleRepository tranches) {
        this.em = em;
        this.tranches = tranches;
    }

    /** Types de présentation de la tranche (R4 : entité inactive → 404). */
    public List<ReferentielItemDto> presentations(UUID trancheId) {
        if (tranches.trancheVisible(trancheId).isEmpty()) {
            throw new ResourceNotFoundException("Tranche introuvable: " + trancheId);
        }
        return em.createQuery("SELECT p FROM StkPresentationType p WHERE p.stkHierarchy.id = :id ORDER BY p.code",
                        StkPresentationType.class)
                .setParameter("id", trancheId).getResultStream()
                .map(p -> new ReferentielItemDto(p.getId(), p.getCode(), p.getDescription(), null)).toList();
    }

    public List<ReferentielItemDto> orientations() {
        return items(StkOrientation.class);
    }

    public List<ReferentielItemDto> categoriesCharge() {
        return items(StkCostCategory.class);
    }

    public List<ReferentielItemDto> facades() {
        return items(StkFacade.class);
    }

    public List<ReferentielItemDto> vues() {
        return items(StkVue.class);
    }

    public List<ContenanceDto> contenances() {
        return tous(StkContenance.class, "sorting", CODE).stream()
                .map(c -> new ContenanceDto(c.getId(), c.getCode(), c.getDescription(), c.getOption1(),
                        Boolean.TRUE.equals(c.getIsOption1WithQuantity()), c.getOption2(),
                        Boolean.TRUE.equals(c.getIsOption2WithQuantity()), c.getOption3(),
                        Boolean.TRUE.equals(c.getIsOption3WithQuantity())))
                .toList();
    }

    <T> T reference(Class<T> classe, UUID id, String champ) {
        return ReferencesPatrimoine.trouver(cle -> Optional.ofNullable(em.find(classe, cle)), id, champ);
    }

    private <T extends CodeLibelleOmran> List<ReferentielItemDto> items(Class<T> classe) {
        return tous(classe, CODE).stream()
                .map(r -> new ReferentielItemDto(r.getId(), r.getCode(), r.getDescription(), null)).toList();
    }

    private <T> List<T> tous(Class<T> classe, String... tri) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<T> requete = cb.createQuery(classe);
        Root<T> racine = requete.from(classe);
        requete.orderBy(Arrays.stream(tri).map(colonne -> cb.asc(racine.get(colonne))).toList());
        return em.createQuery(requete).getResultList();
    }
}
