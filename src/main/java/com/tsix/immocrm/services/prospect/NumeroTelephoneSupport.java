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
 * Voir {@code instructions/FEATURE-indicatif-telephone.md} §5.
 *
 * <p>Existe comme composant séparé plutôt que comme méthodes privées de
 * {@link ProspectService} pour une raison précise : la règle porte sur <b>deux</b> couples
 * (mobile et fixe) écrits au même endroit, et devra s'appliquer telle quelle le jour où un autre
 * porteur de numéro s'ajoutera. La sortir maintenant coûte une classe ; la sortir plus tard
 * coûterait de rattraper une copie qui aura divergé.
 */
@Component
@Transactional(readOnly = true)
public class NumeroTelephoneSupport {

    /**
     * Bornes de longueur, volontairement larges. La règle « 9 chiffres commençant par 6 ou 7 »
     * est vraie et mesurée — mais <b>seulement pour le Maroc et la France</b>. L'imposer ici
     * rejetterait un mobile nord-américain à 10 chiffres, et 47 fiches réelles en portent un.
     * La règle stricte reste où elle est justifiée : dans {@code NumeroWhatsApp}, au moment
     * d'envoyer.
     */
    private static final int CHIFFRES_MIN = 6;
    private static final int CHIFFRES_MAX = 14;

    private final AdmCountryRepository countryRepository;

    public NumeroTelephoneSupport(AdmCountryRepository countryRepository) {
        this.countryRepository = countryRepository;
    }

    /**
     * Valide le couple et renvoie ce qu'il faut écrire en base.
     *
     * @param numero    valeur brute reçue de l'appelant, {@code null} si le champ n'est pas fourni
     * @param indicatif indicatif brut reçu de l'appelant
     * @param libelle   nom du champ tel qu'il apparaît dans la requête ({@code "mobile"},
     *                  {@code "telephone"}) — n'entre que dans les messages d'erreur, mais y
     *                  entre vraiment : un 400 qui ne dit pas quel champ est en cause oblige le
     *                  développeur front à deviner lequel des deux couples est fautif
     * @return les deux valeurs normalisées, ou {@code null} si le champ n'est pas fourni — le
     *         {@code PATCH} restant partiel, « non fourni » et « vidé » ne sont pas la même chose
     */
    public NumeroNormalise valider(String numero, String indicatif, String libelle) {
        if (numero == null) {
            // V5 — une modification qui ne touche pas au numéro n'exige rien. Sans cette porte,
            // les 7 fiches réelles sans indicatif deviendraient immodifiables.
            if (StringUtils.hasText(indicatif)) {
                throw new InvalidRequestException("indicatif" + majuscule(libelle)
                        + " est fourni sans " + libelle + " : l'indicatif seul n'a rien à qualifier.");
            }
            return null;
        }

        // V1 — l'indicatif accompagne le numéro. Aucune reprise implicite de celui déjà en base :
        // un numéro qui change peut changer de pays, et hériter silencieusement de l'ancien
        // indicatif fabriquerait un numéro qui n'existe pas.
        if (!StringUtils.hasText(indicatif)) {
            throw new InvalidRequestException("indicatif" + majuscule(libelle) + " est requis "
                    + "lorsque " + libelle + " est fourni. Valeurs possibles : "
                    + "GET /api/referentiels/pays, champ indicatifTelephone.");
        }

        String indicatifPropre = NumeroTelephone.indicatifNormalise(indicatif);
        if (indicatifPropre == null || !indicatifsConnus().contains(indicatifPropre)) {
            // V2 — c'est cette règle qui supprime les 1483, 0393 et autres 514 rangés dans la
            // mauvaise colonne : 30 fiches réelles en portent, une par faute de frappe.
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

    /**
     * Les indicatifs saisissables, lus depuis {@code AdmCountry.PhoneCode}.
     *
     * <p><b>Le référentiel plafonne le monde à ce qu'il contient</b> — 44 pays aujourd'hui, après
     * l'ajout du Sénégal, de la Côte d'Ivoire, de la Turquie et de l'Afghanistan. Le 45ᵉ sera
     * refusé jusqu'à ce que quelqu'un l'y ajoute. C'est le coût assumé de la validation stricte,
     * et il est plus lourd que les quatre pays qui manquaient : voir §5 de la spec.
     *
     * <p>Relu à chaque appel plutôt que mis en cache : la table fait 47 lignes, la requête est
     * indexée sur rien du tout et coûte moins qu'un cache à invalider le jour où un pays sera
     * ajouté en base pendant que l'application tourne.
     */
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

    /**
     * Le couple prêt à écrire. {@code numero} est au format de stockage du légataire,
     * {@code indicatif} en chiffres seuls — les deux formes que porte déjà la base.
     */
    public record NumeroNormalise(String numero, String indicatif) {
    }
}
