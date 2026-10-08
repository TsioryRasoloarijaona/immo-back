package com.tsix.immocrm.services.prospection;

import com.tsix.immocrm.exceptions.InvalidRequestException;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Lit l'export CSV des leads (Meta / Leads Center), RFC 4180, à la main : pas de bibliothèque CSV
 * dans le {@code pom.xml}. Tolère ce qu'en fait Excel : {@code ;}, Windows-1252, colonnes dans tout
 * ordre. Voir {@code instructions/FEATURE-creation-pre-prospection.md} §5.
 */
public final class LecteurCsvLeads {

    /** Au-delà, le fichier est refusé entier. */
    public static final int LIGNES_MAX = 5000;

    /** Date « Créé » de l'export : {@code 09/23/2026 1:45am} — mois en premier, heure sur 12. */
    private static final DateTimeFormatter FORMAT_EXPORT = new DateTimeFormatterBuilder()
            .parseCaseInsensitive()
            .appendPattern("M/d/uuuu h:mma")
            .toFormatter(Locale.US);

    /** Repli ISO, pour un fichier retouché ({@code 2026-09-23 01:45} ou {@code 2026-09-23T01:45}). */
    private static final DateTimeFormatter FORMAT_ISO = new DateTimeFormatterBuilder()
            .appendPattern("uuuu-MM-dd")
            .optionalStart().appendLiteral('T').optionalEnd()
            .optionalStart().appendLiteral(' ').optionalEnd()
            .appendPattern("HH:mm[:ss]")
            .toFormatter(Locale.ROOT);

    private enum Colonne {
        CREE, NOM, EMAIL, SOURCE, FORMULAIRE, CANAL, ETAPE, PROPRIETAIRE, ETIQUETTES,
        TELEPHONE, TELEPHONE_SECONDAIRE, WHATSAPP
    }

    private static final Map<String, Colonne> ENTETES = Map.ofEntries(
            Map.entry("cree", Colonne.CREE),
            Map.entry("nom", Colonne.NOM),
            Map.entry("adresse e mail", Colonne.EMAIL),
            Map.entry("e mail", Colonne.EMAIL),
            Map.entry("email", Colonne.EMAIL),
            Map.entry("source", Colonne.SOURCE),
            Map.entry("formulaire", Colonne.FORMULAIRE),
            Map.entry("canal", Colonne.CANAL),
            Map.entry("etape", Colonne.ETAPE),
            Map.entry("proprietaire", Colonne.PROPRIETAIRE),
            Map.entry("etiquettes", Colonne.ETIQUETTES),
            Map.entry("telephone", Colonne.TELEPHONE),
            Map.entry("numero de telephone secondaire", Colonne.TELEPHONE_SECONDAIRE),
            Map.entry("numero whatsapp", Colonne.WHATSAPP));

    /**
     * @param numeroLigne en-tête = 1, comme dans un tableur
     * @param erreur      {@code null} si la ligne est exploitable ; une ligne fautive n'arrête pas la lecture
     */
    public record LigneLead(int numeroLigne, LocalDateTime dateCreation, String nom, String email,
                            String source, String formulaire, String canal, String etape,
                            String proprietaire, String etiquettes, String telephone,
                            String telephoneSecondaire, String whatsapp, String erreur) {
    }

    private LecteurCsvLeads() {
    }

    /** @throws InvalidRequestException fichier vide, sans colonne {@code Nom}, ou trop long */
    public static List<LigneLead> lire(byte[] contenu) {
        String texte = decoder(contenu);
        if (texte.isBlank()) {
            throw new InvalidRequestException("Le fichier est vide");
        }
        char separateur = separateur(texte);
        List<Enregistrement> enregistrements = decouper(texte, separateur);

        Map<Colonne, Integer> index = indexer(enregistrements.getFirst().champs());
        if (!index.containsKey(Colonne.NOM)) {
            throw new InvalidRequestException("Colonne « Nom » introuvable dans l'en-tête — format "
                    + "attendu : l'export des leads (Créé, Nom, Adresse e-mail, …, Téléphone)");
        }
        List<Enregistrement> donnees = enregistrements.subList(1, enregistrements.size()).stream()
                .filter(e -> e.champs().stream().anyMatch(c -> !c.isBlank()))
                .toList();
        if (donnees.size() > LIGNES_MAX) {
            throw new InvalidRequestException("Fichier trop long : " + donnees.size()
                    + " lignes, " + LIGNES_MAX + " au plus — le découper");
        }
        return donnees.stream().map(e -> ligne(e, index)).toList();
    }

    private static LigneLead ligne(Enregistrement e, Map<Colonne, Integer> index) {
        String cree = champ(e, index, Colonne.CREE);
        String nom = champ(e, index, Colonne.NOM);
        String email = champ(e, index, Colonne.EMAIL);
        String telephone = champ(e, index, Colonne.TELEPHONE);

        LocalDateTime date = null;
        String erreur = null;
        if (nom == null) {
            erreur = "Nom vide";
        } else if (nom.length() > 255) {
            erreur = "Nom de plus de 255 caractères";
        } else if (email != null && email.length() > 255) {
            erreur = "Email de plus de 255 caractères";
        } else if (telephone != null && telephone.length() > 255) {
            erreur = "Téléphone de plus de 255 caractères";
        } else if (cree == null) {
            erreur = "Date « Créé » vide";
        } else {
            date = date(cree);
            if (date == null) {
                erreur = "Date « Créé » illisible : « " + cree + " » (attendu MM/JJ/AAAA h:mmam, "
                        + "p.ex. 09/23/2026 1:45am)";
            }
        }
        return new LigneLead(e.numeroLigne(), date, nom, email,
                champ(e, index, Colonne.SOURCE), champ(e, index, Colonne.FORMULAIRE),
                champ(e, index, Colonne.CANAL), champ(e, index, Colonne.ETAPE),
                champ(e, index, Colonne.PROPRIETAIRE), champ(e, index, Colonne.ETIQUETTES),
                telephone, champ(e, index, Colonne.TELEPHONE_SECONDAIRE),
                champ(e, index, Colonne.WHATSAPP), erreur);
    }

