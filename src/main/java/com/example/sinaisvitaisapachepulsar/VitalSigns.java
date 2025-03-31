package com.example.sinaisvitaisapachepulsar;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public class VitalSigns {
    public String clientId;
    public String clientName;
    public int heartRate;       // FC (bpm)
    public int respiratoryRate; // FR (rpm)
    public double temperature; // Temp (°C)
    public int systolicPressure;  // Pressão sistólica
    public int diastolicPressure; // Pressão diastólica
    public long timestamp;

    @Override
    public String toString() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        sdf.setTimeZone(TimeZone.getTimeZone("America/Sao_Paulo")); // Fuso do Brasil
        return String.format("💬 Dados de %s (%s): FC=%dbpm, FR=%drpm, Temp=%.1f°C, Pressao=%d/%d [%s - BR]",
                clientName, clientId, heartRate, respiratoryRate, temperature,
                systolicPressure, diastolicPressure, sdf.format(new Date()));
    }

    public String toMessage() {
        return String.format("ID=%s,NOME=%s,FC=%dbpm,FR=%drpm,Temp=%.1f°C,Pressao=%d/%d",
                clientId, clientName, heartRate, respiratoryRate, temperature,
                systolicPressure, diastolicPressure);
    }

    public static VitalSigns fromMessage(String message) {
        VitalSigns vs = new VitalSigns();
        try {
            String[] parts = message.split(",");

            for (String part : parts) {
                if (part.startsWith("ID=")) {
                    vs.clientId = part.substring(3);
                } else if (part.startsWith("NOME=")) {
                    vs.clientName = part.substring(5);
                } else if (part.startsWith("FC=")) {
                    vs.heartRate = Integer.parseInt(part.substring(3).replace("bpm", ""));
                } else if (part.startsWith("FR=")) {
                    vs.respiratoryRate = Integer.parseInt(part.substring(3).replace("rpm", ""));
                } else if (part.startsWith("Temp=")) {
                    String tempStr = part.substring(5).replace("°C", "");
                    vs.temperature = Double.parseDouble(tempStr);
                } else if (part.startsWith("Pressao=")) {
                    String[] pressureParts = part.substring(8).split("/");
                    vs.systolicPressure = Integer.parseInt(pressureParts[0]);
                    vs.diastolicPressure = Integer.parseInt(pressureParts[1]);
                }
            }
        } catch (Exception e) {
            System.err.println("Erro ao parsear mensagem: " + message);
            e.printStackTrace();
        }
        return vs;
    }
}