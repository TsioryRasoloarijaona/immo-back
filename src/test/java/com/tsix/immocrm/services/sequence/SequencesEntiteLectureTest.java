package com.tsix.immocrm.services.sequence;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

/**
 * FEATURE-entites-juridiques.md §7.5 : la création d'une entité écrit 4 {@code AdmSequence} par société (Code
 * « Facture », « Avoir », « Certificat de retenue à la source », « Reçu » ; TypeCode INVOICE, REFUND, RSSheetCode,
 * Receipt). {@code GenerateurCodeSequence} et {@code DevisService} lisent une séquence par <b>Code seul</b> : aucun de
 * ces codes ne doit y être lu, sinon plusieurs lignes répondent (une par société). Preuve sur les sources : chaque
 * lecture par code d'une séquence a un argument résolu en littéral, hors de ces huit valeurs.
 */
class SequencesEntiteLectureTest {

    private static final Path SOURCES = Path.of("src", "main", "java");
    private static final String GENERATEUR = "GenerateurCodeSequence.java";
    private static final Set<String> CODES_PAR_SOCIETE = Set.of("Facture", "Avoir", "Certificat de retenue à la source",
            "Reçu", "INVOICE", "REFUND", "RSSheetCode", "Receipt");
    /** Lecture par code d'une {@code AdmSequence} : repository de séquences, ou {@code GenerateurCodeSequence.suivant}. */
    private static final Pattern LECTURE = Pattern.compile(
            "(?:\\b\\w*[sS]equence\\w*\\.(?:findByCode|findPourIncrementByCode|findFirstByCode)|\\.suivant)\\(\\s*([^,)]+)");

    private record Lecture(String fichier, String argument, Optional<String> valeur) {
    }

    private static String lire(Path fichier) {
        try {
            return Files.readString(fichier, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static Optional<String> constante(String nom, String source) {
        Matcher m = Pattern.compile("\\b" + Pattern.quote(nom) + "\\s*=\\s*\"([^\"]*)\"").matcher(source);
        return m.find() ? Optional.of(m.group(1)) : Optional.empty();
    }

    private static Optional<String> resoudre(String argument, String source, String generateur) {
        String a = argument.trim();
        if (a.startsWith("\"") && a.endsWith("\"") && a.length() >= 2) {
            return Optional.of(a.substring(1, a.length() - 1));
        }
        String nom = a.substring(a.lastIndexOf('.') + 1);
        return constante(nom, source).or(() -> constante(nom, generateur));
    }

    private static List<Lecture> lectures() throws IOException {
        String generateur;
        List<Path> fichiers;
        try (Stream<Path> arbre = Files.walk(SOURCES)) {
            fichiers = arbre.filter(p -> p.toString().endsWith(".java")).toList();
        }
        generateur = fichiers.stream().filter(p -> p.getFileName().toString().equals(GENERATEUR))
                .findFirst().map(SequencesEntiteLectureTest::lire).orElseThrow();
        List<Lecture> lectures = new ArrayList<>();
        for (Path fichier : fichiers) {
            if (fichier.getFileName().toString().equals(GENERATEUR)) {
                continue;
            }
            String source = lire(fichier);
            Matcher m = LECTURE.matcher(source);
            while (m.find()) {
                lectures.add(new Lecture(fichier.getFileName().toString(), m.group(1),
                        resoudre(m.group(1), source, generateur)));
            }
        }
        return lectures;
    }

    @Test
    void r7_5_aucuneLectureParCodeSeulDesQuatreSequencesDEntite() throws IOException {
        List<Lecture> lectures = lectures();

        assertAll(
                () -> assertThat(lectures).as("lectures de séquence trouvées (garde-fou du motif)")
                        .extracting(l -> l.valeur().orElse("?"))
                        .contains("Tier", "Prospection", "Dossier de vente", "Offre"),
                () -> assertThat(lectures).as("argument non résolu en littéral")
                        .filteredOn(l -> l.valeur().isEmpty())
                        .extracting(l -> l.fichier() + " : " + l.argument()).isEmpty(),
                () -> assertThat(lectures).as("séquence d'entité lue par code seul")
                        .filteredOn(l -> l.valeur().filter(CODES_PAR_SOCIETE::contains).isPresent())
                        .extracting(l -> l.fichier() + " : " + l.argument()).isEmpty());
    }
}
