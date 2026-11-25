package com.tsix.immocrm.services.projet;

import com.tsix.immocrm.domain.projet.TypeBordereau;
import com.tsix.immocrm.dto.projet.BordereauDto;
import com.tsix.immocrm.dto.projet.BordereauEstimatifDto;
import com.tsix.immocrm.entities.prj.PrjMarket;
import com.tsix.immocrm.exceptions.InvalidRequestException;
import com.tsix.immocrm.services.droits.DroitsOmran;
import com.tsix.immocrm.services.patrimoine.importation.LectureClasseur;
import com.tsix.immocrm.services.patrimoine.importation.LectureClasseur.Feuille;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

import static com.tsix.immocrm.services.projet.ColonnesBordereauExcel.CODE;

/** Import du bordereau du lot depuis un classeur aux colonnes OMRAN, enregistré comme le PUT (R1, R7, D8). */
@Service
public class ImportBordereauLot {

    private final LotsProjetService lots;
    private final DroitsOmran droits;
    private final EcritureBordereauLot ecriture;

    public ImportBordereauLot(LotsProjetService lots, DroitsOmran droits, EcritureBordereauLot ecriture) {
        this.lots = lots;
        this.droits = droits;
        this.ecriture = ecriture;
    }

    /** 404, 403, 409 avant toute lecture du classeur (D8). */
    @Transactional
    public Object importer(UUID id, TypeBordereau type, MultipartFile fichier) {
        boolean estimatif = type == TypeBordereau.ESTIMATIF;
        PrjMarket lot = lots.visible(id);
        DroitsBordereau.refus(droits, lot, estimatif).ifPresent(refus -> {
            throw refus;
        });
        Feuille feuille = LectureClasseur.lire(fichier, null);
        if (!feuille.entetes().contains(CODE)) {
            throw new InvalidRequestException("La colonne « %s » est absente du classeur".formatted(CODE));
        }
        if (estimatif) {
            BordereauEstimatifDto b = lots.bordereauEstimatif(id);
            return ecriture.enregistrerEstimatif(id,
                    new FusionBordereau(b.groupes(), b.lignes(), b.categorieParDefautId()).fusionner(feuille.lignes()));
        }
        BordereauDto b = lots.bordereau(id);
        return ecriture.enregistrerDefinitif(id,
                new FusionBordereau(b.groupes(), b.lignes(), b.categorieParDefautId()).fusionner(feuille.lignes()));
    }
}
