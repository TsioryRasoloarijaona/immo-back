package com.tsix.immocrm.services.projet;

import com.tsix.immocrm.dto.projet.FiltresTresorerieProjet;
import com.tsix.immocrm.dto.projet.LigneCelluleTresorerieDto;
import com.tsix.immocrm.dto.projet.TresorerieProjetDto;
import com.tsix.immocrm.web.export.FeuilleExport;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TresorerieProjetService {

    public TresorerieProjetDto lire(FiltresTresorerieProjet filtres) {
        throw new UnsupportedOperationException();
    }

    public List<LigneCelluleTresorerieDto> cellule(FiltresTresorerieProjet filtres, int mois, String noeud) {
        throw new UnsupportedOperationException();
    }

    public List<FeuilleExport> feuillesExport(FiltresTresorerieProjet filtres) {
        throw new UnsupportedOperationException();
    }
}
