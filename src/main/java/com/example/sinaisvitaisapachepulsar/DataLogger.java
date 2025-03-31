package com.example.sinaisvitaisapachepulsar;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public class DataLogger {
    private static final String CSV_HEADER = "Data,Hora,Paciente,ID,FC,FR,Temp,Pressao_Sist,Pressao_Diast\n";
    private static final String CSV_FILE = "historico_pacientes.csv";

    public static void logToCSV(VitalSigns vs) {
        if (vs == null) return;

        try (FileWriter writer = new FileWriter("historico_pacientes.csv", true)) {
            SimpleDateFormat df = new SimpleDateFormat("dd/MM/yyyy,HH:mm:ss");
            df.setTimeZone(TimeZone.getTimeZone("America/Sao_Paulo")); // Fuso horário BR

            String[] dataHora = df.format(new Date(vs.timestamp)).split(",");

            writer.write(String.format("%s,%s,%s,%s,%d,%d,%.1f,%d,%d\n",
                    dataHora[0], dataHora[1],
                    vs.clientName, vs.clientId,
                    vs.heartRate, vs.respiratoryRate,
                    vs.temperature, vs.systolicPressure, vs.diastolicPressure));
        } catch (IOException e) {
            System.err.println("Erro no CSV: " + e.getMessage());
        }
    }
}