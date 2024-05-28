package org.example;

import com.hazelcast.client.HazelcastClient;
import com.hazelcast.client.config.ClientConfig;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import com.hazelcast.spi.exception.TargetDisconnectedException;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class ClientSetTps {


    public static void main(String[] args) throws InterruptedException {
        var addresses = args[0].split(",");
        int nbThreads = 1;
        ExecutorService threadExecutor = Executors.newFixedThreadPool(nbThreads);

        final int numberOfClients = 1; // Number of Hazelcast client instances
        ExecutorService clientExecutor = Executors.newFixedThreadPool(numberOfClients);


        AtomicInteger counter = new AtomicInteger();

        for (int clientCount = 0; clientCount < numberOfClients; clientCount++) {
            clientExecutor.submit(() -> {
                ClientConfig clientConfig = new ClientConfig();
                clientConfig.getNetworkConfig().addAddress(addresses);
                HazelcastInstance hz = HazelcastClient.newHazelcastClient(clientConfig);
                IMap<String, Integer> map = hz.getMap("mapName");

                for (int thread = 0; thread < nbThreads; thread++) {
                    threadExecutor.submit(() -> {
                        long beforeOpTime;

                        while (true) {
                            for (int i = 0; i < 20000; i++) {
                                int retryCount = 0;
                                beforeOpTime = System.currentTimeMillis();
                                try {
                                    map.set("key-" + i, i);
                                    map.set("keyWithExpiry-" + i, i, 5, TimeUnit.SECONDS);
                                    counter.addAndGet(2);


                                } catch (TargetDisconnectedException | com.hazelcast.splitbrainprotection.SplitBrainProtectionException e) {
                                    retryCount++;
                                    System.out.println("Issue with connection or operation timed out. Retrying... Attempt: " + retryCount);
                                    // Wait for a short period before retrying
                                    try {
                                        Thread.sleep(100); // wait for 100millis before retrying
                                    } catch (InterruptedException ie) {
                                        //                            Thread.currentThread().interrupt();
                                        //                            return;
                                    }
                                }

                                long currentTimeMilli = System.currentTimeMillis();
                                long updateTime = currentTimeMilli - beforeOpTime;
                                if (updateTime >= 1000) {
                                    // Get the current local time
                                    LocalTime currentTime = LocalTime.now();

                                    // Format the time
                                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");
                                    String formattedTime = currentTime.format(formatter);

                                    System.out.println(formattedTime + ": Slow Operation: " + updateTime + " ms | " + updateTime / 1000 + " s");
                                }
                                if (i == 0) {
                                    System.out.print(".");
                                }
                            }
                        }
                    });
                }
            });
        }

        while (true) {
            Thread.sleep(10000);
            System.out.println("nb operations per seconds: " + counter.get() / 10);
            counter.set(0);
        }
    }
}