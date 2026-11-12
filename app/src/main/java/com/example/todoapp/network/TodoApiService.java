package com.example.todoapp.network;

import com.example.todoapp.models.Category;
import com.example.todoapp.models.Note;
import java.util.List;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

/**
 * Retrofit Service Interface supporting PHP API structure (TodoApp-Api)
 * as well as REST endpoints.
 *
 * PHP Endpoints:
 * - note/read.php
 * - note/create.php
 * - note/update.php
 * - note/delete.php
 *
 * - categories/read.php
 * - categories/create.php
 * - categories/update.php
 * - categories/delete.php
 */
public interface TodoApiService {

    // ==========================================
    // NOTES CRUD OPERATIONS (PHP API)
    // ==========================================

    @GET("note/read.php")
    Call<List<Note>> getNotes();

    @POST("note/read.php")
    Call<List<Note>> getNotesPost();

    @GET("note/read.php")
    Call<Note> getNoteById(@Query("id") int id);

    @GET("note/read.php")
    Call<List<Note>> getNotesByCategory(@Query("categoryId") int categoryId);

    @POST("note/create.php")
    Call<Note> createNote(@Body Note note);

    @POST("note/update.php")
    Call<Note> updateNote(@Query("id") Integer id, @Body Note note);

    @POST("note/delete.php")
    Call<Void> deleteNote(@Query("id") Integer id);


    // ==========================================
    // CATEGORIES CRUD OPERATIONS (PHP API)
    // ==========================================

    @GET("categories/read.php")
    Call<List<Category>> getCategories();

    @POST("categories/read.php")
    Call<List<Category>> getCategoriesPost();

    @POST("categories/create.php")
    Call<Category> createCategory(@Body Category category);

    @POST("categories/update.php")
    Call<Category> updateCategory(@Query("id") Integer id, @Body Category category);

    @POST("categories/delete.php")
    Call<Void> deleteCategory(@Query("id") Integer id);


    // ==========================================
    // REST API FALLBACK ENDPOINTS
    // ==========================================

    @GET("notes")
    Call<List<Note>> getNotesRest();

    @POST("notes")
    Call<Note> createNoteRest(@Body Note note);

    @PUT("notes/{id}")
    Call<Note> updateNoteRest(@Path("id") int id, @Body Note note);

    @DELETE("notes/{id}")
    Call<Void> deleteNoteRest(@Path("id") int id);

    @GET("categories")
    Call<List<Category>> getCategoriesRest();

    @POST("categories")
    Call<Category> createCategoryRest(@Body Category category);

    @PUT("categories/{id}")
    Call<Category> updateCategoryRest(@Path("id") int id, @Body Category category);

    @DELETE("categories/{id}")
    Call<Void> deleteCategoryRest(@Path("id") int id);
}
