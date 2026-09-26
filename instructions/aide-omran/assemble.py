# -*- coding: utf-8 -*-
"""Assemble la reference finale : en-tete, index hierarchique, index des mots-cles, corps."""
import json, re, datetime, sys
import convert

TOC = convert.TOC
byid = {n['id']: n for n in TOC}
kids = {}
for n in TOC:
    kids.setdefault(n['parent'], []).append(n)

body = open('REFERENCE.body.md', encoding='utf-8').read()
missing = json.load(open('missing.json'))
fetched = sum(1 for n in TOC if n['a_attr']['href'] not in missing)

lines = []
lines.append('# Référence — Aide en ligne OMRAN Estate (help.omranestate.com)\n')
lines.append('_Extraction intégrale du site d\'aide du logiciel légataire, réalisée le %s. '
             '%d pages sur %d. Source : https://help.omranestate.com/ (HelpNDoc, éditeur YASMINE Engineering Systems)._\n'
             % (datetime.date.today().strftime('%d/%m/%Y'), fetched, len(TOC)))
lines.append('''
## Comment lire ce fichier

- **Une section par page du site**, dans l'ordre de la table des matières du site, avec le fil
  d'Ariane (`Module › Sous-module`) et le nom du fichier HTML d'origine (`PROSPECTIONS.html`).
  L'ancre de chaque page vaut `page-<nom-du-fichier-en-minuscules>` — les liens internes de
  l'aide sont réécrits vers ces ancres.
- **Le texte est restitué tel quel**, sans résumé ni reformulation : paragraphes, listes, tableaux,
  remarques. Les captures d'écran ne sont pas transcrites ; elles sont signalées par
  `[image : NomDuFichier.png]` à l'endroit où elles apparaissent.
- Les pages d'écran suivent presque toutes le même plan, à retrouver par les titres `### Liste`,
  `### Fiche`, `### Champs`, `### Actions` :
  - `Liste` — les colonnes du masque de liste. Colonne **V** = visible par défaut (`X`).
  - `Fiche` / `Champs` — les champs du masque d'édition. **U** = unique, **O** = obligatoire.
    La colonne `OBSERVATIONS` porte les règles de gestion (calculs, valeurs par défaut,
    listes déroulantes et leur page de configuration, effets de bord).
  - `Actions` — les boutons du masque et ce que chacun déclenche.
  - `Remarque n :` — règles de gestion transverses de l'écran. **Ce sont les phrases à
    reproduire** quand on réimplémente une fonctionnalité.
- Les règles générales à tout le logiciel (`GEN0001`…) sont sur la page « Règles de gestion
  générale » ; les pages d'écran y renvoient par identifiant.
- Un tableau imbriqué dans une cellule apparaît avec ses barres échappées (`\\|`) : c'est une
  limite du Markdown, pas une perte de contenu.

## Ce que cette référence ne remplace pas

Le site décrit le **comportement attendu** du logiciel de bureau. Le comportement **réel**
se lit dans la base (`yesRemsProd` en local, source 115) et dans la trace d'audit
(`C:\\backup\\*.sqlaudit`) — voir `CLAUDE.md`. Quand les deux divergent, la trace fait foi ;
la divergence se note dans la spec `FEATURE-*.md`.
''')

# ---- Index hiérarchique
lines.append('\n## Table des matières\n')
def walk(parent, depth):
    for n in kids.get(parent, []):
        url = n['a_attr']['href']
        flag = ' _(non téléchargée)_' if url in missing else ''
        lines.append('%s- [%s](#%s) `%s`%s' % ('  ' * depth, n['text'], convert.anchor(url), url, flag))
        walk(n['id'], depth + 1)
walk('#', 0)

# ---- Index des mots-clés (index du site)
try:
    kw = json.load(open('_keywords.json', encoding='utf-8'))
    lines.append('\n## Index des mots-clés du site\n')
    lines.append('_L\'index alphabétique du site : chaque entrée renvoie aux pages qui la traitent._\n')
    for k in sorted(kw, key=lambda x: x['text'].lower()):
        rel = json.loads(k['a_attr'].get('data-related', '[]'))
        pages = [(k['a_attr']['href'], None)] + [(r['url'], r['title']) for r in rel]
        seen = []; refs = []
        for url, title in pages:
            if url in seen: continue
            seen.append(url)
            t = title or next((n['text'] for n in TOC if n['a_attr']['href'] == url), url)
            refs.append('[%s](#%s)' % (t, convert.anchor(url)))
        lines.append('- **%s** — %s' % (k['text'], ', '.join(refs)))
except Exception as e:
    lines.append('\n_(index des mots-clés indisponible : %s)_\n' % e)

lines.append('\n---\n\n# Pages\n')
out = '\n'.join(lines) + body + '\n'
open(sys.argv[1] if len(sys.argv) > 1 else 'REFERENCE-aide-omran.md', 'w', encoding='utf-8', newline='\n').write(out)
print('ecrit', len(out), 'caracteres ;', fetched, '/', len(TOC), 'pages')
