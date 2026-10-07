#!/usr/bin/env node
// Vérifie qu'aucun jeton de code ne change hors textes OpenAPI et commentaires
// (invariant I2 de FEATURE-commentaires-et-docs-api.md).
// Usage : node scripts/compare-code.mjs <ref-git> [--tous]   (--tous : compare aussi les fichiers inchangés)
// Compare chaque .java de src/main modifié depuis <ref-git> (arbre de travail compris) :
// commentaires retirés, arguments `description`/`summary` de @Operation, @Schema, @Parameter,
// @ApiResponse et @Tag retirés (l'annotation entière si elle n'a plus d'argument, et l'import
// de ces cinq annotations), blancs ignorés. Code de sortie 1 et liste des fichiers sinon.
import { execFileSync } from 'node:child_process';
import { readFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';

const RACINE = join(dirname(fileURLToPath(import.meta.url)), '..');
// Fichiers du mécanisme ajoutés à l'étape 1 : hors comparaison.
const EXCLUS = ['src/main/java/com/tsix/immocrm/config/apidocs/'];
const ANNOTATIONS = new Set(['Operation', 'Schema', 'Parameter', 'ApiResponse', 'Tag']);
const ARGUMENTS_TEXTE = new Set(['description', 'summary']);
const IMPORTS_IGNORES = new Set([
  'io.swagger.v3.oas.annotations.Operation',
  'io.swagger.v3.oas.annotations.Parameter',
  'io.swagger.v3.oas.annotations.tags.Tag',
  'io.swagger.v3.oas.annotations.media.Schema',
  'io.swagger.v3.oas.annotations.responses.ApiResponse',
]);

function git(...args) {
  return execFileSync('git', args, { cwd: RACINE, encoding: 'utf8', maxBuffer: 64 * 1024 * 1024, stdio: ['ignore', 'pipe', 'pipe'] });
}

/** Découpe un source Java en jetons, sans blancs ni commentaires. */
function jetons(src) {
  const out = [];
  let i = 0;
  const n = src.length;
  const lireChaine = (debut, fin) => {
    let j = debut;
    while (j < n) {
      if (src[j] === '\\') { j += 2; continue; }
      if (src.startsWith(fin, j)) return j + fin.length;
      j++;
    }
    throw new Error(`littéral non fermé à ${debut}`);
  };
  while (i < n) {
    const c = src[i];
    if (/\s/.test(c)) { i++; continue; }
    if (src.startsWith('//', i)) { while (i < n && src[i] !== '\n') i++; continue; }
    if (src.startsWith('/*', i)) {
      const f = src.indexOf('*/', i + 2);
      if (f < 0) throw new Error(`commentaire non fermé à ${i}`);
      i = f + 2;
      continue;
    }
    let j;
    if (src.startsWith('"""', i)) j = lireChaine(i + 3, '"""');
    else if (c === '"') j = lireChaine(i + 1, '"');
    else if (c === "'") j = lireChaine(i + 1, "'");
    else if (/[\p{L}_$]/u.test(c)) { j = i + 1; while (j < n && /[\p{L}\p{N}_$]/u.test(src[j])) j++; }
    else if (/[0-9]/.test(c)) { j = i + 1; while (j < n && /[\w.]/.test(src[j])) j++; }
    else j = i + 1;
    out.push(src.slice(i, j));
    i = j;
  }
  return out;
}

const estIdent = (t) => t !== undefined && /^[\p{L}_$][\p{L}\p{N}_$]*$/u.test(t);

/** Fin (exclue) du groupe ouvert à `debut` (jeton '('). */
function finGroupe(t, debut) {
  let prof = 0;
  for (let k = debut; k < t.length; k++) {
    if (t[k] === '(' || t[k] === '{' || t[k] === '[') prof++;
    else if (t[k] === ')' || t[k] === '}' || t[k] === ']') { prof--; if (prof === 0) return k + 1; }
  }
  throw new Error('parenthèse non fermée');
}

function decouperArguments(t) {
  const segs = [];
  let cour = [];
  let prof = 0;
  for (const x of t) {
    if (x === '(' || x === '{' || x === '[') prof++;
    else if (x === ')' || x === '}' || x === ']') prof--;
    if (x === ',' && prof === 0) { segs.push(cour); cour = []; } else cour.push(x);
  }
  if (cour.length) segs.push(cour);
  return segs;
}

/** Retire textes OpenAPI et imports ignorés d'une suite de jetons. */
function neutraliser(t) {
  const out = [];
  let i = 0;
  while (i < t.length) {
    if (t[i] === 'import') {
      const f = t.indexOf(';', i);
      const nom = t.slice(i + 1, f).join('');
      if (IMPORTS_IGNORES.has(nom)) { i = f + 1; continue; }
    }
    if (t[i] === '@' && estIdent(t[i + 1]) && t[i + 1] !== 'interface') {
      let j = i + 1;
      const nom = [t[j++]];
      while (t[j] === '.' && estIdent(t[j + 1])) { nom.push('.', t[j + 1]); j += 2; }
      if (ANNOTATIONS.has(nom[nom.length - 1])) {
        let garde = [];
        if (t[j] === '(') {
          const f = finGroupe(t, j);
          garde = decouperArguments(t.slice(j + 1, f - 1))
            .filter((s) => !(ARGUMENTS_TEXTE.has(s[0]) && s[1] === '='))
            .map(neutraliser);
          j = f;
        }
        if (garde.length) out.push('@', ...nom, '(', ...garde.flatMap((s, k) => (k ? [',', ...s] : s)), ')');
        i = j;
        continue;
      }
    }
    out.push(t[i]);
    i++;
  }
  return out;
}

// Fins de ligne normalisées : git peut rendre LF et l'arbre de travail CRLF (javac normalise aussi les text blocks).
const traiter = (src) => neutraliser(jetons(src.replace(/\r\n?/g, '\n')));

function premiereDivergence(a, b) {
  let k = 0;
  while (k < a.length && a[k] === b[k]) k++;
  const ctx = (t) => t.slice(Math.max(0, k - 6), k + 6).join(' ');
  return `jeton ${k} : « ${ctx(a)} » ≠ « ${ctx(b)} »`;
}

function main() {
  const ref = process.argv[2];
  if (!ref) {
    console.error('Usage : node scripts/compare-code.mjs <ref-git>');
    process.exit(2);
  }
  const lignes = process.argv.includes('--tous')
    ? git('ls-tree', '-r', '--name-only', ref, '--', 'src/main').split('\n').filter(Boolean).map((p) => ['M', p])
    : git('diff', '--no-renames', '--name-status', ref, '--', 'src/main')
      .split('\n').filter(Boolean).map((l) => l.split('\t'));
  for (const p of git('ls-files', '--others', '--exclude-standard', '--', 'src/main').split('\n').filter(Boolean)) {
    lignes.push(['A', p]);
  }
  const problemes = [];
  let compares = 0;
  for (const [statut, chemin] of lignes) {
    if (!chemin.endsWith('.java')) continue;
    if (statut === 'A') {
      if (!EXCLUS.some((e) => chemin.startsWith(e))) problemes.push(`${chemin} : fichier ajouté`);
      continue;
    }
    if (statut !== 'M') { problemes.push(`${chemin} : statut ${statut}`); continue; }
    compares++;
    const avant = traiter(git('show', `${ref}:${chemin}`));
    const apres = traiter(readFileSync(join(RACINE, chemin), 'utf8'));
    if (avant.length !== apres.length || avant.some((x, k) => x !== apres[k])) {
      problemes.push(`${chemin} : ${premiereDivergence(avant, apres)}`);
    }
  }
  if (problemes.length) {
    for (const p of problemes) console.log(p);
    console.log(`${problemes.length} fichier(s) en écart sur ${compares} comparé(s).`);
    process.exit(1);
  }
  console.log(`Aucun écart de code (${compares} fichier(s) comparé(s)).`);
}

main();
