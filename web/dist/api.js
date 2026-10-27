/* The browser equivalent of the six searches in DBAPI.java. */
(function (root) {
  'use strict';
  const columns = ['pokedex_number', 'name', 'generation', 'primary_type', 'secondary_type',
    'hp', 'attack', 'defense', 'special_attack', 'special_defense', 'speed', 'stat_total'];
  const stats = ['hp', 'attack', 'defense', 'special_attack', 'special_defense', 'speed'];
  const numericColumns = ['pokedex_number', 'generation', ...stats, 'stat_total'];
  const normalize = value => typeof value === 'string' ? value.trim().toLowerCase() : '';

  class PokemonAPI {
    constructor(tsv) {
      if (typeof tsv !== 'string') throw new Error('The Pokemon dataset could not be loaded.');
      const lines = tsv.trim().split(/\r?\n/);
      if (lines.shift() !== columns.join('\t')) throw new Error('The Pokemon dataset has an unexpected format.');
      this.pokemon = lines.map((line, index) => {
        const fields = line.split('\t');
        if (fields.length !== columns.length) throw new Error(`Invalid Pokemon data on row ${index + 2}.`);
        const pokemon = Object.fromEntries(columns.map((column, i) => [column, fields[i]]));
        numericColumns.forEach(column => {
          if (!/^\d+$/.test(pokemon[column])) throw new Error(`Invalid ${column} on row ${index + 2}.`);
          pokemon[column] = Number(pokemon[column]);
        });
        pokemon.id = pokemon.pokedex_number;
        pokemon.types = [pokemon.primary_type, pokemon.secondary_type].filter(Boolean).join(' / ');
        return Object.freeze(pokemon);
      });
      if (this.pokemon.length === 0) throw new Error('The Pokemon dataset is empty.');
      Object.freeze(this.pokemon);
    }

    getAllPokemonByType(type) {
      const target = normalize(type);
      return target ? this.pokemon.filter(pokemon =>
        normalize(pokemon.primary_type) === target || normalize(pokemon.secondary_type) === target) : [];
    }

    getAllPokemonFromGeneration(generation) {
      return this.pokemon.filter(pokemon => pokemon.generation === generation);
    }

    getAllPokemonByName(name) {
      const target = normalize(name);
      return target ? this.pokemon.filter(pokemon => normalize(pokemon.name) === target) : [];
    }

    getAllPokemonById(id) {
      return this.pokemon.filter(pokemon => pokemon.id === id);
    }

    getAllPokemonAboveBST(bst) {
      return this.pokemon.filter(pokemon => pokemon.stat_total > bst);
    }

    getAllPokemonAboveStat(stat, value) {
      const column = normalize(stat).replace(/ /g, '_');
      if (!stats.includes(column)) throw new Error('Choose one of the six base stats.');
      return this.pokemon.filter(pokemon => pokemon[column] > value);
    }
  }

  if (typeof module !== 'undefined' && module.exports) module.exports = { PokemonAPI };
  else root.PokemonAPI = PokemonAPI;
})(typeof globalThis !== 'undefined' ? globalThis : this);
