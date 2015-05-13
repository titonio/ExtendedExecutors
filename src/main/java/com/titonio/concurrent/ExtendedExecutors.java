package com.titonio.concurrent;

import com.google.common.util.concurrent.AbstractListeningExecutorService;
import com.google.common.util.concurrent.ListeningExecutorService;
import org.slf4j.MDC;

import java.util.List;
import java.util.Map;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * This class allows the decoration of Executor and ExecutorService to pass the logging MDC to the threads executing the
 * operation
 * <p/>
 * Created by Fernando on 13/05/2015.
 */
public class ExtendedExecutors {

    private ExtendedExecutors() {
    }


    public static Executor MDCDecorator(Executor executor) {
        return executor instanceof MDCExecutor ? executor : new MDCExecutor(executor);
    }

    public static ExecutorService MDCDecorator(ExecutorService executor) {
        return executor instanceof MDCExecutorService ? executor : new MDCExecutorService(executor);
    }

    public static ListeningExecutorService MDCDecorator(ListeningExecutorService executor) {
        return executor instanceof MDCListeningExecutorService ? executor : new MDCListeningExecutorService(executor);
    }

    private static class MDCExecutor implements Executor {

        private final Executor delegate;

        public MDCExecutor(Executor delegate) {
            this.delegate = delegate;
        }

        public void execute(Runnable command) {
            delegate.execute(new MDCRunnable(checkNotNull(command), MDC.getCopyOfContextMap()));
        }
    }

    private static class MDCRunnable implements Runnable {

        private final Runnable runnable;
        private final Map<String, String> context;

        public MDCRunnable(Runnable runnable, Map<String, String> context) {
            this.runnable = runnable;
            this.context = context;
        }

        public void run() {

            Map<String, String> previous = MDC.getCopyOfContextMap();

            try {
                if (context == null) {
                    MDC.clear();
                } else {
                    MDC.setContextMap(context);
                }

                runnable.run();

            } finally {
                if (previous == null) {
                    MDC.clear();
                } else {
                    MDC.setContextMap(previous);
                }
            }
        }
    }


    private static class MDCExecutorService extends AbstractExecutorService {

        private final ExecutorService delegate;

        public MDCExecutorService(ExecutorService delegate) {
            this.delegate = delegate;
        }

        public void execute(Runnable command) {
            delegate.execute(new MDCRunnable(checkNotNull(command), MDC.getCopyOfContextMap()));
        }

        public void shutdown() {
            delegate.shutdown();
        }

        public List<Runnable> shutdownNow() {
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
    }

    private static class MDCListeningExecutorService extends AbstractListeningExecutorService {

        private final ExecutorService delegate;

        public MDCListeningExecutorService(ListeningExecutorService delegate) {
            this.delegate = delegate;
        }

        public void execute(Runnable command) {
            delegate.execute(new MDCRunnable(checkNotNull(command), MDC.getCopyOfContextMap()));
        }

        public void shutdown() {
            delegate.shutdown();
        }

        public List<Runnable> shutdownNow() {
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
    }

}
