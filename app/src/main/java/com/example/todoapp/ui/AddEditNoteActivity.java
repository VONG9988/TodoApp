package com.example.todoapp.ui;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.example.todoapp.R;
import com.example.todoapp.models.Category;
import com.example.todoapp.models.Note;
import com.example.todoapp.network.ApiClient;
import com.example.todoapp.network.TodoApiService;
import com.example.todoapp.utils.Constants;
import com.google.android.material.appbar.MaterialToolbar;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AddEditNoteActivity extends AppCompatActivity {

    private MaterialToolbar toolbar;
    private EditText etTitle;
    private EditText etDate;
    private ImageButton btnPickDate;
    private Spinner spinnerCategory;
    private EditText etContent;
    private Button btnSaveNote;
    private ProgressBar pbSaving;

    private boolean isEditMode = false;
    private Note existingNote;
    private final Calendar calendar = Calendar.getInstance();
    private final SimpleDateFormat dateFormat = new SimpleDateFormat(Constants.DISPLAY_DATE_FORMAT, Locale.getDefault());

    private final List<Category> categoryList = new ArrayList<>();
    private ArrayAdapter<Category> categorySpinnerAdapter;

    private TodoApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit_note);

        apiService = ApiClient.getApiService(this);

        initViews();
        checkIntentData();
        setupToolbar();
        setupDatePicker();
        setupCategorySpinner();
        setupSaveButton();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        etTitle = findViewById(R.id.etTitle);
        etDate = findViewById(R.id.etDate);
        btnPickDate = findViewById(R.id.btnPickDate);
        spinnerCategory = findViewById(R.id.spinnerCategory);
        etContent = findViewById(R.id.etContent);
        btnSaveNote = findViewById(R.id.btnSaveNote);
        pbSaving = findViewById(R.id.pbSaving);
    }

    private void checkIntentData() {
        isEditMode = getIntent().getBooleanExtra(Constants.EXTRA_IS_EDIT, false);
        if (isEditMode) {
            existingNote = (Note) getIntent().getSerializableExtra(Constants.EXTRA_NOTE);
        }

        if (existingNote != null) {
            etTitle.setText(existingNote.getTitle());
            etContent.setText(existingNote.getContent());
            etDate.setText(existingNote.getDate());
        } else {
            // Default to current date and time
            etDate.setText(dateFormat.format(calendar.getTime()));
        }
    }

    private void setupToolbar() {
        toolbar.setTitle(isEditMode ? R.string.title_edit_note : R.string.title_add_note);
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupDatePicker() {
        View.OnClickListener dateClickListener = v -> {
            new DatePickerDialog(
                    AddEditNoteActivity.this,
                    (view, year, month, dayOfMonth) -> {
                        calendar.set(Calendar.YEAR, year);
                        calendar.set(Calendar.MONTH, month);
                        calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);

                        // Also prompt time
                        new TimePickerDialog(
                                AddEditNoteActivity.this,
                                (timeView, hourOfDay, minute) -> {
                                    calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                                    calendar.set(Calendar.MINUTE, minute);
                                    etDate.setText(dateFormat.format(calendar.getTime()));
                                },
                                calendar.get(Calendar.HOUR_OF_DAY),
                                calendar.get(Calendar.MINUTE),
                                false
                        ).show();
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
            ).show();
        };

        etDate.setOnClickListener(dateClickListener);
        btnPickDate.setOnClickListener(dateClickListener);
    }

    private void setupCategorySpinner() {
        categorySpinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, categoryList);
        categorySpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(categorySpinnerAdapter);

        // Fetch categories from API or load defaults
        apiService.getCategories().enqueue(new Callback<List<Category>>() {
            @Override
            public void onResponse(@NonNull Call<List<Category>> call, @NonNull Response<List<Category>> response) {
                categoryList.clear();
                categoryList.add(new Category(null, "No Category", "#9E9E9E"));

                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    categoryList.addAll(response.body());
                } else {
                    categoryList.add(new Category(1, "Work", "#FF9800"));
                    categoryList.add(new Category(2, "Study", "#4CAF50"));
                    categoryList.add(new Category(3, "Personal", "#2196F3"));
                }
                categorySpinnerAdapter.notifyDataSetChanged();
                selectExistingCategory();
            }

            @Override
            public void onFailure(@NonNull Call<List<Category>> call, @NonNull Throwable t) {
                categoryList.clear();
                categoryList.add(new Category(null, "No Category", "#9E9E9E"));
                categoryList.add(new Category(1, "Work", "#FF9800"));
                categoryList.add(new Category(2, "Study", "#4CAF50"));
                categoryList.add(new Category(3, "Personal", "#2196F3"));
                categorySpinnerAdapter.notifyDataSetChanged();
                selectExistingCategory();
            }
        });
    }

    private void selectExistingCategory() {
        if (existingNote != null && existingNote.getCategoryId() != null) {
            for (int i = 0; i < categoryList.size(); i++) {
                if (existingNote.getCategoryId().equals(categoryList.get(i).getId())) {
                    spinnerCategory.setSelection(i);
                    break;
                }
            }
        }
    }

    private void setupSaveButton() {
        btnSaveNote.setOnClickListener(v -> saveNote());
    }

    /**
     * Handles Add Note (POST /notes) and Update Note (PUT /notes/{id})
     */
    private void saveNote() {
        String title = etTitle.getText().toString().trim();
        String content = etContent.getText().toString().trim();
        String date = etDate.getText().toString().trim();

        if (title.isEmpty()) {
            etTitle.setError(getString(R.string.msg_title_required));
            etTitle.requestFocus();
            return;
        }

        Category selectedCategory = (Category) spinnerCategory.getSelectedItem();
        Integer categoryId = selectedCategory != null ? selectedCategory.getId() : null;
        String categoryName = selectedCategory != null ? selectedCategory.getName() : null;
        String categoryColor = selectedCategory != null ? selectedCategory.getColor() : null;

        Note noteToSave;
        if (isEditMode && existingNote != null) {
            noteToSave = existingNote;
            noteToSave.setTitle(title);
            noteToSave.setContent(content);
            noteToSave.setDate(date);
            noteToSave.setCategoryId(categoryId);
            noteToSave.setCategoryName(categoryName);
            noteToSave.setCategoryColor(categoryColor);
        } else {
            noteToSave = new Note(null, title, content, date, categoryId, categoryName, categoryColor);
        }

        setLoading(true);

        if (isEditMode && existingNote != null && existingNote.getId() != null) {
            // Update Note (POST note/update.php?id={id})
            apiService.updateNote(existingNote.getId(), noteToSave).enqueue(new Callback<Note>() {
                @Override
                public void onResponse(@NonNull Call<Note> call, @NonNull Response<Note> response) {
                    if (response.isSuccessful()) {
                        setLoading(false);
                        Toast.makeText(AddEditNoteActivity.this, R.string.msg_note_saved, Toast.LENGTH_SHORT).show();
                        setResult(RESULT_OK);
                        finish();
                    } else {
                        // REST Fallback (PUT /notes/{id})
                        apiService.updateNoteRest(existingNote.getId(), noteToSave).enqueue(new Callback<Note>() {
                            @Override
                            public void onResponse(@NonNull Call<Note> c, @NonNull Response<Note> r) {
                                setLoading(false);
                                if (r.isSuccessful()) {
                                    Toast.makeText(AddEditNoteActivity.this, R.string.msg_note_saved, Toast.LENGTH_SHORT).show();
                                    setResult(RESULT_OK);
                                    finish();
                                } else {
                                    Toast.makeText(AddEditNoteActivity.this, "Update failed: " + response.code(), Toast.LENGTH_SHORT).show();
                                }
                            }

                            @Override
                            public void onFailure(@NonNull Call<Note> c, @NonNull Throwable t) {
                                setLoading(false);
                                Toast.makeText(AddEditNoteActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                }

                @Override
                public void onFailure(@NonNull Call<Note> call, @NonNull Throwable t) {
                    setLoading(false);
                    Toast.makeText(AddEditNoteActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            // Create Note (POST note/create.php)
            apiService.createNote(noteToSave).enqueue(new Callback<Note>() {
                @Override
                public void onResponse(@NonNull Call<Note> call, @NonNull Response<Note> response) {
                    if (response.isSuccessful()) {
                        setLoading(false);
                        Toast.makeText(AddEditNoteActivity.this, R.string.msg_note_saved, Toast.LENGTH_SHORT).show();
                        setResult(RESULT_OK);
                        finish();
                    } else {
                        // REST Fallback (POST /notes)
                        apiService.createNoteRest(noteToSave).enqueue(new Callback<Note>() {
                            @Override
                            public void onResponse(@NonNull Call<Note> c, @NonNull Response<Note> r) {
                                setLoading(false);
                                if (r.isSuccessful()) {
                                    Toast.makeText(AddEditNoteActivity.this, R.string.msg_note_saved, Toast.LENGTH_SHORT).show();
                                    setResult(RESULT_OK);
                                    finish();
                                } else {
                                    Toast.makeText(AddEditNoteActivity.this, "Creation failed: " + response.code(), Toast.LENGTH_SHORT).show();
                                }
                            }

                            @Override
                            public void onFailure(@NonNull Call<Note> c, @NonNull Throwable t) {
                                setLoading(false);
                                Toast.makeText(AddEditNoteActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                }

                @Override
                public void onFailure(@NonNull Call<Note> call, @NonNull Throwable t) {
                    setLoading(false);
                    Toast.makeText(AddEditNoteActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void setLoading(boolean isLoading) {
        pbSaving.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        btnSaveNote.setEnabled(!isLoading);
        etTitle.setEnabled(!isLoading);
        etContent.setEnabled(!isLoading);
    }
}
