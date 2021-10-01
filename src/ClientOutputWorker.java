/**
 * @author Waldo & Penzen
 */

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.List;

/**
 * The ClientOutputWorker is an extension of the Worker class, it takes in the number of threads 
 * and list of notification queue and it  then send to the super class.
 */
public class ClientOutputWorker extends Worker {
    private Socket client;
    private ObjectOutputStream objectOutputStream;
    
    public ClientOutputWorker(Socket client, int num_threads, List<NotificationQueue<Task>> output_queues)
    {
        // output will be a
        super(num_threads, output_queues);
        this.client = client;
        
        try {
            objectOutputStream = new ObjectOutputStream(this.client.getOutputStream());
        } catch (IOException ex) {
            System.err.println("Client worker error: Could not open Output Stream. Is the socket closed?");
        }
    }
    
    /**
     * Broadcast a Task object over a connected Client socket connection.
     * @param task to broadcast
     */
    @Override
    public void processStep(Task task)
    {
        // Some real magic here....
        // This WorkerRunnable thread will try to output over the OutputStream.
        // If this fails, we know that the connection has been disconnected, which
        // therefore calls requestStop() for itself.
        
        // Since this thread will forever be idle until a new task is passed to it,
        // when a client disconnects, this thread remain 'active' but idling. This
        // thread will only terminate when a new client sends a Task through the 
        // processing pipeline, triggering the disconnection code in the catch
        // block. Not to worry, this thread does not use any CPU time when dormant.
        
        try 
        {                
            synchronized(objectOutputStream)
            {
                objectOutputStream.writeObject(task);      
            }
        }
        catch (IOException ex)
        {
            // client has disconnected
            this.requestStop();
            System.out.println("[Dispatcher] " + client.getInetAddress() + " has disconnected!");
        }
    }
}
