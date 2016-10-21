/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package n2k.procesadoregistros;

import calibracion.Calibracion;
import calibracion.PuntoCalibrado;
import estancia.Estancia;
import estancia.Lectura;
import estancia.RegistroMinuto;
import java.awt.event.ItemEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
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
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JFileChooser;
import javax.swing.table.DefaultTableModel;

/**
 *
 * @author Jaldir
 */
public class Principal extends javax.swing.JFrame {
        Path pathCalibracion = Paths.get("calibracion.csv");
        Path pathLecturas = Paths.get("lecturas.csv");
        String outputJS = "output.js";
        String outputCSV = "output.csv";
        ArrayList<Lectura> lecturas;
        ArrayList<RegistroMinuto> registros;
        ArrayList<Estancia> estancias;
        Calibracion calibracion;
        
        String separador = ",";
        
    private void calibrar(){
        /* 
        Añado el modelo de datos de un fichero CSV
        */
        calibracion = new Calibracion();
        try (BufferedReader reader = Files.newBufferedReader(pathCalibracion)) {
            String linea;
            while((linea = reader.readLine())!=null){
                String[] lineaTokenizada = linea.split(separador);
                PuntoCalibrado punto = new PuntoCalibrado (lineaTokenizada[0],Double.parseDouble(lineaTokenizada[1]),Double.parseDouble(lineaTokenizada[2]),Double.parseDouble(lineaTokenizada[3]));
                calibracion.puntos.add(punto);
            }
        }   catch (IOException ex) {
                Logger.getLogger(Principal.class.getName()).log(Level.SEVERE, null, ex);
            }
    }
    
    private void leerLecturas(){
        /* 
        Añado las lecturas de otro CSV 
        */
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.GERMAN);
        lecturas = new ArrayList<>();
        try (BufferedReader reader = Files.newBufferedReader(pathLecturas)) {
            String linea;
            while((linea = reader.readLine())!=null){                
                String[] lineaTokenizada = linea.split(separador);
                lecturas.add(new Lectura(Double.parseDouble(lineaTokenizada[3]),lineaTokenizada[2],lineaTokenizada[0],sdf.parse(lineaTokenizada[1]).toInstant().truncatedTo(ChronoUnit.MINUTES)));
            }
        }   catch (IOException ex) {
                Logger.getLogger(Principal.class.getName()).log(Level.SEVERE, null, ex);
            } catch (ParseException ex) {
                Logger.getLogger(Principal.class.getName()).log(Level.SEVERE, null, ex);
            }
        
