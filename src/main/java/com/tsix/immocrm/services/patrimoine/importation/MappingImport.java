package com.tsix.immocrm.services.patrimoine.importation;

import com.tsix.immocrm.dto.patrimoine.importation.AnalyseImportDto;
import com.tsix.immocrm.dto.patrimoine.importation.MappingImportDto;
import com.tsix.immocrm.dto.patrimoine.importation.MappingImportDto.ColonneMappeeDto;
import com.tsix.immocrm.exceptions.InvalidRequestException;
import com.tsix.immocrm.services.patrimoine.importation.ColonneGenerique.Nature;
import com.tsix.immocrm.services.patrimoine.importation.LectureClasseur.Feuille;
import com.tsix.immocrm.services.patrimoine.importation.LectureClasseur.LigneLue;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Mapping de l'import générique (FEATURE-patrimoine-import-generique.md §1.3) : proposition par en-tête (trim, sans casse),
 * contrôle d'un mapping reçu (400) et renommage des colonnes du fichier vers les en-têtes cibles, avant le moteur.
 */
final class MappingImport {

    static final String PIVOT = "code";
    static final int COLONNES_MAX = 100;
    static final String MESSAGE_PIVOT = "Il faut définir les pivots et effectuer leurs mapping !";
    static final String MESSAGE_ACTIVE = "La colonne « %s » ne peut pas être importée : elle masque ou rend visible tout le "
            + "contenu de l'entité. Modifiez-la depuis la fiche (Débloquer).";
    private static final int APERCU = 5;
    private static final JsonMapper JSON = JsonMapper.builder().build();

    private MappingImport() {
    }

    static MappingImportDto proposer(List<ColonneGenerique> catalogue, List<String> entetes) {
        return new MappingImportDto(PIVOT, catalogue.stream().filter(c -> !c.estIgnoree())
                .map(c -> new ColonneMappeeDto(c.entete(), rang(entetes, c.entete()), c.propriete())).toList());
    }

    /** Feuille dont les colonnes mappées portent l'en-tête cible ; {@code json} nul = mapping proposé. */
    static Feuille appliquer(List<ColonneGenerique> catalogue, Feuille lue, String json) {
        List<String> entetes = List.copyOf(lue.entetes());
        // Décision du manager (26/10) n° 5 : l'en-tête « Active » reconnu suffit, même non mappé et vide.
        catalogue.stream().filter(c -> c.nature() == Nature.REFUSEE && rang(entetes, c.entete()) >= 0).findFirst()
                .ifPresent(c -> {
                    throw new InvalidRequestException(MESSAGE_ACTIVE.formatted(c.entete()));
                });
        MappingImportDto mapping = json == null || json.isBlank() ? proposer(catalogue, entetes) : lire(json);
        Map<String, String> cibles = valider(catalogue, mapping, entetes);
        Set<String> nouveaux = new LinkedHashSet<>(cibles.values());
        entetes.stream().filter(e -> !cibles.containsKey(e)).forEach(nouveaux::add);
        List<LigneLue> lignes = lue.lignes().stream().map(l -> renommer(l, cibles)).toList();
        return new Feuille(lue.nom(), lue.feuilles(), nouveaux, lignes);
    }

    static AnalyseImportDto analyser(List<ColonneGenerique> catalogue, Feuille lue) {
        List<String> entetes = List.copyOf(lue.entetes());
        List<String> avertissements = new ArrayList<>();
        List<ColonneMappeeDto> colonnes = proposer(catalogue, entetes).colonnes().stream().map(m -> {
            if (m.indexColonne() >= 0 && nature(catalogue, m.propriete()) == Nature.REFUSEE) {
                avertissements.add(MESSAGE_ACTIVE.formatted(m.entete()));
                return new ColonneMappeeDto(m.entete(), -1, m.propriete());
            }
            return m;
        }).toList();
        ignorees(catalogue, entetes).forEach(e -> avertissements.add(ignoree(e)));
        entetes.stream().filter(e -> catalogue.stream().noneMatch(c -> c.entete().equalsIgnoreCase(e)))
                .forEach(e -> avertissements.add("Colonne « " + e + " » ignorée : en-tête inconnu."));
        List<List<String>> apercu = lue.lignes().stream().limit(APERCU)
                .map(l -> entetes.stream().map(e -> Objects.toString(LectureChamps.texte(l.valeurs().get(e)), "")).toList())
                .toList();
        return new AnalyseImportDto(lue.feuilles(), lue.nom(), entetes, new MappingImportDto(PIVOT, colonnes), apercu,
                List.copyOf(avertissements));
    }

