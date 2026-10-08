package com.tsix.immocrm.services.prospection;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.lang.reflect.RecordComponent;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Le découpeur CSV refactoré rend exactement les enregistrements (numéro de ligne inclus) de l'ancienne boucle. */
class DecoupeurCsvEquivalenceTest {

    private record Ref(int numeroLigne, List<String> champs) {
    }

    /** Copie conforme de l'implémentation d'avant le refactor. */
    private static List<Ref> ancien(String texte, char separateur) {
        List<Ref> resultat = new ArrayList<>();
        List<String> champs = new ArrayList<>();
        StringBuilder courant = new StringBuilder();
        boolean cite = false;
        int ligne = 1;
        int debutEnregistrement = 1;
        for (int i = 0; i < texte.length(); i++) {
            char c = texte.charAt(i);
            if (cite) {
                if (c == '"') {
                    if (i + 1 < texte.length() && texte.charAt(i + 1) == '"') {
                        courant.append('"');
                        i++;
                    } else {
                        cite = false;
                    }
                } else {
                    if (c == '\n') {
                        ligne++;
                    }
                    courant.append(c);
                }
            } else if (c == '"') {
                cite = true;
            } else if (c == separateur) {
                champs.add(courant.toString());
                courant.setLength(0);
            } else if (c == '\n' || c == '\r') {
                if (c == '\r' && i + 1 < texte.length() && texte.charAt(i + 1) == '\n') {
                    i++;
                }
                champs.add(courant.toString());
                courant.setLength(0);
                resultat.add(new Ref(debutEnregistrement, champs));
                champs = new ArrayList<>();
                ligne++;
                debutEnregistrement = ligne;
            } else {
                courant.append(c);
            }
        }
        if (courant.length() > 0 || !champs.isEmpty()) {
            champs.add(courant.toString());
            resultat.add(new Ref(debutEnregistrement, champs));
        }
        return resultat;
    }

    @SuppressWarnings("unchecked")
    private static List<Ref> nouveau(String texte, char separateur) throws Exception {
        Method m = LecteurCsvLeads.class.getDeclaredMethod("decouper", String.class, char.class);
        m.setAccessible(true);
        List<Object> brut = (List<Object>) m.invoke(null, texte, separateur);
        List<Ref> out = new ArrayList<>();
        for (Object e : brut) {
            RecordComponent[] rc = e.getClass().getRecordComponents();
            Method num = rc[0].getAccessor();
            Method ch = rc[1].getAccessor();
            num.setAccessible(true);
            ch.setAccessible(true);
            out.add(new Ref((int) num.invoke(e), (List<String>) ch.invoke(e)));
        }
        return out;
    }

    @Test
    void memesEnregistrementsQueLAncienneBoucle() throws Exception {
        List<String> entrees = List.of(
                "",
                "a",
                "a,b,c",
                "a,b,c\n",
                "a,b,\n",
                "a,b,",
                ",",
                "\n",
                "\n\n",
                "\r\n",
                "\r",
                "\r\r\n\n",
                "a\r\nb\rc\nd",
                "\"x\"\"y\",z\n",
                "\"\"\"\"",
                "\"\"",
                "\"a\nb\nc\",d\ne,f",
                "\"a\r\nb\",c\r\nd,e\r\n",
                "\"unterminated\nline",
                "\"a\"b,c",
                "a\"b\"c,d",
                "h1;h2\n\"v;1\";v2\n;\n",
                "x,\"\"\r\n",
                "\"fin\"\"\"",
                "a,\"b\"\"\n\"\"c\",d\n\ne");
        for (String s : entrees) {
            for (char sep : new char[]{',', ';'}) {
                assertThat(nouveau(s, sep)).as("entrée %s sép %s", s.replace("\n", "\\n").replace("\r", "\\r"), sep)
                        .isEqualTo(ancien(s, sep));
            }
        }
    }
}
