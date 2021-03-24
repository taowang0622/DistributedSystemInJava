public class ThreadA {
    public static void main(String[] args) throws InterruptedException {
        ThreadB b = new ThreadB();//<----Create Instance for seconde class

        // b.run() just runs the business code inside the run() on the current thread
        // b.start() will create a thread of instructions from run() in JAVA multi-threading infrastructure
        // ==> creating a thread is expensive and time-consuming!
        b.start();//<--------------------Launch thread

        synchronized(b){
            try{
                System.out.println("Waiting for b to complete...");
                //b.wait() will release the lock on "b"
                b.wait();//<-------------WAIT until the thread of class B finishes
            }catch(InterruptedException e){
                e.printStackTrace();
            }
            System.out.println("Total is: " + b.total);
        }

        Thread.sleep(10000);
    }
}