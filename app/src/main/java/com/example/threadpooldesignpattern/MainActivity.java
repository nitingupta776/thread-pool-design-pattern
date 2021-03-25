package com.example.threadpooldesignpattern;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.os.Process;
import android.util.Log;

import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;


public class MainActivity extends AppCompatActivity {
    private int queue1TaskCount = 0;
    private long queue1SysTime = 0;
    private int queue2TaskCount = 0;
    private long queue2SysTime = 0;
    //ThreadPoolExecutor executor = null;
    Random rand = new Random();

    private Future group1Future = null;
    private Future group2Future = null;

    private LinkedBlockingQueue<PrioritizedTask> group1Queue
            = new LinkedBlockingQueue<>();
    private LinkedBlockingQueue<PrioritizedTask> group2Queue
            = new LinkedBlockingQueue<>();

    private ExecutorService executor = Executors.newFixedThreadPool(2);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //executor = new ThreadPoolExecutor(1, 2, 3000, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(), new ThreadPoolExecutor.CallerRunsPolicy());
        new TrafficGenerator().start();

        startProcessing();
    }

    public void startProcessing() {
        while (true) {
            if (group1Future != null && group1Future.isDone()) {
                if (group1Queue.peek() != null) {
                    group1Future = executor.submit(group1Queue.poll());
                }
            }
            if (group2Future != null && group1Future.isDone()) {
                if (group2Queue.peek() != null) {
                    group2Future = executor.submit(group2Queue.poll());
                }
            }
        }
    }

    public static int poisson(double lambda) {
        double L = Math.exp(-lambda);
        double p = 1.0;
        int k = 0;
        do {
            k++;
            p *= Math.random();
        } while (p > L);
        return k - 1;
    }

    public double exponential(double lambda) {
        return  Math.log(1-rand.nextDouble())/(-lambda);
    }

    private class PrioritizedTask implements Runnable {
        int taskNum;
        long startTime;
        int avgDuration;
        int priority;
        int queue;

        PrioritizedTask(int taskNum, long startTime, int avgDuration, int priority, int queue) {
            this.taskNum = taskNum;

            this.startTime = startTime;
            this.avgDuration = avgDuration;
            this.priority = priority;
            this.queue = queue;
            //Log.i("ThreadPool Test", "Task No: " + taskNum + " Priority: " + priority + " Created at Time: " + startTime);
        }

        public void run() {
            try {
                Process.setThreadPriority(this.priority);
            } catch (Throwable t) {

            }
            int duration = (int) exponential(this.avgDuration) * 1000;

            //  Perform some operations proportional to the duration
            for (int i = 0; i < duration; i++) {
            }

            //Compute the average system time
            if (this.priority == Process.THREAD_PRIORITY_FOREGROUND) {
                queue1TaskCount++;
                queue1SysTime += (System.currentTimeMillis() - this.startTime);
            }else{
                queue2TaskCount++;
                queue2SysTime += (System.currentTimeMillis() - this.startTime);
            }
            //Log.i("ThreadPool Test", "Task No: " + taskNum + " Priority: " + priority + " Completed at:: " +System.currentTimeMillis()+ " Queue #: " +queue);
            if (this.taskNum % 500 == 0) {
                Log.i("ThreadPool", "Queue 1 Task Count = " + queue1TaskCount + " sysTime = " + queue1SysTime + " Average Sys Time = " + (queue1SysTime / queue1TaskCount));
                queue1TaskCount = 0;
                queue1SysTime = 0;
                Log.i("ThreadPool", "Queue 2 Task Count = " + queue2TaskCount + " sysTime = " + queue2SysTime + " Average Sys Time = " + (queue2SysTime / queue2TaskCount));
                queue2TaskCount = 0;
                queue2SysTime = 0;
            }
        }
    }
    class TrafficGenerator extends Thread {
        // get the thread pool manager instance
        public void run() {
            for (int i = 1; i <=5000; i++) {
                int delay = poisson(2);
                try {
                    Thread.sleep(delay);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                int j = rand.nextInt(2);
                int finalI = i;
                if (j % 2 == 0) {
                    // This one contains Thread Priority: -2
                    group1Future = executor.submit(() -> {
                        group1Queue.add(new PrioritizedTask(finalI, System.currentTimeMillis(),20, Process.THREAD_PRIORITY_FOREGROUND,1));
                        return null;
                    });
                    //group1Future = executor.submit(new PrioritizedTask(i, System.currentTimeMillis(),20, Process.THREAD_PRIORITY_FOREGROUND,2));

                } else {
                    // This one contains Thread Priority: 10
                    group2Future = executor.submit(() -> {
                        group2Queue.add(new PrioritizedTask(finalI, System.currentTimeMillis(),20, Process.THREAD_PRIORITY_BACKGROUND,2));
                        return null;
                    });
                   // group2Future = executor.submit(new PrioritizedTask(i, System.currentTimeMillis(),20, Process.THREAD_PRIORITY_FOREGROUND,2));

                }
            }
        }
    }
}