    /** En-têtes du fichier qui désignent une colonne OMRAN sans champ chez nous. */
    static List<String> ignorees(List<ColonneGenerique> catalogue, Collection<String> entetes) {
        return entetes.stream().filter(e -> catalogue.stream().anyMatch(c -> c.estIgnoree()
                && c.entete().equalsIgnoreCase(e.strip()))).toList();
    }

    static String ignoree(String entete) {
        return "Colonne « " + entete + " » ignorée : non gérée par l'écran.";
    }

    private static int rang(List<String> entetes, String cible) {
        for (int i = 0; i < entetes.size(); i++) {
            if (entetes.get(i).strip().equalsIgnoreCase(cible)) {
                return i;
            }
        }
        return -1;
    }

    private static MappingImportDto lire(String json) {
        try {
            return JSON.readValue(json, MappingImportDto.class);
        } catch (JacksonException e) {
            throw new InvalidRequestException("Mapping illisible : un MappingImportDto en JSON est attendu.");
        }
    }

    /** En-tête du fichier → en-tête cible des colonnes mappées ; 400 sur tout mapping invalide (V3, §1.2). */
    private static Map<String, String> valider(List<ColonneGenerique> catalogue, MappingImportDto mapping,
                                               List<String> entetes) {
        List<ColonneMappeeDto> mappees = mapping.colonnes() == null ? List.of()
                : mapping.colonnes().stream().filter(c -> c != null && c.indexColonne() >= 0).toList();
        if (mappees.size() > COLONNES_MAX) {
            throw new InvalidRequestException("Au plus " + COLONNES_MAX + " colonnes peuvent être mappées.");
        }
        Map<String, String> cibles = new HashMap<>();
        Set<String> proprietes = new HashSet<>();
        for (ColonneMappeeDto m : mappees) {
            ColonneGenerique c = catalogue.stream().filter(k -> !k.estIgnoree() && k.propriete().equals(m.propriete()))
                    .findFirst().orElseThrow(() -> new InvalidRequestException("Colonne cible inconnue : " + m.propriete()));
            if (m.indexColonne() >= entetes.size()) {
                throw new InvalidRequestException("Colonne du fichier inconnue : rang " + m.indexColonne());
            }
            String source = entetes.get(m.indexColonne());
            if (!proprietes.add(c.propriete()) || cibles.containsKey(source)) {
                throw new InvalidRequestException("Mapping en double : « %s » ou « %s » est mappée deux fois."
                        .formatted(c.entete(), source));
            }
            if (c.nature() == Nature.REFUSEE) {
                throw new InvalidRequestException(MESSAGE_ACTIVE.formatted(c.entete()));
            }
            cibles.put(source, c.entete());
        }
        if (!PIVOT.equals(mapping.pivot()) || !proprietes.contains(PIVOT)) {
            throw new InvalidRequestException(MESSAGE_PIVOT);
        }
        return cibles;
    }

    private static Nature nature(List<ColonneGenerique> catalogue, String propriete) {
        return catalogue.stream().filter(c -> Objects.equals(c.propriete(), propriete)).map(ColonneGenerique::nature)
                .findFirst().orElse(Nature.IGNOREE);
    }

    private static LigneLue renommer(LigneLue l, Map<String, String> cibles) {
        Map<String, Object> valeurs = new HashMap<>();
        cibles.forEach((fichier, cible) -> {
            Object v = l.valeurs().get(fichier);
            if (v != null) {
                valeurs.put(cible, v);
            }
        });
        return new LigneLue(l.numero(), valeurs);
    }
}
