(function () {
  'use strict';
  const $ = id => document.getElementById(id);
  const form = $('search-form');
  const modeInput = $('search-mode');
  const valueInput = $('value-input');
  const body = $('results-body');
  const pageSize = 12;
  const stats = [['hp', 'HP'], ['attack', 'Attack'], ['defense', 'Defense'],
    ['special_attack', 'Sp. Attack'], ['special_defense', 'Sp. Defense'], ['speed', 'Speed']];
  let api;
  let results = [];
  let page = 0;
  let selectedId = null;
  let sortField = 'id';
  let sortDirection = 1;

  function element(tag, className, text) {
    const node = document.createElement(tag);
    if (className) node.className = className;
    if (text !== undefined) node.textContent = text;
    return node;
  }

  function badges(pokemon) {
    const wrapper = element('div', 'type-badges');
    [pokemon.primary_type, pokemon.secondary_type].filter(Boolean).forEach(type => {
      wrapper.append(element('span', `type-badge type-${type.toLowerCase()}`, type));
    });
    return wrapper;
  }

  function setTheme(dark) {
    document.documentElement.dataset.theme = dark ? 'dark' : 'light';
    $('theme-toggle').setAttribute('aria-pressed', String(dark));
    $('theme-label').textContent = dark ? 'Light mode' : 'Dark mode';
  }

  let storedTheme;
  try { storedTheme = localStorage.getItem('pokemon-explorer-theme'); } catch { /* Storage is optional. */ }
  setTheme(storedTheme ? storedTheme === 'dark' : window.matchMedia('(prefers-color-scheme: dark)').matches);
  $('theme-toggle').addEventListener('click', () => {
    const dark = document.documentElement.dataset.theme !== 'dark';
    setTheme(dark);
    try { localStorage.setItem('pokemon-explorer-theme', dark ? 'dark' : 'light'); } catch { /* Keep the selected theme for this page. */ }
  });

  function clearError() {
    $('form-error').hidden = true;
    valueInput.removeAttribute('aria-invalid');
  }

  function configureForm() {
    clearError();
    const mode = modeInput.value;
    $('type-field').hidden = mode !== 'type';
    $('generation-field').hidden = mode !== 'generation';
    $('stat-field').hidden = mode !== 'stat';
    $('value-field').hidden = mode === 'type' || mode === 'generation';
    const configurations = {
      name: ['Full Pokemon name', 'Lucario', 'Use the full name. Uppercase or lowercase both work.'],
      id: ['Pokedex number', '25', 'Look up a species by its National Pokedex number.'],
      bst: ['Base stat total above', '500', 'BST is the sum of all six base stats.'],
      stat: ['Stat value above', '100', 'Find species with a stat greater than this value.']
    };
    if (configurations[mode]) {
      const [label, defaultValue, hint] = configurations[mode];
      $('value-label').textContent = label;
      valueInput.value = defaultValue;
      valueInput.placeholder = `e.g. ${defaultValue}`;
      valueInput.inputMode = mode === 'name' ? 'text' : 'numeric';
      $('query-hint').textContent = hint;
    } else {
      $('query-hint').textContent = mode === 'type'
        ? 'Matches either primary or secondary type.'
        : 'Explore species introduced in a generation.';
    }
  }

  function showDetails(pokemon) {
    $('detail-empty').hidden = Boolean(pokemon);
    $('detail-content').hidden = !pokemon;
    $('detail-panel').setAttribute('aria-labelledby', pokemon ? 'detail-name' : 'detail-heading');
    if (!pokemon) return;
    $('detail-number').textContent = `POKEDEX #${String(pokemon.id).padStart(4, '0')}`;
    $('specimen-number').textContent = String(pokemon.id).padStart(3, '0');
    $('detail-name').textContent = pokemon.name;
    $('detail-types').replaceChildren(...badges(pokemon).children);
    $('detail-generation').textContent = `Generation ${pokemon.generation}`;
    $('detail-total').textContent = pokemon.stat_total;
    const statRows = stats.map(([key, name]) => {
      const row = element('div', 'stat');
      const progress = element('progress');
      progress.max = 255;
      progress.value = pokemon[key];
      progress.setAttribute('aria-label', `${name}: ${pokemon[key]} out of 255`);
      row.append(element('dt', '', name), element('dd', '', pokemon[key]), progress);
      return row;
    });
    $('detail-stats').replaceChildren(...statRows);
  }

  function selectPokemon(id) {
    selectedId = id;
    body.querySelectorAll('tr').forEach(row => {
      const selected = Number(row.dataset.id) === id;
      row.classList.toggle('selected', selected);
      const button = row.querySelector('button');
      if (selected) button.setAttribute('aria-current', 'true');
      else button.removeAttribute('aria-current');
    });
    showDetails(results.find(pokemon => pokemon.id === id));
  }

  function renderPage() {
    const start = page * pageSize;
    const fragment = document.createDocumentFragment();
    results.slice(start, start + pageSize).forEach(pokemon => {
      const row = element('tr', pokemon.id === selectedId ? 'selected' : '');
      row.dataset.id = pokemon.id;
      const nameCell = element('td');
      const nameButton = element('button', 'pokemon-link', pokemon.name);
      nameButton.type = 'button';
      nameButton.setAttribute('aria-label', `View ${pokemon.name}, Pokedex number ${pokemon.id}`);
      if (pokemon.id === selectedId) nameButton.setAttribute('aria-current', 'true');
      nameCell.append(nameButton);
      const typeCell = element('td');
      typeCell.append(badges(pokemon));
      row.append(element('td', '', String(pokemon.id).padStart(3, '0')), nameCell, typeCell,
        element('td', '', pokemon.generation), element('td', '', pokemon.stat_total));
      fragment.append(row);
    });
    body.replaceChildren(fragment);
    $('table-container').hidden = results.length === 0;
    $('empty-state').hidden = results.length !== 0;
    $('result-count').textContent = `${results.length} ${results.length === 1 ? 'match' : 'matches'}`;
    const totalPages = Math.ceil(results.length / pageSize);
    $('page-summary').textContent = results.length
      ? `Showing ${start + 1}-${Math.min(start + pageSize, results.length)} of ${results.length}` : 'No matching Pokemon';
    $('page-number').textContent = `${results.length ? page + 1 : 0} / ${totalPages}`;
    $('previous-page').disabled = page === 0;
    $('next-page').disabled = page + 1 >= totalPages;
    document.querySelectorAll('th[data-column]').forEach(header => {
      if (header.dataset.column === sortField) header.setAttribute('aria-sort', sortDirection === 1 ? 'ascending' : 'descending');
      else header.removeAttribute('aria-sort');
    });
    showDetails(results.find(pokemon => pokemon.id === selectedId));
  }

  function sortResults() {
    results.sort((a, b) => {
      const first = a[sortField], second = b[sortField];
      const order = typeof first === 'number' ? first - second
        : first.localeCompare(second, undefined, { sensitivity: 'base' });
      return order * sortDirection || a.id - b.id;
    });
  }

  function search() {
    if (!api) return;
    clearError();
    const mode = modeInput.value;
    const text = valueInput.value.trim();
    let value;
    try {
      if (['id', 'bst', 'stat'].includes(mode)) {
        if (!/^\d+$/.test(text) || Number(text) > 2147483647 || (mode === 'id' && Number(text) === 0)) {
          throw new Error(mode === 'id' ? 'Enter a Pokedex number greater than zero.'
            : 'Enter a whole number from 0 to 2147483647.');
        }
        value = Number(text);
      }
      let summary;
      switch (mode) {
        case 'type':
          results = api.getAllPokemonByType($('type-input').value);
          summary = `${$('type-input').value} type - either primary or secondary`;
          break;
        case 'generation':
          value = Number($('generation-input').value);
          results = api.getAllPokemonFromGeneration(value);
          summary = `Species introduced in Generation ${value}`;
          break;
        case 'name':
          if (!text) throw new Error("Enter a Pokemon's full name.");
          results = api.getAllPokemonByName(text);
          summary = `Full name: ${text} (ignoring case)`;
          break;
        case 'id':
          results = api.getAllPokemonById(value);
          summary = `National Pokedex #${value}`;
          break;
        case 'bst':
          results = api.getAllPokemonAboveBST(value);
          summary = `Base stat total greater than ${value}`;
          break;
        case 'stat':
          results = api.getAllPokemonAboveStat($('stat-input').value, value);
          summary = `${$('stat-input').selectedOptions[0].textContent} greater than ${value}`;
          break;
        default:
          throw new Error('Choose a search option.');
      }
      sortResults();
      page = 0;
      selectedId = results[0]?.id ?? null;
      $('result-summary').textContent = summary;
      $('empty-title').textContent = 'No Pokemon found';
      $('empty-message').textContent = 'Try another name, type, or lower stat threshold.';
      renderPage();
      $('status').textContent = results.length ? `${results.length} Pokemon found. Select a Pokemon to explore its stats.`
        : 'No matches. Try a different search.';
    } catch (error) {
      $('form-error').textContent = error.message;
      $('form-error').hidden = false;
      valueInput.setAttribute('aria-invalid', 'true');
      valueInput.focus();
    }
  }

  modeInput.addEventListener('change', configureForm);
  form.addEventListener('submit', event => { event.preventDefault(); search(); });
  $('reset-button').addEventListener('click', () => {
    form.reset();
    sortField = 'id';
    sortDirection = 1;
    configureForm();
    search();
  });
  body.addEventListener('click', event => {
    const row = event.target.closest('tr[data-id]');
    if (row) {
      selectPokemon(Number(row.dataset.id));
      if (window.matchMedia('(max-width: 640px)').matches) {
        $('detail-panel').scrollIntoView({
          behavior: window.matchMedia('(prefers-reduced-motion: reduce)').matches ? 'auto' : 'smooth',
          block: 'start'
        });
      }
    }
  });
  $('results-table').querySelector('thead').addEventListener('click', event => {
    const button = event.target.closest('button[data-sort]');
    if (!button) return;
    sortDirection = sortField === button.dataset.sort ? -sortDirection : 1;
    sortField = button.dataset.sort;
    sortResults();
    page = 0;
    selectedId = results[0]?.id ?? null;
    renderPage();
  });
  function changePage(delta) {
    const nextPage = page + delta;
    if (nextPage < 0 || nextPage >= Math.ceil(results.length / pageSize)) return;
    page = nextPage;
    selectedId = results[page * pageSize].id;
    renderPage();
  }
  $('previous-page').addEventListener('click', () => changePage(-1));
  $('next-page').addEventListener('click', () => changePage(1));

  try {
    api = new window.PokemonAPI(window.POKEMON_TSV);
    $('species-total').textContent = api.pokemon.length.toLocaleString();
    configureForm();
    search();
  } catch (error) {
    $('search-button').disabled = true;
    $('reset-button').disabled = true;
    $('table-container').hidden = true;
    $('empty-state').hidden = false;
    $('result-count').textContent = 'Unavailable';
    $('empty-title').textContent = 'The field guide could not load';
    $('empty-message').textContent = 'Make sure pokemon-data.js, api.js, and app.js are uploaded beside index.html, then refresh.';
    $('status').textContent = error.message;
    $('result-summary').textContent = 'Please check the uploaded files.';
    $('previous-page').disabled = true;
    $('next-page').disabled = true;
  }
})();
