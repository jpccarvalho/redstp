package com.redes.models;

public class Pacote {
    private int numero;
    private int[] dados = new int[350];
    private boolean ackRecebido;
    private boolean pacoteEnviado;

    public Pacote(int numero, int[] dados, boolean pacoteEnviado) {
        this.numero = numero;
        this.dados = dados;
        this.ackRecebido = false;
        this.pacoteEnviado = pacoteEnviado;
    }
    public int getNumero() {
        return numero;
    }
    public void setNumero(int numero) {
        this.numero = numero;
    }
    public int[] getDados() {
        return dados;
    }
    public void setDados(int[] dados) {
        this.dados = dados;
    }
    public boolean getAckRecebido() {
        return ackRecebido;
    }
    public void setAckRecebido(boolean ackRecebido) {
        this.ackRecebido = ackRecebido;
    }
    public boolean getPacoteEnviado() {
        return pacoteEnviado;
    }
    public void setPacoteEnviado(boolean pacoteEnviado) {
        this.pacoteEnviado = pacoteEnviado;
    }
}
