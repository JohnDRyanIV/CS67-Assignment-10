import { readFile, writeFile } from 'node:fs/promises';

const source = new URL('../data/pokemon.tsv', import.meta.url);
const destination = new URL('./dist/pokemon-data.js', import.meta.url);
const tsv = (await readFile(source, 'utf8')).replace(/^\uFEFF/, '');
await writeFile(destination,
  '// Generated from data/pokemon.tsv by web/update-data.mjs.\n'
  + `window.POKEMON_TSV = ${JSON.stringify(tsv)};\n`, 'utf8');
console.log(`Updated web dataset: ${tsv.trim().split(/\r?\n/).length - 1} Pokemon.`);
