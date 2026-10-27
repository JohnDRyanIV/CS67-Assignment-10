import { createServer } from 'node:http';
import { readFile } from 'node:fs/promises';
import { extname, resolve, sep } from 'node:path';
import { fileURLToPath } from 'node:url';

const directory = fileURLToPath(new URL('./dist/', import.meta.url));
const types = { '.html': 'text/html', '.css': 'text/css', '.js': 'text/javascript', '.svg': 'image/svg+xml' };
export function createPreviewServer(base = '/') {
  return createServer(async (request, response) => {
    try {
      const pathname = decodeURIComponent(new URL(request.url, 'http://localhost').pathname);
      if (!pathname.startsWith(base)) {
        response.writeHead(404).end('Not found');
        return;
      }
      const relative = pathname.slice(base.length) || 'index.html';
      const target = resolve(directory, relative);
      if (!target.startsWith(directory) || !(target.startsWith(resolve(directory) + sep))) {
        response.writeHead(403).end('Forbidden');
        return;
      }
      const content = await readFile(target);
      response.writeHead(200, { 'Content-Type': `${types[extname(target)] || 'application/octet-stream'}; charset=utf-8` });
      response.end(content);
    } catch {
      response.writeHead(404).end('Not found');
    }
  });
}

if (process.argv[1] && resolve(process.argv[1]) === fileURLToPath(import.meta.url)) {
  const port = Number(process.env.PORT || 4173);
  const server = createPreviewServer();
  server.listen(port, '127.0.0.1', () => console.log(`Pokemon Explorer: http://127.0.0.1:${port}/`));
}
