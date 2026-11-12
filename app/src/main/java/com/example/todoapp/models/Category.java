package com.example.todoapp.models;

import android.graphics.Color;
import com.google.gson.annotations.SerializedName;
import java.io.Serializable;

/**
 * Category data model for grouping notes
 */
public class Category implements Serializable {

    @SerializedName(value = "id", alternate = {"_id", "categoryId", "category_id"})
    private Integer id;

    @SerializedName(value = "name", alternate = {"title", "category_name", "categoryName", "label"})
    private String name;

    @SerializedName(value = "color", alternate = {"color_code", "hex_color", "colorCode"})
    private String color;

    @SerializedName(value = "noteCount", alternate = {"note_count", "count", "notes_count"})
    private int noteCount;

    public Category() {
    }

    public Category(Integer id, String name, String color) {
        this.id = id;
        this.name = name;
        this.color = color;
    }

    public Category(Integer id, String name, String color, int noteCount) {
        this.id = id;
        this.name = name;
        this.color = color;
        this.noteCount = noteCount;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public int getNoteCount() {
        return noteCount;
    }

    public void setNoteCount(int noteCount) {
        this.noteCount = noteCount;
    }

    public int getColorInt() {
        if (color != null && !color.isEmpty()) {
            try {
                return Color.parseColor(color);
            } catch (Exception ignored) {
            }
        }
        return Color.parseColor("#5E35B1");
    }

    @Override
    public String toString() {
        return name != null ? name : "";
    }
}
