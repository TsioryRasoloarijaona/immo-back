package com.tsix.immocrm.services.projet;

import com.tsix.immocrm.domain.rapport.ColonneRapport;
import com.tsix.immocrm.domain.rapport.ContexteRapport;
import com.tsix.immocrm.domain.rapport.ParametreRapport;
import com.tsix.immocrm.domain.rapport.ResultatRapport;
import com.tsix.immocrm.domain.rapport.SectionRapport;
import com.tsix.immocrm.domain.rapport.TypeColonne;
import com.tsix.immocrm.domain.rapport.TypeParametre;
import com.tsix.immocrm.exceptions.InvalidRequestException;
import com.tsix.immocrm.services.rapport.vente.commercial.PeriodesRapport;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.tsix.immocrm.services.projet.travaux.ValeursNatives.decimal;
import static com.tsix.immocrm.services.rapport.vente.commercial.PeriodesRapport.ANNEE;

/** R2 {@code PrjMarketTresorieByMonth} : décomptes TTC cumulés par mois des lots mono-fournisseur d'un projet. */
@Component
public class SuiviMensuelDecaissementsRapport extends RapportProjet {

    private static final String MESSAGE_UN_PROJET = "Veuillez sélectionner un projet !";
    private static final String MOIS = "mois";
    private static final String REEL = "reel";
    private static final String SQL_MOIS = """
            SELECT MONTH(w.DocDate), SUM(w.AmountTTC) FROM PrjWorkingOut w
            WHERE w.PrjMarketId IN (:lots) AND YEAR(w.DocDate) = :annee GROUP BY MONTH(w.DocDate)""";

    private final Clock clock;

    public SuiviMensuelDecaissementsRapport(SourcesRapportsProjet sources, Clock clock) {
        super(sources);
        this.clock = clock;
    }

    @Override
    public String code() {
        return "PrjMarketTresorieByMonth";
    }

    @Override
    public List<ParametreRapport> parametres() {
        return List.of(sources.requetes().projets(PROJET, "Projet", false), annee());
    }

    @Override
    public List<ParametreRapport> parametresAcceptes(List<ParametreRapport> affiches) {
        return List.of(new ParametreRapport(PROJET, "Projet", TypeParametre.TEXTE, false, false, null, List.of(),
                null, null), annee());
    }

    @Override
    public ResultatRapport executer(ContexteRapport contexte) {
        UUID projet = projetExige(contexte.texte(PROJET));
        Integer saisie = contexte.annee(ANNEE);
        int annee = saisie == null ? LocalDate.now(clock).getYear() : saisie;
        List<UUID> lots = sources.lots().calcules(List.of(projet)).stream()
                .filter(l -> !l.calcul().drapeaux().multi()).map(l -> l.lot().getId()).toList();
        BigDecimal[] parMois = new BigDecimal[13];
        if (!lots.isEmpty()) {
            List<?> lignes = sources.em().createNativeQuery(SQL_MOIS).setParameter("lots", lots)
                    .setParameter("annee", annee).getResultList();
            for (Object o : lignes) {
                Object[] r = (Object[]) o;
                parMois[((Number) r[0]).intValue()] = decimal(r[1]);
            }
        }
        List<Map<String, Object>> sortie = new ArrayList<>();
        BigDecimal cumul = BigDecimal.ZERO;
        for (int m = 1; m <= 12; m++) {
            if (parMois[m] != null) {
                cumul = cumul.add(parMois[m]);
            }
            Map<String, Object> ligne = new LinkedHashMap<>();
            ligne.put(MOIS, PeriodesRapport.mois(m));
            ligne.put(REEL, cumul.setScale(2, RoundingMode.HALF_UP));
            sortie.add(ligne);
        }
        List<ColonneRapport> colonnes = List.of(ColonneRapport.texte(MOIS, "Mois"),
                sansTotal(REEL, "Réel", TypeColonne.MONTANT));
        return new ResultatRapport(List.of(new SectionRapport("suivi", "Suivi mensuel des décaissements", colonnes,
                sortie, null, null, List.of())), List.of());
    }

    /** Projet absent, invalide ou hors périmètre → même 400. */
    private UUID projetExige(String valeur) {
        UUID id;
        try {
            id = valeur == null || valeur.isBlank() ? null : UUID.fromString(valeur.trim());
        } catch (IllegalArgumentException e) {
            id = null;
        }
        if (id == null || sources.perimetre().restreindre(List.of(id)).isEmpty()) {
            throw new InvalidRequestException(MESSAGE_UN_PROJET);
        }
        return id;
    }

    private ParametreRapport annee() {
        return new ParametreRapport(ANNEE, "Année", TypeParametre.ANNEE, true, false,
                String.valueOf(LocalDate.now(clock).getYear()), List.of(), null, null);
    }
}
