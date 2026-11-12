# Note App – Android Java with Retrofit REST API

An Android Note / Todo Application written in Java, designed to connect to a local REST API using Retrofit, Gson, and Material 3 components.

---

## Project Structure

```
app/
├── manifests/
│   └── AndroidManifest.xml              # Permissions, Cleartext config, Activities
├── java/com/example/todoapp/
│   │
│   ├── models/                          # Data structures and response objects
│   │   ├── ApiResponse.java             # Generic wrapper for server responses
│   │   ├── Category.java                # Category data model
│   │   └── Note.java                    # Note data model (id, title, content, date...)
│   │
│   ├── network/                         # HTTP communication and REST APIs
│   │   ├── ApiClient.java               # Retrofit singleton client instance
│   │   └── TodoApiService.java          # API endpoints & HTTP methods interface
│   │
│   ├── adapters/                        # RecyclerView list adapters & ViewHolders
│   │   ├── CategoryAdapter.java         # Category chips & management list
│   │   └── NoteAdapter.java             # Note list rendering with edit/delete actions
│   │
│   ├── ui/                              # Activities and user interface components
│   │   ├── MainActivity.java            # Main dashboard displaying notes/categories
│   │   ├── AddEditNoteActivity.java     # Screen to create or modify a note
│   │   └── CategoryActivity.java        # Screen to manage category items
│   │
│   └── utils/                           # Helper classes and utility constants
│       └── Constants.java               # Base URLs, formatters, and intent keys
│
└── res/
    ├── layout/
    │   ├── activity_main.xml            # Dashboard layout with RecyclerView
    │   ├── activity_add_edit_note.xml   # Form inputs for title, date, category
    │   ├── activity_category.xml        # Screen to view and manage categories
    │   ├── item_note.xml                # Single row/card layout for a note
    │   ├── item_category.xml            # Single chip layout for a category
    │   └── item_category_manage.xml     # Full category list row layout
    ├── values/
    │   ├── colors.xml                   # Theme colors & category badge palette
    │   ├── strings.xml                  # Static text resources
    │   └── themes.xml                   # Application theme
    └── drawable/                        # Vector icons (add, edit, delete, calendar, note)
```

---

## REST API Specification

### Base URL:
- **Android Emulator**: `http://10.0.2.2:8080/api/`
- **Local PC / Host**: `http://localhost:8080/api/`
- **Physical Device**: `http://<your-pc-ip>:8080/api/` (configurable inside the app menu!)

### Endpoints:
| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/notes` | List all notes |
| `GET` | `/notes/{id}` | Get note details by ID |
| `POST` | `/notes` | Create a new note |
| `PUT` | `/notes/{id}` | Update an existing note |
| `DELETE` | `/notes/{id}` | Delete a note |
| `GET` | `/categories` | List all categories |
| `POST` | `/categories` | Create a category |
| `PUT` | `/categories/{id}` | Update a category |
| `DELETE` | `/categories/{id}` | Delete a category |

### JSON Format:
```json
[
  {
    "id": 1,
    "title": "Learn Retrofit",
    "content": "Call API and parse JSON response into models",
    "date": "2026-10-02 10:30 AM",
    "categoryId": 2,
    "categoryName": "Study",
    "categoryColor": "#4CAF50"
  }
]
```

---

## Running the App

### 1. Start the Local API Server
A mock Python server is included in `server/server.py`:
```bash
python server/server.py
```

### 2. Build and Run the Android App
Open the project in Android Studio or compile with Gradle:
```bash
./gradlew assembleDebug
```
The APK is generated at:
`app/build/outputs/apk/debug/app-debug.apk`

---

## Key Features

1. **Retrofit REST Client**:
   - `ApiClient` singleton with `HttpLoggingInterceptor` to monitor HTTP traffic.
   - Dynamic Base URL configuration via `SharedPreferences`.
2. **Dashboard (`MainActivity`)**:
   - Search notes in real time by title or content.
   - Filter notes by category chips.
   - Pull to refresh (`SwipeRefreshLayout`).
   - One-tap Edit and Delete with confirmation dialogs.
   - Floating Action Button (+) to add notes.
   - API Server URL setting dialog.
3. **Add/Edit Screen (`AddEditNoteActivity`)**:
   - Date & Time Picker dialogs.
   - Category selector spinner.
   - Form validation & loading indicator.
4. **Category Manager (`CategoryActivity`)**:
   - Add, edit, and delete custom categories with color palettes.
