package com.example.todoapp.models;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;

/**
 * Note data model matching the REST API schema:
 * {
 *   "id": 1,
 *   "title": "Learn Retrofit",
 *   "content": "Call API...",
 *   "date": "2026-10-02"
 * }
 */
public class Note implements Serializable {

    @SerializedName(value = "id", alternate = {"_id", "noteId", "note_id"})
    private Integer id;

    @SerializedName(value = "title", alternate = {"name", "subject", "title_text"})
    private String title;

    @SerializedName(value = "content", alternate = {"description", "body", "details", "text"})
    private String content;

    @SerializedName(value = "date", alternate = {"createdAt", "created_at", "updatedAt", "updated_at", "datetime", "timestamp", "time"})
    private String date;

    @SerializedName(value = "categoryId", alternate = {"category_id", "cat_id", "catId"})
    private Integer categoryId;

    @SerializedName(value = "categoryName", alternate = {"category_name", "cat_name", "catName"})
    private String categoryName;

    @SerializedName(value = "categoryColor", alternate = {"category_color", "cat_color", "catColor"})
    private String categoryColor;

    public Note() {
    }

    public Note(String title, String content, String date) {
        this.title = title;
        this.content = content;
        this.date = date;
    }

    public Note(Integer id, String title, String content, String date) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.date = date;
    }

    public Note(Integer id, String title, String content, String date, Integer categoryId, String categoryName, String categoryColor) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.date = date;
        this.categoryId = categoryId;
        this.categoryName = categoryName;
        this.categoryColor = categoryColor;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public Integer getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Integer categoryId) {
        this.categoryId = categoryId;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public String getCategoryColor() {
        return categoryColor;
    }

    public void setCategoryColor(String categoryColor) {
        this.categoryColor = categoryColor;
    }
}
