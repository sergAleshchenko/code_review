package org.example;


class Counter implements Runnable
{
    private int counter = 0;
    public void increment() {
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        counter++;
    }
    public void decrement() {
        counter--;
    }
    public int getValue() {
        return counter;
    }

    @Override
    public void run() {
        this.increment();
        System.out.println("Value for Thread After increment " + Thread.currentThread().getName() + " " + this.getValue());

        this.decrement();
        System.out.println("Value for Thread at last " + Thread.currentThread().getName() + " " + this.getValue());
    }
}

public class RaceConditionExample {
    public static void main(String args[]) {
        Counter counter = new Counter();
        Thread t1 = new Thread(counter, "Thread1");
        Thread t2 = new Thread(counter, "Thread2");
        Thread t3 = new Thread(counter, "Thread3");
        t1.start();
        t2.start();
        t3.start();
    }
}
