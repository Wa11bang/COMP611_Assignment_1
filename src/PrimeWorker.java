/**
 * @author Waldo & Penzen
 */

import java.util.List;

/**
 * The PrimeWorker is an extension of the Worker class, it takes in the number of threads 
 * and list of notification queue and it  then send to the super class.
 */
public class PrimeWorker extends Worker
{
    public PrimeWorker(int num_threads, List<NotificationQueue<Task>> output_queues)
    {
        super(num_threads, output_queues);
    }
    
    /**
     * Checks if a Task's calculated Factorial is a Factorial Prime (n - 1) and (n + 1).
     * @param task 
     */
    @Override
    public void processStep(Task task) 
    {
        boolean isPrime = false;

        if(checkForPrime(task.getValue() + 1))
        {
            isPrime = true;
        }
        else
        {
            isPrime = checkForPrime(task.getValue() - 1);
        }

        task.setPrime(isPrime);           
    }        
    
    private boolean checkForPrime(long inputNumber)
    {
        if(inputNumber <= 1) 
        {
            return false;
        }
        else
        {
            for (long i = 2; i <= inputNumber/2; i++) 
            {
                if ((inputNumber % i) == 0)
                {
                    return false;
                }
            }

            return true;
        }
    }
}
