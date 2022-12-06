package org.example;

public class DeadlockExample {
    public static Object lock1 = new Object();
    public static Object lock2 = new Object();

    public static void main(String args[]) {
        Thread1 thread1 = new Thread1();
        Thread2 thread2 = new Thread2();
        thread1.start();
        thread2.start();
    }

    private static class Thread1 extends Thread {
        public void run() {
            synchronized (lock1) {
                System.out.println("Thread1: hold lock1");

                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {}
                System.out.println("Thread1: wait lock2");

                synchronized (lock2) {
                    System.out.println("Thread1: hold lock1 and lock2");
                }
            }
        }
    }
    private static class Thread2 extends Thread {
        public void run() {
            synchronized (lock2) {
                System.out.println("Thread2: hold lock2");

                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {}
                System.out.println("Thread2: wait lock1");

                synchronized (lock1) {
                    System.out.println("Thread2: hold lock1 and lock2");
                }
            }
        }
    }
}
