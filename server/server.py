"""
Local REST API Server for Note App (matches architecture diagram)
Port: 8080
Base URL: http://localhost:8080/api/
From Android Emulator: http://10.0.2.2:8080/api/

Run with:
    python server/server.py
"""

from http.server import HTTPServer, BaseHTTPRequestHandler
import json
import re

notes = [
    {
        "id": 1,
        "title": "Learn Retrofit",
        "content": "Call API and parse JSON response into models",
        "date": "2026-10-02 10:30 AM",
        "categoryId": 2,
        "categoryName": "Study",
        "categoryColor": "#4CAF50"
    },
    {
        "id": 2,
        "title": "Study Android",
        "content": "Build App with RecyclerView and Retrofit REST client",
        "date": "2026-09-30 09:15 AM",
        "categoryId": 2,
        "categoryName": "Study",
        "categoryColor": "#4CAF50"
    },
    {
        "id": 3,
        "title": "Build Great App",
        "content": "Connect Android App to Local API Server",
        "date": "2026-09-28 04:20 PM",
        "categoryId": 1,
        "categoryName": "Work",
        "categoryColor": "#FF9800"
    }
]

categories = [
    {"id": 1, "name": "Work", "color": "#FF9800", "noteCount": 1},
    {"id": 2, "name": "Study", "color": "#4CAF50", "noteCount": 2},
    {"id": 3, "name": "Personal", "color": "#2196F3", "noteCount": 0}
]

next_note_id = 4
next_cat_id = 4


