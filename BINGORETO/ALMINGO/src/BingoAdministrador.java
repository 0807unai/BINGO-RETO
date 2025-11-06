import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.List;

public class BingoAdministrador extends JFrame {
    private JLabel numeroActualLabel;
    private JTextArea numerosLlamadosArea;
    private JButton sacarNumeroBtn;
    private JButton reiniciarBtn;
    private JLabel estadoServidor;
    
    private List<Integer> numerosDisponibles;
    private List<Integer> numerosLlamados;
    
    private ServerSocket serverSocket;
    private List<ClientHandler> clientes;
    private Thread serverThread;
    
    private static final String ARCHIVO_NUMEROS = "num_bingo.txt";
    
    private List<PreguntaSostenibilidad> preguntasLinea;
    private List<PreguntaSostenibilidad> preguntasBingo;
    
    public BingoAdministrador() {
        setTitle("Administrador de Bingo - Sostenibilidad");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        getContentPane().setLayout(new BorderLayout(10, 10));
        getContentPane().setBackground(new Color(34, 139, 34));
        
        clientes = new ArrayList<>();
        inicializarPreguntas();
        cargarNumerosDesdeArchivo();
        inicializarNumeros();
        crearInterfaz();
        iniciarServidor();
        
        setSize(650, 600);
        setLocationRelativeTo(null);
        setVisible(true);
    }
    
    private void inicializarPreguntas() {
    	preguntasLinea = new ArrayList<>();
    	preguntasLinea.add(new PreguntaSostenibilidad(
    			"¿Que es el termino GEI?",
                new String[]{"Gases de Efecto Invernadero","Gases Esenciales e Infinitos", "Gases Especiales Internacionales", "Ninguna de las anteriores"},
                0
        )); 
    	
        
        preguntasLinea.add(new PreguntaSostenibilidad(
            "¿Cuál es una de las 3 R de la sostenibilidad?",
            new String[]{"Reducir", "Reciclar", "Reutilizar", "Todas las anteriores"},
            3
        ));
        preguntasLinea.add(new PreguntaSostenibilidad(
            "¿Qué tipo de energía es renovable?",
            new String[]{"Carbón", "Petróleo", "Solar", "Gas natural"},
            2
        ));
        preguntasLinea.add(new PreguntaSostenibilidad(
            "¿Cuánto tiempo tarda en degradarse una bolsa de plástico?",
            new String[]{"1 año", "10 años", "100 años", "Más de 400 años"},
            3
        ));
        preguntasLinea.add(new PreguntaSostenibilidad(
            "¿Qué significa desarrollo sostenible?",
            new String[]{"Desarrollo rápido", "Desarrollo económico", "Satisfacer necesidades sin comprometer el futuro", "Desarrollo tecnológico"},
            2
        ));
        preguntasLinea.add(new PreguntaSostenibilidad(
            "¿Cuál es el principal gas de efecto invernadero?",
            new String[]{"Oxígeno", "Nitrógeno", "Dióxido de carbono", "Hidrógeno"},
            2
        ));
        
        preguntasLinea.add(new PreguntaSostenibilidad(
        		"¿Cuál es el principal gas de efecto invernadero?",
        		new String[]{"Oxígeno", "Nitrógeno", "Dióxido de carbono", "Hidrógeno"},
        		2
        )); 
        
        preguntasBingo = new ArrayList<>();
        preguntasBingo.add(new PreguntaSostenibilidad(
            "¿Qué porcentaje del agua de la Tierra es dulce y accesible?",
            new String[]{"50%", "25%", "10%", "Menos del 1%"},
            3
        ));
        preguntasBingo.add(new PreguntaSostenibilidad(
            "¿Cuál es la principal causa del cambio climático?",
            new String[]{"Volcanes", "Emisiones humanas de CO2", "El Sol", "Terremotos"},
            1
        ));
        preguntasBingo.add(new PreguntaSostenibilidad(
            "¿Qué es la huella de carbono?",
            new String[]{"Pisada en el suelo", "Emisiones de CO2 generadas", "Un tipo de planta", "Una medida de distancia"},
            1
        ));
        preguntasBingo.add(new PreguntaSostenibilidad(
            "¿Cuántos árboles se necesitan para compensar 1 tonelada de CO2 al año?",
            new String[]{"5 árboles", "20 árboles", "50 árboles", "100 árboles"},
            2
        ));
        preguntasBingo.add(new PreguntaSostenibilidad(
            "¿Qué océano tiene más plástico acumulado?",
            new String[]{"Atlántico", "Índico", "Ártico", "Pacífico"},
            3
        ));
    }
    
