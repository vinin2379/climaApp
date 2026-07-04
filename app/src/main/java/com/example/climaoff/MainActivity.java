package com.example.climaoff;

import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
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
    private EditText    etBuscaCidade;
    private Button      btnBuscar;
    private TextView    tvCidade, tvData, tvTemperaturaAtual,
                        tvDescricao, tvUmidade, tvVento, tvMinMax, tvIconePrincipal,
                        tvStatusOffline, tvUltimasCidadesTitulo;
    private ProgressBar progressBar;
    private RecyclerView rvPrevisoes;
    private PrevisaoAdapter adapter;
    private LinearLayout containerUltimasCidades;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        repository = new WeatherRepository(this);

        inicializarComponentes();
        configurarBusca();
        configurarRecyclerView();

        // Carrega São Paulo como cidade padrão na abertura
        buscarPorCoordenadas("São Paulo", -23.5505, -46.6333);
        atualizarUltimasCidades();
    }

    private void inicializarComponentes() {
        etBuscaCidade           = findViewById(R.id.etBuscaCidade);
        btnBuscar               = findViewById(R.id.btnBuscar);
        tvCidade                = findViewById(R.id.tvCidade);
        tvData                  = findViewById(R.id.tvData);
        tvTemperaturaAtual      = findViewById(R.id.tvTemperaturaAtual);
        tvDescricao             = findViewById(R.id.tvDescricao);
        tvUmidade               = findViewById(R.id.tvUmidade);
        tvVento                 = findViewById(R.id.tvVento);
        tvMinMax                = findViewById(R.id.tvMinMax);
        tvIconePrincipal        = findViewById(R.id.tvIconePrincipal);
        progressBar             = findViewById(R.id.progressBar);
        tvStatusOffline         = findViewById(R.id.tvStatusOffline);
        rvPrevisoes             = findViewById(R.id.rvPrevisoes);
        tvUltimasCidadesTitulo  = findViewById(R.id.tvUltimasCidadesTitulo);
        containerUltimasCidades = findViewById(R.id.containerUltimasCidades);
    }

    private void configurarBusca() {
        // Botão de busca
        btnBuscar.setOnClickListener(v -> executarBuscaPorNome());

        // Teclado: tecla "Buscar" / Enter no campo de texto
        etBuscaCidade.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH
                    || (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                executarBuscaPorNome();
                return true;
            }
            return false;
        });
    }

    private void executarBuscaPorNome() {
        String nomeCidade = etBuscaCidade.getText().toString().trim();

        if (nomeCidade.isEmpty()) {
            Toast.makeText(this, "Digite o nome de uma cidade.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Fecha o teclado
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (imm != null) imm.hideSoftInputFromWindow(etBuscaCidade.getWindowToken(), 0);

        mostrarLoading(true);
        tvStatusOffline.setVisibility(View.GONE);

        repository.buscarPrevisoesPorNome(nomeCidade, criarCallback());
    }

    private void buscarPorCoordenadas(String cidade, double lat, double lon) {
        mostrarLoading(true);
        tvStatusOffline.setVisibility(View.GONE);
        repository.buscarPrevisoesPorCoordenadas(cidade, lat, lon, criarCallback());
    }

    private void configurarRecyclerView() {
        rvPrevisoes.setLayoutManager(new LinearLayoutManager(this));
    }

    private WeatherRepository.Callback criarCallback() {
        return new WeatherRepository.Callback() {
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
                atualizarUltimasCidades();
            }

            @Override
            public void onError(String mensagem) {
                mostrarLoading(false);
                tvStatusOffline.setVisibility(View.VISIBLE);
                tvStatusOffline.setText("❌ " + mensagem);
                Toast.makeText(MainActivity.this, mensagem, Toast.LENGTH_LONG).show();
            }
        };
    }

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

    /**
     * Busca no repositório as últimas cidades pesquisadas (cache) e monta
     * os "chips" clicáveis acima da barra de busca.
     */
    private void atualizarUltimasCidades() {
        repository.buscarResumoCidadesRecentes(resumos -> {
            containerUltimasCidades.removeAllViews();

            if (resumos == null || resumos.isEmpty()) {
                tvUltimasCidadesTitulo.setVisibility(View.GONE);
                containerUltimasCidades.setVisibility(View.GONE);
                return;
            }

            tvUltimasCidadesTitulo.setVisibility(View.VISIBLE);
            containerUltimasCidades.setVisibility(View.VISIBLE);

            for (Previsao p : resumos) {
                containerUltimasCidades.addView(criarChipCidade(p));
            }
        });
    }

    private TextView criarChipCidade(Previsao p) {
        TextView chip = new TextView(this);
        chip.setText(converterIconeParaEmoji(p.getIcone()) + " " + p.getCidade()
                + "  " + String.format("%.0f°", p.getTemperaturaAtual()));
        chip.setTextSize(12);
        chip.setTextColor(getResources().getColor(R.color.text_primary));
        chip.setBackgroundColor(getResources().getColor(R.color.card_bg));
        chip.setPadding(28, 16, 28, 16);
        chip.setSingleLine(true);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMarginEnd(8);
        chip.setLayoutParams(lp);

        chip.setOnClickListener(v -> {
            mostrarLoading(true);
            tvStatusOffline.setVisibility(View.GONE);
            repository.buscarPrevisoesDoCache(p.getCidade(), criarCallback());
        });

        return chip;
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
