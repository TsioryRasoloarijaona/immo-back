package com.tsix.immocrm.services.rapport.vente;

import com.tsix.immocrm.domain.InteractionKpi;
import com.tsix.immocrm.domain.rapport.ContexteRapport;
import com.tsix.immocrm.domain.rapport.ModuleRapport;
import com.tsix.immocrm.domain.rapport.ParametreRapport;
import com.tsix.immocrm.domain.rapport.Rapport;
import com.tsix.immocrm.domain.rapport.ResultatRapport;
import com.tsix.immocrm.domain.rapport.TypeParametre;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * N° 26 TAUX DE TRANSFORMATION PAR NATURE D'ACTION (FEATURE-tdb-vente-rapports-commercial.md §6.2), modèle OMRAN
 * {@code ComTdbCommercialTransformationByComActionMessageType} : le calcul du n° 20 sur le type d'action choisi.
 */
@Component
public class TauxTransformationParNatureRapport implements Rapport {

    static final String CODE = "ComTdbCommercialTransformationByComActionMessageType";
    private static final String TYPE_ACTION = "typeAction";
    private static final String ACTIONS = "actions";

    private final TransformationCommerciale transformation;
    private final EntityManager em;

    public TauxTransformationParNatureRapport(TransformationCommerciale transformation, EntityManager em) {
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

    /** {@code typeAction} : types d'interaction lus en base à chaque requête, défaut = type de code Visite. */
    @Override
    public List<ParametreRapport> parametres() {
        List<Object[]> types = em.createQuery("SELECT t.id, t.code, t.description FROM ComActionMessageType t "
                + "ORDER BY t.description, t.code", Object[].class).getResultList();
        List<ParametreRapport.Option> options = types.stream().map(t -> new ParametreRapport.Option(
                t[0].toString(), Objects.toString(t[2], (String) t[1]))).toList();
        String visite = types.stream().filter(t -> InteractionKpi.TYPE_VISITE.equals(t[1]))
                .map(t -> t[0].toString()).findFirst().orElse(null);
        List<ParametreRapport> parametres = new ArrayList<>(transformation.parametres());
        parametres.add(new ParametreRapport(TYPE_ACTION, "Type d'action", TypeParametre.CHOIX, false, false, visite,
                options, null, null));
        return parametres;
    }

    @Override
    public ResultatRapport executer(ContexteRapport contexte) {
        String choisi = contexte.texte(TYPE_ACTION);
        if (choisi == null) {
            return transformation.executer(contexte, Set.of(), new TransformationCommerciale.Variante(ACTIONS,
                    "Actions", true), List.of("Aucun type d'action choisi ni type Visite en base : actions à 0."));
        }
        UUID type = UUID.fromString(choisi);
        String libelle = em.createQuery("SELECT COALESCE(t.description, t.code) FROM ComActionMessageType t "
                + "WHERE t.id = :id", String.class).setParameter("id", type).getSingleResult();
        return transformation.executer(contexte, Set.of(type),
                new TransformationCommerciale.Variante(ACTIONS, libelle, true), List.of());
    }
}
