package com.example.todoapp.ui;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.example.todoapp.R;
import com.example.todoapp.adapters.CategoryAdapter;
import com.example.todoapp.adapters.NoteAdapter;
import com.example.todoapp.models.Category;
import com.example.todoapp.models.Note;
import com.example.todoapp.network.ApiClient;
import com.example.todoapp.network.TodoApiService;
import com.example.todoapp.utils.Constants;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import java.util.ArrayList;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity implements NoteAdapter.OnNoteListener {

    private DrawerLayout drawerLayout;
    private MaterialToolbar toolbar;
    private EditText etSearch;
    private RecyclerView rvCategories;
    private RecyclerView rvNotes;
    private SwipeRefreshLayout swipeRefresh;
    private ProgressBar pbLoading;
    private LinearLayout llEmptyState;
    private LinearLayout llErrorState;
    private TextView tvErrorMessage;
    private Button btnRetry;
    private Button btnDemoData;
    private Button btnErrorSettings;
    private FloatingActionButton fabAddNote;
    private TextView tvManageCategories;
    private LinearLayout btnNavServerSettings;

    private NoteAdapter noteAdapter;
    private CategoryAdapter categoryAdapter;

    private final List<Note> allNotes = new ArrayList<>();
    private final List<Category> allCategories = new ArrayList<>();
    private Integer selectedCategoryId = -1; // -1 represents "All Notes"

    private TodoApiService apiService;

    // ActivityResultLauncher for Add/Edit activity
    private final ActivityResultLauncher<Intent> addEditNoteLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    loadNotes();
                }
            }
    );

    // ActivityResultLauncher for Categories activity
    private final ActivityResultLauncher<Intent> categoryActivityLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                loadCategories();
                loadNotes();
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
        setupToolbar();
        setupRecyclerViews();
        setupListeners();

        apiService = ApiClient.getApiService(this);

        loadCategories();
        loadNotes();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh base service in case URL changed
        apiService = ApiClient.getApiService(this);
    }

    private void initViews() {
        drawerLayout = findViewById(R.id.drawerLayout);
        toolbar = findViewById(R.id.toolbar);
        etSearch = findViewById(R.id.etSearch);
        rvCategories = findViewById(R.id.rvCategories);
        rvNotes = findViewById(R.id.rvNotes);
        swipeRefresh = findViewById(R.id.swipeRefresh);
        pbLoading = findViewById(R.id.pbLoading);
        llEmptyState = findViewById(R.id.llEmptyState);
        llErrorState = findViewById(R.id.llErrorState);
        tvErrorMessage = findViewById(R.id.tvErrorMessage);
        btnRetry = findViewById(R.id.btnRetry);
        btnDemoData = findViewById(R.id.btnDemoData);
        btnErrorSettings = findViewById(R.id.btnErrorSettings);
        fabAddNote = findViewById(R.id.fabAddNote);
        tvManageCategories = findViewById(R.id.tvManageCategories);
        btnNavServerSettings = findViewById(R.id.btnNavServerSettings);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> {
            if (drawerLayout != null) {
                if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START);
                } else {
                    drawerLayout.openDrawer(GravityCompat.START);
                }
            }
        });
    }

    private void setupRecyclerViews() {
        // Setup Notes RecyclerView
        rvNotes.setLayoutManager(new LinearLayoutManager(this));
        noteAdapter = new NoteAdapter(this, this);
        rvNotes.setAdapter(noteAdapter);

        // Setup Categories Vertical List in Navigation Drawer
        rvCategories.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        categoryAdapter = new CategoryAdapter(this, CategoryAdapter.MODE_CHIPS, new CategoryAdapter.OnCategoryClickListener() {
            @Override
            public void onCategoryClick(Category category, int position) {
                if (category.getId() != null && category.getId() == -999) {
                    showAddCategoryDialog();
                } else {
                    selectedCategoryId = category.getId();
                    filterNotesByCategory(category);
                    if (drawerLayout != null) {
                        drawerLayout.closeDrawer(GravityCompat.START);
                    }
                }
            }

            @Override
            public void onCategoryEdit(Category category) {
                showEditCategoryDialog(category);
            }

            @Override
            public void onCategoryDelete(Category category) {
                confirmDeleteCategoryFromMain(category);
            }

            @Override
            public void onCategoryLongClick(Category category) {
                showCategoryOptionsDialog(category);
            }
        });
        rvCategories.setAdapter(categoryAdapter);
    }

    private void setupListeners() {
        // Swipe to refresh
        swipeRefresh.setColorSchemeResources(R.color.primary, R.color.accent);
        swipeRefresh.setOnRefreshListener(() -> {
            loadCategories();
            loadNotes();
        });

        // Search live filter
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                noteAdapter.filter(s.toString());
                updateEmptyState();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // FAB to Add Note
        fabAddNote.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, AddEditNoteActivity.class);
            intent.putExtra(Constants.EXTRA_IS_EDIT, false);
            addEditNoteLauncher.launch(intent);
        });

        // Retry button
        btnRetry.setOnClickListener(v -> {
            llErrorState.setVisibility(View.GONE);
            loadNotes();
        });

        // Demo Data button (loads sample notes immediately when server is not running)
        btnDemoData.setOnClickListener(v -> loadDemoData());

        // Settings button in error banner
        btnErrorSettings.setOnClickListener(v -> showServerSettingsDialog());

        // Drawer Sidebar Listeners
        if (tvManageCategories != null) {
            tvManageCategories.setOnClickListener(v -> {
                if (drawerLayout != null) {
                    drawerLayout.closeDrawer(GravityCompat.START);
                }
                Intent intent = new Intent(MainActivity.this, CategoryActivity.class);
                categoryActivityLauncher.launch(intent);
            });
        }

        if (btnNavServerSettings != null) {
            btnNavServerSettings.setOnClickListener(v -> {
                if (drawerLayout != null) {
                    drawerLayout.closeDrawer(GravityCompat.START);
                }
                showServerSettingsDialog();
            });
        }

        // Handle back press to close side bar drawer first if open
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (drawerLayout != null && drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START);
                } else {
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                }
            }
        });
    }

    public void loadDemoData() {
        allNotes.clear();
        allNotes.add(new Note(1, "Learn Retrofit", "Call API and parse JSON response into models", "2026-10-02 10:30 AM", 2, "Study", "#4CAF50"));
        allNotes.add(new Note(2, "Study Android", "Build App with RecyclerView and Retrofit REST client", "2026-09-30 09:15 AM", 2, "Study", "#4CAF50"));
        allNotes.add(new Note(3, "Build Great App", "Connect Android App to Local API Server", "2026-09-28 04:20 PM", 1, "Work", "#FF9800"));

        if (allCategories.isEmpty() || allCategories.size() <= 1) {
            allCategories.clear();
            allCategories.add(new Category(-1, getString(R.string.all_categories), "#5E35B1"));
            allCategories.add(new Category(1, "Work", "#FF9800"));
            allCategories.add(new Category(2, "Study", "#4CAF50"));
            allCategories.add(new Category(3, "Personal", "#2196F3"));
            categoryAdapter.setCategories(allCategories);
        }

        filterNotesByCategory(selectedCategoryId);
        updateCategoryCounts();
        llErrorState.setVisibility(View.GONE);
        Toast.makeText(this, "Loaded demo notes from diagram!", Toast.LENGTH_SHORT).show();
    }

    // ==========================================
    // API CALLS (RETROFIT)
    // ==========================================

    /**
     * Fetch Notes (GET note/read.php, fallback POST / REST)
     */
    public void loadNotes() {
        if (!swipeRefresh.isRefreshing()) {
            pbLoading.setVisibility(View.VISIBLE);
        }
        llErrorState.setVisibility(View.GONE);

        apiService.getNotes().enqueue(new Callback<List<Note>>() {
            @Override
            public void onResponse(@NonNull Call<List<Note>> call, @NonNull Response<List<Note>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    onNotesLoaded(response.body());
                } else {
                    loadNotesPostFallback();
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<Note>> call, @NonNull Throwable t) {
                loadNotesPostFallback();
            }
        });
    }

    private void loadNotesPostFallback() {
        apiService.getNotesPost().enqueue(new Callback<List<Note>>() {
            @Override
            public void onResponse(@NonNull Call<List<Note>> call, @NonNull Response<List<Note>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    onNotesLoaded(response.body());
                } else {
                    loadNotesRestFallback();
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<Note>> call, @NonNull Throwable t) {
                loadNotesRestFallback();
            }
        });
    }

    private void loadNotesRestFallback() {
        apiService.getNotesRest().enqueue(new Callback<List<Note>>() {
            @Override
            public void onResponse(@NonNull Call<List<Note>> call, @NonNull Response<List<Note>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    onNotesLoaded(response.body());
                } else {
                    onNotesLoadFailed("Failed to fetch notes: " + response.code() + " " + response.message());
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<Note>> call, @NonNull Throwable t) {
                String currentUrl = Constants.getBaseUrl(MainActivity.this);
                onNotesLoadFailed("Unable to connect to " + currentUrl + "\n(" + t.getLocalizedMessage() + ")");
            }
        });
    }

    private void onNotesLoaded(List<Note> notes) {
        pbLoading.setVisibility(View.GONE);
        swipeRefresh.setRefreshing(false);
        allNotes.clear();
        if (notes != null) {
            allNotes.addAll(notes);
        }
        noteAdapter.setNotes(allNotes);
        filterNotesByCategory(selectedCategoryId);
        updateCategoryCounts();
    }

    private void onNotesLoadFailed(String errorMsg) {
        pbLoading.setVisibility(View.GONE);
        swipeRefresh.setRefreshing(false);
        showError(errorMsg);
        if (allNotes.isEmpty()) {
            loadDemoData();
            llErrorState.setVisibility(View.VISIBLE);
        }
    }

    /**
     * View Categories (GET categories/read.php, fallback POST / REST)
     */
    private void loadCategories() {
        apiService.getCategories().enqueue(new Callback<List<Category>>() {
            @Override
            public void onResponse(@NonNull Call<List<Category>> call, @NonNull Response<List<Category>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    onCategoriesLoaded(response.body());
                } else {
                    loadCategoriesPostFallback();
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<Category>> call, @NonNull Throwable t) {
                loadCategoriesPostFallback();
            }
        });
    }

    private void loadCategoriesPostFallback() {
        apiService.getCategoriesPost().enqueue(new Callback<List<Category>>() {
            @Override
            public void onResponse(@NonNull Call<List<Category>> call, @NonNull Response<List<Category>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    onCategoriesLoaded(response.body());
                } else {
                    loadCategoriesRestFallback();
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<Category>> call, @NonNull Throwable t) {
                loadCategoriesRestFallback();
            }
        });
    }

    private void loadCategoriesRestFallback() {
        apiService.getCategoriesRest().enqueue(new Callback<List<Category>>() {
            @Override
            public void onResponse(@NonNull Call<List<Category>> call, @NonNull Response<List<Category>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    onCategoriesLoaded(response.body());
                } else {
                    onCategoriesLoadDefault();
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<Category>> call, @NonNull Throwable t) {
                onCategoriesLoadDefault();
            }
        });
    }

    private void onCategoriesLoaded(List<Category> categories) {
        allCategories.clear();
        allCategories.add(new Category(-1, getString(R.string.all_categories), "#5E35B1"));
        allCategories.addAll(categories);
        allCategories.add(new Category(-999, "+ Add", "#9C27B0"));
        categoryAdapter.setCategories(allCategories);
        updateCategoryCounts();
    }

    private void onCategoriesLoadDefault() {
        if (allCategories.isEmpty()) {
            allCategories.add(new Category(-1, getString(R.string.all_categories), "#5E35B1"));
            allCategories.add(new Category(1, "Work", "#FF9800"));
            allCategories.add(new Category(2, "Study", "#4CAF50"));
            allCategories.add(new Category(3, "Personal", "#2196F3"));
            allCategories.add(new Category(-999, "+ Add", "#9C27B0"));
            categoryAdapter.setCategories(allCategories);
        }
    }

    private void showCategoryOptionsDialog(Category category) {
        String[] options = {"Edit Category", "Delete Category"};
        new AlertDialog.Builder(this)
                .setTitle("Category: " + category.getName())
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        showEditCategoryDialog(category);
                    } else if (which == 1) {
                        confirmDeleteCategoryFromMain(category);
                    }
                })
                .setNegativeButton(R.string.action_cancel, null)
                .show();
    }

    private void showEditCategoryDialog(Category category) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_category_edit, null);
        TextView tvTitle = dialogView.findViewById(R.id.tvCategoryDialogTitle);
        EditText etName = dialogView.findViewById(R.id.etCategoryName);
        RadioGroup rgColors = dialogView.findViewById(R.id.rgColors);

        tvTitle.setText(R.string.edit_category);
        etName.setText(category.getName());

        if ("#4CAF50".equalsIgnoreCase(category.getColor())) {
            ((RadioButton) dialogView.findViewById(R.id.rbColorGreen)).setChecked(true);
        } else if ("#2196F3".equalsIgnoreCase(category.getColor())) {
            ((RadioButton) dialogView.findViewById(R.id.rbColorBlue)).setChecked(true);
        } else if ("#9C27B0".equalsIgnoreCase(category.getColor())) {
            ((RadioButton) dialogView.findViewById(R.id.rbColorPurple)).setChecked(true);
        } else {
            ((RadioButton) dialogView.findViewById(R.id.rbColorOrange)).setChecked(true);
        }

        new AlertDialog.Builder(this)
                .setView(dialogView)
                .setPositiveButton(R.string.action_save, (dialog, which) -> {
                    String name = etName.getText().toString().trim();
                    if (name.isEmpty()) {
                        Toast.makeText(this, "Category name cannot be empty", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    int checkedId = rgColors.getCheckedRadioButtonId();
                    String color = "#FF9800";
                    if (checkedId == R.id.rbColorGreen) color = "#4CAF50";
                    else if (checkedId == R.id.rbColorBlue) color = "#2196F3";
                    else if (checkedId == R.id.rbColorPurple) color = "#9C27B0";

                    category.setName(name);
                    category.setColor(color);
                    updateCategoryOnServer(category);
                })
                .setNegativeButton(R.string.action_cancel, null)
                .show();
    }

    private void confirmDeleteCategoryFromMain(Category category) {
        if (category.getNoteCount() > 0) {
            new AlertDialog.Builder(this)
                    .setTitle("Cannot Delete")
                    .setMessage("This category has Note inside!")
                    .setPositiveButton(android.R.string.ok, null)
                    .show();
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle(R.string.action_delete)
                .setMessage(getString(R.string.delete_category_confirm) + "\n\"" + category.getName() + "\"")
                .setPositiveButton(R.string.action_delete, (dialog, which) -> deleteCategoryOnServerFromMain(category))
                .setNegativeButton(R.string.action_cancel, null)
                .show();
    }

    private void updateCategoryOnServer(Category category) {
        if (category.getId() == null) return;
        pbLoading.setVisibility(View.VISIBLE);
        apiService.updateCategory(category.getId(), category).enqueue(new Callback<Category>() {
            @Override
            public void onResponse(@NonNull Call<Category> call, @NonNull Response<Category> response) {
                if (response.isSuccessful()) {
                    pbLoading.setVisibility(View.GONE);
                    Toast.makeText(MainActivity.this, R.string.msg_category_saved, Toast.LENGTH_SHORT).show();
                    loadCategories();
                    loadNotes();
                } else {
                    apiService.updateCategoryRest(category.getId(), category).enqueue(new Callback<Category>() {
                        @Override
                        public void onResponse(@NonNull Call<Category> c, @NonNull Response<Category> r) {
                            pbLoading.setVisibility(View.GONE);
                            if (r.isSuccessful()) {
                                Toast.makeText(MainActivity.this, R.string.msg_category_saved, Toast.LENGTH_SHORT).show();
                                loadCategories();
                                loadNotes();
                            } else {
                                Toast.makeText(MainActivity.this, "Update failed: " + response.code(), Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onFailure(@NonNull Call<Category> c, @NonNull Throwable t) {
                            pbLoading.setVisibility(View.GONE);
                            Toast.makeText(MainActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }

            @Override
            public void onFailure(@NonNull Call<Category> call, @NonNull Throwable t) {
                pbLoading.setVisibility(View.GONE);
                Toast.makeText(MainActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void deleteCategoryOnServerFromMain(Category category) {
        if (category.getId() == null) return;
        pbLoading.setVisibility(View.VISIBLE);
        apiService.deleteCategory(category.getId()).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                if (response.isSuccessful()) {
                    pbLoading.setVisibility(View.GONE);
                    Toast.makeText(MainActivity.this, R.string.msg_category_deleted, Toast.LENGTH_SHORT).show();
                    loadCategories();
                    loadNotes();
                } else {
                    apiService.deleteCategoryRest(category.getId()).enqueue(new Callback<Void>() {
                        @Override
                        public void onResponse(@NonNull Call<Void> c, @NonNull Response<Void> r) {
                            pbLoading.setVisibility(View.GONE);
                            if (r.isSuccessful()) {
                                Toast.makeText(MainActivity.this, R.string.msg_category_deleted, Toast.LENGTH_SHORT).show();
                                loadCategories();
                                loadNotes();
                            } else {
                                Toast.makeText(MainActivity.this, "Delete failed: " + response.code(), Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onFailure(@NonNull Call<Void> c, @NonNull Throwable t) {
                            pbLoading.setVisibility(View.GONE);
                            Toast.makeText(MainActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }

            @Override
            public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                pbLoading.setVisibility(View.GONE);
                Toast.makeText(MainActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showAddCategoryDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_category_edit, null);
        TextView tvTitle = dialogView.findViewById(R.id.tvCategoryDialogTitle);
        EditText etName = dialogView.findViewById(R.id.etCategoryName);
        RadioGroup rgColors = dialogView.findViewById(R.id.rgColors);

        tvTitle.setText(R.string.add_category);

        new AlertDialog.Builder(this)
                .setView(dialogView)
                .setPositiveButton(R.string.action_save, (dialog, which) -> {
                    String name = etName.getText().toString().trim();
                    if (name.isEmpty()) {
                        Toast.makeText(this, "Category name cannot be empty", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    int checkedId = rgColors.getCheckedRadioButtonId();
                    String color = "#FF9800";
                    if (checkedId == R.id.rbColorGreen) color = "#4CAF50";
                    else if (checkedId == R.id.rbColorBlue) color = "#2196F3";
                    else if (checkedId == R.id.rbColorPurple) color = "#9C27B0";

                    Category newCat = new Category(null, name, color);
                    createCategoryOnServer(newCat);
                })
                .setNegativeButton(R.string.action_cancel, null)
                .show();
    }

    private void createCategoryOnServer(Category category) {
        pbLoading.setVisibility(View.VISIBLE);
        apiService.createCategory(category).enqueue(new Callback<Category>() {
            @Override
            public void onResponse(@NonNull Call<Category> call, @NonNull Response<Category> response) {
                if (response.isSuccessful()) {
                    pbLoading.setVisibility(View.GONE);
                    Toast.makeText(MainActivity.this, R.string.msg_category_saved, Toast.LENGTH_SHORT).show();
                    loadCategories();
                } else {
                    apiService.createCategoryRest(category).enqueue(new Callback<Category>() {
                        @Override
                        public void onResponse(@NonNull Call<Category> c, @NonNull Response<Category> r) {
                            pbLoading.setVisibility(View.GONE);
                            if (r.isSuccessful()) {
                                Toast.makeText(MainActivity.this, R.string.msg_category_saved, Toast.LENGTH_SHORT).show();
                                loadCategories();
                            } else {
                                Toast.makeText(MainActivity.this, "Save failed: " + response.code(), Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onFailure(@NonNull Call<Category> c, @NonNull Throwable t) {
                            pbLoading.setVisibility(View.GONE);
                            Toast.makeText(MainActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }

            @Override
            public void onFailure(@NonNull Call<Category> call, @NonNull Throwable t) {
                apiService.createCategoryRest(category).enqueue(new Callback<Category>() {
                    @Override
                    public void onResponse(@NonNull Call<Category> c, @NonNull Response<Category> r) {
                        pbLoading.setVisibility(View.GONE);
                        if (r.isSuccessful()) {
                            Toast.makeText(MainActivity.this, R.string.msg_category_saved, Toast.LENGTH_SHORT).show();
                            loadCategories();
                        } else {
                            Toast.makeText(MainActivity.this, "Save failed: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<Category> c, @NonNull Throwable t2) {
                        pbLoading.setVisibility(View.GONE);
                        Toast.makeText(MainActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    /**
     * Delete Note (POST note/delete.php?id={id})
     */
    private void deleteNoteOnServer(Note note) {
        if (note.getId() == null) return;

        pbLoading.setVisibility(View.VISIBLE);
        apiService.deleteNote(note.getId()).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                if (response.isSuccessful()) {
                    pbLoading.setVisibility(View.GONE);
                    Toast.makeText(MainActivity.this, R.string.msg_note_deleted, Toast.LENGTH_SHORT).show();
                    loadNotes();
                } else {
                    // Try REST fallback
                    deleteNoteRestFallback(note);
                }
            }

            @Override
            public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                deleteNoteRestFallback(note);
            }
        });
    }

    private void deleteNoteRestFallback(Note note) {
        apiService.deleteNoteRest(note.getId()).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                pbLoading.setVisibility(View.GONE);
                if (response.isSuccessful()) {
                    Toast.makeText(MainActivity.this, R.string.msg_note_deleted, Toast.LENGTH_SHORT).show();
                    loadNotes();
                } else {
                    Toast.makeText(MainActivity.this, "Failed to delete: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                pbLoading.setVisibility(View.GONE);
                Toast.makeText(MainActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void filterNotesByCategory(Category category) {
        noteAdapter.setNotes(allNotes);
        if (category == null || category.getId() == null || category.getId() == -1 || category.getId() == -999) {
            noteAdapter.filterByCategory(-1, null);
        } else {
            noteAdapter.filterByCategory(category.getId(), category.getName());
        }

        // Apply current search text if any
        String search = etSearch.getText().toString();
        if (!search.isEmpty()) {
            noteAdapter.filter(search);
        }

        updateEmptyState();
    }

    private void filterNotesByCategory(Integer categoryId) {
        noteAdapter.setNotes(allNotes);
        if (categoryId == null || categoryId == -1 || categoryId == -999) {
            noteAdapter.filterByCategory(-1, null);
        } else {
            for (Category c : allCategories) {
                if (c.getId() != null && c.getId().equals(categoryId)) {
                    filterNotesByCategory(c);
                    return;
                }
            }
            noteAdapter.filterByCategory(categoryId, null);
        }

        // Apply current search text if any
        String search = etSearch.getText().toString();
        if (!search.isEmpty()) {
            noteAdapter.filter(search);
        }

        updateEmptyState();
    }

    private void updateCategoryCounts() {
        for (Category cat : allCategories) {
            if (cat.getId() == -1) {
                cat.setNoteCount(allNotes.size());
            } else if (cat.getId() == -999) {
                cat.setNoteCount(0);
            } else {
                int count = 0;
                for (Note n : allNotes) {
                    boolean matchId = (n.getCategoryId() != null && n.getCategoryId().equals(cat.getId()));
                    boolean matchName = (n.getCategoryName() != null && cat.getName() != null && n.getCategoryName().equalsIgnoreCase(cat.getName()));
                    if (matchId || matchName) {
                        count++;
                    }
                }
                cat.setNoteCount(count);
            }
        }
        categoryAdapter.notifyDataSetChanged();
    }

    private void updateEmptyState() {
        if (noteAdapter.getItemCount() == 0) {
            llEmptyState.setVisibility(View.VISIBLE);
        } else {
            llEmptyState.setVisibility(View.GONE);
        }
    }

    private void showError(String msg) {
        tvErrorMessage.setText(msg);
        llErrorState.setVisibility(View.VISIBLE);
        updateEmptyState();
        Snackbar.make(coordinatorLayout(), msg, Snackbar.LENGTH_LONG)
                .setAction(R.string.action_settings, v -> showServerSettingsDialog())
                .show();
    }

    private View coordinatorLayout() {
        return findViewById(R.id.coordinatorLayout);
    }

    // ==========================================
    // ADAPTER CALLBACKS
    // ==========================================

    @Override
    public void onNoteClick(Note note) {
        // Open note in edit mode
        onNoteEdit(note);
    }

    @Override
    public void onNoteEdit(Note note) {
        Intent intent = new Intent(this, AddEditNoteActivity.class);
        intent.putExtra(Constants.EXTRA_IS_EDIT, true);
        intent.putExtra(Constants.EXTRA_NOTE, note);
        addEditNoteLauncher.launch(intent);
    }

    @Override
    public void onNoteDelete(Note note) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.action_delete)
                .setMessage(getString(R.string.delete_note_confirm) + "\n\"" + note.getTitle() + "\"")
                .setPositiveButton(R.string.action_delete, (dialog, which) -> deleteNoteOnServer(note))
                .setNegativeButton(R.string.action_cancel, null)
                .show();
    }

    // ==========================================
    // OPTIONS MENU & SERVER SETTINGS DIALOG
    // ==========================================

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_refresh) {
            swipeRefresh.setRefreshing(true);
            loadCategories();
            loadNotes();
            return true;
        } else if (id == R.id.action_demo_data) {
            loadDemoData();
            return true;
        } else if (id == R.id.action_manage_categories) {
            Intent intent = new Intent(this, CategoryActivity.class);
            categoryActivityLauncher.launch(intent);
            return true;
        } else if (id == R.id.action_server_settings) {
            showServerSettingsDialog();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showServerSettingsDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_server_settings, null);
        EditText etUrl = dialogView.findViewById(R.id.etBaseUrl);
        Button btnReset = dialogView.findViewById(R.id.btnResetUrl);

        String currentUrl = Constants.getBaseUrl(this);
        etUrl.setText(currentUrl);

        btnReset.setOnClickListener(v -> etUrl.setText(Constants.DEFAULT_BASE_URL));

        new AlertDialog.Builder(this)
                .setView(dialogView)
                .setPositiveButton(R.string.server_settings_save, (dialog, which) -> {
                    String newUrl = etUrl.getText().toString().trim();
                    if (!newUrl.isEmpty()) {
                        Constants.setBaseUrl(this, newUrl);
                        ApiClient.reset();
                        apiService = ApiClient.getApiService(this);
                        Toast.makeText(this, "Base URL updated to: " + newUrl, Toast.LENGTH_SHORT).show();
                        loadNotes();
                    }
                })
                .setNegativeButton(R.string.action_cancel, null)
                .show();
    }
}
