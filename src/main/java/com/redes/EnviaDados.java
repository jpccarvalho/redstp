/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.redes;

/**
 * @author flavio
 */

import com.redes.models.Pacote;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.concurrent.Semaphore;

public class EnviaDados extends Thread {

    private final int portaLocalEnvio = 2000;
    private final int portaDestino = 2001;
    private final int portaLocalRecebimento = 2003;
    Semaphore sem;
    private final String funcao;
    private ArrayList<Pacote> sequencia;
    Semaphore fimEnvio;

    public EnviaDados(Semaphore sem, String funcao, ArrayList<Pacote> sequencia, Semaphore fimEnvio) {
        super(funcao);
        this.sem = sem;
        this.funcao = funcao;
        this.sequencia = sequencia;
        this.fimEnvio = fimEnvio;
    }

    public String getFuncao() {
        return funcao;
    }

    private void enviaPct(int[] dados, int numPct) {
        //converte int[] para byte[]
        int[] aux;
        aux = new int[351];
        aux[0] = numPct;
        for (int i = 1; i < dados.length; i++) {
            aux[i] = dados[i-1];
        }

        ByteBuffer byteBuffer = ByteBuffer.allocate(aux.length * 4);
        IntBuffer intBuffer = byteBuffer.asIntBuffer();
        intBuffer.put(aux);

        byte[] buffer = byteBuffer.array();

        try {
            System.out.println("Semaforo: " + sem.availablePermits());
            sem.acquire();
            System.out.println("Semaforo: " + sem.availablePermits());

            InetAddress address = InetAddress.getByName("localhost");
            try (DatagramSocket datagramSocket = new DatagramSocket(portaLocalEnvio)) {
                DatagramPacket packet = new DatagramPacket(
                        buffer, buffer.length, address, portaDestino);

                datagramSocket.send(packet);
                Pacote pct = new Pacote(numPct, dados, true);
                sequencia.add(pct);
            }

            System.out.println("Envio feito.\t Numero de pacote:" + numPct);
        } catch (SocketException ex) {
            Logger.getLogger(EnviaDados.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException | InterruptedException ex) {
            Logger.getLogger(EnviaDados.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    boolean verificaRecebimento(ArrayList<Pacote> seq){
        boolean todosRecebidos = true;
        for(int i = 0; i < seq.size(); i++) {
            todosRecebidos = seq.get(i).getAckRecebido() && todosRecebidos;
        }
        System.out.println(todosRecebidos);

        return todosRecebidos;
    }

    int ultimoRecebido(ArrayList<Pacote> seq){
        int aux = -1;
        for (int i = 0; i < seq.size(); i++) {
            if(seq.get(i).getAckRecebido()){
                aux = seq.get(i).getNumero();
            }
        }
        return aux;
    }

    int ultimoEnviado(ArrayList<Pacote> seq){
        int aux = -1;
        for (int i = 0; i < seq.size(); i++) {
            if(seq.get(i).getPacoteEnviado()){
                aux = seq.get(i).getNumero();
            }
        }
        return aux;
    }

    @Override
    public void run() {
        switch (this.getFuncao()) {
            case "envia":
                //variavel onde os dados lidos serao gravados
                int[] dados = new int[350];
                //contador, para gerar pacotes com 1400 Bytes de tamanho
                //como cada int ocupa 4 Bytes, estamos lendo blocos com 350
                //int's por vez.
                int cont = 0;
                try {
                    fimEnvio.acquire();
                }catch (InterruptedException ex) {
                    Logger.getLogger(EnviaDados.class.getName()).log(Level.SEVERE, null, ex);
                }

                try (FileInputStream fileInput = new FileInputStream("entrada");) {
                    int lido;
                    int numPct = 0;
                    while ((lido = fileInput.read()) != -1) {
                        dados[cont] = lido;
                        cont++;
                        if (cont == 350) {
                            //envia pacotes a cada 350 int's lidos.
                            //ou seja, 1400 Bytes.
                            enviaPct(dados, numPct);
                            cont = 0;
                            numPct += 1;
                        }
                    }

                    //ultimo pacote eh preenchido com
                    //-1 ate o fim, indicando que acabou
                    //o envio dos dados.
                    for (int i = cont; i < 350; i++)
                        dados[i] = -1;
                    enviaPct(dados, numPct);
                } catch (IOException e) {
                    System.out.println("Error message: " + e.getMessage());
                }
                break;
            case "ack":
                try {
                    DatagramSocket serverSocket = new DatagramSocket(portaLocalRecebimento);
                    byte[] receiveData = new byte[3];
                    String ret;
                    int numPct;
                    String retorno = "";
                    while (!retorno.equals("F")) {
                        DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                        serverSocket.receive(receivePacket);
                        ret = new String(receivePacket.getData());
                        retorno = ret.toCharArray()[0]+ "";
                        if(!retorno.equals("F")){
                            String auxStr= "";
                            for (int i = 1; i <ret.toCharArray().length; i++) {
                                auxStr += ret.toCharArray()[i];
                            }
                            numPct = Integer.parseInt(auxStr);


                            int ultimoRecebido = ultimoRecebido(sequencia);



                            if(ultimoRecebido == numPct-1){
                                System.out.println("Ack recebido " + retorno + ".\t" + "numPct: " + numPct + "\n");
                                sequencia.get(numPct).setAckRecebido(true);
                            }
                            sem.release();
                        }

                    }
                } catch (IOException e) {
                    System.out.println("Excecao: " + e.getMessage());
                }
                break;
            //TODO timer
            case "timer":
                int aux = 0;
                while (aux != 1){
                    aux = fimEnvio.availablePermits();
                    System.out.println("aux: " + aux);
                    // olho para o ultimo enviado
                    // assim que ele for enviado, um temporizador será iniciado.
                    // se ao fim, ele não recebeu, deverá ser enviado novamente.
                    int ultimoEnv = ultimoEnviado(sequencia);
                    try {
                        Thread.sleep(500);
                        int ultimoRec = ultimoRecebido(sequencia);
                        if(ultimoEnv != ultimoRec){
                            //reenvio
                            int prox = 0;
                            for (int i = 0; i < sequencia.size(); i++) {
                                if(sequencia.get(i).getNumero() == ultimoRec){
                                    prox = i+1;
                                    break;
                                }
                            }
                            for (int i = prox; i < sequencia.size(); i++) {
                                sequencia.get(i).setPacoteEnviado(false);
                            }
                            enviaPct(sequencia.get(prox).getDados(), ultimoRec+1);

                        }

                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }

                }
                break;

            default:
                break;
        }

    }
}
