package com.tsix.immocrm.services.patrimoine.terrain;

import com.tsix.immocrm.dto.patrimoine.LotissementDetailDto;
import com.tsix.immocrm.dto.patrimoine.LotissementEcritureDto;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.services.patrimoine.EcritureLot;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Fiche et écritures d'un lot de terrain (FEATURE-patrimoine-lotissements.md §1.2-§1.4). Tout contrôle d'une écriture se
 * fait dans sa méthode {@code @Transactional} : un appel readOnly avant elle l'enverrait sur la copie (« Piège readOnly »).
 */
@Service
@Transactional(readOnly = true)
public class LotissementService {

    private final LotissementsVisibles lots;
    private final EcritureLot ecriture;
    private final SuppressionTerrain suppression;

    LotissementService(LotissementsVisibles lots, EcritureLot ecriture, SuppressionTerrain suppression) {
        this.lots = lots;
        this.ecriture = ecriture;
        this.suppression = suppression;
    }

    public LotissementDetailDto lire(UUID id) {
        return ecriture.detail(lots.exiger(id));
    }

    @Transactional
    public LotissementDetailDto creer(LotissementEcritureDto saisie) {
        return ecriture.detail(ecriture.creer(saisie));
    }

    @Transactional
    public LotissementDetailDto modifier(UUID id, LotissementEcritureDto saisie) {
        StkItem lot = lots.exiger(id);
        ecriture.modifier(lot, saisie);
        return ecriture.detail(lot);
    }

    @Transactional
    public void supprimer(UUID id) {
        suppression.supprimer(lots.exiger(id));
    }
}
