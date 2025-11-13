import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.function.Consumer;

public class ClienteBingo {
    private Socket socket;
    private BufferedReader in;
    private Consumer<Integer> receptorNumero;

    public ClienteBingo(String host, int puerto, Consumer<Integer> receptor) throws IOException {
        this.receptorNumero = receptor;
        this.socket = new Socket(host, puerto);
        this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
    }

    public void recibir() {
        new Thread(() -> {
            try {
                String linea;
                while ((linea = in.readLine()) != null) {
                    if (linea.startsWith("NUMERO:")) {
                        String numStr = linea.substring("NUMERO:".length());
                        int numero = Integer.parseInt(numStr.trim());
                        
                        receptorNumero.accept(numero);
                    }
                }
            } catch (IOException e) {
                System.err.println("Conexión con el servidor perdida.");
            }
        }).start();
    }
}