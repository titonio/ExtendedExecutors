package com.titonio.concurrent;

import com.google.common.base.Function;
import com.google.common.util.concurrent.*;
import org.junit.Test;
import org.slf4j.MDC;

import java.util.concurrent.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


public class ExtendedExecutorsTest {


    @Test
    public void testMDCDecoratorExecutor() throws Exception {

        Executor executor = Executors.newFixedThreadPool(1);


        Executor extendedExecutor = ExtendedExecutors.MDCDecorator(executor);

        final String value = "value";
        final String key = "key";
        MDC.put(key, value);

        final CountDownLatch endLatch = new CountDownLatch(1);

        extendedExecutor.execute(createTask(key, value, endLatch));

        assertTrue(endLatch.await(10, TimeUnit.SECONDS));
    }

    private Runnable createTask(final String key, final String value, final CountDownLatch endLatch) {
        return new Runnable() {
            public void run() {
                assertEquals(value, MDC.get(key));
                if (endLatch != null) {
                    endLatch.countDown();
                }

            }
        };
    }

    @Test
    public void testMDCDecoratorExecutorService() throws Exception {

        ExecutorService executor = Executors.newFixedThreadPool(1);

        ExecutorService extendedExecutor = ExtendedExecutors.MDCDecorator(executor);

        final String value = "value";
        final String key = "key";
        final CountDownLatch endLatch = new CountDownLatch(2);
        MDC.put(key, value);
        extendedExecutor.submit(createTask(key, value, endLatch));
        MDC.put(key, "value2");
        extendedExecutor.submit(createTask(key, "value2", endLatch));

        assertTrue(endLatch.await(10, TimeUnit.SECONDS));
    }

    @Test
    public void testMDCDecoratorListeningExecutorService() throws Exception {

        ExecutorService executor = Executors.newFixedThreadPool(1);

//        final ListeningExecutorService extendedExecutor = ExtendedExecutors.MDCDecorator(MoreExecutors.listeningDecorator(executor));
        final ListeningExecutorService extendedExecutor = MoreExecutors.listeningDecorator(ExtendedExecutors.MDCDecorator(executor));

        final String value = "value";
        final String key = "key";

        MDC.put(key, value);
        ListenableFuture<String> result = extendedExecutor.submit(Executors.callable(createTask(key, value, null), "result"));
        ListenableFuture<String> transform = Futures.transform(result, new Function<String, String>() {
            public String apply(String input) {
                assertEquals(value, MDC.get(key));
                return input;
            }
        }, extendedExecutor);

        ListenableFuture<String> transform2 = Futures.transform(transform, new AsyncFunction<String, String>() {
            public ListenableFuture<String> apply(String input) throws Exception {
                return extendedExecutor.submit(Executors.callable(createTask("key", "value", null), input));
            }
        });


        assertEquals("result", transform2.get(10, TimeUnit.SECONDS));
    }


}