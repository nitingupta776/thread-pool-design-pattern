package com.example.threadpooldesignpattern;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.os.Process;
import android.util.Log;

import java.util.Random;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static android.os.Process.THREAD_PRIORITY_BACKGROUND;

public class MainActivity extends AppCompatActivity {
    private int taskCount = 0;
    private long sysTime = 0;
    ThreadPoolExecutor executor = null;
    Random rand = new Random();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        executor = new ThreadPoolExecutor(1, 1, 3000, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>(), new ThreadPoolExecutor.CallerRunsPolicy());
        new TrafficGenerator().start();
    }
    public static int poisson(double lambda) {
        double L = Math.exp(-lambda); double p = 1.0; int k = 0;
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

        PrioritizedTask(int taskNum, long startTime, int avgDuration, int priority) {
            this.taskNum = taskNum;

            this.startTime = startTime;
            this.avgDuration = avgDuration;
            this.priority = priority;
            Log.i("ThreadPool Test", "Task No: " + taskNum + " Priority: " + priority + " Created at Time: " + startTime);
        }

        public void run() {
            try {
                Process.setThreadPriority(this.priority);
            } catch (Throwable t) { }
            int duration = (int) exponential(this.avgDuration) * 1000;

            //  Perform some operations proportional to the duration
            String str = "";
            for (int i = 0; i < duration; i++) { str += "*"; }

            //Compute the average system time
            if (this.priority == Process.THREAD_PRIORITY_FOREGROUND) {
                taskCount++;
                sysTime += (System.currentTimeMillis() - this.startTime);
            }
            Log.i("ThreadPool Test", "Task No: " + taskNum + " Priority: " + priority + " Completed at:: " +System.currentTimeMillis());
            if (this.taskNum % 500 == 0) {
                Log.i("ThreadPool", "High Priority Task Count = " + taskCount + " sysTime = " + sysTime + " Average Sys Time = " + (sysTime / taskCount));
                taskCount = 0;  sysTime = 0;
            }
        }
    }
    class TrafficGenerator extends Thread {
        public void run() {
            for (int i = 1; i <=5000; i++) {
                int delay = poisson(2);
                try {
                    Thread.sleep(delay);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                int j = rand.nextInt(2);
                if (j % 2 == 0) {
                    executor.submit(new PrioritizedTask(i, System.currentTimeMillis(),20, Process.THREAD_PRIORITY_FOREGROUND));
                } else {
                    executor.submit(new PrioritizedTask(i, System.currentTimeMillis(),20, THREAD_PRIORITY_BACKGROUND));
                }
            }
        }
    }
}