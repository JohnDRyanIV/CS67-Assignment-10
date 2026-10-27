const { test } = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const { PokemonAPI } = require('./dist/api.js');
const source = fs.readFileSync(path.join(__dirname, '../data/pokemon.tsv'), 'utf8');
const context = { window: {} };
vm.runInNewContext(fs.readFileSync(path.join(__dirname, 'dist/pokemon-data.js'), 'utf8'), context);
const api = new PokemonAPI(context.window.POKEMON_TSV);

test('the upload includes an unchanged copy of all 1,025 species', () => {
  assert.equal(context.window.POKEMON_TSV, source.replace(/^\uFEFF/, ''));
  assert.equal(api.pokemon.length, 1025);
  assert.equal(api.pokemon[0].name, 'Bulbasaur');
  assert.equal(api.pokemon.at(-1).id, 1025);
});
test('type matching ignores case and searches both type columns', () => {
  const fire = api.getAllPokemonByType('fIrE');
  assert.equal(fire.length, 81);
  assert.ok(fire.some(pokemon => pokemon.primary_type !== 'Fire' && pokemon.secondary_type === 'Fire'));
  assert.deepEqual(api.getAllPokemonByType(null), []);
});
test('generation, full name, and ID match the Java API examples', () => {
  assert.equal(api.getAllPokemonFromGeneration(1).length, 151);
  assert.equal(api.getAllPokemonByName('lUcArIo')[0].id, 448);
  assert.equal(api.getAllPokemonById(25)[0].name, 'Pikachu');
  assert.deepEqual(api.getAllPokemonByName('Luc'), []);
  assert.deepEqual(api.getAllPokemonById(0), []);
});
test('BST thresholds are exclusive', () => {
  assert.deepEqual(api.getAllPokemonAboveBST(700).map(pokemon => pokemon.name), ['Arceus']);
  assert.deepEqual(api.getAllPokemonAboveBST(720), []);
});
test('all six stats support exclusive thresholds and normalized names', () => {
  assert.deepEqual(api.getAllPokemonAboveStat('hP', 200).map(pokemon => pokemon.name),
    ['Chansey', 'Blissey', 'Guzzlord']);
  for (const stat of ['hp', 'attack', 'defense', 'special_attack', 'special_defense', 'speed']) {
    const displayName = stat.toUpperCase().replaceAll('_', ' ');
    assert.deepEqual(api.getAllPokemonAboveStat(displayName, 100), api.pokemon.filter(pokemon => pokemon[stat] > 100));
  }
  assert.deepEqual(api.getAllPokemonAboveStat('HP', 255), []);
  assert.throws(() => api.getAllPokemonAboveStat('name', 100));
  assert.throws(() => api.getAllPokemonAboveStat(null, 100));
});
test('missing and malformed datasets are reported', () => {
  assert.throws(() => new PokemonAPI(undefined));
  assert.throws(() => new PokemonAPI('unexpected\theader'));
});