    private void cargarNumerosDesdeArchivo() {
        numerosLlamados = new ArrayList<>();
        File archivo = new File(ARCHIVO_NUMEROS);
        if (archivo.exists()) {
            try (BufferedReader br = new BufferedReader(new FileReader(archivo))) {
                String linea = br.readLine();
                if (linea != null && !linea.trim().isEmpty()) {
                    String[] numeros = linea.split(",");
                    for (String num : numeros) {
                        try {
                            int numero = Integer.parseInt(num.trim());
                            if (!numerosLlamados.contains(numero)) {
                                numerosLlamados.add(numero);
                            }
                        } catch (NumberFormatException e) {
                            // Ignorar
                        }
                    }
                    System.out.println("Números cargados desde " + ARCHIVO_NUMEROS + ": " + numerosLlamados);
                }
            } catch (IOException e) {
                System.out.println("No se pudo cargar " + ARCHIVO_NUMEROS);
            }
        } else {
            System.out.println("Archivo " + ARCHIVO_NUMEROS + " no existe, se creará al sacar números");
        }
    }
    
    private void inicializarNumeros() {
        numerosDisponibles = new ArrayList<>();
        for (int i = 1; i <= 90; i++) {
            if (!numerosLlamados.contains(i)) {
                numerosDisponibles.add(i);
            }
        }
        Collections.shuffle(numerosDisponibles);
    }
    
