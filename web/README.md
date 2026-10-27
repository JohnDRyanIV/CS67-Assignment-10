# Pokemon Explorer for your website

This is a standalone HTML/CSS/JavaScript version of Pokemon Explorer. It includes
all six search operations, the same 1,025-species dataset through Generation IX,
sortable and paginated results, type badges, a selected Pokemon's base stats,
light/dark themes, and layouts for phones, tablets, and desktop browsers.

## Upload to your website

1. Create a folder such as `pokemon` in your website's public directory.
2. Upload **all files inside `web/dist/`** into that folder. Alternatively,
   upload `web/pokemon-explorer.zip` and extract its contents there.
3. Visit `https://your-domain.example/pokemon/`.

The files belong together:

```text
pokemon/
  index.html
  styles.css
  app.js
  api.js
  pokemon-data.js
  favicon.svg
```

All asset paths are relative, so the page can live in a subfolder or at your
website's root. The upload ZIP contains these six files directly, without an
extra enclosing directory. It can also be imported into static website hosting.

No Java, Maven, Node.js, SQLite server, API keys, or build step is required on
your hosting account or on visitors' computers. All searches execute in the
browser against the bundled dataset. This web version implements equivalents
of the Java API methods; it does not run `DBAPI.java` or query `pokemon.db`.
The Java assignment and desktop application are separate runnable versions.

The page makes no external font, image, or API requests. It uses the default
forms from `data/pokemon.tsv`; alternate and regional forms are not separate
entries. Theme preferences are saved locally in the visitor's browser when
storage is available.

## Preview locally

Open `dist/index.html` in your browser. The dataset is supplied as a script, so
directly opening the HTML also works without a local server.

If you have Node.js installed, you can preview over HTTP from the repository
root:

```sh
node web/serve.mjs
```

Open the local address printed in the terminal. Press Ctrl+C to stop the server.
The server binds to your computer's loopback address and is for local preview.

## Refresh the data

After updating the original `data/pokemon.tsv`, run this from the repository
root with Node.js installed:

```sh
node web/update-data.mjs
node --test web/test-api.cjs
```

This refreshes `dist/pokemon-data.js`. Upload that file again after changing the
dataset. If you use the ZIP, recreate it to include your latest files. In
PowerShell:

```powershell
Compress-Archive -Path web/dist/* -DestinationPath web/pokemon-explorer.zip -Force
```

The tests verify that the upload contains the complete source dataset, all six
searches behave correctly, text matching ignores case, and stat thresholds use
a strict greater-than comparison.
