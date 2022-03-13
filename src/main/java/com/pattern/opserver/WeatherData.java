package com.pattern.opserver;

import java.util.ArrayList;

public class WeatherData implements Subject {

    private ArrayList<Observer> observerArrayList;
    private float temp;
    private float humidity;
    private float pressure;

    public WeatherData() {
        this.observerArrayList = new ArrayList<>();
    }

    @Override
    public void registerObserver(Observer o) {
        observerArrayList.add(o);
    }

    @Override
    public void removeObserver(Observer o) {
        int i = observerArrayList.indexOf(o);
        if(i >= 0){
            observerArrayList.remove(o);
        }
    }

    @Override
    public void notifyObserver() {
        for(int i=0;i<observerArrayList.size();i++){
            Observer observer = observerArrayList.get(i);
            observer.update(temp,humidity,pressure);
        }
    }

    public void measurementsChange(){
        notifyObserver();
    }

    public void setMeasurements(float temp, float humidity, float pressure){
        this.temp = temp;
        this.humidity = humidity;
        this.pressure = pressure;
        measurementsChange();
    }

}
