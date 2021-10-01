/**
 * @author Waldo & Penzen
 */

public class ProcessServerTest {
    public static void main(String... args)
    {
        ProcessServer processServer = new ProcessServer();
        processServer.startServer();
        
        Client client = new Client();
        client.init();
    }
}