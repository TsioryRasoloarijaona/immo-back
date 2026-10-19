package com.tsix.immocrm.services.suiviaffectations;

import com.tsix.immocrm.domain.AgregatDemande;
import com.tsix.immocrm.domain.ColonneSuiviAffectations;
import com.tsix.immocrm.domain.Granularite;
import com.tsix.immocrm.domain.TriGroupe;
import com.tsix.immocrm.dto.common.TotauxListeDto;
import com.tsix.immocrm.dto.suiviaffectations.LigneSuiviAffectationsDto;
import com.tsix.immocrm.dto.suiviaffectations.StatutSuiviAffectationsDto;
import com.tsix.immocrm.services.groupement.GroupementService.ResultatGroupement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class SuiviAffectationsService {

    /** Groupe déplié de {@code /groupes} : sa clé réémise telle quelle, même expression que le comptage. */
    public record Depliage(ColonneSuiviAffectations colonne, String valeur, Granularite granularite) {
    }

    public Page<LigneSuiviAffectationsDto> lister(FiltresSuiviAffectations filtres, Depliage depliage,
                                                  Pageable pageable) {
        throw new UnsupportedOperationException();
    }

    public ResultatGroupement grouper(FiltresSuiviAffectations filtres, ColonneSuiviAffectations groupe,
                                      Granularite granularite, List<AgregatDemande> agregats, TriGroupe tri,
                                      boolean inclureSansValeur, Pageable pageable) {
        throw new UnsupportedOperationException();
    }

    public TotauxListeDto totauxColonnes(FiltresSuiviAffectations filtres, List<AgregatDemande> agregats) {
        throw new UnsupportedOperationException();
    }

    public List<StatutSuiviAffectationsDto> statuts() {
        throw new UnsupportedOperationException();
    }
}
