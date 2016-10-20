/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package calibracion;

/**
 *
 * @author Jaldir
 */
public class PuntoCalibrado {
    public double a,b,c;
    public String nombre;

    public PuntoCalibrado(String nombre, double a, double b, double c) {
        this.a = a;
        this.b = b;
        this.c = c;
        this.nombre = nombre;
    }
    
    
}
