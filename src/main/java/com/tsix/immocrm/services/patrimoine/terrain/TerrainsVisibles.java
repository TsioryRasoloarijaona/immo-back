package com.tsix.immocrm.services.patrimoine.terrain;

import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.entities.stk.StkKind;
import com.tsix.immocrm.entities.str.StrStatus;
import com.tsix.immocrm.exceptions.ConflictException;
import com.tsix.immocrm.exceptions.ResourceNotFoundException;
import com.tsix.immocrm.repositories.stk.StkItemRepository;
import com.tsix.immocrm.utils.Champs;
import org.springframework.stereotype.Component;

import java.util.UUID;

/** Seule recherche par id des routes {@code /api/terrains/{id}/…} : terrain visible (société active), sinon 404. */
@Component
public record TerrainsVisibles(StkItemRepository items) {

    static final String NATURE_TERRAIN = "TER";

    public StkItem exiger(UUID id) {
        return items.findVisible(id).filter(TerrainsVisibles::estTerrain)
                .orElseThrow(() -> new ResourceNotFoundException("Terrain introuvable: " + id));
    }

    static boolean estTerrain(StkItem item) {
        return NATURE_TERRAIN.equalsIgnoreCase(Champs.de(item.getStkKind(), StkKind::getCode));
    }

    /** {@code BoolField001} du statut, mappé {@code venteEffective} pour les dossiers : « Créer un sous-terrain » ici. */
    static boolean sousTerrainsAutorises(StkItem terrain) {
        return Boolean.TRUE.equals(Champs.de(terrain.getStrStatus(), StrStatus::getVenteEffective));
    }

    /** {@code LandProject.cs:361-363, 3965-3986} : sous-terrains créés, modifiés, supprimés sous cette seule condition. */
    static void exigerSousTerrainsAutorises(StkItem parent) {
        if (!sousTerrainsAutorises(parent)) {
            throw new ConflictException("Le statut du terrain %s n'autorise pas les sous-terrains."
                    .formatted(parent.getCode()));
        }
    }
}
