package com.tsix.immocrm.config.audit;

import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.security.AuthenticatedUser;
import jakarta.persistence.EntityManager;
import org.hibernate.engine.spi.SessionImplementor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

/**
 * Pour les tests d'autres paquets : la suite tourne en transactions annulées, que l'audit ne voit jamais. On oublie la
 * préparation, puis on rejoue le commit ({@code flush} + {@code beforeTransactionCompletion}).
 */
public final class AuditDeTest {

    private AuditDeTest() {
    }

    public static void oublierPreparation(EntityManager em) {
        em.flush();
        em.clear();
        EcouteurAuditOmran.viderTampons();
    }

    public static void simulerCommit(EntityManager em, AdmUser auteur) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new AuthenticatedUser(auteur.getId(), auteur.getCode(), null, false, true, 0), null,
                List.of(new SimpleGrantedAuthority("ROLE_Administrateur"))));
        em.flush();
        em.unwrap(SessionImplementor.class).getActionQueue().beforeTransactionCompletion();
    }
}
