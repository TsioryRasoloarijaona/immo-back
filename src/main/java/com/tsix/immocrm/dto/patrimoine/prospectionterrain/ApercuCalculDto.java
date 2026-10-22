package com.tsix.immocrm.dto.patrimoine.prospectionterrain;

import com.tsix.immocrm.domain.foncier.SurfacesTerrain;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "Saisie du bloc surfaces à recalculer sans écrire ; mêmes noms et sens que SurfacesTerrain.")
public record ApercuCalculDto(
        @Schema(description = "Nom du champ qui vient de changer : ce côté de sa paire gagne (ex. superficieCos).")
        String champModifie,
        BigDecimal superficie,
        BigDecimal superficieEquipements,
        BigDecimal superficieEquipementACeder,
        BigDecimal superficieARegler,
        BigDecimal superficieACeder,
        BigDecimal cos,
        BigDecimal superficieCos,
        BigDecimal cuf,
        BigDecimal superficieCuf,
        BigDecimal coefVendable,
        BigDecimal superficieVendable,
        BigDecimal coefCommerce,
        BigDecimal superficieCommerce,
        BigDecimal coefBureau,
        BigDecimal superficieBureau,
        BigDecimal coefParking,
        BigDecimal superficieParking,
        BigDecimal prixM2,
        BigDecimal prix
) {
    public SurfacesTerrain surfaces() {
        return new SurfacesTerrain(superficie, superficieEquipements, superficieEquipementACeder, superficieARegler,
                superficieACeder, null, null, cos, superficieCos, cuf, superficieCuf, coefVendable, superficieVendable,
                coefCommerce, superficieCommerce, coefBureau, superficieBureau, null, coefParking, superficieParking,
                prixM2, prix);
    }
}
