package com.example.climaoff;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private DatabaseHelper dbHelper;
    private TextView tvCidade, tvData, tvTemperaturaAtual, tvDescricao, tvUmidade, tvVento, tvMinMax, tvIconePrincipal;
    private RecyclerView rvPrevisoes;
    private PrevisaoAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dbHelper = new DatabaseHelper(this);
        inicializarComponentes();
        configurarRecyclerView();
        
        // Carregar dados (insere mock se estiver vazio para demonstração)
        carregarDados();
    }

    private void inicializarComponentes() {
        tvCidade = findViewById(R.id.tvCidade);
        tvData = findViewById(R.id.tvData);
        tvTemperaturaAtual = findViewById(R.id.tvTemperaturaAtual);
        tvDescricao = findViewById(R.id.tvDescricao);
        tvUmidade = findViewById(R.id.tvUmidade);
        tvVento = findViewById(R.id.tvVento);
        tvMinMax = findViewById(R.id.tvMinMax);
        tvIconePrincipal = findViewById(R.id.tvIconePrincipal);
        rvPrevisoes = findViewById(R.id.rvPrevisoes);
    }

    private void configurarRecyclerView() {
        rvPrevisoes.setLayoutManager(new LinearLayoutManager(this));
    }

    private void carregarDados() {
        List<Previsao> lista = dbHelper.listarTodos();

        if (lista.isEmpty()) {
            // Criar dados de exemplo se o banco estiver vazio
            dbHelper.inserir(new Previsao("São Paulo", "Hoje, 03 Jun", 18.0, 28.0, 24.0, "Céu Limpo", "01d", 65, 12.0));
            dbHelper.inserir(new Previsao("São Paulo", "Amanhã, 04 Jun", 17.0, 26.0, 22.0, "Parcialmente Nublado", "02d", 70, 10.0));
            dbHelper.inserir(new Previsao("São Paulo", "Qui, 05 Jun", 16.0, 24.0, 20.0, "Chuva Leve", "10d", 85, 15.0));
            dbHelper.inserir(new Previsao("São Paulo", "Sex, 06 Jun", 15.0, 22.0, 19.0, "Nublado", "03d", 80, 8.0));
            lista = dbHelper.listarTodos();
        }

        if (!lista.isEmpty()) {
            // Exibir a primeira previsão como "Clima Atual"
            Previsao atual = lista.get(0);
            tvCidade.setText(atual.getCidade());
            tvData.setText(atual.getData());
            tvTemperaturaAtual.setText(String.format("%.0f°C", atual.getTemperaturaAtual()));
            tvDescricao.setText(atual.getDescricao());
            tvUmidade.setText(atual.getUmidade() + "%");
            tvVento.setText(atual.getVento() + " km/h");
            tvMinMax.setText(String.format("%.0f° / %.0f°", atual.getTemperaturaMin(), atual.getTemperaturaMax()));
            tvIconePrincipal.setText(converterIconeParaEmoji(atual.getIcone()));

            // O restante vai para a lista (RecyclerView)
            List<Previsao> proximosDias = new ArrayList<>(lista);
            proximosDias.remove(0); // Remove o "atual" da lista de próximos
            
            adapter = new PrevisaoAdapter(proximosDias);
            rvPrevisoes.setAdapter(adapter);
        }
    }

    private String converterIconeParaEmoji(String icone) {
        if (icone == null) return "☀️";
        switch (icone) {
            case "01d": case "01n": return "☀️";
            case "02d": case "02n": return "⛅";
            case "03d": case "03n": return "☁️";
            case "04d": case "04n": return "☁️";
            case "09d": case "09n": return "🌧️";
            case "10d": case "10n": return "🌦️";
            case "11d": case "11n": return "⛈️";
            case "13d": case "13n": return "❄️";
            case "50d": case "50n": return "🌫️";
            default: return "☀️";
        }
    }
}
