import java.awt.EventQueue;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.SwingConstants;
import java.awt.Color;
import java.awt.Font;
import javax.swing.JOptionPane;
import java.awt.BorderLayout;
import javax.swing.JScrollPane;
import javax.swing.JTextArea; 
import javax.swing.JSeparator;
import javax.swing.BorderFactory;


class Pregunta {
    private String textoPregunta;
    private String[] opciones; 
    private String respuestaCorrecta; 

    public Pregunta(String textoPregunta, String opcionA, String opcionB, String opcionC, String respuestaCorrecta) {
        this.textoPregunta = textoPregunta;
        this.opciones = new String[] { opcionA, opcionB, opcionC };
        this.respuestaCorrecta = respuestaCorrecta;
    }

    public String getTextoPregunta() { return textoPregunta; }
    public String getOpcion(int indice) { return opciones[indice]; }
    public String getRespuestaCorrecta() { return respuestaCorrecta; }

    public boolean esRespuestaCorrecta(String respuestaUsuario) {
        return respuestaUsuario != null && respuestaUsuario.equalsIgnoreCase(respuestaCorrecta);
    }
}


public class Bingo extends JFrame {

    private static final long serialVersionUID = 1L;
    private JPanel contentPane;
    

    private JButton[] cartonBotones;
    private JLabel lblNumeroActual;
    private JLabel lblBolaGrande;  
    private JButton btnSacarBola;
    private JButton btnNuevaPartida;
    private JButton btnSalir;
    private JButton[] pizarraCantante; 
    
    // Variables de Red
    private boolean esCantante;
    private ServidorBingo servidor; 
    private ClienteBingo cliente;   
    private List<Integer> bombo;
    private List<Integer> numerosCarton;
    private List<Integer> numerosCantados = new ArrayList<>();
    
    // Bandera para el estado del juego
    private boolean juegoTerminado = false;

 
    private List<Pregunta> bancoPreguntas;
    private int indicePreguntaActual = 0; 
    private int lineasCompletadas = 0;

