package com.example.sinaisvitaisapachepulsar;

import org.apache.pulsar.client.api.*;
import java.util.*;
import java.util.concurrent.*;

public class HealthMonitoringClient {
    private static final String SERVICE_URL = "pulsar://localhost:6650";
    private static final String TOPIC = "sinais-vitais";
    private static final int SEND_INTERVAL_MS = 5000;
    private static final double ABNORMALITY_PROBABILITY = 0.15;

    private static PulsarClient client;
    private static Producer<String> producer;
    private static String clientId;
    private static String clientName;

    public static void main(String[] args) {
        try {
            initializeClient();
            registerShutdownHook();
            startDataGeneration();
        } catch (Exception e) {
            System.err.println("Client error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            shutdown();
        }
    }

    private static void initializeClient() throws Exception {
        Scanner scanner = new Scanner(System.in);
        System.out.print("Enter your name: ");
        clientName = scanner.nextLine();
        clientId = UUID.randomUUID().toString().substring(0, 8);

        client = PulsarClient.builder()
                .serviceUrl(SERVICE_URL)
                .build();

        producer = client.newProducer(Schema.STRING)
                .topic(TOPIC)
                .create();

        System.out.printf("\n✅ Client %s (%s) connected!\n", clientName, clientId);
    }

    private static void startDataGeneration() {
        Random random = new Random();
        while (true) {
            try {
                VitalSigns vs = generateVitalSigns(random);
                sendVitalSigns(vs);
                TimeUnit.MILLISECONDS.sleep(SEND_INTERVAL_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                System.err.println("Error sending data: " + e.getMessage());
                attemptReconnect();
            }
        }
    }

    private static VitalSigns generateVitalSigns(Random random) {
        VitalSigns vs = new VitalSigns();
        vs.clientId = clientId;
        vs.clientName = clientName;

        boolean isAbnormal = random.nextDouble() < ABNORMALITY_PROBABILITY;

        // Heart Rate (60-100 normal)
        vs.heartRate = isAbnormal && random.nextBoolean()
                ? 40 + random.nextInt(20) // Low HR (40-60)
                : isAbnormal
                ? 101 + random.nextInt(40) // High HR (101-140)
                : 60 + random.nextInt(41); // Normal HR (60-100)

        // Respiratory Rate (12-20 normal)
        vs.respiratoryRate = isAbnormal && random.nextBoolean()
                ? 8 + random.nextInt(4) // Low RR (8-12)
                : isAbnormal
                ? 21 + random.nextInt(10) // High RR (21-30)
                : 12 + random.nextInt(9); // Normal RR (12-20)

        // Temperature (36.0-37.5°C normal)
        vs.temperature = isAbnormal && random.nextBoolean()
                ? 35.0 + random.nextDouble() // Hypothermia (35.0-36.0)
                : isAbnormal
                ? 37.6 + random.nextDouble() * 2.0 // Fever (37.6-39.6)
                : 36.0 + random.nextDouble() * 1.5; // Normal (36.0-37.5)

        // Blood Pressure
        vs.diastolicPressure = 50 + random.nextInt(51); // 50-100
        vs.systolicPressure = vs.diastolicPressure + 30 + random.nextInt(41); // 80-140 with 30-70 pulse pressure

        vs.timestamp = System.currentTimeMillis();
        return vs;
    }

    private static void sendVitalSigns(VitalSigns vs) throws Exception {
        String data = vs.toMessage();
        producer.send(data);
        System.out.println("📤 Sent: " + vs);
    }

    private static void attemptReconnect() {
        try {
            TimeUnit.SECONDS.sleep(5);
            if (producer != null) producer.close();
            if (client != null) client.close();
            initializeClient();
        } catch (Exception e) {
            System.err.println("Reconnection failed: " + e.getMessage());
        }
    }

    private static void registerShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\n🛑 Shutting down client...");
            shutdown();
        }));
    }

    private static void shutdown() {
        try {
            if (producer != null) producer.close();
            if (client != null) client.close();
        } catch (Exception e) {
            System.err.println("Shutdown error: " + e.getMessage());
        }
    }
}