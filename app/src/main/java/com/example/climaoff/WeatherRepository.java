package com.example.climaoff;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.util.List;

/**
 * Camada de repositório que decide de onde buscar os dados:
 *  - Online  → GeocodingService (nome → coords) → WeatherService (API) → SQLite → retorna dados
 *  - Offline → SQLite (cache local) → retorna dados
 *
 * Guarda em cache as últimas MAX_CIDADES_CACHE cidades pesquisadas
 * (cada uma com seu próprio conjunto de previsões), permitindo acesso
 * rápido offline às últimas cidades consultadas.
 *
 * Deve ser executado em thread secundária (usa callback para retornar ao UI thread).
 */
public class WeatherRepository {

    private static final String TAG = "WeatherRepository";
    private static final int MAX_CIDADES_CACHE = 3;

    private final Context context;
    private final DatabaseHelper dbHelper;
    private final WeatherService weatherService;
    private final GeocodingService geocodingService;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public interface Callback {
        /** Chamado na UI thread com os dados prontos. */
        void onSuccess(List<Previsao> previsoes, boolean fromCache);
        /** Chamado na UI thread quando há falha total (sem dados nem offline). */
        void onError(String mensagem);
    }

    public interface ResumoCallback {
        /** Chamado na UI thread com o resumo (1 item por cidade) das últimas cidades pesquisadas. */
        void onResumo(List<Previsao> resumos);
    }

    public WeatherRepository(Context context) {
        this.context          = context.getApplicationContext();
        this.dbHelper         = new DatabaseHelper(context);
        this.weatherService   = new WeatherService();
        this.geocodingService = new GeocodingService();
    }

    /**
     * Busca previsões para São Paulo (padrão).
     */
    public void buscarPrevisoes(Callback callback) {
        buscarPrevisoesPorCoordenadas("São Paulo", -23.5505, -46.6333, callback);
    }

    /**
     * Busca previsões a partir de um nome de cidade digitado pelo usuário.
     * Faz geocoding primeiro para obter lat/lon, depois busca o clima.
     */
    public void buscarPrevisoesPorNome(String nomeCidade, Callback callback) {
        new Thread(() -> {
            if (!isOnline()) {
                Log.d(TAG, "Offline — usando cache local");
                buscarDoCachePorNomeAproximado(nomeCidade, callback);
                return;
            }

            // Geocoding: nome → coordenadas
            GeocodingService.ResultadoGeo geo = geocodingService.buscarCoordenadas(nomeCidade);

            if (geo == null) {
                notificarErro(callback, "Cidade \"" + nomeCidade + "\" não encontrada. Verifique o nome e tente novamente.");
                return;
            }

            buscarDaApi(geo.nomeExibicao, geo.latitude, geo.longitude, callback);
        }).start();
    }

    /**
     * Busca previsões para coordenadas já conhecidas.
     */
    public void buscarPrevisoesPorCoordenadas(String cidade, double lat, double lon, Callback callback) {
        new Thread(() -> {
            if (isOnline()) {
                Log.d(TAG, "Dispositivo online — buscando da API");
                buscarDaApi(cidade, lat, lon, callback);
            } else {
                Log.d(TAG, "Dispositivo offline — usando cache local");
                buscarDoCachePorNomeAproximado(cidade, callback);
            }
        }).start();
    }

    /**
     * Busca as previsões de uma cidade específica diretamente do cache local
     * (usado ao tocar em um dos "chips" de últimas cidades pesquisadas).
     */
    public void buscarPrevisoesDoCache(String cidade, Callback callback) {
        new Thread(() -> {
            List<Previsao> previsoes = dbHelper.listarPorCidade(cidade);
            if (previsoes != null && !previsoes.isEmpty()) {
                notificarSucesso(callback, previsoes, true);
            } else {
                notificarErro(callback, "Dados não encontrados no cache para essa cidade.");
            }
        }).start();
    }

    /**
     * Retorna um resumo (1 item por cidade) das últimas cidades pesquisadas,
     * para exibir como atalhos rápidos na tela inicial.
     */
    public void buscarResumoCidadesRecentes(ResumoCallback callback) {
        new Thread(() -> {
            List<Previsao> resumos = dbHelper.listarResumoCidadesRecentes(MAX_CIDADES_CACHE);
            mainHandler.post(() -> callback.onResumo(resumos));
        }).start();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Privados
    // ─────────────────────────────────────────────────────────────────────────

    private void buscarDaApi(String cidade, double lat, double lon, Callback callback) {
        List<Previsao> previsoes = weatherService.buscarPrevisao(cidade, lat, lon);

        if (previsoes != null && !previsoes.isEmpty()) {
            salvarNoBanco(cidade, previsoes);
            notificarSucesso(callback, previsoes, false);
        } else {
            Log.w(TAG, "API retornou vazio — tentando cache local");
            buscarDoCachePorNomeAproximado(cidade, callback);
        }
    }

    /** Cache antigo (usado quando falha achar exatamente a cidade pedida): pega qualquer dado salvo. */
    private void buscarDoCache(Callback callback) {
        List<Previsao> previsoes = dbHelper.listarTodos();

        if (previsoes != null && !previsoes.isEmpty()) {
            notificarSucesso(callback, previsoes, true);
        } else {
            notificarErro(callback, "Sem conexão com a internet e sem dados em cache.");
        }
    }

    /** Tenta achar no cache a cidade pedida; se não achar, cai para qualquer dado salvo. */
    private void buscarDoCachePorNomeAproximado(String cidade, Callback callback) {
        List<Previsao> previsoes = dbHelper.listarPorCidade(cidade);
        if (previsoes != null && !previsoes.isEmpty()) {
            notificarSucesso(callback, previsoes, true);
        } else {
            buscarDoCache(callback);
        }
    }

    /**
     * Salva as previsões da cidade pesquisada, apagando somente os dados
     * antigos DAQUELA cidade (preservando as outras em cache), e depois
     * garante que só as MAX_CIDADES_CACHE cidades mais recentes permaneçam
     * salvas no banco.
     */
    private void salvarNoBanco(String cidade, List<Previsao> previsoes) {
        dbHelper.deletarPorCidade(cidade);
        for (int i = 0; i < previsoes.size(); i++) {
            // O primeiro item da lista é sempre o clima atual/"hoje" daquela cidade
            dbHelper.inserir(previsoes.get(i), i == 0);
        }
        dbHelper.limparCidadesAntigas(MAX_CIDADES_CACHE);
        Log.d(TAG, previsoes.size() + " previsões salvas no banco para " + cidade);
    }

    private boolean isOnline() {
        ConnectivityManager cm = (ConnectivityManager)
                context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return false;
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        return netInfo != null && netInfo.isConnected();
    }

    private void notificarSucesso(Callback callback, List<Previsao> lista, boolean fromCache) {
        mainHandler.post(() -> callback.onSuccess(lista, fromCache));
    }

    private void notificarErro(Callback callback, String mensagem) {
        mainHandler.post(() -> callback.onError(mensagem));
    }
}
