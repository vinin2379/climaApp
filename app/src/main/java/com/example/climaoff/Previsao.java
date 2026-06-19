// app/src/main/java/com/exemplo/appclima/models/Previsao.java
package com.example.climaoff;

public class Previsao {
    private int id;
    private String cidade;
    private String data;
    private double temperaturaMin;
    private double temperaturaMax;
    private double temperaturaAtual;
    private String descricao;
    private String icone;
    private int umidade;
    private double vento;

    // Construtor sem id (para inserção)
    public Previsao(String cidade, String data, double temperaturaMin,
                    double temperaturaMax, double temperaturaAtual,
                    String descricao, String icone, int umidade, double vento) {
        this.cidade = cidade;
        this.data = data;
        this.temperaturaMin = temperaturaMin;
        this.temperaturaMax = temperaturaMax;
        this.temperaturaAtual = temperaturaAtual;
        this.descricao = descricao;
        this.icone = icone;
        this.umidade = umidade;
        this.vento = vento;
    }

    // Construtor completo (para leitura do banco)
    public Previsao(int id, String cidade, String data, double temperaturaMin,
                    double temperaturaMax, double temperaturaAtual,
                    String descricao, String icone, int umidade, double vento) {
        this.id = id;
        this.cidade = cidade;
        this.data = data;
        this.temperaturaMin = temperaturaMin;
        this.temperaturaMax = temperaturaMax;
        this.temperaturaAtual = temperaturaAtual;
        this.descricao = descricao;
        this.icone = icone;
        this.umidade = umidade;
        this.vento = vento;
    }

    // Getters e Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getCidade() { return cidade; }
    public void setCidade(String cidade) { this.cidade = cidade; }

    public String getData() { return data; }
    public void setData(String data) { this.data = data; }

    public double getTemperaturaMin() { return temperaturaMin; }
    public void setTemperaturaMin(double temperaturaMin) { this.temperaturaMin = temperaturaMin; }

    public double getTemperaturaMax() { return temperaturaMax; }
    public void setTemperaturaMax(double temperaturaMax) { this.temperaturaMax = temperaturaMax; }

    public double getTemperaturaAtual() { return temperaturaAtual; }
    public void setTemperaturaAtual(double temperaturaAtual) { this.temperaturaAtual = temperaturaAtual; }

    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }

    public String getIcone() { return icone; }
    public void setIcone(String icone) { this.icone = icone; }

    public int getUmidade() { return umidade; }
    public void setUmidade(int umidade) { this.umidade = umidade; }

    public double getVento() { return vento; }
    public void setVento(double vento) { this.vento = vento; }

    @Override
    public String toString() {
        return cidade + " | " + data + " | " + temperaturaAtual + "°C | " + descricao;
    }
}
