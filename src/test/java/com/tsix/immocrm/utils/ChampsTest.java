package com.tsix.immocrm.utils;

import com.tsix.immocrm.entities.cfg.CfgProject;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link Champs} remplace environ 220 lectures de champ à travers une association absente. Ce
 * test verrouille les deux propriétés dont dépend cette équivalence : la propagation du
 * {@code null}, et le fait que le maillon intermédiaire ne soit traversé qu'une fois.
 */
class ChampsTest {

    @Test
    void sourceNulle_rendNullSansAppelerLeLecteur() {
        AtomicInteger appels = new AtomicInteger();
        String resultat = Champs.de((CfgTranche) null, t -> {
            appels.incrementAndGet();
            return t.getCode();
        });
        assertThat(resultat).isNull();
        assertThat(appels).hasValue(0);
    }

    @Test
    void sourcePresente_rendLeChamp() {
        CfgTranche tranche = new CfgTranche();
        tranche.setCode("TR-01");
        assertThat(Champs.de(tranche, CfgTranche::getCode)).isEqualTo("TR-01");
    }

    /** Un champ nul sur une source présente reste nul — pas de confusion avec l'absence de source. */
    @Test
    void champNulSurSourcePresente_rendNull() {
        assertThat(Champs.de(new CfgTranche(), CfgTranche::getCode)).isNull();
    }

    @Test
    void deuxNiveaux_propageNullDepuisChaqueMaillon() {
        CfgTranche sansProjet = new CfgTranche();
        assertThat(Champs.de(sansProjet, CfgTranche::getCfgProject, CfgProject::getCode)).isNull();
        assertThat(Champs.de((CfgTranche) null, CfgTranche::getCfgProject, CfgProject::getCode)).isNull();

        CfgTranche avecProjet = new CfgTranche();
        CfgProject projet = new CfgProject();
        projet.setCode("PRJ-9");
        avecProjet.setCfgProject(projet);
        assertThat(Champs.de(avecProjet, CfgTranche::getCfgProject, CfgProject::getCode)).isEqualTo("PRJ-9");
    }

    /**
     * La raison d'être de la forme à deux niveaux : l'expression qu'elle remplace
     * ({@code a != null && a.getB() != null ? a.getB().getC() : null}) nommait le maillon
     * intermédiaire deux fois, donc le traversait deux fois — deux passages par le proxy
     * Hibernate sur une association {@code LAZY}.
     */
    @Test
    void deuxNiveaux_neTraverseLeMaillonIntermediaireQuUneFois() {
        CfgTranche tranche = new CfgTranche();
        tranche.setCfgProject(new CfgProject());
        AtomicInteger traversees = new AtomicInteger();

        Champs.de(tranche, t -> {
            traversees.incrementAndGet();
            return t.getCfgProject();
        }, CfgProject::getCode);

        assertThat(traversees).hasValue(1);
    }
}
