package org.example;


import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class CrptApi {
    private final TimeUnit timeUnit;
    private final int requestLimit;

    private final Lock lock = new ReentrantLock();
    private final Semaphore semaphore;
    private final ScheduledExecutorService scheduledExecutorService;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public CrptApi(TimeUnit timeUnit, int requestLimit) {
        this.timeUnit = timeUnit;
        this.requestLimit = requestLimit;

        this.semaphore = new Semaphore(requestLimit);
        this.scheduledExecutorService = Executors.newScheduledThreadPool(1);
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();

        long time = timeUnit.toMillis(1);

        Runnable command = () -> {lock.lock();
                            try { semaphore.release(requestLimit - semaphore.availablePermits()); }
                            finally { lock.unlock(); }};

        scheduledExecutorService.scheduleAtFixedRate(command, time, time, TimeUnit.MICROSECONDS);
    }

    public void sendDocument(CrptDocument crptDocument) throws InterruptedException, IOException {
        semaphore.acquire();

        String json = objectMapper.writeValueAsString(crptDocument);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://ismp.crpt.ru/api/v3/lk/documents/create"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

    }


    public static class CrptDocument{
        public Description description;
        public String doc_id;
        public String doc_status;
        public String doc_type;
        public boolean importRequest;
        public String owner_inn;
        public String participant_inn;
        public String producer_inn;
        public String production_date;
        public String production_type;
        public Product[] products;
        public String reg_date;
        public String reg_number;

        public static class Description {
            public String participantInn;
        }

        public static class Product {
            public String certificate_document;
            public String certificate_document_date;
            public String certificate_document_number;
            public String owner_inn;
            public String producer_inn;
            public String production_date;
            public String tnved_code;
            public String uit_code;
            public String uitu_code;
        }
    }
}
