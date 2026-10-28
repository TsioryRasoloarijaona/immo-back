package com.tsix.immocrm.services.modele;

import com.tsix.immocrm.dto.modele.MiseEnPageDto;
import com.tsix.immocrm.exceptions.InvalidRequestException;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

/** Mise en page d'une version : valeurs par défaut, bornes, et JSON de {@code AppModeleDocumentVersion.MiseEnPage}. */
@Component
public class MiseEnPageModele {

    static final String A4 = "A4";
    private static final int MARGE_DEFAUT = 15;
    private static final int MARGE_MAX = 60;

    private final JsonMapper json;

    public MiseEnPageModele(JsonMapper json) {
        this.json = json;
    }

    /** Valeurs par défaut posées ; 400 si le format ou une marge sort des bornes. */
    public MiseEnPageDto normaliser(MiseEnPageDto dto) {
        MiseEnPageDto source = dto != null ? dto : new MiseEnPageDto(null, null, null, null, null, null, null);
        String format = source.format() == null || source.format().isBlank() ? A4 : source.format();
        if (!A4.equals(format)) {
            throw new InvalidRequestException("Format de page non pris en charge : %s (A4 seulement).".formatted(format));
        }
        return new MiseEnPageDto(format,
                marge("margeHaut", source.margeHaut()), marge("margeBas", source.margeBas()),
                marge("margeGauche", source.margeGauche()), marge("margeDroite", source.margeDroite()),
                vide(source.enTete()), vide(source.piedDePage()), source.fondImageId(),
                source.fondImageId() == null ? null : !Boolean.FALSE.equals(source.fondPleinePage()));
    }

    /** Taille bornée en amont : en-tête et pied passent par {@link ModelePrepare} (liste blanche, poids des images). */
    public String ecrire(MiseEnPageDto miseEnPage) {
        return json.writeValueAsString(miseEnPage);
    }

    public MiseEnPageDto lire(String texte) {
        try {
            return normaliser(json.readValue(texte, MiseEnPageDto.class));
        } catch (JacksonException e) {
            throw new IllegalStateException("Mise en page illisible en base", e);
        }
    }

    private static int marge(String nom, Integer valeur) {
        if (valeur == null) {
            return MARGE_DEFAUT;
        }
        if (valeur < 0 || valeur > MARGE_MAX) {
            throw new InvalidRequestException("%s doit être entre 0 et 60 mm (reçu : %d).".formatted(nom, valeur));
        }
        return valeur;
    }

    private static String vide(String html) {
        return html == null || html.isBlank() ? null : html;
    }
}
