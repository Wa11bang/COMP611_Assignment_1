/**
 * @author Waldo & Penzen
 */

import java.util.List;

/**
 * The PrintWorker is an extension of the Worker class, it takes in the number of threads 
 * and list of notification queue and it  then send to the super class.
 */
public class PrintWorker extends Worker 
{
    public PrintWorker(int num_threads, List<NotificationQueue<Task>> output_queues)
    {
        super(num_threads, output_queues);
    }
    /**
     * Prints out the Task which then calls the customized toString
     * method. 
     * @param task 
     */
    
    @Override
    public void processStep(Task task)
    {
        System.out.println(task);
    }
}