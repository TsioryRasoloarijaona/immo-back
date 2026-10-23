package com.tsix.immocrm.config;

import org.apache.poi.openxml4j.util.ZipSecureFile;
import org.apache.poi.util.IOUtils;
import org.springframework.context.annotation.Configuration;

/**
 * Bornes POI contre les classeurs piégés (FEATURE-patrimoine-import-export.md §1.2). Réglages statiques de la JVM :
 * posés une fois au démarrage, jamais par requête.
 */
@Configuration
public class ImportSecurise {

    static final double RATIO_INFLATION_MIN = 0.01;
    // Une feuille de 1 000 lignes tient en quelques Mo ; au-delà, XSSFWorkbook risquerait d'épuiser la mémoire.
    static final long ENTREE_MAX = 20L * 1024 * 1024;
    static final long ENTREES_MAX = 1000;
    static final int TABLEAU_MAX = 150 * 1024 * 1024;

    public ImportSecurise() {
        ZipSecureFile.setMinInflateRatio(Math.max(RATIO_INFLATION_MIN, ZipSecureFile.getMinInflateRatio()));
        ZipSecureFile.setMaxEntrySize(ENTREE_MAX);
        ZipSecureFile.setMaxFileCount(ENTREES_MAX);
        IOUtils.setByteArrayMaxOverride(TABLEAU_MAX);
    }
}
