package com.example.todoapp.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.example.todoapp.R;
import com.example.todoapp.adapters.CategoryAdapter;
import com.example.todoapp.models.Category;
import com.example.todoapp.network.ApiClient;
import com.example.todoapp.network.TodoApiService;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import java.util.ArrayList;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CategoryActivity extends AppCompatActivity {

    private MaterialToolbar toolbar;
    private RecyclerView rvCategoryList;
    private SwipeRefreshLayout swipeRefresh;
    private ProgressBar pbLoading;
    private LinearLayout llEmptyCategory;
    private FloatingActionButton fabAddCategory;

    private CategoryAdapter adapter;
    private final List<Category> categories = new ArrayList<>();
    private TodoApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_category);

        apiService = ApiClient.getApiService(this);

        initViews();
        setupToolbar();
        setupRecyclerView();
        setupListeners();

        loadCategories();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbarCategory);
        rvCategoryList = findViewById(R.id.rvCategoryList);
        swipeRefresh = findViewById(R.id.swipeRefreshCategory);
        pbLoading = findViewById(R.id.pbCategoryLoading);
        llEmptyCategory = findViewById(R.id.llEmptyCategory);
        fabAddCategory = findViewById(R.id.fabAddCategory);
    }

    private void setupToolbar() {
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupRecyclerView() {
        rvCategoryList.setLayoutManager(new LinearLayoutManager(this));
        adapter = new CategoryAdapter(this, CategoryAdapter.MODE_MANAGE, new CategoryAdapter.OnCategoryClickListener() {
            @Override
            public void onCategoryClick(Category category, int position) {}

            @Override
            public void onCategoryEdit(Category category) {
                showCategoryDialog(category);
            }

            @Override
            public void onCategoryDelete(Category category) {
                confirmDeleteCategory(category);
            }
        });
        rvCategoryList.setAdapter(adapter);
    }

    private void setupListeners() {
        swipeRefresh.setOnRefreshListener(this::loadCategories);
        fabAddCategory.setOnClickListener(v -> showCategoryDialog(null));
    }

    private void loadCategories() {
        if (!swipeRefresh.isRefreshing()) {
            pbLoading.setVisibility(View.VISIBLE);
        }

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
                    onCategoriesDefaultFallback();
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<Category>> call, @NonNull Throwable t) {
                onCategoriesDefaultFallback();
            }
        });
    }

    private void onCategoriesLoaded(List<Category> categoryList) {
        pbLoading.setVisibility(View.GONE);
        swipeRefresh.setRefreshing(false);
        categories.clear();
        categories.addAll(categoryList);
        adapter.setCategories(categories);
        updateEmptyState();
    }

    private void onCategoriesDefaultFallback() {
        pbLoading.setVisibility(View.GONE);
        swipeRefresh.setRefreshing(false);
        if (categories.isEmpty()) {
            categories.add(new Category(1, "Work", "#FF9800", 2));
            categories.add(new Category(2, "Study", "#4CAF50", 1));
            categories.add(new Category(3, "Personal", "#2196F3", 0));
        }
        adapter.setCategories(categories);
        updateEmptyState();
    }

    private void updateEmptyState() {
        if (categories.isEmpty()) {
            llEmptyCategory.setVisibility(View.VISIBLE);
        } else {
            llEmptyCategory.setVisibility(View.GONE);
        }
    }

    private void showCategoryDialog(Category existing) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_category_edit, null);
        TextView tvTitle = dialogView.findViewById(R.id.tvCategoryDialogTitle);
        EditText etName = dialogView.findViewById(R.id.etCategoryName);
        RadioGroup rgColors = dialogView.findViewById(R.id.rgColors);

        boolean isEdit = (existing != null);
        tvTitle.setText(isEdit ? R.string.edit_category : R.string.add_category);

        if (isEdit) {
            etName.setText(existing.getName());
            if ("#4CAF50".equalsIgnoreCase(existing.getColor())) {
                ((RadioButton) dialogView.findViewById(R.id.rbColorGreen)).setChecked(true);
            } else if ("#2196F3".equalsIgnoreCase(existing.getColor())) {
                ((RadioButton) dialogView.findViewById(R.id.rbColorBlue)).setChecked(true);
            } else if ("#9C27B0".equalsIgnoreCase(existing.getColor())) {
                ((RadioButton) dialogView.findViewById(R.id.rbColorPurple)).setChecked(true);
            } else {
                ((RadioButton) dialogView.findViewById(R.id.rbColorOrange)).setChecked(true);
            }
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

                    if (isEdit) {
                        existing.setName(name);
                        existing.setColor(color);
                        updateCategoryOnServer(existing);
                    } else {
                        Category newCat = new Category(null, name, color);
                        createCategoryOnServer(newCat);
                    }
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
                    Toast.makeText(CategoryActivity.this, R.string.msg_category_saved, Toast.LENGTH_SHORT).show();
                    loadCategories();
                    setResult(RESULT_OK);
                } else {
                    apiService.createCategoryRest(category).enqueue(new Callback<Category>() {
                        @Override
                        public void onResponse(@NonNull Call<Category> c, @NonNull Response<Category> r) {
                            pbLoading.setVisibility(View.GONE);
                            if (r.isSuccessful()) {
                                Toast.makeText(CategoryActivity.this, R.string.msg_category_saved, Toast.LENGTH_SHORT).show();
                                loadCategories();
                                setResult(RESULT_OK);
                            } else {
                                Toast.makeText(CategoryActivity.this, "Save failed: " + response.code(), Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onFailure(@NonNull Call<Category> c, @NonNull Throwable t) {
                            pbLoading.setVisibility(View.GONE);
                            Toast.makeText(CategoryActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }

            @Override
            public void onFailure(@NonNull Call<Category> call, @NonNull Throwable t) {
                pbLoading.setVisibility(View.VISIBLE);
                apiService.createCategoryRest(category).enqueue(new Callback<Category>() {
                    @Override
                    public void onResponse(@NonNull Call<Category> c, @NonNull Response<Category> r) {
                        pbLoading.setVisibility(View.GONE);
                        if (r.isSuccessful()) {
                            Toast.makeText(CategoryActivity.this, R.string.msg_category_saved, Toast.LENGTH_SHORT).show();
                            loadCategories();
                            setResult(RESULT_OK);
                        } else {
                            Toast.makeText(CategoryActivity.this, "Save failed: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<Category> c, @NonNull Throwable t2) {
                        pbLoading.setVisibility(View.GONE);
                        Toast.makeText(CategoryActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    private void updateCategoryOnServer(Category category) {
        if (category.getId() == null) return;
        pbLoading.setVisibility(View.VISIBLE);
        apiService.updateCategory(category.getId(), category).enqueue(new Callback<Category>() {
            @Override
            public void onResponse(@NonNull Call<Category> call, @NonNull Response<Category> response) {
                if (response.isSuccessful()) {
                    pbLoading.setVisibility(View.GONE);
                    Toast.makeText(CategoryActivity.this, R.string.msg_category_saved, Toast.LENGTH_SHORT).show();
                    loadCategories();
                    setResult(RESULT_OK);
                } else {
                    apiService.updateCategoryRest(category.getId(), category).enqueue(new Callback<Category>() {
                        @Override
                        public void onResponse(@NonNull Call<Category> c, @NonNull Response<Category> r) {
                            pbLoading.setVisibility(View.GONE);
                            if (r.isSuccessful()) {
                                Toast.makeText(CategoryActivity.this, R.string.msg_category_saved, Toast.LENGTH_SHORT).show();
                                loadCategories();
                                setResult(RESULT_OK);
                            } else {
                                Toast.makeText(CategoryActivity.this, "Update failed: " + response.code(), Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onFailure(@NonNull Call<Category> c, @NonNull Throwable t) {
                            pbLoading.setVisibility(View.GONE);
                            Toast.makeText(CategoryActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }

            @Override
            public void onFailure(@NonNull Call<Category> call, @NonNull Throwable t) {
                pbLoading.setVisibility(View.GONE);
                Toast.makeText(CategoryActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void confirmDeleteCategory(Category category) {
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
                .setPositiveButton(R.string.action_delete, (dialog, which) -> deleteCategoryOnServer(category))
                .setNegativeButton(R.string.action_cancel, null)
                .show();
    }

    private void deleteCategoryOnServer(Category category) {
        if (category.getId() == null) return;
        pbLoading.setVisibility(View.VISIBLE);
        apiService.deleteCategory(category.getId()).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                if (response.isSuccessful()) {
                    pbLoading.setVisibility(View.GONE);
                    Toast.makeText(CategoryActivity.this, R.string.msg_category_deleted, Toast.LENGTH_SHORT).show();
                    loadCategories();
                    setResult(RESULT_OK);
                } else {
                    apiService.deleteCategoryRest(category.getId()).enqueue(new Callback<Void>() {
                        @Override
                        public void onResponse(@NonNull Call<Void> c, @NonNull Response<Void> r) {
                            pbLoading.setVisibility(View.GONE);
                            if (r.isSuccessful()) {
                                Toast.makeText(CategoryActivity.this, R.string.msg_category_deleted, Toast.LENGTH_SHORT).show();
                                loadCategories();
                                setResult(RESULT_OK);
                            } else {
                                Toast.makeText(CategoryActivity.this, "Delete failed: " + response.code(), Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onFailure(@NonNull Call<Void> c, @NonNull Throwable t) {
                            pbLoading.setVisibility(View.GONE);
                            Toast.makeText(CategoryActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }

            @Override
            public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                pbLoading.setVisibility(View.GONE);
                Toast.makeText(CategoryActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
