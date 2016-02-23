package com.titonio.monitoring;

import javax.enterprise.concurrent.ManagedExecutorService;
import javax.management.*;
import java.lang.management.ManagementFactory;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.LongAdder;

/**
 * Created by Fernando on 23/02/2016.
 */
public class MonitoredExecutors {

    private MonitoredExecutors() {

    }

    public static ExecutorService monitored(ExecutorService service, String address) {

        MonitoredExecutorService monitoredExecutorService = new MonitoredExecutorService(service, address);

        registerExecutor(address, monitoredExecutorService);

        return monitoredExecutorService;
    }



    private static void registerExecutor(String address, MonitoredExecutorService monitoredExecutorService) {
        MBeanServer platformMBeanServer = ManagementFactory.getPlatformMBeanServer();
        try {
            platformMBeanServer.registerMBean(monitoredExecutorService, new ObjectName(address));
        } catch (InstanceAlreadyExistsException e) {
            e.printStackTrace();
        } catch (MBeanRegistrationException e) {
            e.printStackTrace();
        } catch (NotCompliantMBeanException e) {
            e.printStackTrace();
        } catch (MalformedObjectNameException e) {
            e.printStackTrace();
        }
    }

    private static void unregisterExecutor(String address) {
        MBeanServer platformMBeanServer = ManagementFactory.getPlatformMBeanServer();

        try {
            platformMBeanServer.unregisterMBean(new ObjectName(address));
        } catch (InstanceNotFoundException e) {
            e.printStackTrace();
        } catch (MBeanRegistrationException e) {
            e.printStackTrace();
        } catch (MalformedObjectNameException e) {
            e.printStackTrace();
        }

    }

    private static class ExecutorCounter implements ExecutorMXBean {

        LongAdder waitingTasks = new LongAdder();

        LongAdder runningTasks = new LongAdder();


        public void taskAdded() {
            waitingTasks.increment();
        }

        public long taskStarted(){
            waitingTasks.decrement();
            runningTasks.increment();
            return System.nanoTime();
        }

        public void taskFinished(long mark) {
            runningTasks.decrement();
        }

        @Override
        public long getRunningTasksCount() {
            return runningTasks.sum();
        }

        @Override
        public long getWaitingTasksCount() {
            return waitingTasks.sum();
        }
    }



    private static class MonitoredExecutorService extends AbstractExecutorService implements ExecutorMXBean {

        private final ExecutorService delegate;
        private final String address;
        private final ExecutorCounter counter = new ExecutorCounter();

        public MonitoredExecutorService(ExecutorService delegate, String address) {
            this.delegate = delegate;
            this.address = address;
        }


        public void shutdown() {
            unregisterExecutor(address);
            delegate.shutdown();
        }

        public List<Runnable> shutdownNow() {
            unregisterExecutor(address);
            return delegate.shutdownNow();
        }

        public boolean isShutdown() {
            return delegate.isShutdown();
        }

        public boolean isTerminated() {
            return delegate.isTerminated();
        }

        public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
            return delegate.awaitTermination(timeout, unit);
        }

        public void execute(Runnable command) {
            counter.taskAdded();
            delegate.execute(() -> {
                long mark = 0;
                try {
                    mark = counter.taskStarted();
                    command.run();
                }finally {
                    counter.taskFinished(mark);
                }

            });
        }

        public long getRunningTasksCount() {
            return counter.getRunningTasksCount();
        }

        public long getWaitingTasksCount() {
            return counter.getWaitingTasksCount();
        }
    }

}
