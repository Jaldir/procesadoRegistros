/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package estancia;

import calibracion.Calibracion;
import java.util.ArrayList;
import java.time.Instant;
import java.time.ZoneId;

public class RegistroMinuto {
    public double a,b,c;
    public Instant timestamp;
    public String punto;
    public String area;
    public String mac;
    double minimo = Double.MAX_VALUE;

    public RegistroMinuto(Instant timestamp, String mac) {
        this.a = 0;
        this.b = 0;
        this.c = 0;
        this.timestamp = timestamp;
        this.mac = mac;
    }
    
    public void asignarPunto(Calibracion calibracion){
        calibracion.puntos.forEach(e -> {
            Double valor = Math.sqrt((e.a-a)*(e.a-a)+(e.b-b)*(e.b-b)+(e.c-c)*(e.c-c));
            if(valor<minimo){
                punto = e.nombre;
                minimo = valor;
            }
            if (punto.equals("G")||punto.equals("C")){
                area = "Esperar";
            } else {
                area = "Comer";
            }
        });
    }

    public double getA() {
        return a;
    }

    public void setA(double a) {
        this.a = a;
    }

    public double getB() {
        return b;
    }

    public void setB(double b) {
        this.b = b;
    }

    public double getC() {
        return c;
    }

    public void setC(double c) {
        this.c = c;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public String getPunto() {
        return punto;
    }

    public void setPunto(String punto) {
        this.punto = punto;
    }

    public String getArea() {
        return area;
    }

    public void setArea(String area) {
        this.area = area;
    }

    public String getMac() {
        return mac;
    }

    public void setMac(String mac) {
        this.mac = mac;
    }

    public double getMinimo() {
        return minimo;
    }

    public void setMinimo(double minimo) {
        this.minimo = minimo;
    }

    public Object getHoraTexto() {
        return timestamp.atZone(ZoneId.systemDefault()).getHour()+":"+timestamp.atZone(ZoneId.systemDefault()).getMinute();
    }
    
    
}
