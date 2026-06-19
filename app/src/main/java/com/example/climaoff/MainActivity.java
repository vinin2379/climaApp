package com.example.climaoff;

import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private WeatherRepository repository;

    // Views
    private TextView    tvCidade, tvData, tvTemperaturaAtual,
                        tvDescricao, tvUmidade, tvVento, tvMinMax, tvIconePrincipal,
                        tvStatusOffline;
    private ProgressBar progressBar;
    private RecyclerView rvPrevisoes;
    private PrevisaoAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        repository = new WeatherRepository(this);

        inicializarComponentes();
        configurarRecyclerView();
        buscarDados();
    }

    private void inicializarComponentes() {
        tvCidade           = findViewById(R.id.tvCidade);
        tvData             = findViewById(R.id.tvData);
        tvTemperaturaAtual = findViewById(R.id.tvTemperaturaAtual);
        tvDescricao        = findViewById(R.id.tvDescricao);
        tvUmidade          = findViewById(R.id.tvUmidade);
        tvVento            = findViewById(R.id.tvVento);
        tvMinMax           = findViewById(R.id.tvMinMax);
        tvIconePrincipal   = findViewById(R.id.tvIconePrincipal);
        progressBar        = findViewById(R.id.progressBar);
        tvStatusOffline    = findViewById(R.id.tvStatusOffline);
        rvPrevisoes        = findViewById(R.id.rvPrevisoes);
    }

    private void configurarRecyclerView() {
        rvPrevisoes.setLayoutManager(new LinearLayoutManager(this));
    }

    /**
     * Inicia a busca de dados via repositório.
     * O repositório decide automaticamente entre API ou cache offline.
     */
    private void buscarDados() {
        mostrarLoading(true);
        tvStatusOffline.setVisibility(View.GONE);

        repository.buscarPrevisoes(new WeatherRepository.Callback() {
            @Override
            public void onSuccess(List<Previsao> previsoes, boolean fromCache) {
                mostrarLoading(false);

                if (fromCache) {
                    tvStatusOffline.setVisibility(View.VISIBLE);
                    tvStatusOffline.setText("⚠️ Modo offline — exibindo dados em cache");
                    Toast.makeText(MainActivity.this,
                            "Sem conexão. Exibindo dados salvos.", Toast.LENGTH_LONG).show();
                }

                exibirPrevisoes(previsoes);
            }

            @Override
            public void onError(String mensagem) {
                mostrarLoading(false);
                tvStatusOffline.setVisibility(View.VISIBLE);
                tvStatusOffline.setText("❌ " + mensagem);
                Toast.makeText(MainActivity.this, mensagem, Toast.LENGTH_LONG).show();
            }
        });
    }

    /**
     * Preenche a UI com os dados recebidos.
     */
    private void exibirPrevisoes(List<Previsao> lista) {
        if (lista.isEmpty()) return;

        // Primeiro item = clima atual
        Previsao atual = lista.get(0);
        tvCidade.setText(atual.getCidade());
        tvData.setText(atual.getData());
        tvTemperaturaAtual.setText(String.format("%.0f°C", atual.getTemperaturaAtual()));
        tvDescricao.setText(atual.getDescricao());
        tvUmidade.setText(atual.getUmidade() + "%");
        tvVento.setText(String.format("%.0f km/h", atual.getVento()));
        tvMinMax.setText(String.format("%.0f° / %.0f°",
                atual.getTemperaturaMin(), atual.getTemperaturaMax()));
        tvIconePrincipal.setText(converterIconeParaEmoji(atual.getIcone()));

        // Próximos dias na RecyclerView
        List<Previsao> proximosDias = new ArrayList<>(lista);
        if (!proximosDias.isEmpty()) proximosDias.remove(0);

        if (adapter == null) {
            adapter = new PrevisaoAdapter(proximosDias);
            rvPrevisoes.setAdapter(adapter);
        } else {
            adapter.atualizarDados(proximosDias);
        }
    }

    private void mostrarLoading(boolean mostrar) {
        progressBar.setVisibility(mostrar ? View.VISIBLE : View.GONE);
        rvPrevisoes.setVisibility(mostrar ? View.GONE : View.VISIBLE);
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