class NoteApiHandler(BaseHTTPRequestHandler):

    def _set_headers(self, status=200):
        self.send_response(status)
        self.send_header('Content-Type', 'application/json')
        self.send_header('Access-Control-Allow-Origin', '*')
        self.send_header('Access-Control-Allow-Methods', 'GET, POST, PUT, DELETE, OPTIONS')
        self.send_header('Access-Control-Allow-Headers', 'Content-Type')
        self.end_headers()

    def do_OPTIONS(self):
        self._set_headers(200)

    def do_GET(self):
        parsed = self.path.split('?')
        path = parsed[0].rstrip('/')
        if not path:
            path = '/'

        query_params = {}
        if len(parsed) > 1:
            for pair in parsed[1].split('&'):
                if '=' in pair:
                    k, v = pair.split('=', 1)
                    query_params[k] = v

        if path in ("/", "/api"):
            self._set_headers(200)
            self.wfile.write(json.dumps({
                "status": "online",
                "message": "Note App REST API Server is running!",
                "endpoints": [
                    "GET /api/notes",
                    "GET /api/notes/{id}",
                    "POST /api/notes",
                    "PUT /api/notes/{id}",
                    "DELETE /api/notes/{id}",
                    "GET /api/categories",
                    "POST /api/categories",
                    "PUT /api/categories/{id}",
                    "DELETE /api/categories/{id}"
                ]
            }).encode('utf-8'))
            return

        if path in ("/api/notes", "/notes"):
            cat_id = query_params.get("categoryId")
            self._set_headers(200)
            if cat_id:
                filtered = [n for n in notes if str(n.get("categoryId")) == str(cat_id)]
                self.wfile.write(json.dumps(filtered).encode('utf-8'))
            else:
                self.wfile.write(json.dumps(notes).encode('utf-8'))
            return

        note_match = re.match(r"^(?:/api)?/notes/(\d+)$", path)
        if note_match:
            note_id = int(note_match.group(1))
            note = next((n for n in notes if n["id"] == note_id), None)
            if note:
                self._set_headers(200)
                self.wfile.write(json.dumps(note).encode('utf-8'))
            else:
                self._set_headers(404)
                self.wfile.write(json.dumps({"error": "Note not found"}).encode('utf-8'))
            return

        if path in ("/api/categories", "/categories"):
            self._set_headers(200)
            self.wfile.write(json.dumps(categories).encode('utf-8'))
            return

        cat_match = re.match(r"^(?:/api)?/categories/(\d+)$", path)
        if cat_match:
            cat_id = int(cat_match.group(1))
            cat = next((c for c in categories if c["id"] == cat_id), None)
            if cat:
                self._set_headers(200)
                self.wfile.write(json.dumps(cat).encode('utf-8'))
            else:
                self._set_headers(404)
                self.wfile.write(json.dumps({"error": "Category not found"}).encode('utf-8'))
            return

        self._set_headers(404)
        self.wfile.write(json.dumps({"error": "Endpoint not found: " + path}).encode('utf-8'))

    def do_POST(self):
        global next_note_id, next_cat_id
        path = self.path.split('?')[0].rstrip('/')
        content_length = int(self.headers.get('Content-Length', 0))
        body = self.rfile.read(content_length).decode('utf-8')
        data = json.loads(body) if body else {}

        if path in ("/api/notes", "/notes"):
            data["id"] = next_note_id
            next_note_id += 1
            notes.append(data)
            self._set_headers(201)
            self.wfile.write(json.dumps(data).encode('utf-8'))
            return

        if path in ("/api/categories", "/categories"):
            data["id"] = next_cat_id
            next_cat_id += 1
            data["noteCount"] = 0
            categories.append(data)
            self._set_headers(201)
            self.wfile.write(json.dumps(data).encode('utf-8'))
            return

        self._set_headers(404)
        self.wfile.write(json.dumps({"error": "Endpoint not found: " + path}).encode('utf-8'))

    def do_PUT(self):
        path = self.path.split('?')[0].rstrip('/')
        content_length = int(self.headers.get('Content-Length', 0))
        body = self.rfile.read(content_length).decode('utf-8')
        data = json.loads(body) if body else {}

        note_match = re.match(r"^(?:/api)?/notes/(\d+)$", path)
        if note_match:
            note_id = int(note_match.group(1))
            for i, note in enumerate(notes):
                if note["id"] == note_id:
                    data["id"] = note_id
                    notes[i] = data
                    self._set_headers(200)
                    self.wfile.write(json.dumps(data).encode('utf-8'))
                    return
            self._set_headers(404)
            self.wfile.write(json.dumps({"error": "Note not found"}).encode('utf-8'))
            return

        cat_match = re.match(r"^(?:/api)?/categories/(\d+)$", path)
        if cat_match:
            cat_id = int(cat_match.group(1))
            for i, cat in enumerate(categories):
                if cat["id"] == cat_id:
                    data["id"] = cat_id
                    categories[i] = data
                    self._set_headers(200)
                    self.wfile.write(json.dumps(data).encode('utf-8'))
                    return
            self._set_headers(404)
            self.wfile.write(json.dumps({"error": "Category not found"}).encode('utf-8'))
            return

        self._set_headers(404)
        self.wfile.write(json.dumps({"error": "Endpoint not found: " + path}).encode('utf-8'))

    def do_DELETE(self):
        path = self.path.split('?')[0].rstrip('/')

        note_match = re.match(r"^(?:/api)?/notes/(\d+)$", path)
        if note_match:
            note_id = int(note_match.group(1))
            for i, note in enumerate(notes):
                if note["id"] == note_id:
                    notes.pop(i)
                    self._set_headers(200)
                    self.wfile.write(json.dumps({"success": True}).encode('utf-8'))
                    return
            self._set_headers(404)
            self.wfile.write(json.dumps({"error": "Note not found"}).encode('utf-8'))
            return

        cat_match = re.match(r"^(?:/api)?/categories/(\d+)$", path)
        if cat_match:
            cat_id = int(cat_match.group(1))
            for i, cat in enumerate(categories):
                if cat["id"] == cat_id:
                    categories.pop(i)
                    self._set_headers(200)
                    self.wfile.write(json.dumps({"success": True}).encode('utf-8'))
                    return
            self._set_headers(404)
            self.wfile.write(json.dumps({"error": "Category not found"}).encode('utf-8'))
            return

        self._set_headers(404)
        self.wfile.write(json.dumps({"error": "Endpoint not found: " + path}).encode('utf-8'))


def run():
    server_address = ('0.0.0.0', 8080)
    httpd = HTTPServer(server_address, NoteApiHandler)
    print("=" * 60)
    print("  Note App Local REST API Server running on port 8080")
    print("  Local PC URL:        http://localhost:8080/api/notes")
    print("  Android Emulator:    http://10.0.2.2:8080/api/notes")
    print("=" * 60)
    httpd.serve_forever()


if __name__ == '__main__':
    run()
