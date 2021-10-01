/**
 * @author Waldo & Penzen
 */

import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

/*
    Process Diagram

    Client -> ProcessServer (Input)
    Client -> ProcessServer (Output)

    Client -> Task (Input)

    ProcessServer -> Task -> Input Queues
    FactorialWorkers -> Input Queues -> Task -> Process -> Intermediatary Queues
    PrimeWorkers -> Intermediatary Queues -> Task -> Process -> Output Queues

    ClientOutputWorker -> Output Queues -> Task (Output)
*/

/**
 * A ProcessServer object manages incoming and outgoing socket connections.
 * Default port 420 for Outgoing
 * Default port 422 for Incoming
 * @author Waldo
 */
public class ProcessServer {    
    private static final int DISPATCHER_PORT = 420;
    private static final int RECEIVER_PORT = 422;
    
    private static List<Worker> workers = new ArrayList();
    private List<InputHandler> clients = new ArrayList();
        
    private ServerSocket dispatcherSocket;
    private ServerSocket receiverSocket;
    private Thread dispatcher;
    private Thread receiver;
    
    private boolean running = false;
    
    private List<NotificationQueue<Task>> input;
    private List<NotificationQueue<Task>> output;

    public ProcessServer()
    {
    }
    
    /**
     * Internal Helper method which is used to create the necessary NotificationQueues for
     * the processing pipeline. The processing Workers are created here as well.
     */
    private void setup()
    {
        // Input Queue List contains all queues which start the pipeline.
        input = new ArrayList<>();
        NotificationQueue<Task> queue1 = new NotificationQueue<>(new LinkedBlockingQueue());
        input.add(queue1);

        // Intermediatary Queue List containing sub-lists which are used between tasks.
        List<NotificationQueue<Task>> step2 = new ArrayList<>();
        NotificationQueue<Task> queue2 = new NotificationQueue<>(new LinkedBlockingQueue());
        step2.add(queue2);

        // Output Queue List which contains all queues that are at the end of the pipeline
        output = new ArrayList<>();
        NotificationQueue<Task> queue3 = new NotificationQueue<>(new LinkedBlockingQueue());
        output.add(queue3);

        Worker factorialWorker = new FactorialWorker(10, step2);
        Worker primeWorker = new PrimeWorker(10, output);

        queue1.addListener(factorialWorker);
        queue2.addListener(primeWorker);
            
        workers.add(factorialWorker);
        workers.add(primeWorker);
    }
    
    /**
     * Starts initializing the server but creating two threads both of which
     * accept new client connections. (Input / Output).
     */
    public void startServer()
    {
        running = true;
        
        dispatcher = new Thread(new Runnable(){
            @Override
            public void run() {
                ServerSocket dispatcherSocket = null;
                try
                {  
                    dispatcherSocket = new ServerSocket(DISPATCHER_PORT);
                    
                    System.out.println("Dispatcher started at " + InetAddress.getLocalHost() + " on port " + DISPATCHER_PORT);
                    
                }
                catch (IOException e)
                {
                    System.err.println("Server can't listen on port: " + e);
                    System.exit(-1);
                }
                try
                {  
                    while (running)
                    {  
                        Socket dispatcherClient = dispatcherSocket.accept();

                        System.out.println("Dispatcher: Connection made with " + dispatcherClient.getInetAddress());                         
                        
                        // Adds new ClientOutputWorker which will handle sending tasks
                        // back to Clients. Have used an empty anonymous ArrayList as 
                        // this is the final step.
                        Worker worker = new ClientOutputWorker(dispatcherClient, 1, new ArrayList());
                        
                        for(NotificationQueue q : output)
                        {
                            q.addListener(worker);
                        }
                        
                        workers.add(worker);
                    }

                    dispatcherSocket.close();
                }
                catch (IOException e)
                {  
                    System.err.println("Can't accept client connection: " + e);
                }
                
                System.out.println("Dispatcher has closed");
            }            
        });
        
        receiver = new Thread(new Runnable(){
            @Override
            public void run() {
                ServerSocket receiverSocket = null;
                try
                {  
                    receiverSocket = new ServerSocket(RECEIVER_PORT);
                    System.out.println("Receiver started at " + InetAddress.getLocalHost() + " on port " + RECEIVER_PORT);   
                }
                catch (IOException e)
                {  
                    System.err.println("Server can't listen on port: " + e);
                    System.exit(-1);
                }
                try
                {  
                    while (running)
                    {
                        Socket receiverClient = receiverSocket.accept();

                        System.out.println("Receiver: Connection made with " + receiverClient.getInetAddress());

                        InputHandler client = new InputHandler(receiverClient);
                        Thread thread = new Thread(client);
                        thread.start();
                        
                        // Keep record of all connected clients for future use.
                        clients.add(client);
                    }
                    
                    receiverSocket.close();
                }
                catch (IOException e)
                {  
                    System.err.println("Can't accept client connection: " + e);
                }
                System.out.println("Receiver has closed");
            }
            
        });
        
        dispatcher.start();
        receiver.start();
        
        this.setup();
        
    }
    
    /**
     * Method to initiate a safe, clean shutdown of all Workers and InputHandlers.
     */
    public void stopServer()
    {
        // Shutdown InputHandlers and Server Threads
        this.running = false;
        
        // Shutdown Workers
        for(Worker worker : workers)
        {
            worker.requestStop();
        }
        
        // technically the server wont stop due to threads being in the NotificationQueues
        // therefore we have implemented similar requestStop methods for NotificationQueues
        // however did not know whether it was necessary to stop them.
    }
    
    /**
     * InputHandler manages incoming Task objects from a specific client, adding them
     * to the processing pipeline.
     */
    public class InputHandler implements Runnable {
        private Socket client;
        private Task task;
        
        public InputHandler(Socket socket)
        {
            this.client = socket;
        }
        
        @Override
        public void run() {
            ObjectInputStream ois;
                        
            try 
            {
                ois = new ObjectInputStream(client.getInputStream());              
                
                do {                    
                    if(client.getInputStream().available() > 0)
                    {
                        task = ((Task)ois.readObject());      

                        // kill switch
                        if(task.getFactorial() == 69420)
                        {
                            // stop server
                            ProcessServer.this.stopServer();
                            break;
                        }
                    
                        for(Queue queue : input)
                        {
                            queue.add(task);
                        }
                    }
                    
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException ex) {
                        // Ignored
                    }
                    
                } while(isSocketConnected(client) && running);                
               
                // client has disconnected
                System.out.println("[Receiver] " + client.getInetAddress() + " has disconnected!");
                
                client.close();
                ois.close();
                clients.remove(this);
                
            }
            catch (IOException | ClassNotFoundException ex)
            {
                System.out.println("[Receiver] " + client.getInetAddress() + " has disconnected!");
                clients.remove(this);
                
                System.err.println("Server thread error: " + ex);   
            }
        }
    }
    
    /**
     * Internal helper method which is used to determine whether a given socket
     * has been disconnected.
     * @param socket to check
     * @return whether the socket is closed
     */
    private boolean isSocketConnected(Socket socket)
    {
        boolean connected = true;
        try {
            socket.getOutputStream().write(0);
        } catch (IOException ex) {
            connected = false;
        }
        
        return connected;
    }
    
    /**
     * Object testing method.
     * Demonstrates the functionality of a ProcessServer Object
     * @param args 
     */
    public static void main(String[] args)
    {
        ProcessServer server = new ProcessServer();
        server.startServer();
    }
}