    /**
     * Launch the application.
     */
    public static void main(String[] args) {
        
        // ELEGIR ROL DE JUGADOR O CANTANTE
        String[] opciones = {"Cantante", "Jugador"};
        int seleccion = JOptionPane.showOptionDialog(
            null, 
            "Selecciona tu rol:", 
            "Configuración de Bingo", 
            JOptionPane.YES_NO_OPTION, 
            JOptionPane.QUESTION_MESSAGE, 
            null, 
            opciones, 
            opciones[1]
        );
        
        boolean isCantante = (seleccion == 0);
        if (seleccion == -1) {
            return; 
        }

        String host = "127.0.0.1";
        int puerto = 12345; 

        if (!isCantante) {
            host = JOptionPane.showInputDialog(null, "Introduce la IP del Cantante:", "127.0.0.1");
            if (host == null || host.trim().isEmpty()) {
                 return;
            }
        }

        final String hostFinal = host;

        EventQueue.invokeLater(new Runnable() {
            public void run() {
                try {
                    Bingo frame = new Bingo(isCantante, hostFinal, puerto); 
                    frame.setVisible(true);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    /**
     * Create the frame.
     */
    public Bingo(boolean isCantante, String host, int puerto) {
        this.esCantante = isCantante;
        
        setTitle("Juego de Bingo - " + (esCantante ? "CANTANTE" : "JUGADOR"));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setBounds(100, 100, 700, 500); 
        contentPane = new JPanel();
        contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
        setContentPane(contentPane);
        
        // Inicialización de componentes
        lblNumeroActual = new JLabel("Número Sorteado:");
        lblBolaGrande = new JLabel("N/A"); 
        lblBolaGrande.setHorizontalAlignment(SwingConstants.CENTER);
        lblBolaGrande.setFont(new Font("Tahoma", Font.BOLD, 48));
        lblBolaGrande.setForeground(Color.RED);
        
        btnSacarBola = new JButton("Sacar Bola");
        btnSacarBola.setBackground(Color.GREEN.darker());
        btnSacarBola.setForeground(Color.WHITE);
        
        btnNuevaPartida = new JButton("Nueva Partida"); 
        
        btnSalir = new JButton("Salir");

        // INTERFAZ SEGUN LO QUE ELIJAS
        if (esCantante) {

            setBounds(100, 100, 950, 650); 
            contentPane.setLayout(new BorderLayout(5, 5));
            contentPane.setBackground(Color.DARK_GRAY.darker());
            
 
            JPanel panelSorteo = new JPanel(new BorderLayout(10, 10));
            panelSorteo.setBackground(Color.DARK_GRAY);
            panelSorteo.setBorder(new EmptyBorder(10, 10, 10, 10));
            
            JPanel panelBotones = new JPanel(new GridLayout(1, 2, 10, 10));
            panelBotones.add(btnSacarBola);
            panelBotones.add(btnSalir);

            lblBolaGrande.setFont(new Font("Tahoma", Font.BOLD, 120));
            lblBolaGrande.setForeground(Color.YELLOW);
            lblBolaGrande.setText("---");

            panelSorteo.add(lblBolaGrande, BorderLayout.CENTER);
            panelSorteo.add(panelBotones, BorderLayout.EAST);
            contentPane.add(panelSorteo, BorderLayout.NORTH);


            pizarraCantante = new JButton[75];
            JPanel panelPizarra = new JPanel(new GridLayout(7, 11, 2, 2)); 
            
            for (int i = 0; i < 75; i++) {
                JButton btn = new JButton(String.valueOf(i + 1));
                btn.setFont(new Font("Tahoma", Font.BOLD, 10));
                btn.setBackground(Color.WHITE);
                btn.setForeground(Color.BLACK);
                pizarraCantante[i] = btn;
                panelPizarra.add(btn);
            }
            
            JScrollPane scrollPane = new JScrollPane(panelPizarra); 
            
            JPanel panelCentro = new JPanel(new BorderLayout());
            panelCentro.add(scrollPane, BorderLayout.CENTER);
            contentPane.add(panelCentro, BorderLayout.CENTER);
            
        } else {

            setBounds(100, 100, 800, 600); 
            contentPane.setLayout(new BorderLayout(10, 10));
            

            JPanel panelCarton = new JPanel(new GridLayout(6, 5, 5, 5)); 
            panelCarton.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
            
            cartonBotones = new JButton[25];
            numerosCarton = generarCartonDePrueba(); 
            
   
            String[] letras = {"B", "I", "N", "G", "O"};
            for (String letra : letras) {
                JLabel lblLetra = new JLabel(letra);
                lblLetra.setFont(new Font("Tahoma", Font.BOLD, 22));
                lblLetra.setForeground(Color.BLUE.darker());
                lblLetra.setHorizontalAlignment(SwingConstants.CENTER);
                panelCarton.add(lblLetra);
            }


            for (int i = 0; i < 25; i++) {
                JButton btn = new JButton(String.valueOf(numerosCarton.get(i)));
                btn.setFont(new Font("Tahoma", Font.BOLD, 16));
                btn.setBackground(Color.WHITE);
                cartonBotones[i] = btn;
                panelCarton.add(btn);
            }
            contentPane.add(panelCarton, BorderLayout.CENTER);
            

            JPanel panelControl = new JPanel(new GridLayout(4, 1, 10, 10));
            panelControl.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
            
            panelControl.add(new JLabel("ÚLTIMA BOLA:"));
            panelControl.add(lblBolaGrande); 
            panelControl.add(btnSalir);
            contentPane.add(panelControl, BorderLayout.EAST);
            btnSacarBola.setVisible(false); 
        }


        if (esCantante) {
            try {
                servidor = new ServidorBingo(puerto, this::setJuegoTerminado);
                servidor.iniciar();
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this, "Error al iniciar el Servidor.", "Error de Red", JOptionPane.ERROR_MESSAGE);
            }
        } else {
            try {
                Consumer<Integer> actualizarUI = this::actualizarNumeroCantado; 
                cliente = new ClienteBingo(host, puerto, actualizarUI);
                cliente.recibir();
                cargarPreguntas(); 
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this, "Error al conectar con el Cantante.", "Error de Red", JOptionPane.ERROR_MESSAGE);
            }
        }

        inicializarJuego();
        registrarEventos();
    }

    // Método para que ServidorBingo pueda notificar el fin del juego al Cantante
    public void setJuegoTerminado(boolean terminado) {
        EventQueue.invokeLater(() -> {
            this.juegoTerminado = terminado;
            if (terminado) {
                lblBolaGrande.setText("BINGO!");
                btnSacarBola.setEnabled(false);
                JOptionPane.showMessageDialog(this, "¡Un jugador ha cantado BINGO! El juego ha terminado.", "FIN DE PARTIDA", JOptionPane.INFORMATION_MESSAGE);
            }
        });
    }

	public void registrarEventos() {
		btnSalir.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				System.exit(0);
			}
		});
		
		btnSacarBola.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				sacarNuevaBola();
			}
		});
		
		btnNuevaPartida.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {

				inicializarJuego();
			}
		});
		
		if (!esCantante) {
	
			 for(JButton boton : cartonBotones) {
				boton.addActionListener(new ActionListener() {
					
					@Override
					public void actionPerformed(ActionEvent e) {
						marcarNumero(boton);
					}
				});
			 }
		}
		
	}
    
    // FUNCIONES DEL JUEGO
    
    private void cargarPreguntas() {
        bancoPreguntas = new ArrayList<>();
        try (java.io.BufferedReader br = new java.io.BufferedReader(new java.io.FileReader("preguntas_sostenibilidad.txt"))) {
            String linea;
            while ((linea = br.readLine()) != null) {
                String pregunta = linea;
                String opA = br.readLine();
                String opB = br.readLine();
                String opC = br.readLine();
                String respuesta = br.readLine();
                
                if (pregunta != null && opA != null && opB != null && opC != null && respuesta != null) {
                    bancoPreguntas.add(new Pregunta(
                        pregunta, 
                        opA.length() > 2 && opA.charAt(1) == '.' ? opA.substring(2).trim() : opA.trim(), 
                        opB.length() > 2 && opB.charAt(1) == '.' ? opB.substring(2).trim() : opB.trim(), 
                        opC.length() > 2 && opC.charAt(1) == '.' ? opC.substring(2).trim() : opC.trim(), 
                        respuesta.trim()));
                }
                br.readLine(); 
            }
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Error al cargar el archivo de preguntas (preguntas_sostenibilidad.txt). Asegúrate de que exista.", "Error de Archivo", JOptionPane.ERROR_MESSAGE);
        }
    }

    private List<Integer> generarCartonDePrueba() {
        List<Integer> cartonFinalOrdenado = new ArrayList<>();
        int[][] rangos = {
            {1, 15}, {16, 30}, {31, 45}, {46, 60}, {61, 75}
        };
        List<List<Integer>> columnasOrdenadas = new ArrayList<>();
        
        for (int i = 0; i < 5; i++) {
            int min = rangos[i][0];
            int max = rangos[i][1];
            List<Integer> rangoCompleto = new ArrayList<>();
            for (int num = min; num <= max; num++) {
                rangoCompleto.add(num);
            }
            Collections.shuffle(rangoCompleto);
            List<Integer> columna = rangoCompleto.subList(0, 5);
            Collections.sort(columna);
            columnasOrdenadas.add(columna);
        }
        
        for (int fila = 0; fila < 5; fila++) {
            cartonFinalOrdenado.add(columnasOrdenadas.get(0).get(fila)); 
            cartonFinalOrdenado.add(columnasOrdenadas.get(1).get(fila)); 
            cartonFinalOrdenado.add(columnasOrdenadas.get(2).get(fila)); 
            cartonFinalOrdenado.add(columnasOrdenadas.get(3).get(fila)); 
            cartonFinalOrdenado.add(columnasOrdenadas.get(4).get(fila)); 
        }
        return cartonFinalOrdenado;
    }
    
    private void inicializarJuego() {
        juegoTerminado = false; 
        if (esCantante) {
            bombo = new ArrayList<>();
            for (int i = 1; i <= 75; i++) {
                bombo.add(i);
            }
            Collections.shuffle(bombo);
            lblBolaGrande.setText("---");
            if (pizarraCantante != null) {
                for (JButton btn : pizarraCantante) {
                    btn.setBackground(Color.WHITE);
                    btn.setForeground(Color.BLACK);
                }
            }
        }
        numerosCantados.clear(); 
        lineasCompletadas = 0; 
        
        if (!esCantante) {
            numerosCarton = generarCartonDePrueba();
            for(int i = 0; i < 25; i++) {
                cartonBotones[i].setText(String.valueOf(numerosCarton.get(i)));
                cartonBotones[i].setBackground(Color.WHITE);
                cartonBotones[i].setEnabled(true);
            }
        }
        
        btnSacarBola.setEnabled(esCantante); 
    }
    
    private void sacarNuevaBola() {
        if (!esCantante || servidor == null || juegoTerminado) return;

        if (bombo.isEmpty()) {
            lblBolaGrande.setText("¡FIN!");
            btnSacarBola.setEnabled(false);
            return;
        }
        
        int numeroSorteado = bombo.remove(0);
        lblBolaGrande.setText(String.valueOf(numeroSorteado));

        JButton btnMarcado = pizarraCantante[numeroSorteado - 1];
        btnMarcado.setBackground(Color.RED);
        btnMarcado.setForeground(Color.WHITE);

        numerosCantados.add(numeroSorteado); 

        servidor.enviarNumero(numeroSorteado);
    }


    private void actualizarNumeroCantado(int numero) {
        EventQueue.invokeLater(() -> {
            lblBolaGrande.setText(String.valueOf(numero));
            numerosCantados.add(numero); 
            
            if (!esCantante && !juegoTerminado) {
                // Asistencia visual (NARANJA) para el jugador
                for (JButton boton : cartonBotones) {
                    try {
                        int numeroBoton = Integer.parseInt(boton.getText());
                        if (numeroBoton == numero && boton.getBackground().equals(Color.WHITE)) {
                            boton.setBackground(Color.ORANGE); 
                        }
                    } catch (NumberFormatException ex) { /* Ignorar */ }
                }
            }
            
            comprobarLineaBingo();
        });
    }
    
    private void marcarNumero(JButton boton) {
        if (juegoTerminado) return; 
        
        try {
            int numeroBoton = Integer.parseInt(boton.getText());
            
            
            if (numerosCantados.contains(numeroBoton)) {
                
                // Si el número está en NARANJA (cantado, no marcado) o BLANCO (no marcado, pero cantado)
                if (boton.getBackground().equals(Color.ORANGE) || boton.getBackground().equals(Color.WHITE)) {
                    
                    boton.setBackground(Color.GREEN);
                    comprobarLineaBingo(); 
                    
                } else if (boton.getBackground().equals(Color.GREEN)) {
                    // Si ya está en verde (marcado)
                    JOptionPane.showMessageDialog(this, "Este número (" + numeroBoton + ") ya está marcado.");
                }
                
            } else {
                JOptionPane.showMessageDialog(this, "ERROR: El número " + numeroBoton + " aún no ha sido cantado (recibido por red).");
            }
            
        } catch (NumberFormatException ex) {
            // Manejar error de formato
        }
    }
    
    private void comprobarLineaBingo() {
        if (esCantante || juegoTerminado) return; 

        int lineasActuales = 0;

        // 1. Contar Líneas Horizontales 
        for (int i = 0; i < 5; i++) {
            boolean lineaCompleta = true;
            for (int j = 0; j < 5; j++) {
                // Comprobamos el color, no .isEnabled()
                if (!cartonBotones[(i * 5) + j].getBackground().equals(Color.GREEN)) { 
                    lineaCompleta = false;
                    break;
                }
            }
            if (lineaCompleta) { lineasActuales++; }
        }
        
        // 2. Contar Líneas Verticales 
        for (int j = 0; j < 5; j++) {
            boolean lineaCompleta = true;
            for (int i = 0; i < 5; i++) {
                // Comprobamos el color, no .isEnabled()
                if (!cartonBotones[(i * 5) + j].getBackground().equals(Color.GREEN)) {
                    lineaCompleta = false;
                    break;
                }
            }
            if (lineaCompleta) { lineasActuales++; }
        }
        
        // 3. Contar Diagonales (2)
        // Diagonal Principal (0, 6, 12, 18, 24)
        if (cartonBotones[0].getBackground().equals(Color.GREEN) && cartonBotones[6].getBackground().equals(Color.GREEN) && 
            cartonBotones[12].getBackground().equals(Color.GREEN) && cartonBotones[18].getBackground().equals(Color.GREEN) && 
            cartonBotones[24].getBackground().equals(Color.GREEN)) {
            lineasActuales++;
        }
        // Diagonal Secundaria (4, 8, 12, 16, 20)
        if (cartonBotones[4].getBackground().equals(Color.GREEN) && cartonBotones[8].getBackground().equals(Color.GREEN) && 
            cartonBotones[12].getBackground().equals(Color.GREEN) && cartonBotones[16].getBackground().equals(Color.GREEN) && 
            cartonBotones[20].getBackground().equals(Color.GREEN)) {
            lineasActuales++;
        }

        int totalMarcados = 0;
        for (JButton btn : cartonBotones) {
            // Comprobamos el color, no .isEnabled()
            if (btn.getBackground().equals(Color.GREEN)) {
                totalMarcados++;
            }
        }
        
        // LOGICA DEL QUIZ
        if (lineasActuales > lineasCompletadas) {
            
            lineasCompletadas = lineasActuales; 
            
            if (!bancoPreguntas.isEmpty()) {
                
                // SELECCIÓN ALEATORIA: Elegir una pregunta al azar
                java.util.Random rand = new java.util.Random();
                int indiceAleatorio = rand.nextInt(bancoPreguntas.size());
                
                Pregunta pregunta = bancoPreguntas.get(indiceAleatorio);
                indicePreguntaActual++; 
                
                JPanel panel = new JPanel(new GridLayout(0, 1, 5, 5));
                panel.add(new JLabel("¡FELICIDADES! LÍNEA #" + lineasCompletadas + " COMPLETA!"));
                panel.add(new JSeparator());
                
                // Usamos JTextArea con un tamaño fijo y JScrollPane para asegurar la visibilidad
                JTextArea areaPregunta = new JTextArea(pregunta.getTextoPregunta(), 3, 30); // 3 filas, 30 columnas
                areaPregunta.setWrapStyleWord(true);
                areaPregunta.setLineWrap(true);
                areaPregunta.setEditable(false);
                areaPregunta.setBackground(panel.getBackground());
                
                JScrollPane scrollPregunta = new JScrollPane(areaPregunta);
                scrollPregunta.setBorder(BorderFactory.createEmptyBorder());
                panel.add(scrollPregunta);
                panel.add(new JSeparator());
                
                // Muestra las opciones como etiquetas dentro del panel
                panel.add(new JLabel("Selecciona la respuesta:"));
                panel.add(new JLabel("A: " + pregunta.getOpcion(0))); 
                panel.add(new JLabel("B: " + pregunta.getOpcion(1))); 
                panel.add(new JLabel("C: " + pregunta.getOpcion(2))); 

                // Los botones de opción son solo las letras
                String[] opciones = { "A", "B", "C" }; 
                
                // Usamos DEFAULT_OPTION y mapeamos por índice para asegurar la visibilidad de los 3 botones
                int seleccion = JOptionPane.showOptionDialog(
                    this, 
                    panel, 
                    "¡BONUS POR LÍNEA!", 
                    JOptionPane.DEFAULT_OPTION, 
                    JOptionPane.QUESTION_MESSAGE, 
                    null, 
                    opciones, 
                    opciones[0] 
                );
                
                String respuestaUsuario = null;
                // Mapeamos la selección por índice (0=A, 1=B, 2=C)
                if (seleccion == 0) { respuestaUsuario = "A"; } 
                else if (seleccion == 1) { respuestaUsuario = "B"; } 
                else if (seleccion == 2) { respuestaUsuario = "C"; }

                if (pregunta.esRespuestaCorrecta(respuestaUsuario)) {
                    JOptionPane.showMessageDialog(this, "¡RESPUESTA CORRECTA! Ganaste puntos de sostenibilidad.", "Éxito", JOptionPane.INFORMATION_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(this, "Respuesta Incorrecta. La respuesta correcta era: " + pregunta.getRespuestaCorrecta(), "Fallaste", JOptionPane.ERROR_MESSAGE);
                }
            }
        }
        
        // Comprobar BINGO
        if (totalMarcados == 25) {
             JOptionPane.showMessageDialog(this, "¡¡BINGO!! ¡Has ganado!", "BINGO", JOptionPane.INFORMATION_MESSAGE);
             juegoTerminado = true;
             for(JButton btn : cartonBotones) btn.setEnabled(false);
             
             if (!esCantante && cliente != null) {
                 cliente.enviarMensaje("BINGO"); 
             }
        }
    }
}