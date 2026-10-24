package com.tsix.immocrm.web.export;

import com.tsix.immocrm.domain.Colonne;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

/**
 * {@code GET <liste>/export?format=xlsx|pdf} : format lu avant toute lecture, lignes lues une fois par la lecture
 * de la liste, puis classeur ({@link ExportTableur}) ou PDF ({@link ExportPdf}).
 */
@Component
public class ExportListe {

    public static final String DESCRIPTION = "Lignes de la liste (mêmes filtres, même visibilité) en .xlsx (feuille DONNEES) "
            + "ou .pdf A4 paysage.";
    public static final String REFUS = "Format inconnu, colonne inconnue, ou plus de 2 000 lignes en PDF.";

    /** Paramètres de la requête qui ne filtrent pas les lignes : absents de l'en-tête du PDF. */
    private static final Set<String> HORS_FILTRES = Set.of("format", "colonnes", "sort", "page", "size");

    private final ExportTableur tableur;
    private final ExportPdf pdf;

    public ExportListe(ExportTableur tableur, ExportPdf pdf) {
        this.tableur = tableur;
        this.pdf = pdf;
    }

    public ResponseEntity<StreamingResponseBody> reponse(DemandeExport demande, String liste, String titre,
                                                         Collection<? extends Colonne> colonnes,
                                                         Supplier<List<?>> lecture) {
        FormatExport choisi = FormatExport.lire(demande.format());
        List<FeuilleExport> feuilles = List.of(new FeuilleExport(ExportTableur.FEUILLE, List.copyOf(colonnes),
                LignesAplaties.toutes(lecture.get()), null));
        return choisi == FormatExport.PDF
                ? pdf.reponse(liste, titre, filtresDeLaRequete(), feuilles)
                : tableur.reponse(liste, titre, feuilles);
    }

    private static List<String> filtresDeLaRequete() {
        if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributs)) {
            return List.of();
        }
        return attributs.getRequest().getParameterMap().entrySet().stream()
                .filter(e -> !HORS_FILTRES.contains(e.getKey()))
                .map(ExportListe::filtre)
                .toList();
    }

    private static String filtre(Map.Entry<String, String[]> parametre) {
        return parametre.getKey() + " : " + String.join(", ", parametre.getValue());
    }
}