    private void guardarNumerosEnArchivo() {
        try (PrintWriter pw = new PrintWriter(new FileWriter(ARCHIVO_NUMEROS))) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < numerosLlamados.size(); i++) {
                sb.append(numerosLlamados.get(i));
                if (i < numerosLlamados.size() - 1) {
                    sb.append(",");
                }
            }
            pw.print(sb.toString());
            pw.flush();
            System.out.println("Guardado en " + ARCHIVO_NUMEROS + ": " + sb.toString());
        } catch (IOException e) {
            System.err.println("Error al guardar en " + ARCHIVO_NUMEROS + ": " + e.getMessage());
            JOptionPane.showMessageDialog(this,
                "Error al guardar números en el archivo",
                "Error",
                JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void limpiarArchivo() {
        try (PrintWriter pw = new PrintWriter(new FileWriter(ARCHIVO_NUMEROS))) {
            pw.print("");
            pw.flush();
            System.out.println("Archivo " + ARCHIVO_NUMEROS + " limpiado");
        } catch (IOException e) {
            System.err.println("Error al limpiar " + ARCHIVO_NUMEROS);
        }
    }
    
    private void iniciarServidor() {
        serverThread = new Thread(() -> {
            try {
                serverSocket = new ServerSocket(12345);
                SwingUtilities.invokeLater(() -> 
                    estadoServidor.setText("✓ Servidor activo - Puerto 12345 - Esperando jugadores..."));
                
                while (!serverSocket.isClosed()) {
                    try {
                        Socket clientSocket = serverSocket.accept();
                        ClientHandler cliente = new ClientHandler(clientSocket);
                        clientes.add(cliente);
                        new Thread(cliente).start();
                        
                        cliente.enviarNumerosLlamados(numerosLlamados);
                        
                        SwingUtilities.invokeLater(() -> 
                            estadoServidor.setText("✓ Servidor activo - Jugadores conectados: " + clientes.size()));
                    } catch (IOException e) {
                        if (!serverSocket.isClosed()) {
                            e.printStackTrace();
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        serverThread.start();
    }
    
    private void crearInterfaz() {
        JPanel panelSuperior = new JPanel();
        panelSuperior.setBackground(new Color(34, 139, 34));
        panelSuperior.setLayout(new BorderLayout());
        
        JLabel tituloLabel = new JLabel("ALMINGO ", SwingConstants.CENTER);
        tituloLabel.setFont(new Font("Arial", Font.BOLD, 20));
        tituloLabel.setForeground(Color.WHITE);
        tituloLabel.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
        
        numeroActualLabel = new JLabel("--", SwingConstants.CENTER);
        numeroActualLabel.setFont(new Font("Arial", Font.BOLD, 120));
        numeroActualLabel.setForeground(new Color(255, 128, 0));
        numeroActualLabel.setBorder(BorderFactory.createLineBorder(Color.WHITE, 3));
        numeroActualLabel.setOpaque(true);
        numeroActualLabel.setBackground(new Color(0, 100, 0));
        
        panelSuperior.add(tituloLabel, BorderLayout.NORTH);
        panelSuperior.add(numeroActualLabel, BorderLayout.CENTER);
        
        JPanel panelCentral = new JPanel(new BorderLayout());
        panelCentral.setBackground(new Color(34, 139, 34));
        
        JLabel historialLabel = new JLabel("Numeros recientes:", SwingConstants.LEFT);
        historialLabel.setFont(new Font("Arial", Font.BOLD, 14));
        historialLabel.setForeground(Color.WHITE);
        historialLabel.setBorder(BorderFactory.createEmptyBorder(10, 10, 5, 10));
        
        numerosLlamadosArea = new JTextArea();
        numerosLlamadosArea.setEditable(false);
        numerosLlamadosArea.setFont(new Font("Arial", Font.PLAIN, 18));
        numerosLlamadosArea.setLineWrap(true);
        numerosLlamadosArea.setWrapStyleWord(true);
        numerosLlamadosArea.setBackground(new Color(255, 255, 255));
        
        JScrollPane scrollPane = new JScrollPane(numerosLlamadosArea);
        scrollPane.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
        
        panelCentral.add(historialLabel, BorderLayout.NORTH);
        panelCentral.add(scrollPane, BorderLayout.CENTER);
        
        // Actualizar historial si hay números cargados
        if (!numerosLlamados.isEmpty()) {
            actualizarHistorial();
            if (!numerosLlamados.isEmpty()) {
                numeroActualLabel.setText(String.valueOf(numerosLlamados.get(numerosLlamados.size() - 1)));
            }
        }
        
        JPanel panelInferior = new JPanel(new BorderLayout());
        panelInferior.setBackground(new Color(34, 139, 34));
        
        estadoServidor = new JLabel("Iniciando servidor...", SwingConstants.CENTER);
        estadoServidor.setFont(new Font("Arial", Font.PLAIN, 12));
        estadoServidor.setForeground(Color.WHITE);
        estadoServidor.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        
        JPanel panelBotones = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
        panelBotones.setBackground(new Color(34, 139, 34));
        
        sacarNumeroBtn = new JButton("SACAR NÚMERO");
        sacarNumeroBtn.setFont(new Font("Arial", Font.BOLD, 18));
        sacarNumeroBtn.setPreferredSize(new Dimension(220, 50));
        sacarNumeroBtn.setBackground(new Color(255, 215, 0));
        sacarNumeroBtn.setFocusPainted(false);
        sacarNumeroBtn.addActionListener(e -> sacarNumero());
        
        reiniciarBtn = new JButton("REINICIAR");
        reiniciarBtn.setFont(new Font("Arial", Font.BOLD, 18));
        reiniciarBtn.setPreferredSize(new Dimension(170, 50));
        reiniciarBtn.setBackground(new Color(220, 20, 60));
        reiniciarBtn.setForeground(Color.WHITE);
        reiniciarBtn.setFocusPainted(false);
        reiniciarBtn.addActionListener(e -> reiniciar());
        
        panelBotones.add(sacarNumeroBtn);
        panelBotones.add(reiniciarBtn);
        
        panelInferior.add(estadoServidor, BorderLayout.NORTH);
        panelInferior.add(panelBotones, BorderLayout.CENTER);
        
        getContentPane().add(panelSuperior, BorderLayout.NORTH);
        getContentPane().add(panelCentral, BorderLayout.CENTER);
        getContentPane().add(panelInferior, BorderLayout.SOUTH);
    }
    
    private void sacarNumero() {
        if (numerosDisponibles.isEmpty()) {
            JOptionPane.showMessageDialog(this, 
                "¡Todos los números han sido llamados!", 
                "Bingo Finalizado", 
                JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        
        int numero = numerosDisponibles.remove(0);
        numerosLlamados.add(numero);
        
        numeroActualLabel.setText(String.valueOf(numero));
        actualizarHistorial();
        
        guardarNumerosEnArchivo();
        enviarNumeroATodos(numero);
        
        if (numerosDisponibles.isEmpty()) {
            sacarNumeroBtn.setEnabled(false);
        }
    }
    
    private void actualizarHistorial() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < numerosLlamados.size(); i++) {
            sb.append(numerosLlamados.get(i));
            if (i < numerosLlamados.size() - 1) {
                sb.append(", ");
            }
        }
        numerosLlamadosArea.setText(sb.toString());
        numerosLlamadosArea.setCaretPosition(numerosLlamadosArea.getDocument().getLength());
    }
    
    private void enviarNumeroATodos(int numero) {
        for (ClientHandler cliente : clientes) {
            cliente.enviarNumero(numero);
        }
    }
    
    private void reiniciar() {
        int confirmacion = JOptionPane.showConfirmDialog(this,
            "¿Estás seguro de reiniciar el bingo?\nEsto limpiará num_bingo.txt y reiniciará todos los jugadores.",
            "Confirmar reinicio",
            JOptionPane.YES_NO_OPTION);
        
        if (confirmacion == JOptionPane.YES_OPTION) {
            numerosLlamados.clear();
            inicializarNumeros();
            numeroActualLabel.setText("--");
            numerosLlamadosArea.setText("");
            sacarNumeroBtn.setEnabled(true);
            
            limpiarArchivo();
            
            for (ClientHandler cliente : clientes) {
                cliente.enviarReinicio();
            }
        }
    }
    
    class ClientHandler implements Runnable {
        private Socket socket;
        private PrintWriter out;
        private BufferedReader in;
        
        public ClientHandler(Socket socket) {
            this.socket = socket;
            try {
                out = new PrintWriter(socket.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        
        public void enviarNumero(int numero) {
            if (out != null) {
                out.println("NUMERO:" + numero);
            }
        }
        
        public void enviarNumerosLlamados(List<Integer> numeros) {
            if (out != null && !numeros.isEmpty()) {
                StringBuilder sb = new StringBuilder("HISTORIAL:");
                for (int i = 0; i < numeros.size(); i++) {
                    sb.append(numeros.get(i));
                    if (i < numeros.size() - 1) {
                        sb.append(",");
                    }
                }
                out.println(sb.toString());
            }
        }
        
        public void enviarReinicio() {
            if (out != null) {
                out.println("REINICIAR");
            }
        }
        
        public void enviarPregunta(PreguntaSostenibilidad pregunta, boolean esLinea) {
            if (out != null) {
                String tipo = esLinea ? "LINEA" : "BINGO";
                out.println("PREGUNTA:" + tipo + "|" + pregunta.toString());
            }
        }
        
        @Override
        public void run() {
            try {
                String mensaje;
                while ((mensaje = in.readLine()) != null) {
                    if (mensaje.startsWith("VERIFICAR_LINEA:")) {
                        String[] numeros = mensaje.substring(16).split(",");
                        boolean valido = verificarNumerosConArchivo(numeros);
                        if (valido) {
                            PreguntaSostenibilidad pregunta = preguntasLinea.get(
                                new Random().nextInt(preguntasLinea.size())
                            );
                            enviarPregunta(pregunta, true);
                        } else {
                            out.println("VERIFICACION:false");
                        }
                    } else if (mensaje.startsWith("VERIFICAR_BINGO:")) {
                        String[] numeros = mensaje.substring(16).split(",");
                        boolean valido = verificarNumerosConArchivo(numeros);
                        if (valido) {
                            PreguntaSostenibilidad pregunta = preguntasBingo.get(
                                new Random().nextInt(preguntasBingo.size())
                            );
                            enviarPregunta(pregunta, false);
                        } else {
                            out.println("VERIFICACION:false");
                        }
                    } else if (mensaje.startsWith("RESPUESTA:")) {
                        String[] partes = mensaje.substring(10).split(":");
                        boolean correcto = partes[0].equals("true");
                        out.println("RESULTADO:" + correcto);
                    }
                }
            } catch (IOException e) {
                clientes.remove(this);
                SwingUtilities.invokeLater(() -> 
                    estadoServidor.setText("Servidor activo - Jugadores: " + clientes.size()));
            }
        }
        
        private boolean verificarNumerosConArchivo(String[] numeros) {
            Set<Integer> numerosArchivo = new HashSet<>();
            try (BufferedReader br = new BufferedReader(new FileReader(ARCHIVO_NUMEROS))) {
                String linea = br.readLine();
                if (linea != null && !linea.trim().isEmpty()) {
                    String[] nums = linea.split(",");
                    for (String num : nums) {
                        try {
                            numerosArchivo.add(Integer.parseInt(num.trim()));
                        } catch (NumberFormatException e) {
                            // Ignorar
                        }
                    }
                }
            } catch (IOException e) {
                System.err.println("Error al leer " + ARCHIVO_NUMEROS + " para verificar");
                return false;
            }
            
            for (String num : numeros) {
                try {
                    int numero = Integer.parseInt(num.trim());
                    if (!numerosArchivo.contains(numero)) {
                        System.out.println("Número " + numero + " NO está en " + ARCHIVO_NUMEROS);
                        return false;
                    }
                } catch (NumberFormatException e) {
                    return false;
                }
            }
            
            System.out.println("Todos los números verificados con " + ARCHIVO_NUMEROS);
            return true;
        }
    }
    
    static class PreguntaSostenibilidad {
        String pregunta;
        String[] opciones;
        int respuestaCorrecta;
        
        public PreguntaSostenibilidad(String pregunta, String[] opciones, int respuestaCorrecta) {
            this.pregunta = pregunta;
            this.opciones = opciones;
            this.respuestaCorrecta = respuestaCorrecta;
        }
        
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append(pregunta).append("|");
            for (int i = 0; i < opciones.length; i++) {
                sb.append(opciones[i]);
                if (i < opciones.length - 1) {
                    sb.append("|");
                }
            }
            sb.append("|").append(respuestaCorrecta);
            return sb.toString();
        }
    }
    
    @Override
    public void dispose() {
        try {
            if (serverSocket != null) {
                serverSocket.close();
            }
            for (ClientHandler cliente : clientes) {
                if (cliente.socket != null) {
                    cliente.socket.close();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        super.dispose();
    }
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new BingoAdministrador());
    }
}