# Outils de génération de `instructions/REFERENCE-aide-omran.md`

La référence est l'extraction intégrale du site d'aide du légataire,
https://help.omranestate.com/ (HelpNDoc). Trois scripts la reconstruisent :

| Script | Rôle |
|---|---|
| `fetch.sh` | Découvre la liste des pages (`js/hndsd.min.js`), la table des matières (`_toc.json`) et télécharge chaque page, **une à la fois avec une pause** — une rafale parallèle a fait tomber le serveur le 26/09/2026. Relançable, ne reprend que ce qui manque. |
| `convert.py` | Convertit le corps de chaque page (`<div class="main-content">`) en Markdown fidèle : paragraphes, listes, tableaux, liens réécrits vers les ancres internes, images signalées par leur nom de fichier. Aucun résumé. Produit `REFERENCE.body.md` et `missing.json`. |
| `assemble.py` | Ajoute l'en-tête, le mode d'emploi, la table des matières hiérarchique et l'index des mots-clés, puis écrit le fichier final. |

## Regénérer

```bash
mkdir -p /tmp/aide-omran && cd /tmp/aide-omran
sh <repo>/instructions/aide-omran/fetch.sh .
cp <repo>/instructions/aide-omran/*.py .
python convert.py "rvts14,rvts47,rvts92,rvts114,rvts185,rvts95,rvts169,rvts24,rvts164,rvts125,rvts22,rvts30,rvts27,rvts73,rvts20,rvts91"
python assemble.py <repo>/instructions/REFERENCE-aide-omran.md
```

L'argument de `convert.py` est la liste des classes de style HelpNDoc (`rvtsNN`) que le
site utilise comme **titres de section** (`Liste`, `Fiche`, `Champs`, `Actions`…). Elle a
été déduite de l'usage (spans seules dans un paragraphe court) parce que la feuille de
style `css/hnd.content.css` n'était pas accessible ; si la CSS est présente dans le dossier
de travail, `convert.py` y lit aussi les classes en gras.

Après regénération, relancer `/graphify . --update` (les hooks git ne couvrent pas
`instructions/**`).
