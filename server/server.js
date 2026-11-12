const http = require('http');
const url = require('url');

let notes = [
  {
    id: 1,
    title: "Learn Retrofit",
    content: "Call API and parse JSON response into models",
    date: "2026-10-02 10:30 AM",
    categoryId: 2,
    categoryName: "Study",
    categoryColor: "#4CAF50"
  },
  {
    id: 2,
    title: "Study Android",
    content: "Build App with RecyclerView and Retrofit REST client",
    date: "2026-09-30 09:15 AM",
    categoryId: 2,
    categoryName: "Study",
    categoryColor: "#4CAF50"
  },
  {
    id: 3,
    title: "Build Great App",
    content: "Connect Android App to Local API Server",
    date: "2026-09-28 04:20 PM",
    categoryId: 1,
    categoryName: "Work",
    categoryColor: "#FF9800"
  }
];

let categories = [
  { id: 1, name: "Work", color: "#FF9800", noteCount: 1 },
  { id: 2, name: "Study", color: "#4CAF50", noteCount: 2 },
  { id: 3, name: "Personal", color: "#2196F3", noteCount: 0 }
];

let nextNoteId = 4;
let nextCatId = 4;

function sendJson(res, statusCode, data) {
  const payload = JSON.stringify(data);
  res.writeHead(statusCode, {
    'Content-Type': 'application/json',
    'Access-Control-Allow-Origin': '*',
    'Access-Control-Allow-Methods': 'GET, POST, PUT, DELETE, OPTIONS',
    'Access-Control-Allow-Headers': 'Content-Type, Authorization'
  });
  res.end(payload);
}

function parseBody(req) {
  return new Promise((resolve, reject) => {
    let body = '';
    req.on('data', chunk => { body += chunk.toString(); });
    req.on('end', () => {
      if (!body) return resolve({});
      try {
        resolve(JSON.parse(body));
      } catch (err) {
        resolve({});
      }
    });
    req.on('error', reject);
  });
}

const server = http.createServer(async (req, res) => {
  const parsedUrl = url.parse(req.url, true);
  let pathname = parsedUrl.pathname.replace(/\/+$/, '');
  if (!pathname.startsWith('/')) pathname = '/' + pathname;

  console.log(`[${new Date().toLocaleTimeString()}] ${req.method} ${pathname}`);

  // Handle CORS Preflight
  if (req.method === 'OPTIONS') {
    res.writeHead(200, {
      'Access-Control-Allow-Origin': '*',
      'Access-Control-Allow-Methods': 'GET, POST, PUT, DELETE, OPTIONS',
      'Access-Control-Allow-Headers': 'Content-Type, Authorization'
    });
    return res.end();
  }

  // Normalize path to support both /api/notes and /notes
  const isNotes = pathname === '/api/notes' || pathname === '/notes';
  const isCategories = pathname === '/api/categories' || pathname === '/categories';
  const noteIdMatch = pathname.match(/^(?:\/api)?\/notes\/(\d+)$/);
  const catIdMatch = pathname.match(/^(?:\/api)?\/categories\/(\d+)$/);

  try {
    // GET /notes
    if (req.method === 'GET' && isNotes) {
      const catId = parsedUrl.query.categoryId;
      if (catId) {
        const filtered = notes.filter(n => n.categoryId == catId);
        return sendJson(res, 200, filtered);
      }
      return sendJson(res, 200, notes);
    }

    // GET /notes/:id
    if (req.method === 'GET' && noteIdMatch) {
      const id = parseInt(noteIdMatch[1], 10);
      const note = notes.find(n => n.id === id);
      if (note) return sendJson(res, 200, note);
      return sendJson(res, 404, { error: 'Note not found' });
    }

    // POST /notes
    if (req.method === 'POST' && isNotes) {
      const body = await parseBody(req);
      const newNote = {
        id: nextNoteId++,
        title: body.title || 'Untitled',
        content: body.content || '',
        date: body.date || new Date().toISOString().slice(0, 10),
        categoryId: body.categoryId || null,
        categoryName: body.categoryName || '',
        categoryColor: body.categoryColor || '#FF9800'
      };
      notes.unshift(newNote);
      console.log('-> Created note:', newNote);
      return sendJson(res, 201, newNote);
    }

    // PUT /notes/:id
    if (req.method === 'PUT' && noteIdMatch) {
      const id = parseInt(noteIdMatch[1], 10);
      const body = await parseBody(req);
      const index = notes.findIndex(n => n.id === id);
      if (index !== -1) {
        notes[index] = { ...notes[index], ...body, id };
        console.log('-> Updated note:', notes[index]);
        return sendJson(res, 200, notes[index]);
      }
      return sendJson(res, 404, { error: 'Note not found' });
    }

    // DELETE /notes/:id
    if (req.method === 'DELETE' && noteIdMatch) {
      const id = parseInt(noteIdMatch[1], 10);
      const index = notes.findIndex(n => n.id === id);
      if (index !== -1) {
        const removed = notes.splice(index, 1);
        console.log('-> Deleted note:', removed[0]);
        return sendJson(res, 200, { success: true });
      }
      return sendJson(res, 404, { error: 'Note not found' });
    }

    // GET /categories
    if (req.method === 'GET' && isCategories) {
      return sendJson(res, 200, categories);
    }

    // POST /categories
    if (req.method === 'POST' && isCategories) {
      const body = await parseBody(req);
      const newCat = {
        id: nextCatId++,
        name: body.name || 'New Category',
        color: body.color || '#2196F3',
        noteCount: 0
      };
      categories.push(newCat);
      return sendJson(res, 201, newCat);
    }

    // PUT /categories/:id
    if (req.method === 'PUT' && catIdMatch) {
      const id = parseInt(catIdMatch[1], 10);
      const body = await parseBody(req);
      const index = categories.findIndex(c => c.id === id);
      if (index !== -1) {
        categories[index] = { ...categories[index], ...body, id };
        return sendJson(res, 200, categories[index]);
      }
      return sendJson(res, 404, { error: 'Category not found' });
    }

    // DELETE /categories/:id
    if (req.method === 'DELETE' && catIdMatch) {
      const id = parseInt(catIdMatch[1], 10);
      const index = categories.findIndex(c => c.id === id);
      if (index !== -1) {
        categories.splice(index, 1);
        return sendJson(res, 200, { success: true });
      }
      return sendJson(res, 404, { error: 'Category not found' });
    }

    // 404 fallback
    return sendJson(res, 404, { error: 'Endpoint not found: ' + pathname });

  } catch (err) {
    console.error('Server error:', err);
    return sendJson(res, 500, { error: 'Internal Server Error' });
  }
});

const PORT = 8080;
server.listen(PORT, '0.0.0.0', () => {
  console.log('='.repeat(55));
  console.log(` Note App REST API Server is RUNNING on port ${PORT}`);
  console.log(` Android Emulator URL: http://10.0.2.2:${PORT}/api/notes`);
  console.log(` Host PC Browser URL: http://localhost:${PORT}/api/notes`);
  console.log('='.repeat(55));
});
