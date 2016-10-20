/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package n2k.procesadoregistros;

import calibracion.*;
import calibracion.Calibracion;
import estancia.*;
import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class ProcesadorRegistros {

    /**
     * @param args the command line arguments
     */
    public static final int MIN_ESTANCIA = 5;
    public static final int MAX_ESTANCIA = 120;
    public static final int MAX_DIFF_REGISTROS = 120;
    
    public static void main(String[] args) throws IOException, ParseException {
        Path pathCalibracion = Paths.get("calibracion.csv");
        Path pathLecturas = Paths.get("lecturas.csv");
        String linea;
        String separador = ",";
        Instant instate = Instant.now();
        
        /* 
        Añado el modelo de datos de un fichero CSV
        */
        Calibracion calibracion = new Calibracion();
        try (BufferedReader reader = Files.newBufferedReader(pathCalibracion)) {
            while((linea = reader.readLine())!=null){                
                String[] lineaTokenizada = linea.split(separador);
                PuntoCalibrado punto = new PuntoCalibrado (lineaTokenizada[0],Double.parseDouble(lineaTokenizada[1]),Double.parseDouble(lineaTokenizada[2]),Double.parseDouble(lineaTokenizada[3]));
                calibracion.puntos.add(punto);
            }
        }
        
        /* 
        Añado las lecturas de otro CSV 
        */
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.GERMAN);
        ArrayList<Lectura> lecturas = new ArrayList<>();
        try (BufferedReader reader = Files.newBufferedReader(pathLecturas)) {
            while((linea = reader.readLine())!=null){                
                String[] lineaTokenizada = linea.split(separador);
                lecturas.add(new Lectura(Double.parseDouble(lineaTokenizada[3]),lineaTokenizada[2],lineaTokenizada[0],sdf.parse(lineaTokenizada[1]).toInstant().truncatedTo(ChronoUnit.MINUTES)));
            }
        }
        
        /* 
        Ordeno las lecturas 
        */
        Comparator<Lectura> comparadorLecturas = (l1, l2) -> l1.mac.compareTo(l2.mac);
        comparadorLecturas = comparadorLecturas.thenComparing((l1, l2) -> l1.timestamp.compareTo(l2.timestamp));
        comparadorLecturas = comparadorLecturas.thenComparing((l1, l2) -> l1.sensor.compareTo(l2.sensor));
        lecturas.sort(comparadorLecturas);
        //lecturas.forEach(e -> System.out.println(String.format("%s,%f,%s,%s", e.mac, e.intensidad, e.sensor, e.timestamp.toString())));
        
        
        /* 
        Paso de lecturas a registros por minuto
        */
        ArrayList<RegistroMinuto> registros = new ArrayList<>();
        ArrayList<Lectura> lecturasMinuto = new ArrayList<>();
        int s = 0;
        Iterator lista = lecturas.iterator();
        Lectura lecturaAnterior = (Lectura) lista.next();
        lecturasMinuto.add(lecturaAnterior);
        while (lista.hasNext()){
            Lectura lectura = (Lectura) lista.next();
            if (lectura.mac.equals(lecturaAnterior.mac) && lectura.timestamp.equals(lecturaAnterior.timestamp)){
                lecturasMinuto.add(lectura);
            } else {
                Map<String, Double> counting = lecturasMinuto.stream().collect(
                    Collectors.groupingBy(Lectura::getSensor, Collectors.averagingDouble(Lectura::getIntensidad)));
                
                RegistroMinuto registro = new RegistroMinuto(lecturaAnterior.timestamp,lecturaAnterior.mac);
                registro.a = counting.get("28051")==null?0:counting.get("28051");
                registro.b = counting.get("28052")==null?0:counting.get("28052");
                registro.c = counting.get("28053")==null?0:counting.get("28053");
                registros.add(registro);
                
                lecturasMinuto = new ArrayList<>();
                lecturasMinuto.add(lectura);
            }
            lecturaAnterior = lectura;
        }
        
        /* 
        Triangulo los registros
        */
        //calibracion.puntos.forEach(e -> System.out.println(String.format("%s: %f, %f, %f", e.nombre, e.a, e.b, e.c)));
        registros.forEach(e -> e.asignarPunto(calibracion));
        lecturas = null;
        
        /* 
        Ordeno los registros por minuto
        */
        Comparator<RegistroMinuto> comparadorRegistros = (l1, l2) -> l1.mac.compareTo(l2.mac);
        comparadorRegistros = comparadorRegistros.thenComparing((l1, l2) -> l1.timestamp.compareTo(l2.timestamp));
        registros.sort(comparadorRegistros);
        //registros.forEach(a -> System.out.println(String.format("%s [%s] %s: %f, %f, %f [%s]", a.mac, a.punto, a.area, a.a, a.b, a.c, a.timestamp.toString())));
        
        /* 
        Creo estancias
        */
        Iterator<RegistroMinuto> listaregistros = registros.iterator();
        ArrayList<Estancia> estancias = new ArrayList<>();
        Estancia estanciaCurrent = new Estancia (listaregistros.next());
        while (listaregistros.hasNext()){
            RegistroMinuto registro = listaregistros.next();
            if((estanciaCurrent.mac.equals(registro.mac))&&(Duration.between(estanciaCurrent.salida, registro.timestamp).toMinutes()<MAX_DIFF_REGISTROS)){
                estanciaCurrent.addRegistro(registro);
            } else {
                if((Duration.between(estanciaCurrent.entrada, estanciaCurrent.salida).toMinutes()>MIN_ESTANCIA)&&(Duration.between(estanciaCurrent.entrada, estanciaCurrent.salida).toMinutes()<MAX_ESTANCIA)){
                    estancias.add(estanciaCurrent);
                }
                estanciaCurrent = new Estancia (registro);
            }
        }
        
        estancias.forEach(e -> e.generarSubestancias());
        //estancias.forEach(a-> System.out.println(String.format("%s,%s,%s,%s,%s,%s", a.mac, a.entrada.toString(), a.punto1.toString(), a.punto2.toString(), a.punto3.toString(), a.salida.toString())));

        FileWriter writer = new FileWriter("output.csv"); 
        estancias.forEach(a-> {
            try {
                writer.write(String.format("%s;%s;%s;%s;%s;%s\n", a.mac, a.entrada.plus(2, ChronoUnit.HOURS).toString(), a.punto1.plus(2, ChronoUnit.HOURS).toString(), a.punto2.plus(2, ChronoUnit.HOURS).toString(), a.punto3.plus(2, ChronoUnit.HOURS).toString(), a.salida.plus(2, ChronoUnit.HOURS).toString()));
            } catch (IOException ex) {
                Logger.getLogger(ProcesadorRegistros.class.getName()).log(Level.SEVERE, null, ex);
            }
        });
        writer.close();
        /*
        int u = 123;
        System.out.println(estancias.get(u).mac);
        System.out.println(estancias.get(u).registros.size());
        estancias.get(u).subestancias.forEach(e -> {
            System.out.println(e.inicio.toString()+","+e.fin.toString()+","+e.area+","+e.registros.size());
            e.registros.forEach(a -> System.out.println(" - "+a.timestamp.toString()+" "+a.area+" "+a.punto));
                });*/
        System.out.println("Milisegundos:"+Duration.between(instate, Instant.now()).toMillis());
    }
    
}
