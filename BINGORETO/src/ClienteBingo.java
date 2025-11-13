import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.function.Consumer;

public class ClienteBingo {
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private Consumer<Integer> receptorNumero;

    public ClienteBingo(String host, int puerto, Consumer<Integer> receptor) throws IOException {
        this.receptorNumero = receptor;
        this.socket = new Socket(host, puerto);
        this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        this.out = new PrintWriter(socket.getOutputStream(), true); // Inicializa el escritor con autoflush
    }

    // Método para enviar mensajes al Cantante (usado para notificar el "BINGO")
    public void enviarMensaje(String mensaje) {
        if (out != null) {
            out.println(mensaje);
        }
    }

    public void recibir() {
        new Thread(() -> {
            try {
                String linea;
                while ((linea = in.readLine()) != null) {
                    try {
                        int numero = Integer.parseInt(linea.trim()); 
                        receptorNumero.accept(numero);
                    } catch (NumberFormatException e) {
                        System.out.println("Mensaje de control/Error recibido: " + linea);
                    }
                }
            } catch (IOException e) {
                System.err.println("Conexión con el servidor perdida.");
            }
        }).start();
    }
}