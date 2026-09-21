package com.tsix.immocrm.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * La règle qui choisit la base, éprouvée seule.
 *
 * <p>Une seule décision, mais c'est elle qui détermine où atterrit chaque instruction SQL de
 * l'application. Elle mérite d'être lisible et vérifiée hors de tout contexte Spring.
 *
 * <p>Ce que ce test ne couvre pas, et il faut le dire : que
 * {@code LazyConnectionDataSourceProxy} soit bien en place. Sans lui la règle reste juste, mais
 * elle est consultée <b>trop tôt</b> — avant que la transaction n'ait posé son drapeau — et tout
 * part alors sur la source de vérité. Ce défaut-là ne se voit qu'en exécution réelle, en
 * regardant quelle base reçoit un {@code SELECT}.
 */
class RoutageCibleTest {

    @AfterEach
    void nettoyer() {
        TransactionSynchronizationManager.setCurrentTransactionReadOnly(false);
    }

    @Test
    void uneTransactionEnLectureSeuleVaSurLaCopie() {
        TransactionSynchronizationManager.setCurrentTransactionReadOnly(true);
        assertThat(DataSourceConfig.cibleCourante()).isEqualTo(DataSourceConfig.Cible.LECTURE);
    }

    @Test
    void uneTransactionDEcritureVaSurLaSourceDeVerite() {
        TransactionSynchronizationManager.setCurrentTransactionReadOnly(false);
        assertThat(DataSourceConfig.cibleCourante()).isEqualTo(DataSourceConfig.Cible.ECRITURE);
    }

    @Test
    void horsTransactionOnVaSurLaSourceDeVerite() {
        // Aucun drapeau posé : le cas d'un appel hors @Transactional. On choisit la lenteur
        // plutôt que le risque — servir une écriture depuis la copie perdrait la ligne.
        assertThat(DataSourceConfig.cibleCourante()).isEqualTo(DataSourceConfig.Cible.ECRITURE);
    }
}
