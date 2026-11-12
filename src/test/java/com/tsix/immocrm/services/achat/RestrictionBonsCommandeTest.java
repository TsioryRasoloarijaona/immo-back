package com.tsix.immocrm.services.achat;

import org.junit.jupiter.api.Test;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Lot A2a, E1 « une seule règle partagée » (Vérification n° 2, règle 6) : le « qui est restreint » vit dans le bean
 * {@code RestrictionBonsCommande}, et le fragment SQL du déclencheur est une constante de {@code RequetesDocumentsAchat}
 * reprise par {@code LISTE} et par {@code FICHE}. Lu par réflexion : les noms des membres ne sont pas fixés par le spec.
 */
class RestrictionBonsCommandeTest {

    private static final String PAQUET = "com.tsix.immocrm.services.achat.";
    private static final Set<String> REQUETES = Set.of("LISTE", "FICHE");

    @Test
    void e1_restrictionBonsCommande_estUnBeanDuPaquetAchat() throws Exception {
        Class<?> classe = Class.forName(PAQUET + "RestrictionBonsCommande");

        assertThat(AnnotatedElementUtils.hasAnnotation(classe, Component.class)).isTrue();
    }

    @Test
    void e1_fragmentDeclencheur_constantePartagee_parListeEtParFiche() throws Exception {
        String liste = constante("LISTE");
        String fiche = constante("FICHE");

        List<String> fragments = new ArrayList<>();
        for (Field champ : RequetesDocumentsAchat.class.getDeclaredFields()) {
            if (estConstanteTexte(champ) && !REQUETES.contains(champ.getName())) {
                champ.setAccessible(true);
                String valeur = (String) champ.get(null);
                if (valeur.contains("CfgTierTriggerId") && liste.contains(valeur) && fiche.contains(valeur)) {
                    fragments.add(champ.getName());
                }
            }
        }

        assertThat(fragments).as("constante portant CfgTierTriggerId reprise par LISTE et FICHE").isNotEmpty();
    }

    private static String constante(String nom) throws ReflectiveOperationException {
        Field champ = RequetesDocumentsAchat.class.getDeclaredField(nom);
        champ.setAccessible(true);
        return (String) champ.get(null);
    }

    private static boolean estConstanteTexte(Field champ) {
        int m = champ.getModifiers();
        return Modifier.isStatic(m) && Modifier.isFinal(m) && champ.getType() == String.class;
    }
}
