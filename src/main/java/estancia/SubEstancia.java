/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package estancia;

import java.time.Duration;
import java.util.ArrayList;
import java.time.Instant;
import java.time.ZoneId;

/**
 *
 * @author Jaldir
 */
public class SubEstancia {
    public long duracion;
    public Instant inicio, fin;
    public ArrayList<RegistroMinuto> registros;
    public String area;

    public SubEstancia(RegistroMinuto registro) {
        duracion = 0;
        registros = new ArrayList<>();
        registros.add(registro);
        inicio = registro.timestamp;
        fin = registro.timestamp;
        area = registro.area;
    }
    public SubEstancia(Instant inicio, String area) {
        duracion = 0;
        registros = new ArrayList<>();
        this.inicio = inicio;
        this.fin = inicio;
        this.area = area;
    }
    
    public void addRegistro(RegistroMinuto registro){
        registros.add(registro);
        if (registro.timestamp.isAfter(fin)){
            fin = registro.timestamp;
            duracion = Duration.between(inicio, fin).toMinutes();
        }
    }
    
    public Object getHoraInicio() {
        return inicio.atZone(ZoneId.systemDefault()).getHour()+":"+inicio.atZone(ZoneId.systemDefault()).getMinute();
    }
    public Object getHoraFin() {
        return fin.atZone(ZoneId.systemDefault()).getHour()+":"+fin.atZone(ZoneId.systemDefault()).getMinute();
    }
}
