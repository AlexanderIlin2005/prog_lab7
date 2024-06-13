package client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class MultiThreadTester {
        public static void main(String[] args) throws IOException, IOException {
            List<Process> processes = new ArrayList<>();

            for (int i = 0; i < 3; i++) { // запустить 3 экземпляра Main
                ProcessBuilder pb = new ProcessBuilder("java", "Main");
                Process process = pb.start();
                processes.add(process);

                // Перенаправление ввода из текущего потока ввода в процесс
                BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
                String input;
                while ((input = reader.readLine()) != null) {
                    process.getOutputStream().write((input + "\n").getBytes());
                    process.getOutputStream().flush();
                }
            }

            for (Process process : processes) {
                try {
                    process.waitFor();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

