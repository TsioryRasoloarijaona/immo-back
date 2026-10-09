# FIX — remettre le Quality Gate SonarCloud de `main` au vert

Branche `fix/sonar-gate-main` (immo-crm). Aucun changement fonctionnel, aucun changement d'API.

## Constat (09/10/2026, `node .claude/scripts/sonar.mjs branch main`)

Gate **ERROR** sur une seule condition : `new_reliability_rating = 2 (> 1)`, causée par **un** bug :

- `java:S5841` — `src/test/java/com/tsix/immocrm/controller/hotline/TracabiliteHistoriqueHotlineTest.java:593` :
  « Test the emptiness of the list before calling this assertion predicate. » Un `allMatch` (ou
  équivalent) sur une liste qui peut être vide passe toujours → le test ne prouve rien.

Deux autres problèmes sur du code écrit le 08-09/10, à corriger au passage :

- `java:S3358` — `src/main/java/com/tsix/immocrm/services/prospect/ProspectSubResourcesService.java:271` :
  ternaire imbriqué → `if/else` ou méthode nommée, comportement identique.
- `java:S1192` — `src/main/java/com/tsix/immocrm/config/RemonteeLecture.java:128` : littéral `" WHERE "`
  répété 3 fois → constante `private static final String`.

## Attendu

- S5841 : ajouter `isNotEmpty()` (ou une vérification de taille attendue) **avant** le prédicat, sans
  affaiblir ce que le test vérifie. Corriger toute autre occurrence du même motif dans les fichiers de
  test modifiés depuis le 08/10 (`grep -n "allMatch\|allSatisfy\|noneMatch"`).
- S3358 et S1192 : refactorisation pure, aucun changement de résultat.
- Respecter le skill `regles-qualite` (§1, §3).

**Annulé après analyse de la PR** : la constante `WHERE` dans `RemonteeLecture` a fait reclasser
la ligne SQL dynamique en nouveau code → S2077 (vulnérabilité) → gate sécurité en échec. S1192
n'étant qu'un code smell, le fichier est revenu à l'état de `main`.

## Vérification

- `gate.mjs back` sur `TracabiliteHistoriqueHotline*,ProspectSubResources*,Remontee*` : vert.
- Manager : CI verte puis `sonar.mjs pr <n>` → `GATE … OK`, 0 BUG sur le nouveau code.
