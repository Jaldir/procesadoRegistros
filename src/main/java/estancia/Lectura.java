/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package estancia;

import java.util.Comparator;
import java.time.Instant;

public class Lectura {
    public double intensidad;
    public String sensor;
    public String mac;
    public Instant timestamp;

    public Lectura(double intensidad, String mac, String sensor, Instant timestamp) {
        this.intensidad = intensidad;
        this.sensor = sensor;
        this.mac = mac;
        this.timestamp = timestamp;
    }

    public double getIntensidad() {
        return intensidad;
    }

    public void setIntensidad(double intensidad) {
        this.intensidad = intensidad;
    }

    public String getSensor() {
        return sensor;
    }

    public void setSensor(String sensor) {
        this.sensor = sensor;
    }

    public String getMac() {
        return mac;
    }

    public void setMac(String mac) {
        this.mac = mac;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

}