    static LocalDateTime date(String valeur) {
        String v = valeur.trim().replaceAll("\\s+", " ");
        for (DateTimeFormatter format : List.of(FORMAT_EXPORT, FORMAT_ISO)) {
            try {
                return LocalDateTime.parse(v, format);
            } catch (DateTimeParseException ignoree) {
                // format suivant
            }
        }
        return null;
    }

    private static String champ(Enregistrement e, Map<Colonne, Integer> index, Colonne colonne) {
        Integer i = index.get(colonne);
        if (i == null || i >= e.champs().size()) {
            return null;
        }
        String v = e.champs().get(i).trim();
        return v.isEmpty() ? null : v;
    }

    private static Map<Colonne, Integer> indexer(List<String> entete) {
        Map<Colonne, Integer> index = new EnumMap<>(Colonne.class);
        for (int i = 0; i < entete.size(); i++) {
            Colonne c = ENTETES.get(normaliserEntete(entete.get(i)));
            if (c != null) {
                index.putIfAbsent(c, i);
            }
        }
        return index;
    }

    /** {@code "Adresse e-mail"} → {@code "adresse e mail"}, {@code "Étape"} → {@code "etape"}. */
    static String normaliserEntete(String entete) {
        String sansAccents = Normalizer.normalize(entete, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return sansAccents.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", " ").trim();
    }

    private static String decoder(byte[] contenu) {
        int debut = contenu.length >= 3 && (contenu[0] & 0xFF) == 0xEF && (contenu[1] & 0xFF) == 0xBB
                && (contenu[2] & 0xFF) == 0xBF ? 3 : 0;
        ByteBuffer octets = ByteBuffer.wrap(contenu, debut, contenu.length - debut);
        try {
            return StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(octets).toString();
        } catch (CharacterCodingException pasUtf8) {
            // Un CSV réenregistré par Excel sous Windows.
            return new String(contenu, debut, contenu.length - debut, Charset.forName("windows-1252"));
        }
    }

    /** Le plus fréquent de {@code ,} et {@code ;} sur la première ligne, hors guillemets. */
    private static char separateur(String texte) {
        int virgules = 0;
        int pointsVirgules = 0;
        boolean cite = false;
        for (int i = 0; i < texte.length(); i++) {
            char c = texte.charAt(i);
            if (c == '"') {
                cite = !cite;
            } else if (!cite && (c == '\n' || c == '\r')) {
                break;
            } else if (!cite && c == ',') {
                virgules++;
            } else if (!cite && c == ';') {
                pointsVirgules++;
            }
        }
        return pointsVirgules > virgules ? ';' : ',';
    }

    private record Enregistrement(int numeroLigne, List<String> champs) {
    }

    /** RFC 4180 : {@code ""} dans un champ cité vaut {@code "}, et un champ cité peut couvrir plusieurs lignes. */
    private static List<Enregistrement> decouper(String texte, char separateur) {
        return new DecoupeurCsv(texte, separateur).decouper();
    }

    /** Automate de {@link #decouper} : une instance par fichier, état mutable non partagé. */
    private static final class DecoupeurCsv {
        private final String texte;
        private final char separateur;
        private final List<Enregistrement> resultat = new ArrayList<>();
        private List<String> champs = new ArrayList<>();
        private final StringBuilder courant = new StringBuilder();
        private boolean cite = false;
        private int ligne = 1;
        private int debutEnregistrement = 1;
        private int position = 0;

        private DecoupeurCsv(String texte, char separateur) {
            this.texte = texte;
            this.separateur = separateur;
        }

        private List<Enregistrement> decouper() {
            while (position < texte.length()) {
                char c = texte.charAt(position);
                if (cite) {
                    lireDansCitation(c);
                } else {
                    lireHorsCitation(c);
                }
                position++;
            }
            if (!courant.isEmpty() || !champs.isEmpty()) {
                finirChamp();
                resultat.add(new Enregistrement(debutEnregistrement, champs));
            }
            return resultat;
        }

        private void lireDansCitation(char c) {
            if (c == '"') {
                if (suivantEst('"')) {
                    courant.append('"');
                    position++;
                } else {
                    cite = false;
                }
                return;
            }
            if (c == '\n') {
                ligne++;
            }
            courant.append(c);
        }

        private void lireHorsCitation(char c) {
            if (c == '"') {
                cite = true;
            } else if (c == separateur) {
                finirChamp();
            } else if (c == '\n' || c == '\r') {
                if (c == '\r' && suivantEst('\n')) {
                    position++;
                }
                finirEnregistrement();
            } else {
                courant.append(c);
            }
        }

        private boolean suivantEst(char attendu) {
            return position + 1 < texte.length() && texte.charAt(position + 1) == attendu;
        }

        private void finirChamp() {
            champs.add(courant.toString());
            courant.setLength(0);
        }

        private void finirEnregistrement() {
            finirChamp();
            resultat.add(new Enregistrement(debutEnregistrement, champs));
            champs = new ArrayList<>();
            ligne++;
            debutEnregistrement = ligne;
        }
    }
}
