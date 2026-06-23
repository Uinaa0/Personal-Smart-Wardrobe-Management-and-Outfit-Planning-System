package com.aiman.smartwardrobe.data.network;

import com.google.gson.annotations.SerializedName;
import java.util.List;

/**
 * ============================================================================
 * WeatherResponse — GSON Data Model for OpenWeatherMap Current Weather API
 * ============================================================================
 *
 * <p>Represents the JSON response structure returned by:
 * {@code https://api.openweathermap.org/data/2.5/weather}</p>
 *
 * @author Aiman — Final Year Project
 * @version 1.0
 */
public class WeatherResponse {

    @SerializedName("main")
    private MainData main;

    @SerializedName("weather")
    private List<WeatherDescription> weather;

    @SerializedName("name")
    private String cityName;

    public MainData getMain() {
        return main;
    }

    public void setMain(MainData main) {
        this.main = main;
    }

    public List<WeatherDescription> getWeather() {
        return weather;
    }

    public void setWeather(List<WeatherDescription> weather) {
        this.weather = weather;
    }

    public String getCityName() {
        return cityName;
    }

    public void setCityName(String cityName) {
        this.cityName = cityName;
    }

    /**
     * Inner class mapping the "main" JSON object.
     */
    public static class MainData {
        @SerializedName("temp")
        private double temp;

        @SerializedName("feels_like")
        private double feelsLike;

        @SerializedName("humidity")
        private int humidity;

        public double getTemp() {
            return temp;
        }

        public void setTemp(double temp) {
            this.temp = temp;
        }

        public double getFeelsLike() {
            return feelsLike;
        }

        public void setFeelsLike(double feelsLike) {
            this.feelsLike = feelsLike;
        }

        public int getHumidity() {
            return humidity;
        }

        public void setHumidity(int humidity) {
            this.humidity = humidity;
        }
    }

    /**
     * Inner class mapping the elements of the "weather" JSON array.
     */
    public static class WeatherDescription {
        @SerializedName("main")
        private String main;

        @SerializedName("description")
        private String description;

        @SerializedName("icon")
        private String icon;

        public String getMain() {
            return main;
        }

        public void setMain(String main) {
            this.main = main;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getIcon() {
            return icon;
        }

        public void setIcon(String icon) {
            this.icon = icon;
        }
    }
}
