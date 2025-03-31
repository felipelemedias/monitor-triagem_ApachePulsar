package com.example.sinaisvitaisapachepulsar;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;

public class AlertLogger {
    private static final String LOG_FILE = "alertas_clinicos.log";

    public static void logAlert(String alerta) {
        try (FileWriter writer = new FileWriter(LOG_FILE, true)) {
            SimpleDateFormat sdf = new SimpleDateFormat("[dd/MM/yyyy HH:mm:ss]");
            writer.write(sdf.format(new Date()) + " " + alerta + "\n");
        } catch (IOException e) {
            System.err.println("Erro ao registrar alerta: " + e.getMessage());
        }
    }
}