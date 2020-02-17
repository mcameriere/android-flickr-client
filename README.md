# android-flickr-client

Flickr Client app for Android

Apply to https://www.flickr.com/services/ to obtain your API key.

## Add API key to global Gradle properties

Create or edit ~/.gradle/gradle.properties file by adding one line:

    FlickrAPIKey=123456789012345678901234567890123456789012
    
## Edit build.gradle

    def FLICKR_API_KEY = '"' + FlickrAPIKey + '"'

    android {

        ...
        
        compileOptions {
            sourceCompatibility = 1.8
            targetCompatibility = 1.8
        }
    }
    
    android.buildTypes.each { type ->
        type.buildConfigField 'String', 'FLICKR_API_KEY', FLICKR_API_KEY
    }
    
## Read API key from code

First build your app. You should be able to read API key from code, as follows: 

```java
String apiKey = BuildConfig.FLICKR_API_KEY;
```

## Add RxJava and Retrofit dependencies

    dependencies {
    
        ...

        // RxJava
        implementation 'io.reactivex.rxjava2:rxjava:2.0.0'
        implementation 'io.reactivex.rxjava2:rxandroid:2.0.0'

        // Retrofit
        implementation 'com.squareup.retrofit2:retrofit:2.6.4'
        implementation 'com.squareup.retrofit2:adapter-rxjava2:2.6.4'
        implementation 'com.squareup.retrofit2:converter-gson:2.6.4'

    }

## Define the FlickrApi interface

```java
import com.google.gson.JsonObject;

import io.reactivex.Observable;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface FlickrApi {

    @GET("/services/rest/?method=flickr.photos.search&format=json&nojsoncallback=1")
    Observable<JsonObject> searchPhotos(@Query("api_key") String apiKey,
                                        @Query("tags") String tags,
                                        @Query("per_page") int limit);

}
```

## Add INTERNET permission to Android Manifest

```xml
<uses-permission android:name="android.permission.INTERNET" />
```

## Edit MainActivity.java (1/3)

```java

String apiKey = BuildConfig.FLICKR_API_KEY;

Retrofit retrofit = new Retrofit.Builder()
        .addConverterFactory(GsonConverterFactory.create())
        .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
        .baseUrl("https://api.flickr.com")
        .build();

FlickrApi api = retrofit.create(FlickrApi.class);

api.searchPhotos(apiKey, "star", 3)
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe();
```

## Add new method to FlickrApi

```java
@GET("/services/rest/?method=flickr.photos.getSizes&format=json&nojsoncallback=1")
Observable<JsonObject> getSizes(@Query("api_key") String apiKey,
                                @Query("photo_id") String photoId);
```

## Edit MainActivity.java (2/3)

```java
api.searchPhotos(apiKey, "star", 3)
        .map(response -> response.getAsJsonObject("photos").getAsJsonArray("photo"))
        .flatMap(Observable::fromIterable)
        .map(photo -> photo.getAsJsonObject().get("id").getAsString())
        .flatMap(id -> api.getSizes(apiKey, id))
        .map(response -> response.getAsJsonObject("sizes").getAsJsonArray("size"))
        .map(sizes -> {
            String url = "";
            for (int i = 0; i < sizes.size(); i++) {
                JsonElement element = sizes.get(i);
                String label = element.getAsJsonObject().get("label").getAsString();
                String source = element.getAsJsonObject().get("source").getAsString();
                if ("Square".equals(label)) {
                    url = source;
                    break;
                }
            }
            return url;
        })
        .doOnNext(url -> Log.d(TAG, url))
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe();
```

## Add RecyclerView and Picasso dependencies

    dependencies {
    
        ...

        implementation 'androidx.recyclerview:recyclerview:1.2.0-alpha01'
        implementation 'com.squareup.picasso:picasso:2.5.2'

    }

## Create RecyclerView Adapter

See PhotoAdapter.java

## Add RecyclerView to layout activity_main.xml

```xml
<?xml version="1.0" encoding="utf-8"?>
<androidx.constraintlayout.widget.ConstraintLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    xmlns:tools="http://schemas.android.com/tools"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    tools:context=".MainActivity">

    <androidx.recyclerview.widget.RecyclerView
        android:id="@+id/recyclerView"
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        app:layout_constraintBottom_toBottomOf="parent"
        app:layout_constraintLeft_toLeftOf="parent"
        app:layout_constraintRight_toRightOf="parent"
        app:layout_constraintTop_toTopOf="parent" />

</androidx.constraintlayout.widget.ConstraintLayout>
```

## Edit MainActivity.java (3/3)

- Create RecyclerView field
- Get reference to RecyclerView
- Add toList operator
- Extract getJsonArrayStringFunction
- Add updateUI method

```java
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
```
