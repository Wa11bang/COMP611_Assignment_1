/**
 * @author Waldo & Penzen
 */

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * A Worker object which manages a Thread pool for processing Tasks.
 */
public abstract class Worker implements NotificationQueue.Listener<Task>, Runnable 
{
    private Queue<Task> input_queue = new LinkedBlockingQueue();
    private Lock lock = new ReentrantLock();
    private WorkerRunnable[] threads;
    private List<NotificationQueue<Task>> output_queues;
    
    private boolean running = true;

    /**
     * Constructor for a Worker object.
     * @param num_threads to create in Thread pool
     * @param output_queues to output processed tasks to.
     */
    public Worker(int num_threads, List<NotificationQueue<Task>> output_queues)
    {
        this.output_queues = output_queues;
        this.createThreads(num_threads);
        this.startUpdater();
    }

    /**
     * Internal helper method to start Worker thread.
     */
    private void startUpdater()
    {        
        Thread thread = new Thread(this);
        thread.start();
    }
    
    /**
     * Internal helper method to create WorkerRunnable threads.
     * @param num_threads to create
     */
    private void createThreads(int num_threads)
    {
        this.threads = new WorkerRunnable[num_threads];
        
        for(int i = 0; i < num_threads; ++i)
        {
            threads[i] = new Worker.WorkerRunnable();
            new Thread(threads[i]).start();
        }
    }
    
    /**
     * Method which adds a given Task object into the internal input queue.
     * @param task to add to queue.
     */
    public void process(Task task)
    {
        input_queue.add(task);
    }

    /**
     * Abstract method which is implemented by Sub-Workers and called by WorkerRunnable
     * threads.
     * @param task to process.
     */
    public abstract void processStep(Task task);

    /**
     * Method to initiate a safe, clean shutdown of all WorkerRunnable threads in
     * the pool.
     */
    public void requestStop()
    {
        for(WorkerRunnable thread : threads)
        {
            if(thread != null)
                thread.requestStop();
        }
        
        this.running = false;
    }

    /**
     * Method which adds a processed task to all specified output queues.
     * @param task to add to output queues
     */
    protected void addToQueues(Task task)
    {
        synchronized (output_queues)
        {
            for(NotificationQueue<Task> queue : output_queues)
            {
                queue.add(task);
            }
        }
    }
    
    @Override
    public void ping(Task obj) 
    {
        process(obj);        
    }
    
    @Override
    public void run() 
    {
        System.out.println(this.getClass().getName() + " ["+Thread.currentThread().getName()+"] created!");
        while(running)
        {
            if(!input_queue.isEmpty())
            {
                // notify threads
                synchronized(Worker.class)
                {
                    Worker.class.notifyAll();
                }
            }
                        
            try {
                Thread.sleep(50);
            } catch (InterruptedException ex) {
                /* Ignored */
            }
        }
            
        // release all waiting threads
        synchronized(Worker.class)
        {
            Worker.class.notifyAll();
        }
            
        System.out.println("["+Thread.currentThread().getName()+"] Worker Internal Updater stopped!");
    }
    
    /**
     * Implements locking / unlocking of the input_queue access. Calls the processStep
     * method while insure itself is the only thread processing the task.
     */
    private class WorkerRunnable implements Runnable
    {
        private volatile Task task;
        private boolean running = true;

        public WorkerRunnable()
        {
        }

        @Override
        public void run() 
        {
            while(running)
            {
                if(input_queue.isEmpty())
                {
                    System.out.println(Worker.this.getClass().getName()+" ["+Thread.currentThread().getName()+"] waiting!");
                    
                    // wait
                    synchronized(Worker.class)
                    {
                        try {
                            Worker.class.wait();
                        } catch (InterruptedException ignored) {
                            /* Ignored */
                        }
                    }
                }
                else
                {
                    // critical section - start
                    try {
                        lock.lock();
                        criticalSection();
                    } finally {
                        lock.unlock();
                    }
                    
                    // critical section - end
                    
                    if(task != null)
                    {
                        System.out.println(Worker.this.getClass().getName()+" ["+Thread.currentThread().getName()+"] working!");

                        processStep(task); // using pass by reference
                        addToQueues(task);
                    }
                }
                
                try {
                    Thread.sleep(50);
                } catch (InterruptedException ignored) {
                    /* Ignored */
                }
            }
            
            System.out.println("["+Thread.currentThread().getName()+"] Worker Runnable stopped!");
        }

        /**
         * Method which sets the internal task field to a given Task object.
         * @param task to assign
         */
        public void setTask(Task task)
        {
            this.task = task;
        }
        
        /**
         * This method polls a Task from the input queue, ensuring that only one
         * WorkerRunnable is polling at one time.
         */
        private void criticalSection()
        {
            setTask(input_queue.poll());
        }
        
        /**
         * Method to initiate a safe, clean shutdown of the current WorkerRunnable
         * thread.
         */
        public void requestStop()
        {
            this.running = false;
        }
    }
    
    /**
     * Object testing method.
     * Demonstrates the functionality of a Worker Object
     * @param args 
     */
    public static void main(String... args)
    {
        System.out.println("\n\n------- Worker Tester -------\n\n");
        
        // queue 1
        System.out.println("[Preparing Queue 1]");
        List<NotificationQueue<Task>> step1 = new ArrayList<>();
        NotificationQueue<Task> queue1 = new NotificationQueue<>(new LinkedBlockingQueue());
        step1.add(queue1);
        
        // queue 2
        System.out.println("[Preparing Queue 2]");
        List<NotificationQueue<Task>> step2 = new ArrayList<>();
        NotificationQueue<Task> queue2 = new NotificationQueue<>(new LinkedBlockingQueue());
        step2.add(queue2);
        
        // queue 3 (output)
        System.out.println("[Preparing Queue 3 (Output)]");
        List<NotificationQueue<Task>> output = new ArrayList<>();
        NotificationQueue<Task> queue3 = new NotificationQueue<>(new LinkedBlockingQueue());
        output.add(queue3);
        
        System.out.println("\n[Creating Factorial Worker with 1 sub-thread]");
        Worker worker = new FactorialWorker(1, step2);
        System.out.println("[Creating Prime Worker with 1 sub-thread]");
        Worker worker2 = new PrimeWorker(1, output);
        System.out.println("[Creating Print Worker with 1 sub-thread]");
        Worker worker3 = new PrintWorker(1, new ArrayList<>());

        System.out.println("\n[Adding Factorial Worker to Queue 1 Listeners]");
        queue1.addListener(worker);
        
        System.out.println("[Adding Prime Worker to Queue 2 Listeners]");
        queue2.addListener(worker2);

        System.out.println("[Adding Print Worker to Output Queue Listeners]");
        queue3.addListener(worker3);
        
        System.out.println("[Creating 5 testing Task objects] \n\n");
        queue1.add(new Task(1, 1));
        queue1.add(new Task(2, 2));
        queue1.add(new Task(3, 4));
        queue1.add(new Task(4, 6));
        queue1.add(new Task(5, 8));

             
        System.out.println("\n\n------- Results -------\n\n");
        

        try {
            Thread.sleep(5000);
        } catch (InterruptedException ex) {
            
        }
        
        System.out.println("\n\n------- Requesting Stop -------\n\n");
        worker.requestStop();
        worker2.requestStop();
        worker3.requestStop();
    }
}
