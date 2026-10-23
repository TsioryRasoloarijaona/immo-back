package com.tsix.immocrm.services.rapport.vente;

import com.tsix.immocrm.domain.InteractionKpi;
import com.tsix.immocrm.domain.rapport.ContexteRapport;
import com.tsix.immocrm.domain.rapport.ModuleRapport;
import com.tsix.immocrm.domain.rapport.ParametreRapport;
import com.tsix.immocrm.domain.rapport.Rapport;
import com.tsix.immocrm.domain.rapport.ResultatRapport;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * N° 20 TAUX DE TRANSFORMATION PAR COMMERCIAL (FEATURE-tdb-vente-rapports-commercial.md §6.1), modèle OMRAN
 * {@code ComTdbCommercialTransformation} : actions comptées = prospections de type Visite (lu par code).
 */
@Component
public class TauxTransformationCommercialRapport implements Rapport {

    static final String CODE = "ComTdbCommercialTransformation";
    private static final TransformationCommerciale.Variante VARIANTE =
            new TransformationCommerciale.Variante("visites", "Visites", false);

    private final TransformationCommerciale transformation;
    private final EntityManager em;

    public TauxTransformationCommercialRapport(TransformationCommerciale transformation, EntityManager em) {
        this.transformation = transformation;
        this.em = em;
    }

    @Override
    public String code() {
        return CODE;
    }

    @Override
    public ModuleRapport module() {
        return ModuleRapport.VENTE;
    }

    @Override
    public List<ParametreRapport> parametres() {
        return transformation.parametres();
    }

    @Override
    public ResultatRapport executer(ContexteRapport contexte) {
        Set<UUID> visites = Set.copyOf(em.createQuery("SELECT t.id FROM ComActionMessageType t WHERE t.code = :code",
                UUID.class).setParameter("code", InteractionKpi.TYPE_VISITE).getResultList());
        return transformation.executer(contexte, visites, VARIANTE, List.of());
    }
}
