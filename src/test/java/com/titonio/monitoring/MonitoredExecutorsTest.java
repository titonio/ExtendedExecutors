package com.titonio.monitoring;

import org.junit.Test;

import javax.management.*;
import java.lang.management.ManagementFactory;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.Assert.*;

/**
 * Created by Fernando on 23/02/2016.
 */
public class MonitoredExecutorsTest {


    @Test
    public void testMonitoredExecutorService() throws Exception {

        ExecutorService executorService = Executors.newSingleThreadExecutor();

        String address = "base.test:service=exe";
        ExecutorService monitored = MonitoredExecutors.monitored(executorService, address);
        try {
        ObjectName instance = ObjectName.getInstance(address);
        MBeanServer platformMBeanServer = ManagementFactory.getPlatformMBeanServer();
        MBeanInfo mBeanInfo = platformMBeanServer.getMBeanInfo(instance);

        assertEquals(Long.valueOf(0) ,  platformMBeanServer.getAttribute(instance, "RunningTasksCount"));
        assertEquals(Long.valueOf(0) ,  platformMBeanServer.getAttribute(instance, "WaitingTasksCount"));

        }finally {
            monitored.shutdownNow();
            assertTrue(monitored.awaitTermination(5, TimeUnit.SECONDS));
        }
    }

    @Test
    public void testMonitoredExecutorServiceOneRunning() throws Exception {

        ExecutorService executorService = Executors.newSingleThreadExecutor();

        String address = "base.test:service=exe";
        ExecutorService monitored = MonitoredExecutors.monitored(executorService, address);

        try {
            final CountDownLatch start = new CountDownLatch(1);
            final CountDownLatch end = new CountDownLatch(1);

            monitored.submit(() -> {
                try {
                    start.countDown();
                    end.await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            });

            assertTrue(start.await(5, TimeUnit.SECONDS));

            ObjectName instance = ObjectName.getInstance(address);
            MBeanServer platformMBeanServer = ManagementFactory.getPlatformMBeanServer();
            MBeanInfo mBeanInfo = platformMBeanServer.getMBeanInfo(instance);

            assertEquals(Long.valueOf(1), platformMBeanServer.getAttribute(instance, "RunningTasksCount"));
            assertEquals(Long.valueOf(0), platformMBeanServer.getAttribute(instance, "WaitingTasksCount"));

            end.countDown();

        }finally {
            monitored.shutdownNow();
            assertTrue(monitored.awaitTermination(5, TimeUnit.SECONDS));
        }

    }

    @Test
    public void testMonitoredExecutorServiceOneRunningAndOneWaiting() throws Exception {

        int nThreads = 6;
        ExecutorService executorService = Executors.newFixedThreadPool(nThreads);

        String address = "base.test:service=exe";
        ExecutorService monitored = MonitoredExecutors.monitored(executorService, address);

        try {
            ObjectName instance = ObjectName.getInstance(address);
            MBeanServer platformMBeanServer = ManagementFactory.getPlatformMBeanServer();
            MBeanInfo mBeanInfo = platformMBeanServer.getMBeanInfo(instance);
            int numberOfTasks = nThreads * 10;
            final AtomicLong pending = new AtomicLong(numberOfTasks);
            final CyclicBarrier cb1 = new CyclicBarrier(nThreads+1, () -> {
                try {
                    System.out.println("CHECK");
                    assertEquals(Long.valueOf(nThreads), platformMBeanServer.getAttribute(instance, "RunningTasksCount"));
                    assertEquals(pending.addAndGet(-nThreads), platformMBeanServer.getAttribute(instance, "WaitingTasksCount"));
                } catch (MBeanException e) {
                    e.printStackTrace();
                } catch (AttributeNotFoundException e) {
                    e.printStackTrace();
                } catch (InstanceNotFoundException e) {
                    e.printStackTrace();
                } catch (ReflectionException e) {
                    e.printStackTrace();
                }

            });

            Runnable runnable = () -> {
                try {
                    cb1.await(5, TimeUnit.SECONDS);
                } catch (BrokenBarrierException | InterruptedException | TimeoutException e) {
                    e.printStackTrace();
                }
            };

            for (int i = 0; i < numberOfTasks; i++) {
                monitored.submit(runnable);
            }


            for (int i = 0; i < (numberOfTasks / nThreads); i++) {
                cb1.await(5, TimeUnit.SECONDS);
            }

        }finally {
            monitored.shutdownNow();
            assertTrue(monitored.awaitTermination(5, TimeUnit.SECONDS));
        }

    }
}