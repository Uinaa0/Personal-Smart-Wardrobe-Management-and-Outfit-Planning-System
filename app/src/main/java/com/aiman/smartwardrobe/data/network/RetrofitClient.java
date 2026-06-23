package com.aiman.smartwardrobe.data.network;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava3.RxJava3CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * ============================================================================
 * RetrofitClient — OkHttp + Retrofit Network Client Singleton
 * ============================================================================
 *
 * <p>Builds the Retrofit instance to communicate with the OpenWeatherMap API.
 * Employs OkHttp timeouts and HTTP logging for easy network debugging.</p>
 *
 * @author Aiman — Final Year Project
 * @version 1.0
 */
public class RetrofitClient {

    private static final String BASE_URL = "https://api.openweathermap.org/data/2.5/";
    private static volatile RetrofitClient instance;
    private final WeatherApiService weatherApiService;

    private RetrofitClient() {
        // OkHttp logging interceptor to log HTTP requests and responses in Logcat
        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .addInterceptor(loggingInterceptor)
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .addCallAdapterFactory(RxJava3CallAdapterFactory.create())
                .build();

        weatherApiService = retrofit.create(WeatherApiService.class);
    }

    public static RetrofitClient getInstance() {
        if (instance == null) {
            synchronized (RetrofitClient.class) {
                if (instance == null) {
                    instance = new RetrofitClient();
                }
            }
        }
        return instance;
    }

    public WeatherApiService getWeatherApiService() {
        return weatherApiService;
    }
}
