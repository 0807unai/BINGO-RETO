import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

public class ServidorBingo {
    private ServerSocket serverSocket;
    private List<PrintWriter> clientWriters; 
    private int puerto;
    
    // Referencia al método setJuegoTerminado en la clase Bingo (Cantante)
    private Consumer<Boolean> juegoTerminadoSetter; 

    // Constructor que acepta el puerto y el Consumer (requerido por Bingo.java)
    public ServidorBingo(int puerto, Consumer<Boolean> juegoTerminadoSetter) throws IOException {
        this.puerto = puerto;
        this.juegoTerminadoSetter = juegoTerminadoSetter;
        serverSocket = new ServerSocket(puerto);
        clientWriters = Collections.synchronizedList(new ArrayList<>()); 
    }

    public void iniciar() {
    
        new Thread(() -> {
            try {
                System.out.println("Servidor de Bingo iniciado. Esperando conexiones en el puerto " + serverSocket.getLocalPort() + "...");
                
 
                while(true) {
                    Socket clienteSocket = serverSocket.accept();
                    System.out.println("Nuevo Jugador conectado: " + clienteSocket.getInetAddress());


                    PrintWriter out = new PrintWriter(clienteSocket.getOutputStream(), true);
                    clientWriters.add(out);
                    
                    // CRÍTICO: Iniciar un hilo para ESCUCHAR a CADA cliente
                    new Thread(new ClientHandler(clienteSocket, this)).start(); 
                }

            } catch (IOException e) {

                System.err.println("Error en el bucle del servidor: " + e.getMessage());
            }
        }).start();
    }

    public void enviarNumero(int numero) {
        String mensaje = String.valueOf(numero);
    
        for (PrintWriter cliente : new ArrayList<>(clientWriters)) {
            try {
                cliente.println(mensaje);
            } catch (Exception e) {
                System.err.println("Error al enviar a un cliente. Removiendo.");
                clientWriters.remove(cliente); 
            }
        }
    }
    
    // Clase interna para manejar la comunicación bidireccional con cada Jugador
    private static class ClientHandler implements Runnable {
        private Socket socket;
        private ServidorBingo servidor;

        public ClientHandler(Socket socket, ServidorBingo servidor) {
            this.socket = socket;
            this.servidor = servidor;
        }

        @Override
        public void run() {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
                String mensaje;
                while ((mensaje = reader.readLine()) != null) {
                    System.out.println("Mensaje de Jugador: " + mensaje);

                    if (mensaje.trim().equalsIgnoreCase("BINGO")) {
                        // Detener el juego del Cantante
                        if (servidor.juegoTerminadoSetter != null) {
                            servidor.juegoTerminadoSetter.accept(true); 
                        }
                        break; 
                    }
                }
            } catch (IOException e) {
                System.err.println("Un jugador se desconectó inesperadamente.");
            } finally {
                try {
                    // Remover el cliente de la lista de envío
                    servidor.clientWriters.remove(new PrintWriter(socket.getOutputStream(), true));
                    socket.close();
                } catch (IOException ignored) {}
            }
        }
    }
}