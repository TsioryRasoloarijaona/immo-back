#!/bin/sh
# Télécharge l'intégralité du site d'aide OMRAN (HelpNDoc) dans le dossier courant :
#   topics.json  — liste [url, titre] de toutes les pages (lue dans js/hndsd.min.js)
#   _toc.json    — hiérarchie de la table des matières
#   _keywords.json, hnd.content.css
#   pages/*.html — une page par sujet
#
# Une requête à la fois, avec une pause : une rafale parallèle a fait tomber le serveur
# (ECONNREFUSED pendant plus de trente minutes) le 26/09/2026. Relançable : ne retélécharge
# que ce qui manque ou a échoué.
#
# Usage :  sh fetch.sh [dossier-de-travail]     puis  python convert.py && python assemble.py <sortie.md>
set -u
BASE="https://help.omranestate.com"
WORK="${1:-.}"; mkdir -p "$WORK/pages"; cd "$WORK"

get() { curl -sL --max-time 40 -o "$2" -w "%{http_code}" "$BASE/$1"; }

for f in js/hndsd.min.js _toc.json _keywords.json css/hnd.content.css; do
  out=$(basename "$f"); code=$(get "$f" "$out"); echo "$f $code"; sleep 1
done

python - <<'EOF'
import re, json, html, urllib.parse
s = open('hndsd.min.js', encoding='utf-8', errors='ignore').read()
m = re.search(r'var aTl=(\[.*?\]);', s, re.S)
items = re.findall(r"\['([^']+)','([^']*)'\]", m.group(1))
dec = lambda t: html.unescape(urllib.parse.unquote(t.replace('%u2019', '’')))
json.dump([(u, dec(t)) for u, t in items], open('topics.json', 'w', encoding='utf-8'), ensure_ascii=False, indent=0)
print(len(items), 'pages')
EOF

touch fetch.log
python -c "import json;[print(u) for u,_ in json.load(open('topics.json',encoding='utf-8'))]" | while read u; do
  if [ -s "pages/$u" ] && grep -q "^$u 200" fetch.log; then continue; fi
  code=$(get "$u" "pages/$u"); echo "$u $code $(wc -c < "pages/$u")" >> fetch.log
  [ "$code" = "200" ] || { echo "$u -> $code"; sleep 20; }
  sleep 2
done
echo "OK : $(grep -c ' 200 ' fetch.log) pages"; grep -v ' 200 ' fetch.log
