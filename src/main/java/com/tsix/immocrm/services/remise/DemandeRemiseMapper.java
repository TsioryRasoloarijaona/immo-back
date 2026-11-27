package com.tsix.immocrm.services.remise;

import com.tsix.immocrm.dto.remise.DemandeRemiseDto;
import com.tsix.immocrm.entities.app.AppDemandeRemise;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.services.dossier.edition.DossierEditionService;
import com.tsix.immocrm.services.perimetre.AgencesDesVendeurs;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Component
class DemandeRemiseMapper {

    private final AgencesDesVendeurs agences;
    private final DossierEditionService edition;

    DemandeRemiseMapper(AgencesDesVendeurs agences, DossierEditionService edition) {
        this.agences = agences;
        this.edition = edition;
    }

    /** Demandes dont le dossier figure dans {@code dossiers} ; les autres sont écartées. */
    List<DemandeRemiseDto> dtos(List<AppDemandeRemise> demandes, Map<UUID, ComFolder> dossiers) {
        Map<UUID, AgencesDesVendeurs.Agence> carte = agences.carte();
        return demandes.stream()
                .flatMap(d -> Optional.ofNullable(dossiers.get(d.getDossierId())).map(f -> dto(d, f, carte)).stream())
                .toList();
    }

    DemandeRemiseDto dto(AppDemandeRemise demande, ComFolder dossier) {
        return dto(demande, dossier, agences.carte());
    }

    private DemandeRemiseDto dto(AppDemandeRemise d, ComFolder dossier, Map<UUID, AgencesDesVendeurs.Agence> carte) {
        CfgTier vendeur = dossier.getCfgCommercial();
        return new DemandeRemiseDto(d.getId(), d.getDossierId(), d.getStatut(), d.getTauxDemande(), d.getTauxAccorde(),
                d.getMotif(), d.getMotifRefus(), d.getDemandeLe(), d.getTraiteLe(), dossier.getCode(),
                AgencesDesVendeurs.de(carte, vendeur).libelle(),
                vendeur == null ? null : vendeur.getDescription(),
                dossier.getStkItem() == null ? null : dossier.getStkItem().getCode(),
                dossier.getPriceAfterDiscount(), edition.tauxRemise(dossier));
    }
}
