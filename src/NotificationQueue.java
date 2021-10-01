/**
 * @author Waldo & Penzen
 */

import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * A NotificationQueue object decorates an AbstractQueue which implements the 
 * Observer Pattern to notify any listening objects.
 */
public class NotificationQueue<E> extends AbstractQueue<E>
{
    /**
     * A Listener interface which contains a callable ping method.
     * @param <E> 
     */
    public interface Listener<E> {
        /**
         * Object passed to listening object.
         * @param obj 
         */
        public void ping(E obj);
    }
    
    protected Queue<E> queue;
    private final List<Listener<E>> listeners = new ArrayList<>();
    private boolean running = true;

    /**
     * Constructor for a NotificationQueue object.
     * @param queue to decorate.
     */
    public NotificationQueue(final Queue<E> queue)
    {
        super();
        this.queue = queue;

        // Start internal thread
        this.startUpdater();
    }
    
    /**
     * Internal helper method to start the peeking updater thread.
     */
    private final void startUpdater()
    {
        Thread thread = new Thread(new InternalUpdater());
        thread.start();
    }
    
    /**
     * This method adds a given Listener object into listeners list.
     * @param listener to add.
     * @return whether successful
     */
    public boolean addListener(Listener<E> listener)
    {
        return this.listeners.add(listener);
    }

    /**
     * This method removes a given Listener object from the listeners list.
     * @param listener to remove
     * @return whether successful
     */
    public boolean removeListener(Listener<E> listener)
    {
        return this.listeners.remove(listener);
    }

    /**
     * This method passes an object through to all of the Listeners in the
     * listeners list.
     * @param obj to pass through
     */
    public void notifyAll(E obj)
    {
        for(Listener listener : listeners) {
            listener.ping(obj);
        }
    }

    @Override
    public boolean add(E e) {
        return this.queue.add(e);
    }

    @Override
    public Iterator<E> iterator() {
        return this.queue.iterator();
    }

    @Override
    public int size() {
        return this.queue.size();
    }

    @Override
    public boolean offer(E arg0) {
        return this.queue.offer(arg0);
    }

    @Override
    public synchronized E poll() {
        return this.queue.poll();
    }

    @Override
    public E peek() {
        return this.queue.peek();
    }
    
    /**
     * Method to initiate a safe, clean shutdown of the InternalUpdater thread.
     */
    public void requestStop()
    {
        this.running = false;
    }
    
    /**
     * This InternalUpdater will frequently check to see whether the decorated
     * queue is not empty. If not empty, the next element in the queue is passed
     * to all Listeners via notifyAll.
     */
    private class InternalUpdater implements Runnable {

        @Override
        public void run() {
            while(running)
            {
                if(!queue.isEmpty())
                {
                    NotificationQueue.this.notifyAll(NotificationQueue.this.poll());                    
                }
                
                try {
                    Thread.sleep(50);
                } catch (InterruptedException ex) {
                    /* Ignored */
                }
                
            }
        }        
    }
    
    /**
     * Object testing method.
     * Demonstrates the functionality of a NotificationQueue Object
     * @param args 
     */
    public static void main(String... args)
    {
        NotificationQueue<String> queue = new NotificationQueue(new LinkedBlockingQueue());
        
        Listener listener1 = new Listener(){
            @Override
            public void ping(Object obj) {
                System.out.println("[Listener 1]: notified for - " + obj);
            }            
        };
        
        Listener listener2 = new Listener(){
            @Override
            public void ping(Object obj) {
                System.out.println("[Listener 2]: notified for - " + obj);
            }            
        };
        
        queue.addListener(listener1);
        queue.addListener(listener2);
        
        Thread testThread = new Thread(new Runnable(){
            @Override
            public void run() {
                for(int i = 0; i < 5; ++i)
                {                
                    queue.add("Hello " + i);
                    
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException ignored) {
                        
                    }
                }
                
                System.out.println("Removing Listener 1 !!!");
                queue.removeListener(listener1);
                queue.add("Test");
            }
            
        });
        testThread.start();
    }
}
