package com.tsix.immocrm.services.dossier.workflow;

import com.tsix.immocrm.domain.EtapeMilestone;
import com.tsix.immocrm.domain.StatutDossier;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.str.StrStatus;
import com.tsix.immocrm.repositories.str.StrChampObligatoireRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

/**
 * « Date associée » d'un statut : {@code StrStatus.StrFieldDateId} → {@code StrField.Code} → colonne du dossier
 * ({@link ChampsDossier}) ; sans paramétrage, repli {@link EtapeMilestone}. Seule source de la colonne écrite par
 * {@code PATCH …/statut} et annoncée par {@code GET …/etapes-suivantes}.
 */
@Component
public class DateAssociee {

    private static final EtapeMilestone.Info SANS_DATE =
            new EtapeMilestone.Info(null, "Aucune date associée", EtapeMilestone.Confiance.AUCUN);

    private final StrChampObligatoireRepository champRepository;

    public DateAssociee(StrChampObligatoireRepository champRepository) {
        this.champRepository = champRepository;
    }

    /** {@code champ} en camelCase (nom de propriété de l'API), nul si le statut n'a pas de date associée. */
    public EtapeMilestone.Info de(StrStatus cible, StatutDossier code) {
        List<Object[]> parametree = cible != null ? champRepository.champDateDuStatut(cible.getId()) : List.of();
        if (!parametree.isEmpty() && parametree.get(0)[0] != null) {
            String champ = (String) parametree.get(0)[0];
            String libelle = parametree.get(0)[1] != null ? (String) parametree.get(0)[1] : champ;
            return new EtapeMilestone.Info(camel(champ), libelle, EtapeMilestone.Confiance.CONFIRME);
        }
        EtapeMilestone.Info repli = code != null ? EtapeMilestone.of(code) : null;
        return repli != null ? repli : SANS_DATE;
    }

    /** Écrit {@code date} sur la date associée ; sans date associée (ou colonne inconnue), rien n'est écrit. */
    public void ecrire(ComFolder dossier, StrStatus cible, StatutDossier code, LocalDateTime date) {
        String champ = de(cible, code).champ();
        if (champ == null || date == null) {
            return;
        }
        ChampsDossier.champ(pascal(champ))
                .map(ChampsDossier.Champ::ecriture)
                .ifPresent(ecriture -> ecriture.accept(dossier, date));
    }

    private static String camel(String code) {
        return code.isEmpty() ? code : code.substring(0, 1).toLowerCase(Locale.ROOT) + code.substring(1);
    }

    private static String pascal(String champ) {
        return champ.isEmpty() ? champ : champ.substring(0, 1).toUpperCase(Locale.ROOT) + champ.substring(1);
    }
}
