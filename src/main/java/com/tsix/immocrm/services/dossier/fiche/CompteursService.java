package com.tsix.immocrm.services.dossier.fiche;

import com.tsix.immocrm.dto.dossier.compteurs.CompteursDto;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.exceptions.ConflictException;
import com.tsix.immocrm.exceptions.InvalidRequestException;
import com.tsix.immocrm.repositories.stk.StkItemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.function.Consumer;

/** Compteurs eau / électricité / gaz de l'article principal du dossier. Voir FEATURE-vente-fiche.md §B3. */
@Service
public class CompteursService {

    private static final int LONGUEUR_MAX = 50;

    private final EcritureDossier ecriture;
    private final StkItemRepository stkItemRepository;

    public CompteursService(EcritureDossier ecriture, StkItemRepository stkItemRepository) {
        this.ecriture = ecriture;
        this.stkItemRepository = stkItemRepository;
    }

    @Transactional(readOnly = true)
    public CompteursDto lire(UUID dossierId) {
        return versDto(article(ecriture.charger(dossierId)));
    }

    @Transactional
    public CompteursDto modifier(UUID dossierId, CompteursDto request) {
        if (request == null) {
            throw new InvalidRequestException("Corps de requête requis");
        }
        ComFolder dossier = ecriture.charger(dossierId);
        StkItem article = article(dossier);
        appliquer(request.referenceSteg(), "referenceSteg", article::setElectricityRef);
        appliquer(request.compteurElectricite(), "compteurElectricite", article::setElectricityMeter);
        appliquer(request.releveElectricite(), "releveElectricite", article::setElectricityMeterIndex);
        appliquer(request.policeElectricite(), "policeElectricite", article::setElectricityMeterPolice);
        appliquer(request.compteurEau(), "compteurEau", article::setWaterMeter);
        appliquer(request.releveEau(), "releveEau", article::setWaterMeterIndex);
        appliquer(request.policeEau(), "policeEau", article::setWaterMeterPolice);
        appliquer(request.compteurGaz(), "compteurGaz", article::setGasMeter);
        appliquer(request.releveGaz(), "releveGaz", article::setGasMeterIndex);
        appliquer(request.policeGaz(), "policeGaz", article::setGasMeterPolice);
        article.setUserUpdate(ecriture.auteur().nom());
        article.setUserUpdateDateTime(ecriture.maintenant());
        StkItem enregistre = stkItemRepository.save(article);
        ecriture.signer(dossier);
        return versDto(enregistre);
    }

    /** null = inchangé ; une chaîne vide efface ; au-delà de 50 caractères (nvarchar(50)) = 400. */
    private static void appliquer(String valeur, String champ, Consumer<String> setter) {
        if (valeur == null) {
            return;
        }
        String propre = valeur.trim();
        if (propre.length() > LONGUEUR_MAX) {
            throw new InvalidRequestException(champ + " dépasse " + LONGUEUR_MAX + " caractères");
        }
        setter.accept(propre.isEmpty() ? null : propre);
    }

    private static StkItem article(ComFolder dossier) {
        StkItem article = dossier.getStkItem();
        if (article == null) {
            throw new ConflictException("Ce dossier n'a pas d'article principal : aucun compteur à lire.");
        }
        return article;
    }

    private static CompteursDto versDto(StkItem a) {
        return new CompteursDto(a.getElectricityRef(), a.getElectricityMeter(), a.getElectricityMeterIndex(),
                a.getElectricityMeterPolice(), a.getWaterMeter(), a.getWaterMeterIndex(), a.getWaterMeterPolice(),
                a.getGasMeter(), a.getGasMeterIndex(), a.getGasMeterPolice());
    }
}
