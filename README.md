# android-flickr-client

## Create or edit ~/.gradle/gradle.properties

    FlickrAPIKey=123456789012345678901234567890123456789012

## Edit build.gradle

    def FLICKR_API_KEY = '"' + FlickrAPIKey + '"'

    android {
        ...
    }
    
    android.buildTypes.each { type ->
        type.buildConfigField 'String', 'FLICKR_API_KEY', FLICKR_API_KEY
    }
    
## Add dependencies

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
## Define FlickrApi

```java
@GET("/services/rest/?method=flickr.photos.search&format=json&nojsoncallback=1")
Observable<JsonObject> searchPhotos(@Query("api_key") String apiKey,
                                    @Query("tags") String tags,
                                    @Query("per_page") int limit);
```

## Add INTERNET permission

```xml
<uses-permission android:name="android.permission.INTERNET" />
```

## Edit MainActivity.java

```java
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
