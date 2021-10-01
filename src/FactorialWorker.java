/**
 * @author Waldo & Penzen
 */

import java.util.List;

/**
 * The FactorialWorker is an extension of the Worker class, it takes in the number of threads 
 * and list of notification queue and it  then send to the super class.
 */
public class FactorialWorker extends Worker
{
    public FactorialWorker(int num_threads, List<NotificationQueue<Task>> output_queues)
    {
        super(num_threads, output_queues);
    }

    /**
     * Calculates the factorial of the given value stored in the Task parameter. 
     * @param task 
     */
    @Override
    public void processStep(Task task)
    {
        long factorial = task.getValue();
        for(int i = 0; i < task.getFactorial(); ++i)
        {
            factorial *= (i + 1);
        }
        
        task.setValue(factorial);
    }
}