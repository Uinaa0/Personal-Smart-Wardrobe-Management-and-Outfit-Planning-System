package com.aiman.smartwardrobe.data.network;

import io.reactivex.rxjava3.core.Single;
import retrofit2.http.GET;
import retrofit2.http.Query;

/**
 * ============================================================================
 * WeatherApiService — Retrofit Network Service Definition
 * ============================================================================
 *
 * <p>Defines the network request endpoints for the weather service.
 * Room annotations generate the HTTP call execution logic at build time.</p>
 *
 * @author Aiman — Final Year Project
 * @version 1.0
 */
public interface WeatherApiService {

    /**
     * Retrieve the current weather details for a specific city.
     *
     * @param city     The city name to search (e.g. "Kuala Lumpur")
     * @param units    Measurement units (use "metric" for Celsius)
     * @param apiKey   OpenWeatherMap API Key
     * @return Single emitting the parsed WeatherResponse
     */
    @GET("weather")
    Single<WeatherResponse> getCurrentWeather(
            @Query("q") String city,
            @Query("units") String units,
            @Query("appid") String apiKey
    );

    /**
     * Retrieve current weather details by latitude and longitude.
     */
    @GET("weather")
    Single<WeatherResponse> getCurrentWeatherByCoords(
            @Query("lat") double latitude,
            @Query("lon") double longitude,
            @Query("units") String units,
            @Query("appid") String apiKey
    );
}
