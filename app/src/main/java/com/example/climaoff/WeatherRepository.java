package com.example.climaoff;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import java.util.List;

/**
 * Camada de repositório que decide de onde buscar os dados:
 *  - Online  → WeatherService (API) → salva no SQLite → retorna dados
 *  - Offline → SQLite (cache local) → retorna dados
 *
 * Deve ser executado em thread secundária (usa callback para retornar ao UI thread).
 */
public class WeatherRepository {

    private static final String TAG = "WeatherRepository";

    private final Context context;
    private final DatabaseHelper dbHelper;
    private final WeatherService weatherService;

    public interface Callback {
        /** Chamado na UI thread com os dados prontos. */
        void onSuccess(List<Previsao> previsoes, boolean fromCache);
        /** Chamado na UI thread quando há falha total (sem dados nem offline). */
        void onError(String mensagem);
    }

    public WeatherRepository(Context context) {
        this.context        = context.getApplicationContext();
        this.dbHelper       = new DatabaseHelper(context);
        this.weatherService = new WeatherService();
    }

    /**
     * Busca previsões para São Paulo.
     * Executa em background thread e devolve resultado via callback na UI thread.
     */
    public void buscarPrevisoes(Callback callback) {
        buscarPrevisoes("São Paulo", -23.5505, -46.6333, callback);
    }

    /**
     * Busca previsões para a cidade/coordenadas fornecidas.
     */
    public void buscarPrevisoes(String cidade, double lat, double lon, Callback callback) {
        new Thread(() -> {
            if (isOnline()) {
                Log.d(TAG, "Dispositivo online — buscando da API");
                buscarDaApi(cidade, lat, lon, callback);
            } else {
                Log.d(TAG, "Dispositivo offline — usando cache local");
                buscarDoCache(callback);
            }
        }).start();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Privados
    // ─────────────────────────────────────────────────────────────────────────

    private void buscarDaApi(String cidade, double lat, double lon, Callback callback) {
        List<Previsao> previsoes = weatherService.buscarPrevisao(cidade, lat, lon);

        if (previsoes != null && !previsoes.isEmpty()) {
            // Limpa dados antigos e salva os novos no banco
            salvarNoBanco(previsoes);
            notificarSucesso(callback, previsoes, false);
        } else {
            Log.w(TAG, "API retornou vazio — tentando cache local");
            buscarDoCache(callback);
        }
    }

    private void buscarDoCache(Callback callback) {
        List<Previsao> previsoes = dbHelper.listarTodos();

        if (previsoes != null && !previsoes.isEmpty()) {
            notificarSucesso(callback, previsoes, true);
        } else {
            notificarErro(callback, "Sem conexão com a internet e sem dados em cache.");
        }
    }

    /**
     * Apaga todas as previsões antigas e insere as novas.
     * Mantém o banco sempre atualizado com a consulta mais recente.
     */
    private void salvarNoBanco(List<Previsao> previsoes) {
        // Remove todos os registros antigos
        List<Previsao> antigos = dbHelper.listarTodos();
        for (Previsao p : antigos) {
            dbHelper.deletar(p.getId());
        }
        // Insere os novos
        for (Previsao p : previsoes) {
            dbHelper.inserir(p);
        }
        Log.d(TAG, previsoes.size() + " previsões salvas no banco.");
    }

    private boolean isOnline() {
        ConnectivityManager cm = (ConnectivityManager)
                context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return false;
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        return netInfo != null && netInfo.isConnected();
    }

    // Garante que o callback é chamado na UI thread
    private android.os.Handler mainHandler = new android.os.Handler(
            android.os.Looper.getMainLooper());

    private void notificarSucesso(Callback callback, List<Previsao> lista, boolean fromCache) {
        mainHandler.post(() -> callback.onSuccess(lista, fromCache));
    }

    private void notificarErro(Callback callback, String mensagem) {
        mainHandler.post(() -> callback.onError(mensagem));
    }
}
