#!/usr/bin/env node
// Compare deux documents OpenAPI (invariant I1 de FEATURE-commentaires-et-docs-api.md).
// Usage : node scripts/compare-api-docs.mjs <reference> <url-ou-fichier>
// Égalité JSON stricte, sauf dans les valeurs chaîne des clés `description` et `summary`
// où toute suite de blancs est ramenée à un espace. Code de sortie 1 s'il y a une différence.
import { readFile } from 'node:fs/promises';

const NORMALISEES = new Set(['description', 'summary']);

async function charger(source) {
  if (/^https?:\/\//.test(source)) {
    const res = await fetch(source);
    if (!res.ok) throw new Error(`${source} : HTTP ${res.status}`);
    return res.json();
  }
  return JSON.parse(await readFile(source, 'utf8'));
}

const genre = (v) => (v === null ? 'null' : Array.isArray(v) ? 'array' : typeof v);
const normaliser = (s) => s.replace(/\s+/g, ' ');
const segment = (cle) => (/^[A-Za-z_$][\w$]*$/.test(cle) ? `.${cle}` : `[${JSON.stringify(cle)}]`);

function comparer(a, b, chemin, cle, diffs) {
  const ga = genre(a);
  const gb = genre(b);
  if (ga !== gb) {
    diffs.push(`${chemin} : type ${ga} ≠ ${gb}`);
    return;
  }
  if (ga === 'array') {
    if (a.length !== b.length) diffs.push(`${chemin} : longueur ${a.length} ≠ ${b.length}`);
    const n = Math.min(a.length, b.length);
    for (let i = 0; i < n; i++) comparer(a[i], b[i], `${chemin}[${i}]`, null, diffs);
    return;
  }
  if (ga === 'object') {
    const cles = new Set([...Object.keys(a), ...Object.keys(b)]);
    for (const k of cles) {
      const p = chemin + segment(k);
      if (!(k in b)) diffs.push(`${p} : absent à droite`);
      else if (!(k in a)) diffs.push(`${p} : absent à gauche (référence)`);
      else comparer(a[k], b[k], p, k, diffs);
    }
    return;
  }
  if (ga === 'string' && NORMALISEES.has(cle)) {
    const na = normaliser(a);
    const nb = normaliser(b);
    if (na !== nb) {
      let i = 0;
      while (i < na.length && na[i] === nb[i]) i++;
      const extrait = (s) => JSON.stringify(s.slice(Math.max(0, i - 20), i + 40));
      diffs.push(`${chemin} : texte différent à ${i} — ${extrait(na)} ≠ ${extrait(nb)}`);
    }
    return;
  }
  if (a !== b) diffs.push(`${chemin} : ${JSON.stringify(a)} ≠ ${JSON.stringify(b)}`);
}

const [reference, cible] = process.argv.slice(2);
if (!reference || !cible) {
  console.error('Usage : node scripts/compare-api-docs.mjs <reference> <url-ou-fichier>');
  process.exit(2);
}
const [a, b] = await Promise.all([charger(reference), charger(cible)]);
const diffs = [];
comparer(a, b, '$', null, diffs);
if (diffs.length === 0) {
  console.log('Aucune différence.');
} else {
  for (const d of diffs) console.log(d);
  console.log(`${diffs.length} différence(s).`);
  process.exitCode = 1; // pas process.exit() : sous Windows il coupe fetch en cours de fermeture
}
