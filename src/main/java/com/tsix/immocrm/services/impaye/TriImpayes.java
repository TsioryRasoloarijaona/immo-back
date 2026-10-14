package com.tsix.immocrm.services.impaye;

import com.tsix.immocrm.domain.ColonneImpaye;
import com.tsix.immocrm.domain.Colonnes;
import com.tsix.immocrm.dto.impaye.ImpayeDto;
import com.tsix.immocrm.exceptions.InvalidRequestException;
import org.springframework.data.domain.Sort;

import java.util.Comparator;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;

/**
 * Tri en mémoire des impayés : le reste se calcule hors SQL, la liste entière est triée avant d'être
 * paginée. Les vides finissent en bas dans les deux sens ; {@code echeanceId} départage les ex æquo.
 */
final class TriImpayes {

    private static final Map<String, ColonneImpaye> PAR_NOM = Colonnes.index(ColonneImpaye.class);

    private TriImpayes() {
    }

    /** @throws InvalidRequestException (400) si une propriété ne figure pas au catalogue des colonnes. */
    static Comparator<ImpayeDto> comparateur(Sort sort) {
        Comparator<ImpayeDto> resultat = (a, b) -> 0;
        for (Sort.Order ordre : sort) {
            resultat = resultat.thenComparing(par(colonne(ordre.getProperty()), ordre.isDescending()));
        }
        return resultat.thenComparing(ImpayeDto::echeanceId);
    }

    private static ColonneImpaye colonne(String propriete) {
        ColonneImpaye colonne = PAR_NOM.get(propriete.trim().toLowerCase(Locale.ROOT));
        if (colonne == null) {
            throw new InvalidRequestException("sort : colonne inconnue « " + propriete + " » ; valeurs acceptées : "
                    + ColonneImpaye.nomsValides());
        }
        return colonne;
    }

    private static Comparator<ImpayeDto> par(ColonneImpaye colonne, boolean decroissant) {
        return switch (colonne) {
            case DATE_ECHEANCE -> cle(ImpayeDto::dateEcheance, decroissant);
            case DATE_RECOUVREMENT -> cle(ImpayeDto::dateRecouvrement, decroissant);
            case DOSSIER_CODE -> cle(ImpayeDto::dossierCode, decroissant);
            case CLIENT_NOM -> cle(ImpayeDto::clientNom, decroissant);
            case PROJET -> cle(ImpayeDto::projet, decroissant);
            case TRANCHE -> cle(ImpayeDto::tranche, decroissant);
            case ARTICLE -> cle(ImpayeDto::article, decroissant);
            case CATEGORIE -> cle(ImpayeDto::categorie, decroissant);
            case MONTANT_A_PAYER -> cle(ImpayeDto::montantAPayer, decroissant);
            case MONTANT_RESTE -> cle(ImpayeDto::montantReste, decroissant);
            case JOURS_RETARD -> cle(ImpayeDto::joursRetard, decroissant);
            case COMMERCIAL -> cle(ImpayeDto::commercial, decroissant);
            case NOTES -> cle(ImpayeDto::notes, decroissant);
            case MONTANT_REGLE -> cle(ImpayeDto::montantRegle, decroissant);
            case STATUT_DOSSIER -> cle(ImpayeDto::statutDossier, decroissant);
            case SOCIETE -> cle(ImpayeDto::societe, decroissant);
            case DESCRIPTION -> cle(ImpayeDto::description, decroissant);
            case PENALITE_ID -> cle(ImpayeDto::penaliteId, decroissant);
            case ID -> cle(ImpayeDto::echeanceId, decroissant);
            case DOSSIER_ID -> cle(ImpayeDto::dossierId, decroissant);
            case CLIENT_ID -> cle(ImpayeDto::clientId, decroissant);
        };
    }

    private static <T extends Comparable<? super T>> Comparator<ImpayeDto> cle(Function<ImpayeDto, T> valeur,
                                                                                boolean decroissant) {
        Comparator<T> sens = decroissant ? Comparator.reverseOrder() : Comparator.naturalOrder();
        return Comparator.comparing(valeur, Comparator.nullsLast(sens));
    }
}
