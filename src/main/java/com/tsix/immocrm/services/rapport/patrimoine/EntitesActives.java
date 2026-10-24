package com.tsix.immocrm.services.rapport.patrimoine;

import com.tsix.immocrm.domain.rapport.ParametreRapport;
import com.tsix.immocrm.services.perimetre.VisibiliteEntites;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;

import java.util.List;

/** Sociétés actives (R4, clause {@link VisibiliteEntites#CLAUSE_JPQL}), triées par libellé ; valeur = PKey. */
@Component
class EntitesActives {

    private static final String ACTIVES = "SELECT soc.id, soc.description FROM CfgCompany soc WHERE "
            + VisibiliteEntites.CLAUSE_JPQL + " ORDER BY soc.description";

    private final EntityManager em;

    EntitesActives(EntityManager em) {
        this.em = em;
    }

    List<ParametreRapport.Option> lire() {
        return em.createQuery(ACTIVES, Object[].class).getResultList().stream()
                .map(r -> new ParametreRapport.Option(r[0].toString(), (String) r[1])).toList();
    }
}
