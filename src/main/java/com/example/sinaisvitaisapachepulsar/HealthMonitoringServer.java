package com.example.sinaisvitaisapachepulsar;

import org.apache.pulsar.client.api.*;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class HealthMonitoringServer {
    // Configuration
    private static final String SERVICE_URL = "pulsar://localhost:6650";
    private static final String TOPIC = "sinais-vitais";
    private static final long ALERT_TIMEOUT_MS = 300_000; // 5 minutes

    // State control
    private static final Map<String, AlertData> activeAlerts = new ConcurrentHashMap<>();
    private static final BlockingQueue<Message<String>> pendingMessages = new LinkedBlockingQueue<>();
    private static final AtomicBoolean isRunning = new AtomicBoolean(true);
    private static final AtomicBoolean isPaused = new AtomicBoolean(false);
    private static final Object pauseLock = new Object();
    private static PulsarClient client;
    private static Consumer<String> consumer;

    static class AlertData {
        String patientId;
        String patientName;
        String condition;
        boolean confirmed;
        long timestamp;
    }

    public static void main(String[] args) {
        try {
            initialize();
            startCommandListener();
            startAlertDashboard();
            processMessages();
        } catch (Exception e) {
            System.err.println("Fatal error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            shutdown();
        }
    }

    private static void initialize() throws Exception {
        createDataFiles();
        client = PulsarClient.builder().serviceUrl(SERVICE_URL).build();
        consumer = client.newConsumer(Schema.STRING)
                .topic(TOPIC)
                .subscriptionName("health-monitor")
                .subscribe();

        System.out.println(
                "\n🖥️  Health Monitoring Server Started\n" +
                        "📊 History: historico_pacientes.csv\n" +
                        "⚠️  Alerts: alertas_clinicos.log\n" +
                        "▶️  Ready to receive data..."
        );
    }

    private static void processMessages() throws Exception {
        while (isRunning.get()) {
            checkPauseState();

            Message<String> msg = consumer.receive(100, TimeUnit.MILLISECONDS);
            if (msg != null) {
                if (isPaused.get()) {
                    pendingMessages.put(msg);
                } else {
                    processMessage(msg);
                }
            }
        }
    }

    private static void processMessage(Message<String> msg) {
        try {
            VitalSigns vs = VitalSigns.fromMessage(msg.getValue());
            if (validateVitalSigns(vs)) {
                System.out.println("\n" + vs);
                DataLogger.logToCSV(vs);
                checkForAlerts(vs);
            }
            consumer.acknowledge(msg);
        } catch (Exception e) {
            System.err.println("Error processing message: " + e.getMessage());
        }
    }

    private static boolean validateVitalSigns(VitalSigns vs) {
        // Validate physiological ranges
        return vs.heartRate > 30 && vs.heartRate < 200 &&
                vs.respiratoryRate > 5 && vs.respiratoryRate < 50 &&
                vs.temperature > 32 && vs.temperature < 42 &&
                vs.systolicPressure > 50 && vs.systolicPressure < 250 &&
                vs.diastolicPressure > 30 && vs.diastolicPressure < 150;
    }

    private static void checkForAlerts(VitalSigns vs) {
        List<String> alertConditions = new ArrayList<>();

        // Alert thresholds
        if (vs.systolicPressure > 140) alertConditions.add("High SBP (" + vs.systolicPressure + ")");
        if (vs.diastolicPressure > 90) alertConditions.add("High DBP (" + vs.diastolicPressure + ")");
        if (vs.heartRate < 60) alertConditions.add("Low HR (" + vs.heartRate + "bpm)");
        if (vs.heartRate > 100) alertConditions.add("High HR (" + vs.heartRate + "bpm)");
        if (vs.temperature < 35.5) alertConditions.add("Low Temp (" + vs.temperature + "°C)");
        if (vs.temperature > 37.5) alertConditions.add("High Temp (" + vs.temperature + "°C)");

        if (!alertConditions.isEmpty()) {
            triggerAlert(vs, alertConditions);
        }
    }

    private static void triggerAlert(VitalSigns vs, List<String> conditions) {
        String alertId = UUID.randomUUID().toString();
        AlertData alert = new AlertData();
        alert.patientId = vs.clientId;
        alert.patientName = vs.clientName;
        alert.condition = String.join(", ", conditions);
        alert.timestamp = System.currentTimeMillis();

        activeAlerts.put(alertId, alert);
        AlertLogger.logAlert("ALERT: " + vs.clientName + " - " + alert.condition);
        pauseSystem(alertId, alert);
    }

    private static void pauseSystem(String alertId, AlertData alert) {
        isPaused.set(true);
        System.out.printf(
                "\n🚨 CRITICAL ALERT [%s]\n" +
                        "Patient: %s\n" +
                        "Conditions: %s\n" +
                        "⏸️  System paused. Action: CONFIRM %s | IGNORE %s | RESUME\n",
                alertId, alert.patientName, alert.condition, alertId, alertId
        );
    }

    private static void checkPauseState() throws InterruptedException {
        synchronized (pauseLock) {
            while (isPaused.get()) {
                pauseLock.wait();
            }
        }
    }

    private static void startCommandListener() {
        new Thread(() -> {
            Scanner scanner = new Scanner(System.in);
            while (isRunning.get()) {
                processCommand(scanner.nextLine().trim());
            }
            scanner.close();
        }).start();
    }

    private static void processCommand(String input) {
        try {
            String lowerInput = input.toLowerCase();
            if (lowerInput.startsWith("confirm ")) {
                handleAlertConfirmation(input.substring(8).trim());
            } else if (lowerInput.startsWith("ignore ")) {
                handleAlertIgnoring(input.substring(7).trim());
            } else if (lowerInput.equals("resume")) {
                resumeSystem();
            } else if (lowerInput.equals("exit")) {
                isRunning.set(false);
                resumeSystem(); // Unblock main thread
            } else {
                System.out.println("⚠️  Invalid command. Use: CONFIRM <id> | IGNORE <id> | RESUME");
            }
        } catch (Exception e) {
            System.err.println("Command error: " + e.getMessage());
        }
    }

    private static void handleAlertConfirmation(String alertId) {
        AlertData alert = activeAlerts.get(alertId);
        if (alert != null) {
            alert.confirmed = true;
            String msg = "✅ Alert " + alertId + " confirmed: " + alert.condition;
            AlertLogger.logAlert(msg);
            System.out.println(msg);
        } else {
            System.out.println("⚠️  Alert not found: " + alertId);
        }
    }

    private static void handleAlertIgnoring(String alertId) {
        AlertData alert = activeAlerts.remove(alertId);
        if (alert != null) {
            String msg = "❌ Alert " + alertId + " ignored: " + alert.condition;
            AlertLogger.logAlert(msg);
            System.out.println(msg);
        } else {
            System.out.println("⚠️  Alert not found: " + alertId);
        }
    }

    private static void resumeSystem() {
        synchronized (pauseLock) {
            isPaused.set(false);
            processPendingMessages();
            pauseLock.notifyAll();
        }
        cleanOldAlerts();
        System.out.println("▶️  System resumed");
    }

    private static void processPendingMessages() {
        new Thread(() -> {
            while (!pendingMessages.isEmpty()) {
                processMessage(pendingMessages.poll());
            }
        }).start();
    }

    private static void cleanOldAlerts() {
        long now = System.currentTimeMillis();
        activeAlerts.entrySet().removeIf(entry ->
                entry.getValue().confirmed ||
                        (now - entry.getValue().timestamp) > ALERT_TIMEOUT_MS
        );
    }

    private static void startAlertDashboard() {
        Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(() -> {
            cleanOldAlerts();

            if (!activeAlerts.isEmpty()) {
                System.out.println("\n📋 ALERT SUMMARY (" +
                        new SimpleDateFormat("HH:mm:ss").format(new Date()) + ")");

                activeAlerts.forEach((id, alert) -> {
                    String status = alert.confirmed ? "✅" : "⚠️";
                    System.out.printf("%s [%s] %s: %s (%.1f min)%n",
                            status, id, alert.patientName, alert.condition,
                            (System.currentTimeMillis() - alert.timestamp) / 60000.0);
                });

                System.out.println("Commands: CONFIRM <id> | IGNORE <id> | RESUME | EXIT");
            }
        }, 0, 30, TimeUnit.SECONDS);
    }

    private static void createDataFiles() throws IOException {
        try (FileWriter csv = new FileWriter("historico_pacientes.csv", false)) {
            csv.write("Data,Hora,Paciente,ID,FC,FR,Temp,Pressao_Sist,Pressao_Diast\n");
        }
        new FileWriter("alertas_clinicos.log", false).close();
    }

    private static void shutdown() {
        try {
            System.out.println("\n🛑 Shutting down server...");
            AlertLogger.logAlert("Server shutdown");
            if (consumer != null) consumer.close();
            if (client != null) client.close();
        } catch (Exception e) {
            System.err.println("Shutdown error: " + e.getMessage());
        }
    }
}