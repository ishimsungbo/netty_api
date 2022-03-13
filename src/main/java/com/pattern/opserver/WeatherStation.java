package com.pattern.opserver;

public class WeatherStation {
    public static void main(String[] args) {
        // Subject
        WeatherData weatherData = new WeatherData();

        // Observer
        CurrentConditionsDisplay conditionsDisplay = new CurrentConditionsDisplay(weatherData);
        ForecastDisplay forecastDisplay = new ForecastDisplay(weatherData);


        //예) Subject 가 A 시스템이고 정보가 자동으로 수신 된다 했을때.
        //   Observer CurrentConditionsDisplay 를 구현해서 여러 갈래로 개별 비즈니스 로직을 수행할 수 있다.

        //옵저버들에게 메세지 날림
        weatherData.setMeasurements(80,30,4f);

    }
}