        /* 
        Ordeno las lecturas 
        */
        Comparator<Lectura> comparadorLecturas = (l1, l2) -> l1.mac.compareTo(l2.mac);
        comparadorLecturas = comparadorLecturas.thenComparing((l1, l2) -> l1.timestamp.compareTo(l2.timestamp));
        comparadorLecturas = comparadorLecturas.thenComparing((l1, l2) -> l1.sensor.compareTo(l2.sensor));
        lecturas.sort(comparadorLecturas);
    }
    
    private void generarRegistros(){
        registros = new ArrayList<>();
        ArrayList<Lectura> lecturasMinuto = new ArrayList<>();
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
        registros.forEach(e -> e.asignarPunto(calibracion));
        lecturas = null;
        
        /* 
        Ordeno los registros por minuto
        */
        Comparator<RegistroMinuto> comparadorRegistros = (l1, l2) -> l1.mac.compareTo(l2.mac);
        comparadorRegistros = comparadorRegistros.thenComparing((l1, l2) -> l1.timestamp.compareTo(l2.timestamp));
        registros.sort(comparadorRegistros);
    }
    
    private void generarEstancias(){
        /* 
        Creo estancias
        */
        Iterator<RegistroMinuto> listaregistros = registros.iterator();
        estancias = new ArrayList<>();
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
    }
    
    private void escribirCSV() throws IOException{
        FileWriter CSVwriter = new FileWriter(outputCSV); 
        estancias.forEach(a-> {
            try {
                CSVwriter.write(String.format("%s;%s;%s;%s;%s;%s\n", a.mac, a.entrada.plus(2, ChronoUnit.HOURS).toString(), a.punto1.plus(2, ChronoUnit.HOURS).toString(), a.punto2.plus(2, ChronoUnit.HOURS).toString(), a.punto3.plus(2, ChronoUnit.HOURS).toString(), a.salida.plus(2, ChronoUnit.HOURS).toString()));
            } catch (IOException ex) {
                Logger.getLogger(Principal.class.getName()).log(Level.SEVERE, null, ex);
            }
        });
        CSVwriter.close();
    }
    
    private void escribirJS() throws IOException{
        Map<Integer, Long> horasEntrada = estancias.stream().collect(
                    Collectors.groupingBy(Estancia::getHoraEntrada, Collectors.counting()));
        Map<Integer, Long> horasSalida = estancias.stream().collect(
                    Collectors.groupingBy(Estancia::getHoraSalida, Collectors.counting()));
        
        FileWriter JSwriter = new FileWriter(outputJS);
        JSwriter.write("var datos = [");
        for (int i = 0; i < 23; i++) {
            JSwriter.write(String.format("[%d,%d,%d],",i,horasEntrada.get(i), horasSalida.get(i)));
        }
        JSwriter.write("];");
        JSwriter.close();
    }
    
    public Principal() throws IOException, ParseException {
        initComponents();
        calibrar();
        leerLecturas();
        generarRegistros();
        generarEstancias();
        escribirCSV();
        escribirJS();
        
        selectorEstancia.setModel(new DefaultComboBoxModel(estancias.toArray()));
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel3 = new javax.swing.JPanel();
        selectorEstancia = new javax.swing.JComboBox<>();
        jLabel1 = new javax.swing.JLabel();
        panelTablas = new javax.swing.JPanel();
        jPanel4 = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        tablaRegistros = new javax.swing.JTable();
        jLabel2 = new javax.swing.JLabel();
        jPanel1 = new javax.swing.JPanel();
        jLabel3 = new javax.swing.JLabel();
        jScrollPane2 = new javax.swing.JScrollPane();
        tablaSubEstancias = new javax.swing.JTable();
        jMenuBar1 = new javax.swing.JMenuBar();
        menuFicheros = new javax.swing.JMenu();
        botonCalibrar = new javax.swing.JMenuItem();
        botonLeer = new javax.swing.JMenuItem();
        jMenu2 = new javax.swing.JMenu();
        botonProcesar = new javax.swing.JMenuItem();
        botonGenerarCSV = new javax.swing.JMenuItem();
        botonGenerarJSON = new javax.swing.JMenuItem();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("Procesador de Registros");
        setMinimumSize(new java.awt.Dimension(700, 700));

        selectorEstancia.setModel(new javax.swing.DefaultComboBoxModel<>());
        selectorEstancia.setToolTipText("");
        selectorEstancia.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        selectorEstancia.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                selectorEstanciaItemStateChanged(evt);
            }
        });

        jLabel1.setText("Seleccionar estancia");

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(selectorEstancia, 0, 213, Short.MAX_VALUE)
                .addContainerGap())
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(selectorEstancia, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel1))
                .addContainerGap())
        );

        tablaRegistros.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
            },
            new String [] {
                "Hora", "Lectura 1", "Lectura 2", "Lectura 3" , "Punto"
            }
        ));
        jScrollPane1.setViewportView(tablaRegistros);

        jLabel2.setText("Registros");

        jLabel3.setText("SubEstancias");

        tablaSubEstancias.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {},
            new String [] {
                "Area", "Inicio", "Fin", "Duración"
            }
        ));
        jScrollPane2.setViewportView(tablaSubEstancias);

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel3)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jScrollPane2)
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGap(30, 30, 30)
                        .addComponent(jLabel3))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 154, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout jPanel4Layout = new javax.swing.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel2)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 27, Short.MAX_VALUE)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 516, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
            .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel2)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 270, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout panelTablasLayout = new javax.swing.GroupLayout(panelTablas);
        panelTablas.setLayout(panelTablasLayout);
        panelTablasLayout.setHorizontalGroup(
            panelTablasLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelTablasLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(28, 28, 28))
        );
        panelTablasLayout.setVerticalGroup(
            panelTablasLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelTablasLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel4, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );

        menuFicheros.setText("Ficheros");

        botonCalibrar.setText("Seleccionar calibración");
        botonCalibrar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                botonCalibrarActionPerformed(evt);
            }
        });
        menuFicheros.add(botonCalibrar);

        botonLeer.setText("Seleccionar lecturas");
        botonLeer.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                botonLeerActionPerformed(evt);
            }
        });
        menuFicheros.add(botonLeer);

        jMenuBar1.add(menuFicheros);

        jMenu2.setText("Acciones");

        botonProcesar.setText("Procesar");
        botonProcesar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                botonProcesarActionPerformed(evt);
            }
        });
        jMenu2.add(botonProcesar);

        botonGenerarCSV.setText("Generar CSV");
        botonGenerarCSV.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                botonGenerarCSVActionPerformed(evt);
            }
        });
        jMenu2.add(botonGenerarCSV);

        botonGenerarJSON.setText("Generar JSON");
        botonGenerarJSON.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                botonGenerarJSONActionPerformed(evt);
            }
        });
        jMenu2.add(botonGenerarJSON);

        jMenuBar1.add(jMenu2);

        setJMenuBar(jMenuBar1);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(panelTablas, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 9, Short.MAX_VALUE))
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap(124, Short.MAX_VALUE)
                .addComponent(jPanel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(panelTablas, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void selectorEstanciaItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_selectorEstanciaItemStateChanged
       if (evt.getStateChange() == ItemEvent.SELECTED) {
        Estancia item = (Estancia) evt.getItem();
        DefaultTableModel dtm = (DefaultTableModel) tablaRegistros.getModel();
        dtm.setRowCount(0);
        item.registros.forEach(e -> dtm.addRow(new Object [] {e.getHoraTexto(),e.a,e.b,e.c,e.punto}));
        DefaultTableModel tm = (DefaultTableModel) tablaSubEstancias.getModel();
        tm.setRowCount(0);
        item.subestancias.forEach(e -> tm.addRow(new Object [] {e.area,e.getHoraInicio(),e.getHoraFin(),e.duracion}));
       }
    }//GEN-LAST:event_selectorEstanciaItemStateChanged

    private void botonCalibrarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_botonCalibrarActionPerformed
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setCurrentDirectory(new File(System.getProperty("user.home")));
        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            pathCalibracion = Paths.get(selectedFile.getAbsolutePath());
        }
        calibrar();
    }//GEN-LAST:event_botonCalibrarActionPerformed

    private void botonLeerActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_botonLeerActionPerformed
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setCurrentDirectory(new File(System.getProperty("user.home")));
        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            pathLecturas = Paths.get(selectedFile.getAbsolutePath());
        }
        leerLecturas();
    }//GEN-LAST:event_botonLeerActionPerformed

    private void botonProcesarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_botonProcesarActionPerformed
        generarRegistros();
        generarEstancias();
        selectorEstancia.setModel(new DefaultComboBoxModel(estancias.toArray()));
    }//GEN-LAST:event_botonProcesarActionPerformed

    private void botonGenerarCSVActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_botonGenerarCSVActionPerformed
            try {
                escribirCSV();
            } catch (IOException ex) {
                Logger.getLogger(Principal.class.getName()).log(Level.SEVERE, null, ex);
            }
    }//GEN-LAST:event_botonGenerarCSVActionPerformed

    private void botonGenerarJSONActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_botonGenerarJSONActionPerformed
            try {
                escribirJS();
            } catch (IOException ex) {
                Logger.getLogger(Principal.class.getName()).log(Level.SEVERE, null, ex);
            }
    }//GEN-LAST:event_botonGenerarJSONActionPerformed
    
    public static final int MIN_ESTANCIA = 5;
    public static final int MAX_ESTANCIA = 120;
    public static final int MAX_DIFF_REGISTROS = 120;
    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) throws IOException, ParseException {
  
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(Principal.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(Principal.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(Principal.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(Principal.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */

        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                try {
                    new Principal().setVisible(true);
                } catch (IOException ex) {
                    Logger.getLogger(Principal.class.getName()).log(Level.SEVERE, null, ex);
                } catch (ParseException ex) {
                    Logger.getLogger(Principal.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JMenuItem botonCalibrar;
    private javax.swing.JMenuItem botonGenerarCSV;
    private javax.swing.JMenuItem botonGenerarJSON;
    private javax.swing.JMenuItem botonLeer;
    private javax.swing.JMenuItem botonProcesar;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JMenu jMenu2;
    private javax.swing.JMenuBar jMenuBar1;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JMenu menuFicheros;
    private javax.swing.JPanel panelTablas;
    public javax.swing.JComboBox<String> selectorEstancia;
    private javax.swing.JTable tablaRegistros;
    private javax.swing.JTable tablaSubEstancias;
    // End of variables declaration//GEN-END:variables

}
