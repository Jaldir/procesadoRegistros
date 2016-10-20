/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package estancia;

import java.util.ArrayList;
import java.time.Instant;
import java.util.Iterator;

public class Estancia {
    public Instant entrada, salida;
    public Instant punto1, punto2, punto3;
    public String mac;
    public ArrayList<SubEstancia> subestancias;
    public ArrayList<RegistroMinuto> registros;
    public final int MAX_DIFERENTES = 2;

    public Estancia(RegistroMinuto registro) {
        this.entrada = registro.timestamp;
        this.salida = registro.timestamp;
        this.mac = registro.mac;
        this.registros = new ArrayList<>();
        addRegistro(registro);
    }
    
    public void addRegistro(RegistroMinuto registro){
        registros.add(registro);
        salida = registro.timestamp;
    }
    
    public void generarSubestancias(){
        subestancias = new ArrayList<>();
        registros.sort((l1, l2) -> l1.timestamp.compareTo(l2.timestamp));
        
        Iterator<RegistroMinuto> iterador = registros.iterator();
        SubEstancia subestancia = new SubEstancia(entrada,"Esperar");
        subestancias.add(subestancia);
        ArrayList<RegistroMinuto> buffer = new ArrayList<>();
        String estado = "Esperar";
        int continuar = 0;
        
        while (iterador.hasNext()){
            RegistroMinuto registro = iterador.next();
            if (registro.area.equals(estado)) {
                if (continuar!=0){
                    Iterator<RegistroMinuto> bufferiterator = buffer.iterator();
                    while (bufferiterator.hasNext()){
                        subestancia.addRegistro(bufferiterator.next());
                    }
                    buffer = new ArrayList<>();
                    continuar = 0;
                }
                subestancia.addRegistro(registro);
            } else if (continuar >= MAX_DIFERENTES){
                
                if (estado.equals("Esperar")){
                    estado = "Comer";
                } else {
                    estado = "Esperar";
                }
                subestancia = new SubEstancia (buffer.get(0));
                Iterator<RegistroMinuto> bufferiterator = buffer.iterator();
                while (bufferiterator.hasNext()){
                    subestancia.addRegistro(bufferiterator.next());
                }
                subestancia.addRegistro(registro);
                buffer = new ArrayList<>();
                continuar = 0;
                subestancias.add(subestancia);
            } else {
                continuar = continuar+1;
                buffer.add(registro);
            }
        }
        if (continuar!=0){
            Iterator<RegistroMinuto> bufferiterator = buffer.iterator();
            while (bufferiterator.hasNext()){
                subestancia.addRegistro(bufferiterator.next());
            }
            continuar = 0;
        }
        
        while(subestancias.size()<4){
            if (estado.equals("Esperar")){
                    estado = "Comer";
                } else {
                    estado = "Esperar";
            }
            subestancias.add(new SubEstancia(salida,estado));
        }
        punto1 = subestancias.get(0).fin;
        punto2 = subestancias.get(1).fin;
        punto3 = subestancias.get(2).fin;
    }
}
