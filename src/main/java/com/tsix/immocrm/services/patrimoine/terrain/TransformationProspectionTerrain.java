package com.tsix.immocrm.services.patrimoine.terrain;

import com.tsix.immocrm.dto.patrimoine.TerrainEcritureDto;
import com.tsix.immocrm.dto.patrimoine.prospectionterrain.TerrainCreeDto;
import com.tsix.immocrm.dto.patrimoine.prospectionterrain.TransformationTerrainDto;
import com.tsix.immocrm.entities.CodeLibelleOmran;
import com.tsix.immocrm.entities.adm.AdmCity;
import com.tsix.immocrm.entities.adm.AdmCountry;
import com.tsix.immocrm.entities.lnd.LndLand;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.exceptions.ConflictException;
import com.tsix.immocrm.exceptions.ProspectionDejaTransformeeException;
import com.tsix.immocrm.repositories.stk.StkItemRepository;
import com.tsix.immocrm.services.patrimoine.TracePatrimoine;
import com.tsix.immocrm.services.patrimoine.prospectionterrain.ProspectionTerrainService;
import com.tsix.immocrm.utils.Champs;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;
import java.util.UUID;

/**
 * « Transformer en terrain » ({@code Prospection.cs:CreateItem} l.1983-2038, front l.318-351) : le terrain est créé par
 * {@link EcritureTerrain#creer} comme depuis sa fiche, complété, puis la prospection archivée, en une transaction.
 */
@Service
public class TransformationProspectionTerrain {

    private final ProspectionTerrainService prospections;
    private final StkItemRepository items;
    private final EcritureTerrain ecriture;
    private final TracePatrimoine trace;

    TransformationProspectionTerrain(ProspectionTerrainService prospections, StkItemRepository items,
                                     EcritureTerrain ecriture, TracePatrimoine trace) {
        this.prospections = prospections;
        this.items = items;
        this.ecriture = ecriture;
        this.trace = trace;
    }

    @Transactional
    public TerrainCreeDto transformer(UUID prospectionId, TransformationTerrainDto d) {
        LndLand p = prospections.exiger(prospectionId);
        Optional<StkItem> existant = items.findFirstByLndLandId(prospectionId);
        String motif = prospections.motifNonTransformable(p, existant);
        if (existant.isPresent()) {
            throw new ProspectionDejaTransformeeException(motif, existant.get().getId());
        }
        if (motif != null) {
            throw new ConflictException(motif);
        }
        StkItem t = ecriture.creer(saisie(p, d));
        ecriture.completerDepuisProspection(t, p);
        TracePatrimoine.Trace m = trace.maintenant();
        p.setIsInArchive(true);
        p.setIsTransformedToLand(true);
        p.setUserUpdate(m.auteur());
        p.setUserUpdateDateTime(m.le());
        return new TerrainCreeDto(t.getId(), t.getCode());
    }

    /** Prix au m² TTC = prix proposé / superficie, TVA 0 : prix de vente = prix d'achat = prix proposé (§5.7). */
    private static TerrainEcritureDto saisie(LndLand p, TransformationTerrainDto d) {
        BigDecimal propose = Optional.ofNullable(p.getProspectionPriceProposed()).orElse(BigDecimal.ZERO);
        BigDecimal prixM2 = propose.divide(p.getArea(), 6, RoundingMode.HALF_UP);
        return new TerrainEcritureDto(d.code() == null || d.code().isBlank() ? p.getCode() : d.code(), p.getDescription(),
                d.entiteId(), Champs.de(p.getAdmCountry(), AdmCountry::getId), Champs.de(p.getAdmCity(), AdmCity::getId),
                null, d.vocationId(), d.categorieId(), Champs.de(p.getType(), CodeLibelleOmran::getId),
                Champs.de(p.getLndLevel(), CodeLibelleOmran::getId), Champs.de(p.getLndVocation(), CodeLibelleOmran::getId),
                Champs.de(p.getStkOrientation(), CodeLibelleOmran::getId), null, p.getIsFinished(), p.getArea(),
                p.getArea(), null, prixM2, null, prixM2, BigDecimal.ZERO, null, propose, null, null, null, null, null, null,
                p.getAdress(), p.getGmapLink(), p.getNotes(), p.getAnomalies(), null);
    }
}
