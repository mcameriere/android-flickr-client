package com.example.flickrclienttuto;

import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.List;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

public class MainActivity extends AppCompatActivity {

    final String TAG = MainActivity.class.getSimpleName();
    private RecyclerView recyclerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        recyclerView = findViewById(R.id.recyclerView);

        String apiKey = BuildConfig.FLICKR_API_KEY;

        Retrofit retrofit = new Retrofit.Builder()
                .addConverterFactory(GsonConverterFactory.create())
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .baseUrl("https://api.flickr.com")
                .build();

        FlickrApi api = retrofit.create(FlickrApi.class);

        api.searchPhotos(apiKey, "star", 3)
                .map(response -> response.getAsJsonObject("photos").getAsJsonArray("photo"))
                .flatMap(Observable::fromIterable)
                .map(photo -> photo.getAsJsonObject().get("id").getAsString())
                .flatMap(id -> api.getSizes(apiKey, id))
                .map(response -> response.getAsJsonObject("sizes").getAsJsonArray("size"))
                .map(getJsonArrayStringFunction())
                .doOnNext(url -> Log.d(TAG, url))
                .toList()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::updateUI);

    }

    private void updateUI(List<String> urls) {
        PhotoAdapter adapter = new PhotoAdapter(this, urls);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }

    private Function<JsonArray, String> getJsonArrayStringFunction() {
        return sizes -> {
            String url = "";
            for (int i = 0; i < sizes.size(); i++) {
                JsonObject size = sizes.get(i).getAsJsonObject();
                String label = size.get("label").getAsString();
                String source = size.get("source").getAsString();
                if ("Large".equals(label)) {
                    url = source;
                    break;
                }
            }
            return url;
        };
    }

}
