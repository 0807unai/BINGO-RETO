import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.List;

public class BingoJugador extends JFrame {
    private JButton[][] botonesCarton;
    private int[][] numeroCarton;
    private boolean[][] marcados;
    private JLabel estadoLabel;
    private JLabel conexionLabel;
    private JLabel lineasLabel;
    private JTextField ipTextField;
    private static final int FILAS = 3;
    private static final int COLUMNAS = 9;
    
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private Set<Integer> numerosLlamados;
    private boolean esperandoRespuesta = false;
    private int lineasConseguidas = 0;
    
    private static final String ARCHIVO_NUMEROS = "num_bingo.txt";
    
    public BingoJugador() {
        setTitle("Mi Cartón de Bingo Sostenible");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));
        getContentPane().setBackground(new Color(70, 130, 180));
        
        botonesCarton = new JButton[FILAS][COLUMNAS];
        numeroCarton = new int[FILAS][COLUMNAS];
        marcados = new boolean[FILAS][COLUMNAS];
        numerosLlamados = new HashSet<>();
        
        generarCarton();
        crearInterfaz();
        cargarNumerosDesdeArchivo();
        conectarAlServidor();
        
        setSize(850, 620);
        setLocationRelativeTo(null);
        setVisible(true);
    }
    
    private void cargarNumerosDesdeArchivo() {
        File archivo = new File(ARCHIVO_NUMEROS);
        if (archivo.exists()) {
            try (BufferedReader br = new BufferedReader(new FileReader(archivo))) {
                String linea = br.readLine();
                if (linea != null && !linea.trim().isEmpty()) {
                    String[] numeros = linea.split(",");
                    for (String num : numeros) {
                        try {
                            int numero = Integer.parseInt(num.trim());
                            numerosLlamados.add(numero);
                        } catch (NumberFormatException e) {
                            // Ignorar
                        }
                    }
                    
                    SwingUtilities.invokeLater(() -> {
                        habilitarNumerosLlamados();
                        estadoLabel.setText("📄 Números cargados de num_bingo.txt: " + numerosLlamados.size());
                    });
                    System.out.println("Números cargados de " + ARCHIVO_NUMEROS + ": " + numerosLlamados);
                }
            } catch (IOException e) {
                System.out.println("No se pudo cargar " + ARCHIVO_NUMEROS);
            }
        }
    }
    
    private Set<Integer> leerNumerosDelArchivo() {
        Set<Integer> numeros = new HashSet<>();
        try (BufferedReader br = new BufferedReader(new FileReader(ARCHIVO_NUMEROS))) {
            String linea = br.readLine();
            if (linea != null && !linea.trim().isEmpty()) {
                String[] nums = linea.split(",");
                for (String num : nums) {
                    try {
                        numeros.add(Integer.parseInt(num.trim()));
                    } catch (NumberFormatException e) {
                        // Ignorar
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Error al leer " + ARCHIVO_NUMEROS + ": " + e.getMessage());
        }
        return numeros;
    }
    
    private void guardarNumerosEnArchivo() {
        try (PrintWriter pw = new PrintWriter(new FileWriter(ARCHIVO_NUMEROS))) {
            StringBuilder sb = new StringBuilder();
            List<Integer> listaNumeros = new ArrayList<>(numerosLlamados);
            Collections.sort(listaNumeros);
            for (int i = 0; i < listaNumeros.size(); i++) {
                sb.append(listaNumeros.get(i));
                if (i < listaNumeros.size() - 1) {
                    sb.append(",");
                }
            }
            pw.print(sb.toString());
            pw.flush();
            System.out.println("✓ Guardado en " + ARCHIVO_NUMEROS + ": " + numerosLlamados.size() + " números");
            System.out.println("  Contenido: " + sb.toString());
        } catch (IOException e) {
            System.err.println("✗ Error al guardar " + ARCHIVO_NUMEROS + ": " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void conectarAlServidor() {
        String ip = JOptionPane.showInputDialog(this, "Ingrese la IP del servidor:", "Conexión al Servidor", JOptionPane.PLAIN_MESSAGE);
        if (ip == null || ip.trim().isEmpty()) {
            estadoLabel.setText("✗ IP no válida, conexión cancelada");
            return;
        }
        
        Thread conexionThread = new Thread(() -> {
            try {
                socket = new Socket(ip, 12345);
                out = new PrintWriter(socket.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                
                SwingUtilities.invokeLater(() -> {
                    conexionLabel.setText("✓ Conectado al servidor: " + ip);
                    ipTextField.setText(ip);
                });
                
                String mensaje;
                while ((mensaje = in.readLine()) != null) {
                    procesarMensajeServidor(mensaje);
                }
            } catch (IOException e) {
                SwingUtilities.invokeLater(() -> {
                    conexionLabel.setText("✗ Sin conexión al servidor");
                    estadoLabel.setText("⚠️ Sin servidor, verificación solo con archivo num_bingo.txt");
                });
            }
        });
        conexionThread.start();
    }
    
    private void procesarMensajeServidor(String mensaje) {
        if (mensaje.startsWith("NUMERO:")) {
            int numero = Integer.parseInt(mensaje.substring(7));
            numerosLlamados.add(numero);
            guardarNumerosEnArchivo();
            SwingUtilities.invokeLater(() -> {
                resaltarYHabilitarNumero(numero);
                estadoLabel.setText("🎲 Nuevo número: " + numero + " - ¡Puedes marcarlo!");
            });
        } else if (mensaje.startsWith("HISTORIAL:")) {
            String[] numeros = mensaje.substring(10).split(",");
            System.out.println("📥 Recibido historial del servidor: " + numeros.length + " números");
            for (String num : numeros) {
                int numero = Integer.parseInt(num.trim());
                numerosLlamados.add(numero);
            }
            System.out.println("📊 Total de números en memoria: " + numerosLlamados.size());
            guardarNumerosEnArchivo();
            SwingUtilities.invokeLater(() -> {
                habilitarNumerosLlamados();
                estadoLabel.setText("✓ Sincronizado - Números disponibles: " + numerosLlamados.size());
            });
        } else if (mensaje.equals("REINICIAR")) {
            numerosLlamados.clear();
            guardarNumerosEnArchivo();
            SwingUtilities.invokeLater(() -> {
                lineasConseguidas = 0;
                actualizarContadorLineas();
                reiniciarMarcas();
                deshabilitarTodosLosNumeros();
                estadoLabel.setText("🔄 Juego reiniciado por el administrador");
            });
        } else if (mensaje.startsWith("PREGUNTA:")) {
            SwingUtilities.invokeLater(() -> mostrarPregunta(mensaje.substring(9)));
        } else if (mensaje.startsWith("RESULTADO:")) {
            boolean correcto = Boolean.parseBoolean(mensaje.substring(10));
            SwingUtilities.invokeLater(() -> {
                esperandoRespuesta = false;
                if (correcto) {
                    lineasConseguidas++;
                    actualizarContadorLineas();
                    mostrarExitoLinea();
                } else {
                    mostrarFalloPregunta();
                }
            });
        } else if (mensaje.startsWith("VERIFICACION:")) {
            boolean valido = Boolean.parseBoolean(mensaje.substring(13));
            SwingUtilities.invokeLater(() -> {
                esperandoRespuesta = false;
                if (!valido) {
                    mostrarVerificacionErronea();
                }
            });
        }
    }
    
    private void resaltarYHabilitarNumero(int numero) {
        for (int fila = 0; fila < FILAS; fila++) {
            for (int col = 0; col < COLUMNAS; col++) {
                if (numeroCarton[fila][col] == numero) {
                    JButton boton = botonesCarton[fila][col];
                    boton.setEnabled(true);
                    
                    boton.setBackground(new Color(144, 238, 144));
                    
                    return;
                }
            }
        }
    }
    
    private void habilitarNumerosLlamados() {
        for (int fila = 0; fila < FILAS; fila++) {
            for (int col = 0; col < COLUMNAS; col++) {
                if (numeroCarton[fila][col] != 0 && numerosLlamados.contains(numeroCarton[fila][col])) {
                    botonesCarton[fila][col].setEnabled(true);
                }
            }
        }
    }
    
    private void deshabilitarTodosLosNumeros() {
        for (int fila = 0; fila < FILAS; fila++) {
            for (int col = 0; col < COLUMNAS; col++) {
                if (numeroCarton[fila][col] != 0) {
                    botonesCarton[fila][col].setEnabled(false);
                    botonesCarton[fila][col].setBackground(Color.WHITE);
                }
            }
        }
    }
    
    private void generarCarton() {
        for (int col = 0; col < COLUMNAS; col++) {
            List<Integer> numerosColumna = new ArrayList<>();
            int inicio = col * 10 + 1;
            int fin = (col == COLUMNAS - 1) ? 90 : (col + 1) * 10;
            
            for (int i = inicio; i <= fin; i++) {
                numerosColumna.add(i);
            }
            Collections.shuffle(numerosColumna);
            
            for (int fila = 0; fila < FILAS; fila++) {
                if (fila < numerosColumna.size()) {
                    numeroCarton[fila][col] = numerosColumna.get(fila);
                }
            }
        }
        
        for (int fila = 0; fila < FILAS; fila++) {
            List<Integer> posiciones = new ArrayList<>();
            for (int col = 0; col < COLUMNAS; col++) {
                if (numeroCarton[fila][col] != 0) {
                    posiciones.add(col);
                }
            }
            
            while (posiciones.size() > 5) {
                int idx = new Random().nextInt(posiciones.size());
                int col = posiciones.remove(idx);
                numeroCarton[fila][col] = 0;
            }
            
            while (posiciones.size() < 5) {
                int col = new Random().nextInt(COLUMNAS);
                if (numeroCarton[fila][col] == 0) {
                    int inicio = col * 10 + 1;
                    int fin = (col == COLUMNAS - 1) ? 90 : (col + 1) * 10;
                    int num = inicio + new Random().nextInt(fin - inicio + 1);
                    
                    boolean repetido = false;
                    for (int f = 0; f < FILAS; f++) {
                        if (numeroCarton[f][col] == num) {
                            repetido = true;
                            break;
                        }
                    }
                    
                    if (!repetido) {
                        numeroCarton[fila][col] = num;
                        posiciones.add(col);
                    }
                }
            }
        }
    }
    
    private void crearInterfaz() {
        JPanel panelTitulo = new JPanel(new BorderLayout());
        panelTitulo.setBackground(new Color(70, 130, 180));
        
        JLabel tituloLabel = new JLabel("🌱 MI CARTÓN DE BINGO SOSTENIBLE");
        tituloLabel.setFont(new Font("Arial", Font.BOLD, 24));
        tituloLabel.setForeground(Color.WHITE);
        tituloLabel.setBorder(BorderFactory.createEmptyBorder(15, 0, 5, 0));
        tituloLabel.setHorizontalAlignment(SwingConstants.CENTER);
        
        JPanel panelInfo = new JPanel(new GridLayout(2, 1));
        panelInfo.setBackground(new Color(70, 130, 180));
        
        conexionLabel = new JLabel("Conectando al servidor...", SwingConstants.CENTER);
        conexionLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        conexionLabel.setForeground(Color.WHITE);
        
        lineasLabel = new JLabel("🏆 Líneas conseguidas: 0", SwingConstants.CENTER);
        lineasLabel.setFont(new Font("Arial", Font.BOLD, 14));
        lineasLabel.setForeground(Color.YELLOW);
        
        panelInfo.add(conexionLabel);
        panelInfo.add(lineasLabel);
        panelInfo.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        
        panelTitulo.add(tituloLabel, BorderLayout.NORTH);
        panelTitulo.add(panelInfo, BorderLayout.SOUTH);
        
        JPanel panelCarton = new JPanel(new GridLayout(FILAS, COLUMNAS, 5, 5));
        panelCarton.setBackground(new Color(70, 130, 180));
        panelCarton.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        for (int fila = 0; fila < FILAS; fila++) {
            for (int col = 0; col < COLUMNAS; col++) {
                JButton boton = new JButton();
                botonesCarton[fila][col] = boton;
                
                if (numeroCarton[fila][col] != 0) {
                    boton.setText(String.valueOf(numeroCarton[fila][col]));
                    boton.setFont(new Font("Arial", Font.BOLD, 28));
                    boton.setBackground(Color.WHITE);
                    boton.setEnabled(false);
                    boton.setFocusPainted(false);
                    boton.setBorder(BorderFactory.createLineBorder(new Color(0, 0, 139), 2));
                    
                    int filaActual = fila;
                    int colActual = col;
                    
                    boton.addActionListener(e -> marcarNumero(filaActual, colActual));
                } else {
                    boton.setText("");
                    boton.setEnabled(false);
                    boton.setBackground(new Color(200, 200, 200));
                }
                
                panelCarton.add(boton);
            }
        }
        
        JPanel panelInferior = new JPanel(new BorderLayout());
        panelInferior.setBackground(new Color(70, 130, 180));
        
        estadoLabel = new JLabel("⏳ Esperando números...", SwingConstants.CENTER);
        estadoLabel.setFont(new Font("Arial", Font.BOLD, 13));
        estadoLabel.setForeground(Color.WHITE);
        estadoLabel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        JPanel panelBotones = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));
        panelBotones.setBackground(new Color(70, 130, 180));
        
        JButton verificarBtn = new JButton("✓ VERIFICAR LÍNEA");
        verificarBtn.setFont(new Font("Arial", Font.BOLD, 14));
        verificarBtn.setPreferredSize(new Dimension(190, 40));
        verificarBtn.setBackground(new Color(50, 205, 50));
        verificarBtn.setForeground(Color.WHITE);
        verificarBtn.setFocusPainted(false);
        verificarBtn.addActionListener(e -> verificarLinea());
        
        JButton verificarBingoBtn = new JButton("🎉 ¡BINGO!");
        verificarBingoBtn.setFont(new Font("Arial", Font.BOLD, 14));
        verificarBingoBtn.setPreferredSize(new Dimension(150, 40));
        verificarBingoBtn.setBackground(new Color(255, 215, 0));
        verificarBingoBtn.setFocusPainted(false);
        verificarBingoBtn.addActionListener(e -> verificarBingo());
        
        JButton reiniciarBtn = new JButton("🔄 NUEVO CARTÓN");
        reiniciarBtn.setFont(new Font("Arial", Font.BOLD, 14));
        reiniciarBtn.setPreferredSize(new Dimension(190, 40));
        reiniciarBtn.setBackground(new Color(220, 20, 60));
        reiniciarBtn.setForeground(Color.WHITE);
        reiniciarBtn.setFocusPainted(false);
        reiniciarBtn.addActionListener(e -> nuevoCarton());
        
        panelBotones.add(verificarBtn);
        panelBotones.add(verificarBingoBtn);
        panelBotones.add(reiniciarBtn);
        
        panelInferior.add(estadoLabel, BorderLayout.NORTH);
        panelInferior.add(panelBotones, BorderLayout.CENTER);
        
        add(panelTitulo, BorderLayout.NORTH);
        add(panelCarton, BorderLayout.CENTER);
        add(panelInferior, BorderLayout.SOUTH);
    }
    
    private void actualizarContadorLineas() {
        lineasLabel.setText("🏆 Líneas conseguidas: " + lineasConseguidas);
    }
    
    private void marcarNumero(int fila, int col) {
        int numero = numeroCarton[fila][col];
        
        Set<Integer> numerosArchivo = leerNumerosDelArchivo();
        if (!numerosArchivo.contains(numero)) {
            estadoLabel.setText("❌ ERROR: El número " + numero + " NO está en num_bingo.txt");
            JOptionPane.showMessageDialog(this,
                "No puedes marcar el número " + numero + "\n\n¡Ese número NO está en el archivo num_bingo.txt!\nSolo puedes marcar números que el administrador haya sacado.",
                "Número no disponible",
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        JButton boton = botonesCarton[fila][col];
        
        if (!marcados[fila][col]) {
            boton.setBackground(new Color(255, 140, 0));
            boton.setForeground(Color.WHITE);
            marcados[fila][col] = true;
            estadoLabel.setText("✓ Número " + numero + " marcado");
        } else {
            boton.setBackground(Color.WHITE);
            boton.setForeground(Color.BLACK);
            marcados[fila][col] = false;
            estadoLabel.setText("○ Número " + numero + " desmarcado");
        }
    }
    
    private void verificarLinea() {
        if (esperandoRespuesta) {
            return;
        }
        
        List<Integer> numerosLinea = new ArrayList<>();
        boolean lineaMarcada = false;
        
        for (int fila = 0; fila < FILAS; fila++) {
            boolean lineaCompleta = true;
            List<Integer> numerosFilaActual = new ArrayList<>();
            
            for (int col = 0; col < COLUMNAS; col++) {
                if (numeroCarton[fila][col] != 0) {
                    numerosFilaActual.add(numeroCarton[fila][col]);
                    if (!marcados[fila][col]) {
                        lineaCompleta = false;
                    }
                }
            }
            
            if (lineaCompleta) {
                numerosLinea = numerosFilaActual;
                lineaMarcada = true;
                break;
            }
        }
        
        if (!lineaMarcada) {
            estadoLabel.setText("⚠️ No tienes línea completa");
            JOptionPane.showMessageDialog(this,
                "No tienes ninguna línea completa marcada.\n\nMarca todos los números de una fila para hacer línea.",
                "Línea incompleta",
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        Set<Integer> numerosArchivo = leerNumerosDelArchivo();
        boolean todosEnArchivo = true;
        StringBuilder faltantes = new StringBuilder();
        
        for (int num : numerosLinea) {
            if (!numerosArchivo.contains(num)) {
                todosEnArchivo = false;
                if (faltantes.length() > 0) faltantes.append(", ");
                faltantes.append(num);
            }
        }
        
        if (!todosEnArchivo) {
            estadoLabel.setText("❌ ERROR: Números no están en num_bingo.txt");
            JOptionPane.showMessageDialog(this,
                "¡VERIFICACIÓN FALLIDA!\n\nLos siguientes números NO están en num_bingo.txt:\n" + faltantes.toString() + "\n\nSolo puedes hacer línea con números que estén en el archivo.",
                "Error - Verificación con archivo",
                JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        esperandoRespuesta = true;
        StringBuilder sb = new StringBuilder("VERIFICAR_LINEA:");
        for (int i = 0; i < numerosLinea.size(); i++) {
            sb.append(numerosLinea.get(i));
            if (i < numerosLinea.size() - 1) {
                sb.append(",");
            }
        }
        
        if (out != null) {
            out.println(sb.toString());
            estadoLabel.setText("⏳ Verificando línea con servidor...");
        } else {
            esperandoRespuesta = false;
            estadoLabel.setText("⚠️ Sin conexión, no se puede verificar");
        }
    }
    
    private void verificarBingo() {
        if (esperandoRespuesta) {
            return;
        }
        
        List<Integer> todosNumeros = new ArrayList<>();
        boolean todoMarcado = true;
        
        for (int fila = 0; fila < FILAS; fila++) {
            for (int col = 0; col < COLUMNAS; col++) {
                if (numeroCarton[fila][col] != 0) {
                    todosNumeros.add(numeroCarton[fila][col]);
                    if (!marcados[fila][col]) {
                        todoMarcado = false;
                    }
                }
            }
        }
        
        if (!todoMarcado) {
            estadoLabel.setText("⚠️ Cartón incompleto");
            JOptionPane.showMessageDialog(this,
                "No has marcado todos los números del cartón.\n\nMarca todos los números para cantar BINGO.",
                "Cartón incompleto",
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        Set<Integer> numerosArchivo = leerNumerosDelArchivo();
        boolean todosEnArchivo = true;
        StringBuilder faltantes = new StringBuilder();
        
        for (int num : todosNumeros) {
            if (!numerosArchivo.contains(num)) {
                todosEnArchivo = false;
                if (faltantes.length() > 0) faltantes.append(", ");
                faltantes.append(num);
            }
        }
        
        if (!todosEnArchivo) {
            estadoLabel.setText("❌ ERROR: Números no están en num_bingo.txt");
            JOptionPane.showMessageDialog(this,
                "¡VERIFICACIÓN FALLIDA!\n\nLos siguientes números NO están en num_bingo.txt:\n" + faltantes.toString() + "\n\nSolo puedes hacer BINGO con números que estén en el archivo.",
                "Error - Verificación con archivo",
                JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        esperandoRespuesta = true;
        StringBuilder sb = new StringBuilder("VERIFICAR_BINGO:");
        for (int i = 0; i < todosNumeros.size(); i++) {
            sb.append(todosNumeros.get(i));
            if (i < todosNumeros.size() - 1) {
                sb.append(",");
            }
        }
        
        if (out != null) {
            out.println(sb.toString());
            estadoLabel.setText("⏳ Verificando BINGO con servidor...");
        } else {
            esperandoRespuesta = false;
            estadoLabel.setText("⚠️ Sin conexión, no se puede verificar");
        }
    }
    
    private void mostrarPregunta(String datos) {
        String[] partes = datos.split("\\|");
        String tipo = partes[0];
        String pregunta = partes[1];
        String[] opciones = Arrays.copyOfRange(partes, 2, partes.length - 1);
        int respuestaCorrecta = Integer.parseInt(partes[partes.length - 1]);
        
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        
        JLabel labelPregunta = new JLabel("<html><div style='width:350px'><b>🌱 Pregunta de Sostenibilidad:</b><br><br>" + pregunta + "</div></html>");
        labelPregunta.setFont(new Font("Arial", Font.PLAIN, 14));
        panel.add(labelPregunta, BorderLayout.NORTH);
        
        JPanel panelOpciones = new JPanel(new GridLayout(opciones.length, 1, 5, 8));
        ButtonGroup grupo = new ButtonGroup();
        JRadioButton[] botones = new JRadioButton[opciones.length];
        
        for (int i = 0; i < opciones.length; i++) {
            botones[i] = new JRadioButton((i + 1) + ". " + opciones[i]);
            botones[i].setFont(new Font("Arial", Font.PLAIN, 13));
            grupo.add(botones[i]);
            panelOpciones.add(botones[i]);
        }
        
        panel.add(panelOpciones, BorderLayout.CENTER);
        
        String titulo = tipo.equals("LINEA") ? "✓ ¡LÍNEA VÁLIDA! - Responde para ganar" : "🎉 ¡BINGO VÁLIDO! - Responde para ganar";
        
        int resultado = JOptionPane.showConfirmDialog(this, panel, titulo,
            JOptionPane.OK_CANCEL_OPTION,
            JOptionPane.QUESTION_MESSAGE);
        
        if (resultado == JOptionPane.OK_OPTION) {
            int seleccionada = -1;
            for (int i = 0; i < botones.length; i++) {
                if (botones[i].isSelected()) {
                    seleccionada = i;
                    break;
                }
            }
            
            boolean correcto = (seleccionada == respuestaCorrecta);
            out.println("RESPUESTA:" + correcto + ":0");
            
            if (!correcto) {
                estadoLabel.setText("❌ Respuesta incorrecta - La correcta era: " + opciones[respuestaCorrecta]);
            }
        } else {
            out.println("RESPUESTA:false:0");
            estadoLabel.setText("❌ Pregunta cancelada");
        }
    }
    
    private void mostrarExitoLinea() {
        JOptionPane.showMessageDialog(this,
            "¡FELICIDADES!\n\n✓ Has conseguido una LÍNEA\n✓ Respuesta correcta\n\n¡Sigue jugando para conseguir BINGO!",
            "🎉 ¡LÍNEA CONSEGUIDA!",
            JOptionPane.INFORMATION_MESSAGE);
        estadoLabel.setText("🎉 ¡LÍNEA conseguida! Total: " + lineasConseguidas);
    }
    
    private void mostrarFalloPregunta() {
        JOptionPane.showMessageDialog(this,
            "Lo siento, has fallado la pregunta de sostenibilidad.\n\nAunque tu línea/bingo era válida, necesitas responder\ncorrectamente la pregunta para ganar.\n\n¡Sigue intentándolo!",
            "❌ Respuesta Incorrecta",
            JOptionPane.ERROR_MESSAGE);
        estadoLabel.setText("❌ Pregunta fallada - No se cuenta como línea");
    }
    
    private void mostrarVerificacionErronea() {
        estadoLabel.setText("❌ VERIFICACIÓN ERRÓNEA");
        JOptionPane.showMessageDialog(this,
            "¡ERROR EN LA VERIFICACIÓN!\n\nAlgunos números marcados NO han sido llamados.\nRevisa que todos los números estén en num_bingo.txt",
            "Verificación Incorrecta",
            JOptionPane.ERROR_MESSAGE);
    }
    
    private void reiniciarMarcas() {
        for (int fila = 0; fila < FILAS; fila++) {
            for (int col = 0; col < COLUMNAS; col++) {
                if (numeroCarton[fila][col] != 0) {
                    botonesCarton[fila][col].setBackground(Color.WHITE);
                    botonesCarton[fila][col].setForeground(Color.BLACK);
                    marcados[fila][col] = false;
                }
            }
        }
    }
    
    private void nuevoCarton() {
        int confirmacion = JOptionPane.showConfirmDialog(this,
            "¿Generar un nuevo cartón?\n\n(Perderás el progreso actual)",
            "Nuevo cartón",
            JOptionPane.YES_NO_OPTION);
        
        if (confirmacion == JOptionPane.YES_OPTION) {
            cerrarConexion();
            dispose();
            new BingoJugador();
        }
    }
    
    private void cerrarConexion() {
        try {
            if (socket != null) {
                socket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    @Override
    public void dispose() {
        cerrarConexion();
        super.dispose();
    }
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new BingoJugador());
    }
}