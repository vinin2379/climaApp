package com.example.climaoff;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

/**
 * Serviço de geocoding usando a API Open-Meteo Geocoding.
 *
 * Endpoint: https://geocoding-api.open-meteo.com/v1/search
 *   ?name={cidade}&count=1&language=pt&format=json
 *
 * Gratuito, sem chave de API, mesma origem do Open-Meteo.
 */
public class GeocodingService {

    private static final String TAG = "GeocodingService";
    private static final String BASE_URL = "https://geocoding-api.open-meteo.com/v1/search";

    /**
     * Resultado do geocoding com os dados essenciais da cidade encontrada.
     */
    public static class ResultadoGeo {
        public final String nomeExibicao; // Ex: "Campinas, São Paulo, Brasil"
        public final double latitude;
        public final double longitude;

        public ResultadoGeo(String nomeExibicao, double latitude, double longitude) {
            this.nomeExibicao = nomeExibicao;
            this.latitude     = latitude;
            this.longitude    = longitude;
        }
    }

    /**
     * Busca as coordenadas de uma cidade pelo nome.
     * DEVE ser chamado em thread secundária.
     *
     * @param nomeCidade Nome da cidade digitado pelo usuário
     * @return ResultadoGeo com lat/lon e nome formatado, ou null se não encontrado
     */
    public ResultadoGeo buscarCoordenadas(String nomeCidade) {
        HttpURLConnection connection = null;

        try {
            String query = URLEncoder.encode(nomeCidade.trim(), "UTF-8");
            String urlStr = BASE_URL
                    + "?name=" + query
                    + "&count=1"
                    + "&language=pt"
                    + "&format=json";

            Log.d(TAG, "Geocoding: " + urlStr);

            URL url = new URL(urlStr);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(10000);

            int responseCode = connection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                Log.e(TAG, "Erro HTTP geocoding: " + responseCode);
                return null;
            }

            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(connection.getInputStream()));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            reader.close();

            return parsearResposta(sb.toString());

        } catch (Exception e) {
            Log.e(TAG, "Erro no geocoding: " + e.getMessage(), e);
            return null;
        } finally {
            if (connection != null) connection.disconnect();
        }
    }

    private ResultadoGeo parsearResposta(String json) throws Exception {
        JSONObject root = new JSONObject(json);

        if (!root.has("results")) {
            Log.w(TAG, "Nenhum resultado de geocoding encontrado.");
            return null;
        }

        JSONArray results = root.getJSONArray("results");
        if (results.length() == 0) return null;

        JSONObject cidade = results.getJSONObject(0);

        double lat    = cidade.getDouble("latitude");
        double lon    = cidade.getDouble("longitude");
        String nome   = cidade.getString("name");
        String pais   = cidade.optString("country", "");
        String estado = cidade.optString("admin1", ""); // Estado / Província

        // Monta nome de exibição: "Campinas, São Paulo, Brasil"
        StringBuilder nomeExibicao = new StringBuilder(nome);
        if (!estado.isEmpty()) nomeExibicao.append(", ").append(estado);
        if (!pais.isEmpty())   nomeExibicao.append(", ").append(pais);

        Log.d(TAG, "Cidade encontrada: " + nomeExibicao + " (" + lat + ", " + lon + ")");
        return new ResultadoGeo(nomeExibicao.toString(), lat, lon);
    }
}
