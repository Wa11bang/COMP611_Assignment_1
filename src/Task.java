/**
 * @author Waldo & Penzen
 */

import java.io.Serializable;

/**
 * The Task class has variables that will be used by the workers to computer the prime number.
 * We have implemented Serializable because we are going to be  sending the tasks through the 
 * Server in bytes, Serializable will help us revert this object back to it's original form.
 * 
 */

public class Task implements Serializable 
{
    private final int identifier;
    private final int factorial;
    
    private int current = 1;
    private boolean isPrime = false;
    private long value = 1;

    public Task(int identifier, int factorial)
    {
        this.identifier = identifier;
        this.factorial = ((factorial == 0) ? 1 : factorial);
    }

    /**
     * The methods below are the getters and setter for the private variables above. 
     * 
     */
    public long getValue()
    {
        return this.value;
    }

    public void setValue(long value)
    {
        this.value = value;
    }

    public int getIdentifier() 
    {
        return this.identifier;
    }
    
    public int getFactorial() 
    {
        return this.factorial;
    }
    
    public int getCurrent()
    {
        return this.current;
    }
    
    public void setCurrent(int current)
    {
        this.current = current;
    }
    
    public boolean isPrime()
    {
        return this.isPrime;
    }
    
    public void setPrime(boolean isPrime)
    {
        this.isPrime = isPrime;
    }
    
    @Override
    public String toString()
    {
        return ("Task("+this.getIdentifier()+") - val("+this.getValue()+") isPrime("+this.isPrime()+")");
    }
}