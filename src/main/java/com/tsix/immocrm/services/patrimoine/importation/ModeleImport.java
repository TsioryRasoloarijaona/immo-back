package com.tsix.immocrm.services.patrimoine.importation;

import com.tsix.immocrm.domain.ColonneLibre;
import com.tsix.immocrm.domain.TypeExport;
import com.tsix.immocrm.entities.CodeLibelleOmran;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.security.UtilisateurAffiche;
import com.tsix.immocrm.services.patrimoine.importation.ChampsArticle.ChampImport;
import com.tsix.immocrm.services.patrimoine.importation.ImportParType.Modele;
import com.tsix.immocrm.web.export.ExportTableur;
import com.tsix.immocrm.web.export.FeuilleExport;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Classeur modèle d'import (FEATURE-patrimoine-import-export.md §1.1) : feuille de données du type (en-têtes OMRAN, une
 * ligne par objet existant), puis une feuille d'aide (Code, Libellé) par référentiel cité.
 */
@Service
@Transactional(readOnly = true)
public class ModeleImport {

    private static final DateTimeFormatter HORODATAGE = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    private static final String CODE = "Code";
    private static final String LIBELLE = "Libellé";
    private static final List<Colonne> AIDE = List.of(new Colonne(CODE, TypeExport.TEXTE), new Colonne(LIBELLE, TypeExport.TEXTE));

    private final ImportPatrimoineService importation;
    private final ExportTableur export;
    private final UtilisateurAffiche utilisateur;
    private final Clock clock;

    public ModeleImport(ImportPatrimoineService importation, ExportTableur export, UtilisateurAffiche utilisateur,
                        Clock clock) {
        this.importation = importation;
        this.export = export;
        this.utilisateur = utilisateur;
        this.clock = clock;
    }

    /** Colonne du modèle : nom = libellé = en-tête OMRAN. */
    record Colonne(String nom, TypeExport typeExport) implements ColonneLibre {
        @Override
        public String libelle() {
            return nom;
        }

        @Override
        public boolean parDefaut() {
            return true;
        }
    }

    public ResponseEntity<byte[]> classeur(String type, UUID trancheId) {
        ImportParType<?> t = importation.parType(type);
        Modele modele = t.modele(type, importation.tranche(t, trancheId));
        String fichier = modele.prefixe() + "_" + LocalDateTime.now(clock).format(HORODATAGE) + ".xlsx";
        return ResponseEntity.ok()
                .contentType(ExportTableur.XLSX)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(fichier).build().toString())
                .body(octets(modele.feuilles().get(0).nom(), modele.feuilles()));
    }

    /** Feuille de données : une colonne par en-tête, lignes = en-tête → valeur. */
    static FeuilleExport donnees(String nom, List<ChampImport> champs, List<Map<String, Object>> lignes) {
        return new FeuilleExport(nom, champs.stream().map(ModeleImport::colonne).toList(), lignes, null);
    }

    /** Une feuille d'aide par référentiel des champs, nommée comme son entité. */
    static List<FeuilleExport> aides(List<ChampImport> champs, ResolutionCodes codes) {
        Set<Class<? extends CodeLibelleOmran>> referentiels = new LinkedHashSet<>();
        champs.stream().filter(c -> c.reference() != null).forEach(c -> referentiels.add(c.reference().classe()));
        return referentiels.stream().map(classe -> aide(classe.getSimpleName(), codes.toutes(classe))).toList();
    }

    static FeuilleExport aide(String nom, List<? extends CodeLibelleOmran> lignes) {
        return aideDe(nom, lignes.stream().map(r -> new String[]{r.getCode(), r.getDescription()}).toList());
    }

    /** Feuille d'aide depuis des paires {@code [code, libellé]}. */
    static FeuilleExport aideDe(String nom, List<String[]> codesLibelles) {
        return new FeuilleExport(nom, AIDE, codesLibelles.stream().map(ModeleImport::aide).toList(), null);
    }

    /** Ligne d'un objet ; {@code sousArticles} remplit les colonnes de rang 1 à n. */
    static Map<String, Object> ligne(StkItem item, List<StkItem> sousArticles, List<ChampImport> champs) {
        Map<String, Object> ligne = new HashMap<>();
        for (ChampImport c : champs) {
            StkItem objet = c.rang() == 0 ? item : rang(sousArticles, c.rang());
            if (objet != null) {
                ligne.put(c.entete(), ChampsArticle.pourModele(c, objet));
            }
        }
        return ligne;
    }

    private static StkItem rang(List<StkItem> sousArticles, int rang) {
        return rang <= sousArticles.size() ? sousArticles.get(rang - 1) : null;
    }

    private static Map<String, Object> aide(String[] codeLibelle) {
        Map<String, Object> ligne = new HashMap<>();
        ligne.put(CODE, codeLibelle[0]);
        ligne.put(LIBELLE, codeLibelle[1]);
        return ligne;
    }

    private static Colonne colonne(ChampImport c) {
        TypeExport type = switch (c.type()) {
            case NOMBRE, ENTIER, TVA -> TypeExport.NOMBRE;
            case DATE -> TypeExport.DATE;
            case BOOLEEN, BOOLEEN_INVERSE -> TypeExport.BOOLEEN;
            default -> TypeExport.TEXTE;
        };
        return new Colonne(c.entete(), type);
    }

    private byte[] octets(String titre, List<FeuilleExport> feuilles) {
        ByteArrayOutputStream sortie = new ByteArrayOutputStream();
        try {
            export.ecrire(sortie, titre, Objects.toString(utilisateur.courant().nom(), ""), feuilles);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return sortie.toByteArray();
    }
}
