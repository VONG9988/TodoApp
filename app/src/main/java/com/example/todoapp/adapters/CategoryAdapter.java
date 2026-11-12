package com.example.todoapp.adapters;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.example.todoapp.R;
import com.example.todoapp.models.Category;
import com.google.android.material.card.MaterialCardView;
import java.util.ArrayList;
import java.util.List;

public class CategoryAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public static final int MODE_CHIPS = 0;
    public static final int MODE_MANAGE = 1;

    public interface OnCategoryClickListener {
        void onCategoryClick(Category category, int position);
        default void onCategoryEdit(Category category) {}
        default void onCategoryDelete(Category category) {}
        default void onCategoryLongClick(Category category) {}
    }

    private final Context context;
    private final List<Category> categories = new ArrayList<>();
    private final OnCategoryClickListener listener;
    private final int mode;
    private int selectedPosition = 0; // Default to first item ("All")

    public CategoryAdapter(Context context, int mode, OnCategoryClickListener listener) {
        this.context = context;
        this.mode = mode;
        this.listener = listener;
    }

    public void setCategories(List<Category> newCategories) {
        this.categories.clear();
        if (newCategories != null) {
            this.categories.addAll(newCategories);
        }
        notifyDataSetChanged();
    }

    public void setSelectedPosition(int position) {
        int previous = this.selectedPosition;
        this.selectedPosition = position;
        notifyItemChanged(previous);
        notifyItemChanged(selectedPosition);
    }

    public int getSelectedPosition() {
        return selectedPosition;
    }

    @Override
    public int getItemViewType(int position) {
        return mode;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == MODE_MANAGE) {
            View view = LayoutInflater.from(context).inflate(R.layout.item_category_manage, parent, false);
            return new ManageViewHolder(view);
        } else {
            View view = LayoutInflater.from(context).inflate(R.layout.item_category, parent, false);
            return new ChipViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Category category = categories.get(position);

        if (holder instanceof ChipViewHolder) {
            ChipViewHolder chipHolder = (ChipViewHolder) holder;
            chipHolder.tvName.setText(category.getName());

            boolean isAddButton = (category.getId() != null && category.getId() == -999);
            boolean isSelected = (!isAddButton && position == selectedPosition);

            if (isAddButton) {
                chipHolder.cardCategory.setCardBackgroundColor(ContextCompat.getColor(context, R.color.surface));
                chipHolder.tvName.setTextColor(ContextCompat.getColor(context, R.color.primary));
                chipHolder.viewIndicator.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.primary)));
                chipHolder.tvCount.setVisibility(View.GONE);
            } else {
                if (isSelected) {
                    chipHolder.cardCategory.setCardBackgroundColor(ContextCompat.getColor(context, R.color.primary));
                    chipHolder.tvName.setTextColor(ContextCompat.getColor(context, R.color.white));
                    chipHolder.viewIndicator.setBackgroundTintList(ColorStateList.valueOf(Color.WHITE));
                } else {
                    chipHolder.cardCategory.setCardBackgroundColor(ContextCompat.getColor(context, R.color.surface));
                    chipHolder.tvName.setTextColor(ContextCompat.getColor(context, R.color.text_primary));
                    chipHolder.viewIndicator.setBackgroundTintList(ColorStateList.valueOf(category.getColorInt()));
                }

                if (category.getNoteCount() > 0) {
                    chipHolder.tvCount.setText(String.valueOf(category.getNoteCount()));
                    chipHolder.tvCount.setVisibility(View.VISIBLE);
                    if (isSelected) {
                        chipHolder.tvCount.setTextColor(ContextCompat.getColor(context, R.color.primary));
                        chipHolder.tvCount.setBackgroundTintList(ColorStateList.valueOf(Color.WHITE));
                    } else {
                        chipHolder.tvCount.setTextColor(ContextCompat.getColor(context, R.color.text_secondary));
                        chipHolder.tvCount.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#F0F0F0")));
                    }
                } else {
                    chipHolder.tvCount.setVisibility(View.GONE);
                }
            }

            chipHolder.cardCategory.setOnClickListener(v -> {
                int adapterPos = chipHolder.getAdapterPosition();
                if (adapterPos != RecyclerView.NO_POSITION) {
                    if (!isAddButton) {
                        int previous = selectedPosition;
                        selectedPosition = adapterPos;
                        notifyItemChanged(previous);
                        notifyItemChanged(selectedPosition);
                    }
                    if (listener != null) {
                        listener.onCategoryClick(category, adapterPos);
                    }
                }
            });

            chipHolder.cardCategory.setOnLongClickListener(v -> {
                int adapterPos = chipHolder.getAdapterPosition();
                if (adapterPos != RecyclerView.NO_POSITION && !isAddButton && category.getId() != null && category.getId() != -1) {
                    if (listener != null) {
                        listener.onCategoryLongClick(category);
                    }
                    return true;
                }
                return false;
            });

        } else if (holder instanceof ManageViewHolder) {
            ManageViewHolder manageHolder = (ManageViewHolder) holder;
            manageHolder.tvName.setText(category.getName());
            manageHolder.viewColor.setBackgroundTintList(ColorStateList.valueOf(category.getColorInt()));

            if (category.getNoteCount() > 0) {
                manageHolder.tvSub.setText(category.getNoteCount() + " notes");
            } else {
                manageHolder.tvSub.setText("0 notes");
            }

            manageHolder.btnEdit.setOnClickListener(v -> {
                if (listener != null) listener.onCategoryEdit(category);
            });

            manageHolder.btnDelete.setOnClickListener(v -> {
                if (listener != null) listener.onCategoryDelete(category);
            });
        }
    }

    @Override
    public int getItemCount() {
        return categories.size();
    }

    static class ChipViewHolder extends RecyclerView.ViewHolder {
        final MaterialCardView cardCategory;
        final View viewIndicator;
        final TextView tvName;
        final TextView tvCount;

        public ChipViewHolder(@NonNull View itemView) {
            super(itemView);
            cardCategory = itemView.findViewById(R.id.cardCategory);
            viewIndicator = itemView.findViewById(R.id.viewColorIndicator);
            tvName = itemView.findViewById(R.id.tvCategoryName);
            tvCount = itemView.findViewById(R.id.tvCategoryCount);
        }
    }

    static class ManageViewHolder extends RecyclerView.ViewHolder {
        final View viewColor;
        final TextView tvName;
        final TextView tvSub;
        final ImageButton btnEdit;
        final ImageButton btnDelete;

        public ManageViewHolder(@NonNull View itemView) {
            super(itemView);
            viewColor = itemView.findViewById(R.id.viewManageCategoryColor);
            tvName = itemView.findViewById(R.id.tvManageCategoryName);
            tvSub = itemView.findViewById(R.id.tvManageCategorySub);
            btnEdit = itemView.findViewById(R.id.btnEditCategory);
            btnDelete = itemView.findViewById(R.id.btnDeleteCategory);
        }
    }
}
