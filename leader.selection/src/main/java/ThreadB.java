public class ThreadB extends Thread {
    int total;

    @Override
    public void run() {
        synchronized (this) {
            for (int i = 0; i < 100; i++) {
                total += i;
            }
            // this.notify() would wake up the waiting threads?????
            this.notify();//<----------------Notify the class which wait until my    finish
//and tell that I'm finish
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println("ThreadB is finished!!");
        }
    }
}
