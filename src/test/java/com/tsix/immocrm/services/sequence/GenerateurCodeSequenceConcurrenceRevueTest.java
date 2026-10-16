package com.tsix.immocrm.services.sequence;

import com.tsix.immocrm.entities.adm.AdmSequence;
import com.tsix.immocrm.repositories.adm.AdmSequenceRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Revue FEATURE-vente-echeancier.md §1.4 : deux insertions de frais concurrentes ne doivent pas recevoir le même
 * QuittanceCode. A lit le compteur, B lit, incrémente et valide, puis A valide : sans verrou, A réécrit la même valeur.
 */
@SpringBootTest
class GenerateurCodeSequenceConcurrenceRevueTest {

    @Autowired private GenerateurCodeSequence generateur;
    @Autowired private AdmSequenceRepository sequenceRepository;
    @Autowired private PlatformTransactionManager transactionManager;

    private String code;
    private UUID sequenceId;

    @BeforeEach
    void compteur() {
        code = "REVUE-CONC-" + UUID.randomUUID().toString().substring(0, 8);
        AdmSequence s = new AdmSequence();
        sequenceId = UUID.randomUUID();
        s.setId(sequenceId);
        s.setCode(code);
        s.setSeqValue(100);
        s.setSeqValueNumber(5);
        s.setSeqYearNumber(2);
        s.setBasedOnSystemYear(true);
        new TransactionTemplate(transactionManager).executeWithoutResult(t -> sequenceRepository.save(s));
    }

    @AfterEach
    void nettoyer() {
        new TransactionTemplate(transactionManager).executeWithoutResult(t -> sequenceRepository.deleteById(sequenceId));
    }

    @Test
    void deuxTransactionsConcurrentes_obtiennentDesNumerosDistincts() throws Exception {
        CountDownLatch aALu = new CountDownLatch(1);
        CountDownLatch bAValide = new CountDownLatch(1);
        TransactionTemplate tx = new TransactionTemplate(transactionManager);

        CompletableFuture<String> a = CompletableFuture.supplyAsync(() -> tx.execute(t -> {
            String numero = generateur.suivant(code, 2, 5);
            aALu.countDown();
            // Avec un verrou, B attend la fin de A : A valide alors après le délai, sans échouer.
            patienter(bAValide);
            return numero;
        }));
        attendre(aALu);
        String b;
        try {
            b = tx.execute(t -> generateur.suivant(code, 2, 5));
        } finally {
            bAValide.countDown();
        }
        String numeroA = a.get(30, TimeUnit.SECONDS);

        assertThat(numeroA).isNotNull().isNotEqualTo(b);
    }

    private static boolean patienter(CountDownLatch latch) {
        try {
            return latch.await(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    private static void attendre(CountDownLatch latch) {
        try {
            if (!latch.await(10, TimeUnit.SECONDS)) {
                throw new IllegalStateException("délai dépassé");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }
}
