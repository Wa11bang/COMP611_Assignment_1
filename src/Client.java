/**
 * @author Waldo & Penzen
 */

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Scanner;

/**
 * Client object which is used to connect with a ProcessServer and submit Tasks
 * which need to be 'processed'. A Client is multi-threaded with safe handling of
 * threads.
 */
public class Client
{
    private int id;
    private List<Integer> taskIDs = new ArrayList();
    
    private String hostName;
    private int hostPort;
    private String receiverHostName;
    private int receiverHostPort;
    
    private final Scanner keyboardInput = new Scanner(System.in);
    
    private Thread receiver;
    private Thread sender;
    
    private Socket socket;
    private Socket receiverSocket;
    
    private Status senderSocketStatus;
    private Status receiverSocketStatus;    

    /**
     * Enumeration of different states which Client socket connections can currently
     * be in.
     */
    enum Status {
        INITIALISED,
        CONNECTING,
        CONNECTED,
        REQUESTED_STOP,
        CLOSED,
        FAILED
    }

    /**
     * Constructor for a Client object that generates a random positive integer
     * between 1 and 1000 as the Client's ID.
     */
    public Client()
    {
        // generate random client ID
        this.id = (int) ((Math.random() * 1000)) + 1;
    }

    /**
     * Internal helper method which is used to request the user to enter connection
     * details for the Processing Server. 
     * 
     * Defaults:
     *  localhost:422 for the input connection
     *  localhost:420 for the output connection
     * 
     */
    private void requestInput()
    {        
        System.out.println("Client interface used to communicate with a ProcessServer. \n\nTasks are 2-stage calculations starting with a factorial calculation,\nfollowed by checking whether the value is a Factorial Prime (n - 1) & (n + 1).\n");
        System.out.print("Please enter the server input hostname and port (localhost:422): ");
        String[] address = keyboardInput.nextLine().split(":");
        if(address.length == 1)
        {
            hostName = "localhost";
            hostPort = 422;
        }
        else
        {
            hostName = address[0];
            hostPort = Integer.parseInt(address[1]);
        }
            
        System.out.print("Please enter the server output hostname and port (localhost:420): ");
        address = keyboardInput.nextLine().split(":");
        if(address.length == 1)
        {
            receiverHostName = "localhost";
            receiverHostPort = 420;
        }
        else
        {
            receiverHostName = address[0];
            receiverHostPort = Integer.parseInt(address[1]);
        }
    }

    /**
     * This method will start requesting the user to connect to a ProcessServer.
     * Two threads - one for receiving and one for sending Tasks over the socket.
     */
    public void init()
    {
        requestInput();

        senderSocketStatus = Status.INITIALISED;
        receiverSocketStatus = Status.INITIALISED;
        try
        {
            senderSocketStatus = Status.CONNECTING;
            receiverSocketStatus = Status.CONNECTING;
            
            socket = new Socket(hostName, hostPort);
            System.out.println("Client Input socket connected...");
            
            receiverSocket = new Socket(receiverHostName, receiverHostPort);
            System.out.println("Client Output socket connected...");
            
            senderSocketStatus = Status.CONNECTED;
            receiverSocketStatus = Status.CONNECTED;
        }
        catch (IOException e)
        {
            System.err.println("Client could not make connection: " + e);
            senderSocketStatus = Status.FAILED;
            receiverSocketStatus = Status.FAILED;
            return;
        }

        sender = new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                ObjectOutputStream oos; // output stream to server
                try
                {
                    oos = new ObjectOutputStream(socket.getOutputStream());
                    
                    do {   
                        System.out.println("Enter a whole number: ");
                        String line = keyboardInput.nextLine();
                        
                        // client wants to quit. lets request to stop both client threads
                        if(line.toLowerCase().contains("quit"))
                        {
                            senderSocketStatus = Status.REQUESTED_STOP;
                            receiverSocketStatus = Status.REQUESTED_STOP;
                            break;
                        }
                        
                        int num = 0;
                        
                        try {
                            num = Integer.parseInt(line);
                        } catch (NumberFormatException ex)
                        {
                            System.out.println("Please enter a whole number greater than 0!");
                            continue;
                        }

                        // User made task
                        // Random task ID generation
                        int taskID = new Random().nextInt(100) + Client.this.hashCode() + Client.this.id;
                        
                        Task task = new Task(taskID, num);

                        oos.writeObject(task);
                        taskIDs.add(taskID); 
                        
                        // Computer generated tasks
                        /*for(int i = 1; i < 11; ++i)
                        {
                            taskID = new Random().nextInt(100);
                            task = new Task(taskID, i);

                            oos.writeObject(task);
                            taskIDs.add(taskID);                           
                        }*/                      
                    }
                    while(senderSocketStatus == Status.CONNECTED);

                    oos.close();
                    socket.close();
                    senderSocketStatus = Status.CLOSED;
                }
                catch (IOException e)
                {
                    senderSocketStatus = Status.FAILED;
                    receiverSocketStatus = Status.REQUESTED_STOP;
                    System.err.println("Client error: " + e);
                }
                System.out.println("Client message dispatcher terminated...");
            }
        });

        sender.start();
        
        receiver = new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                ObjectInputStream ois; // input stream to server
                try
                {
                    ois = new ObjectInputStream(receiverSocket.getInputStream());
                    
                    do {
                        if(receiverSocket.getInputStream().available() > 0)
                        {
                            Task task = ((Task)ois.readObject());
                            if(taskIDs.contains(task.getIdentifier()))
                            {                  
                                
                                System.out.println("\n--------------------");
                                System.out.println(task.toString());
                                System.out.println("--------------------\n");
                                
                                taskIDs.remove( taskIDs.indexOf(task.getIdentifier()) );
                            }                           
                        }
                        
                        try {
                            Thread.sleep(50);
                        } catch (InterruptedException ex) {
                            /* Ignored */
                        }
                    }
                    while(receiverSocketStatus == Status.CONNECTED);

                    ois.close();
                    receiverSocket.close();
                    receiverSocketStatus = Status.CLOSED;
                }
                catch (IOException e)
                {
                    receiverSocketStatus = Status.FAILED;
                    senderSocketStatus = Status.REQUESTED_STOP;
                    System.err.println("Client error: " + e);
                } catch (ClassNotFoundException ex) {
                    
                }
                System.out.println("Client message receiver terminated...");
            }
        });

        receiver.start();
    }

    /**
     * Object testing method.
     * Demonstrates the functionality of a Client Object
     * @param args 
     */
    public static void main(String... args)
    {
        Client client = new Client();
        client.init();
    }
}
