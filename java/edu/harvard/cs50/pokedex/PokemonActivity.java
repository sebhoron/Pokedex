package edu.harvard.cs50.pokedex;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URL;

public class PokemonActivity extends AppCompatActivity {
    private ImageView imageView;
    private TextView nameTextView;
    private TextView numberTextView;
    private TextView type1TextView;
    private TextView type2TextView;
    private String url;
    private RequestQueue requestQueue;
    private TextView descriptionTextView;
    private String pokemonName;
    private String bitmapUrl;
    private String descriptionUrl;


    int pokemonId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pokemon);

        requestQueue = Volley.newRequestQueue(getApplicationContext());
        url = getIntent().getStringExtra("url");
        imageView = findViewById(R.id.pokemon_image);
        nameTextView = findViewById(R.id.pokemon_name);
        numberTextView = findViewById(R.id.pokemon_number);
        type1TextView = findViewById(R.id.pokemon_type1);
        type2TextView = findViewById(R.id.pokemon_type2);
        descriptionTextView = findViewById(R.id.pokemon_description);

        load();
    }

    public void load() {
        type1TextView.setText("");
        type2TextView.setText("");

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null, new Response.Listener<JSONObject>() {
            @SuppressLint("DefaultLocale")
            @Override
            public void onResponse(JSONObject response) {
                try {
                    pokemonId = response.getInt("id");
                    nameTextView.setText(response.getString("name"));
                    numberTextView.setText(String.format("#%03d", pokemonId));
                    bitmapUrl = response.getString("front_default");
                    new DownloadSpriteTask().execute(bitmapUrl);

                    descriptionUrl = "https://pokeapi.co/api/v2/pokemon-species/" + pokemonId;;

                    JSONArray typeEntries = response.getJSONArray("types");
                    for (int i = 0; i < typeEntries.length(); i++) {
                        JSONObject typeEntry = typeEntries.getJSONObject(i);
                        int slot = typeEntry.getInt("slot");
                        String type = typeEntry.getJSONObject("type").getString("name");

                        if (slot == 1) {
                            type1TextView.setText(type);
                        } else if (slot == 2) {
                            type2TextView.setText(type);
                        }
                    }
                } catch (JSONException e) {
                    Log.e("cs50", "Pokemon json error", e);
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e("cs50", "Pokemon details error", error);
            }
        });

        requestQueue.add(request);

        JsonObjectRequest descriptionRequest = new JsonObjectRequest(Request.Method.GET, descriptionUrl, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                boolean isEnglish = false;
                int index = 0;
                try {
                    JSONArray descriptionEntries = response.getJSONArray("flavor_text_entries");
                    JSONObject entry, languageEntry;

                    while (!isEnglish) {
                        entry = descriptionEntries.getJSONObject(index);
                        languageEntry = entry.getJSONObject("language");
                        if (languageEntry.getString("name").equals("en")) {
                            isEnglish = true;
                            descriptionTextView.setText(entry.getString("flavor_text"));
                        } else {
                            index++;
                        }
                    }

                } catch (JSONException e) {
                    Log.e("cs50", "Pokemon json error");
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e("cs50", "Pokemon description error");
            }
        });

        requestQueue.add(descriptionRequest);
    }

    public void toggleCatch(View view) {
        pokemonName = String.valueOf(nameTextView);
        boolean caught = getPreferences(Context.MODE_PRIVATE).getBoolean(pokemonName, false);
        Button button = (Button)view;
        if (!caught) {
            getPreferences(Context.MODE_PRIVATE).edit().putBoolean(pokemonName, true).apply();
            button.setText("Release");
        }
        else {
            getPreferences(Context.MODE_PRIVATE).edit().putBoolean(pokemonName, false).apply();
            button.setText("Catch");
        }
    }


    private class DownloadSpriteTask extends AsyncTask<String, Void, Bitmap> {
        @Override
        protected Bitmap doInBackground(String... strings) {
            try {
                URL url = new URL(strings[0]);
                return BitmapFactory.decodeStream(url.openStream());
            }
            catch (IOException e) {
                Log.e("cs50", "Download sprite error", e);
                return null;
            }
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            imageView.setImageBitmap(bitmap);
        }
    }

}
