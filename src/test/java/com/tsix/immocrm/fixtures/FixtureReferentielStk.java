package com.tsix.immocrm.fixtures;

import com.tsix.immocrm.entities.stk.StkKind;
import com.tsix.immocrm.repositories.stk.StkKindRepository;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Référentiel {@code stk} minimal partagé par les fixtures de test.
 *
 * <p>{@code StkItemType.StkKindId} et {@code StkItemCategory.StkKindId} sont NOT NULL en base :
 * une typologie ou une catégorie existe toujours au sein d'une nature physique. Les fixtures
 * n'ont pour la plupart aucune opinion sur cette nature — elles ont seulement besoin qu'il y en
 * ait une. Ce composant la fournit, plutôt que de faire recopier le même bloc de création dans
 * la vingtaine de classes concernées.
 *
 * <p><b>Créer-si-absent, jamais supposer.</b> La nature n'est pas semée par le bootstrap de la
 * base : la suite doit tourner aussi bien contre le schéma de production reconstitué en CI que
 * contre la base {@code yesRemsTest} historique, où cette ligne n'existe pas. Les tests étant
 * {@code @Transactional}, la création est annulée avec le reste.
 *
 * <p>Les tests qui <em>dépendent</em> de la nature — la génération documentaire distingue
 * {@code Code == "TER"} (terrain) du bâti, voir {@code ActeReservationService} — continuent de
 * construire la leur explicitement. Ce helper ne sert qu'à satisfaire la contrainte.
 */
@Component
public class FixtureReferentielStk {

    /** Identifiant fixe : rend la ligne réutilisable d'un appel à l'autre dans un même test. */
    private static final UUID NATURE_PAR_DEFAUT = UUID.fromString("0F8B1A54-6C2E-4E7D-9B31-2A5D7C0E4411");

    private final StkKindRepository stkKindRepository;

    public FixtureReferentielStk(StkKindRepository stkKindRepository) {
        this.stkKindRepository = stkKindRepository;
    }

    /** Nature physique générique, créée au premier appel puis réutilisée. */
    public StkKind natureParDefaut() {
        return stkKindRepository.findById(NATURE_PAR_DEFAUT).orElseGet(() -> {
            StkKind nature = new StkKind();
            nature.setId(NATURE_PAR_DEFAUT);
            nature.setCode("NATURE-FIXTURE");
            nature.setDescription("Nature par defaut des fixtures de test");
            return stkKindRepository.save(nature);
        });
    }
}
