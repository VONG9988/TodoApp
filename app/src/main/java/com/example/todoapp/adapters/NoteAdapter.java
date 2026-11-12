package com.example.todoapp.adapters;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.example.todoapp.R;
import com.example.todoapp.models.Note;
import com.google.android.material.card.MaterialCardView;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class NoteAdapter extends RecyclerView.Adapter<NoteAdapter.NoteViewHolder> {

    public interface OnNoteListener {
        void onNoteClick(Note note);
        void onNoteEdit(Note note);
        void onNoteDelete(Note note);
    }

    private final Context context;
    private final List<Note> originalNotes = new ArrayList<>();
    private final List<Note> displayedNotes = new ArrayList<>();
    private final OnNoteListener listener;

    // Palette matching the diagram UI (Orange, Green, Blue, Purple)
    private final int[] noteIconColors = new int[]{
            Color.parseColor("#FF9800"), // Orange
            Color.parseColor("#4CAF50"), // Green
            Color.parseColor("#2196F3"), // Blue
            Color.parseColor("#9C27B0")  // Purple
    };

    private final int[] noteBadgeBgColors = new int[]{
            Color.parseColor("#FFF3E0"), // Orange light
            Color.parseColor("#E8F5E9"), // Green light
            Color.parseColor("#E3F2FD"), // Blue light
            Color.parseColor("#F3E5F5")  // Purple light
    };

    public NoteAdapter(Context context, OnNoteListener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void setNotes(List<Note> notes) {
        this.originalNotes.clear();
        this.displayedNotes.clear();
        if (notes != null) {
            this.originalNotes.addAll(notes);
            this.displayedNotes.addAll(notes);
        }
        notifyDataSetChanged();
    }

    public void filter(String query) {
        displayedNotes.clear();
        if (query == null || query.trim().isEmpty()) {
            displayedNotes.addAll(originalNotes);
        } else {
            String lower = query.toLowerCase(Locale.getDefault()).trim();
            for (Note note : originalNotes) {
                boolean matchTitle = note.getTitle() != null && note.getTitle().toLowerCase(Locale.getDefault()).contains(lower);
                boolean matchContent = note.getContent() != null && note.getContent().toLowerCase(Locale.getDefault()).contains(lower);
                boolean matchCategory = note.getCategoryName() != null && note.getCategoryName().toLowerCase(Locale.getDefault()).contains(lower);
                if (matchTitle || matchContent || matchCategory) {
                    displayedNotes.add(note);
                }
            }
        }
        notifyDataSetChanged();
    }

    public void filterByCategory(Integer categoryId) {
        filterByCategory(categoryId, null);
    }

    public void filterByCategory(Integer categoryId, String categoryName) {
        displayedNotes.clear();
        if ((categoryId == null || categoryId == -1) && (categoryName == null || categoryName.isEmpty() || "All".equalsIgnoreCase(categoryName))) {
            displayedNotes.addAll(originalNotes);
        } else {
            for (Note note : originalNotes) {
                boolean matchId = (categoryId != null && categoryId != -1 && note.getCategoryId() != null && note.getCategoryId().equals(categoryId));
                boolean matchName = (categoryName != null && !categoryName.isEmpty() && note.getCategoryName() != null && note.getCategoryName().equalsIgnoreCase(categoryName));
                if (matchId || matchName) {
                    displayedNotes.add(note);
                }
            }
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public NoteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_note, parent, false);
        return new NoteViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NoteViewHolder holder, int position) {
        Note note = displayedNotes.get(position);

        holder.tvTitle.setText(note.getTitle() != null ? note.getTitle() : "");

        if (note.getContent() != null && !note.getContent().trim().isEmpty()) {
            holder.tvContent.setText(note.getContent());
            holder.tvContent.setVisibility(View.VISIBLE);
        } else {
            holder.tvContent.setVisibility(View.GONE);
        }

        if (note.getDate() != null && !note.getDate().isEmpty()) {
            holder.tvDate.setText(note.getDate());
            holder.tvDate.setVisibility(View.VISIBLE);
        } else {
            holder.tvDate.setVisibility(View.GONE);
        }

        // Color badge & icon tint matching screenshot
        int colorIndex = Math.abs(position % noteIconColors.length);
        if (note.getCategoryColor() != null && !note.getCategoryColor().isEmpty()) {
            try {
                int catColor = Color.parseColor(note.getCategoryColor());
                holder.ivIcon.setImageTintList(ColorStateList.valueOf(catColor));
            } catch (Exception e) {
                holder.ivIcon.setImageTintList(ColorStateList.valueOf(noteIconColors[colorIndex]));
            }
        } else {
            holder.ivIcon.setImageTintList(ColorStateList.valueOf(noteIconColors[colorIndex]));
        }
        holder.flIconContainer.setBackgroundTintList(ColorStateList.valueOf(noteBadgeBgColors[colorIndex]));

        // Category Tag
        if (note.getCategoryName() != null && !note.getCategoryName().isEmpty()) {
            holder.tvCategory.setText(note.getCategoryName());
            holder.tvCategory.setVisibility(View.VISIBLE);
        } else {
            holder.tvCategory.setVisibility(View.GONE);
        }

        // Item click listeners
        holder.cardNote.setOnClickListener(v -> {
            if (listener != null) listener.onNoteClick(note);
        });

        holder.btnEdit.setOnClickListener(v -> {
            if (listener != null) listener.onNoteEdit(note);
        });

        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null) listener.onNoteDelete(note);
        });
    }

    @Override
    public int getItemCount() {
        return displayedNotes.size();
    }

    static class NoteViewHolder extends RecyclerView.ViewHolder {
        final MaterialCardView cardNote;
        final FrameLayout flIconContainer;
        final ImageView ivIcon;
        final TextView tvTitle;
        final TextView tvContent;
        final TextView tvDate;
        final TextView tvCategory;
        final ImageButton btnEdit;
        final ImageButton btnDelete;

        public NoteViewHolder(@NonNull View itemView) {
            super(itemView);
            cardNote = itemView.findViewById(R.id.cardNote);
            flIconContainer = itemView.findViewById(R.id.flIconContainer);
            ivIcon = itemView.findViewById(R.id.ivNoteIcon);
            tvTitle = itemView.findViewById(R.id.tvNoteTitle);
            tvContent = itemView.findViewById(R.id.tvNoteContent);
            tvDate = itemView.findViewById(R.id.tvNoteDate);
            tvCategory = itemView.findViewById(R.id.tvNoteCategory);
            btnEdit = itemView.findViewById(R.id.btnEditNote);
            btnDelete = itemView.findViewById(R.id.btnDeleteNote);
        }
    }
}
