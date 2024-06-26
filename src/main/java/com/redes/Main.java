package com.redes;
import com.redes.models.Pacote;

import java.util.ArrayList;
import java.util.concurrent.Semaphore;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Main {
    public static void main(String[] args) {
        RecebeDados rd = new RecebeDados();
        rd.start();

        ArrayList<Pacote> sequencia = new ArrayList<Pacote>();
        Semaphore sem = new Semaphore(10);
        Semaphore fimEnvio = new Semaphore(1);
        EnviaDados ed1 = new EnviaDados(sem, "envia", sequencia, fimEnvio);
        EnviaDados ed2 = new EnviaDados(sem, "ack", sequencia, fimEnvio);
        EnviaDados timer = new EnviaDados(sem, "timer", sequencia, fimEnvio);

        ed2.start();
        ed1.start();
        timer.start();

        try {
            ed1.join();
            ed2.join();
            timer.join();
            rd.join();
        } catch (InterruptedException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}