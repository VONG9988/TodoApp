package com.example.todoapp.network;

import android.content.Context;
import com.example.todoapp.models.Category;
import com.example.todoapp.models.Note;
import com.example.todoapp.utils.Constants;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Retrofit singleton client manager.
 * Configured with resilient JSON parsing that supports both:
 * 1) Direct Arrays: [ { "id": 1, ... } ]
 * 2) Wrapped ApiResponse: { "success": true, "data": [ ... ] }
 */
public class ApiClient {

    private static Retrofit retrofit = null;
    private static TodoApiService apiService = null;
    private static String currentBaseUrl = null;

    private ApiClient() {
        // Private constructor for singleton
    }

    public static synchronized TodoApiService getApiService(Context context) {
        String baseUrl = Constants.getBaseUrl(context);

        // If URL changed or instance null, rebuild Retrofit
        if (retrofit == null || apiService == null || !baseUrl.equals(currentBaseUrl)) {
            currentBaseUrl = baseUrl;

            HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
            loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

            OkHttpClient okHttpClient = new OkHttpClient.Builder()
                    .addInterceptor(loggingInterceptor)
                    .connectTimeout(10, TimeUnit.SECONDS)
                    .readTimeout(10, TimeUnit.SECONDS)
                    .writeTimeout(10, TimeUnit.SECONDS)
                    .retryOnConnectionFailure(true)
                    .build();

            // Flexible Deserializer for List<Note>
            Type noteListType = new TypeToken<List<Note>>() {}.getType();
            JsonDeserializer<List<Note>> noteListDeserializer = (json, typeOfT, ctx) -> {
                List<Note> list = new ArrayList<>();
                if (json == null || json.isJsonNull()) return list;

                JsonArray array = null;
                if (json.isJsonArray()) {
                    array = json.getAsJsonArray();
                } else if (json.isJsonObject()) {
                    JsonObject obj = json.getAsJsonObject();
                    if (obj.has("data") && obj.get("data").isJsonArray()) {
                        array = obj.getAsJsonArray("data");
                    } else if (obj.has("notes") && obj.get("notes").isJsonArray()) {
                        array = obj.getAsJsonArray("notes");
                    }
                }

                if (array != null) {
                    for (JsonElement elem : array) {
                        Note note = ctx.deserialize(elem, Note.class);
                        if (note != null) list.add(note);
                    }
                }
                return list;
            };

            // Flexible Deserializer for single Note
            JsonDeserializer<Note> singleNoteDeserializer = (json, typeOfT, ctx) -> {
                if (json == null || json.isJsonNull()) return null;
                if (json.isJsonObject()) {
                    JsonObject obj = json.getAsJsonObject();
                    if (obj.has("data") && obj.get("data").isJsonObject()) {
                        return new Gson().fromJson(obj.get("data"), Note.class);
                    }
                }
                return new Gson().fromJson(json, Note.class);
            };

            // Flexible Deserializer for List<Category>
            Type categoryListType = new TypeToken<List<Category>>() {}.getType();
            JsonDeserializer<List<Category>> categoryListDeserializer = (json, typeOfT, ctx) -> {
                List<Category> list = new ArrayList<>();
                if (json == null || json.isJsonNull()) return list;

                JsonArray array = null;
                if (json.isJsonArray()) {
                    array = json.getAsJsonArray();
                } else if (json.isJsonObject()) {
                    JsonObject obj = json.getAsJsonObject();
                    if (obj.has("data") && obj.get("data").isJsonArray()) {
                        array = obj.getAsJsonArray("data");
                    } else if (obj.has("categories") && obj.get("categories").isJsonArray()) {
                        array = obj.getAsJsonArray("categories");
                    }
                }

                if (array != null) {
                    for (JsonElement elem : array) {
                        Category cat = ctx.deserialize(elem, Category.class);
                        if (cat != null) list.add(cat);
                    }
                }
                return list;
            };

            // Flexible Deserializer for single Category
            JsonDeserializer<Category> singleCategoryDeserializer = (json, typeOfT, ctx) -> {
                if (json == null || json.isJsonNull()) return null;
                if (json.isJsonObject()) {
                    JsonObject obj = json.getAsJsonObject();
                    if (obj.has("data") && obj.get("data").isJsonObject()) {
                        return new Gson().fromJson(obj.get("data"), Category.class);
                    }
                }
                return new Gson().fromJson(json, Category.class);
            };

            Gson gson = new GsonBuilder()
                    .registerTypeAdapter(noteListType, noteListDeserializer)
                    .registerTypeAdapter(Note.class, singleNoteDeserializer)
                    .registerTypeAdapter(categoryListType, categoryListDeserializer)
                    .registerTypeAdapter(Category.class, singleCategoryDeserializer)
                    .setLenient()
                    .create();

            retrofit = new Retrofit.Builder()
                    .baseUrl(currentBaseUrl)
                    .client(okHttpClient)
                    .addConverterFactory(GsonConverterFactory.create(gson))
                    .build();

            apiService = retrofit.create(TodoApiService.class);
        }

        return apiService;
    }

    public static synchronized TodoApiService getApiService() {
        return getApiService(null);
    }

    /**
     * Resets the Retrofit client when the user updates the API URL
     */
    public static synchronized void reset() {
        retrofit = null;
        apiService = null;
        currentBaseUrl = null;
    }
}
