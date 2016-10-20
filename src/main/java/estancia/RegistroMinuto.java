/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package estancia;

import calibracion.Calibracion;
import java.util.ArrayList;
import java.time.Instant;

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
}
