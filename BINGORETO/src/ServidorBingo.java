import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ServidorBingo {
    private ServerSocket serverSocket;
    private List<PrintWriter> clientes; 

    public ServidorBingo(int puerto) throws IOException {
        serverSocket = new ServerSocket(puerto);
        clientes = Collections.synchronizedList(new ArrayList<>()); 
    }

    public void iniciar() {
    
        new Thread(() -> {
            try {
                System.out.println("Servidor de Bingo iniciado. Esperando conexiones en el puerto " + serverSocket.getLocalPort() + "...");
                
 
                while(true) {
                    Socket clienteSocket = serverSocket.accept();
                    System.out.println("Nuevo Jugador conectado: " + clienteSocket.getInetAddress());


                    PrintWriter out = new PrintWriter(clienteSocket.getOutputStream(), true);
                    clientes.add(out);
                    

                }

            } catch (IOException e) {

                System.err.println("Error en el bucle del servidor: " + e.getMessage());
            }
        }).start();
    }

    public void enviarNumero(int numero) {
        String mensaje = "NUMERO:" + numero;
    
        for (PrintWriter cliente : clientes) {
            try {
                cliente.println(mensaje);
            } catch (Exception e) {
     
                System.err.println("Error al enviar a un cliente.");
            }
        }
    }
}