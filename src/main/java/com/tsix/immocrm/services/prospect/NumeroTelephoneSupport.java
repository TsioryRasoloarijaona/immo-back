package com.tsix.immocrm.services.prospect;

import com.tsix.immocrm.entities.adm.AdmCountry;
import com.tsix.immocrm.exceptions.InvalidRequestException;
import com.tsix.immocrm.repositories.adm.AdmCountryRepository;
import com.tsix.immocrm.utils.NumeroTelephone;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Contrôle et met en forme un couple (indicatif, numéro) avant écriture sur une fiche.
 * Voir {@code instructions/FEATURE-indicatif-telephone.md} §5–6.
 */
@Component
@Transactional(readOnly = true)
public class NumeroTelephoneSupport {

    // Volontairement larges : la règle « 9 chiffres, 6 ou 7 » ne vaut que pour le Maroc et la
    // France ; elle reste dans NumeroWhatsApp, à l'envoi (voir FEATURE-indicatif-telephone.md §5 V4).
    private static final int CHIFFRES_MIN = 6;
    private static final int CHIFFRES_MAX = 14;

    private final AdmCountryRepository countryRepository;

    public NumeroTelephoneSupport(AdmCountryRepository countryRepository) {
        this.countryRepository = countryRepository;
    }

    /**
     * @param libelle nom du champ dans la requête ({@code "mobile"}, {@code "telephone"}), cité
     *                par les messages d'erreur
     * @return {@code null} si le numéro n'est pas fourni : « non fourni » n'est pas « vidé »
     */
    public NumeroNormalise valider(String numero, String indicatif, String libelle) {
        if (numero == null) {
            // V5 — sans cette porte, les fiches sans indicatif deviendraient immodifiables.
            if (StringUtils.hasText(indicatif)) {
                throw new InvalidRequestException("indicatif" + majuscule(libelle)
                        + " est fourni sans " + libelle + " : l'indicatif seul n'a rien à qualifier.");
            }
            return null;
        }

        // V1 — aucune reprise de l'indicatif en base : un numéro qui change peut changer de pays.
        if (!StringUtils.hasText(indicatif)) {
            throw new InvalidRequestException("indicatif" + majuscule(libelle) + " est requis "
                    + "lorsque " + libelle + " est fourni. Valeurs possibles : "
                    + "GET /api/referentiels/pays, champ indicatifTelephone.");
        }

        String indicatifPropre = NumeroTelephone.indicatifNormalise(indicatif);
        if (indicatifPropre == null || !indicatifsConnus().contains(indicatifPropre)) {
            // V2 — écarte les indicatifs fautifs du légataire (1483, 0393…).
            throw new InvalidRequestException("indicatif" + majuscule(libelle) + " « " + indicatif
                    + " » ne correspond à aucun pays connu. Valeurs possibles : "
                    + "GET /api/referentiels/pays, champ indicatifTelephone.");
        }

        String chiffres = NumeroTelephone.chiffres(numero);
        if (chiffres.length() < CHIFFRES_MIN || chiffres.length() > CHIFFRES_MAX) {
            throw new InvalidRequestException(libelle + " « " + numero + " » ne ressemble pas à un "
                    + "numéro : " + chiffres.length() + " chiffre(s), attendu entre "
                    + CHIFFRES_MIN + " et " + CHIFFRES_MAX + ".");
        }

        return new NumeroNormalise(NumeroTelephone.versFormatDeStockage(numero), indicatifPropre);
    }

    // Un pays absent d'AdmCountry est refusé (coût assumé, spec §5 V2). Pas de cache : table
    // minuscule, et un pays ajouté en base doit compter sans redémarrage.
    private Set<String> indicatifsConnus() {
        return countryRepository.findAll().stream()
                .map(AdmCountry::getPhoneCode)
                .map(NumeroTelephone::indicatifNormalise)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());
    }

    private static String majuscule(String libelle) {
        return Character.toUpperCase(libelle.charAt(0)) + libelle.substring(1);
    }

    /** {@code numero} au format de stockage du légataire, {@code indicatif} en chiffres seuls. */
    public record NumeroNormalise(String numero, String indicatif) {
    }
}
