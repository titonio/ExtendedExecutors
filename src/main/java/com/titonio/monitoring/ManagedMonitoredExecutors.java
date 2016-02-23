package com.titonio.monitoring;

import javax.enterprise.concurrent.ManagedExecutorService;

/**
 * Created by Fernando on 23/02/2016.
 */
public class ManagedMonitoredExecutors {


    public static ManagedExecutorService monitored(ManagedExecutorService service, String address) {

//        MonitoredExecutorService monitoredExecutorService = new MonitoredExecutorService(service, address);
//
//        registerExecutor(address, monitoredExecutorService);
//
//        return monitoredExecutorService;
        return service;
    }
}
