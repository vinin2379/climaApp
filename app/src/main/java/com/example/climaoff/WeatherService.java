package com.example.climaoff;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Serviço responsável por consumir a API Open-Meteo.
 *
 * Endpoint usado:
 * https://api.open-meteo.com/v1/forecast
 *   ?latitude={lat}&longitude={lon}
 *   &current=temperature_2m,relative_humidity_2m,weathercode,windspeed_10m
 *   &daily=temperature_2m_max,temperature_2m_min,weathercode
 *   &timezone=America/Sao_Paulo
 *   &forecast_days=5
 *
 * Não requer chave de API.
 */
public class WeatherService {

    private static final String TAG = "WeatherService";

    // Coordenadas padrão: São Paulo, SP
    private static final double DEFAULT_LAT = -23.5505;
    private static final double DEFAULT_LON = -46.6333;
    private static final String DEFAULT_CITY = "São Paulo";
    private static final String TIMEZONE = "America%2FSao_Paulo";

    /**
     * Busca previsão do tempo da API Open-Meteo.
     * DEVE ser chamado em uma thread secundária (não na UI thread).
     *
     * @param cidade Nome da cidade (apenas para exibição)
     * @param lat    Latitude
     * @param lon    Longitude
     * @return Lista de Previsao com dados atuais + próximos dias, ou lista vazia em caso de erro.
     */
    public List<Previsao> buscarPrevisao(String cidade, double lat, double lon) {
        List<Previsao> lista = new ArrayList<>();
        HttpURLConnection connection = null;

        try {
            String urlStr = "https://api.open-meteo.com/v1/forecast"
                    + "?latitude=" + lat
                    + "&longitude=" + lon
                    + "&current=temperature_2m,relative_humidity_2m,weathercode,windspeed_10m"
                    + "&daily=temperature_2m_max,temperature_2m_min,weathercode"
                    + "&timezone=" + TIMEZONE
                    + "&forecast_days=5";

            Log.d(TAG, "Requisitando: " + urlStr);

            URL url = new URL(urlStr);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(10000); // 10 segundos
            connection.setReadTimeout(10000);

            int responseCode = connection.getResponseCode();
            Log.d(TAG, "Response code: " + responseCode);

            if (responseCode != HttpURLConnection.HTTP_OK) {
                Log.e(TAG, "Erro HTTP: " + responseCode);
                return lista;
            }

            // Ler resposta
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(connection.getInputStream()));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            reader.close();

            lista = parsearResposta(sb.toString(), cidade);

        } catch (Exception e) {
            Log.e(TAG, "Erro ao buscar previsão: " + e.getMessage(), e);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }

        return lista;
    }

    /**
     * Busca previsão com coordenadas padrão (São Paulo).
     */
    public List<Previsao> buscarPrevisaoDefault() {
        return buscarPrevisao(DEFAULT_CITY, DEFAULT_LAT, DEFAULT_LON);
    }

    /**
     * Parseia o JSON retornado pela Open-Meteo e monta a lista de Previsao.
     */
    private List<Previsao> parsearResposta(String json, String cidade) throws Exception {
        List<Previsao> lista = new ArrayList<>();
        JSONObject root = new JSONObject(json);

        // ── Dados do clima ATUAL ──────────────────────────────────────────────
        JSONObject current = root.getJSONObject("current");
        double tempAtual   = current.getDouble("temperature_2m");
        int umidade        = current.getInt("relative_humidity_2m");
        double vento       = current.getDouble("windspeed_10m");
        int weatherCode    = current.getInt("weathercode");

        // ── Dados DIÁRIOS ─────────────────────────────────────────────────────
        JSONObject daily        = root.getJSONObject("daily");
        JSONArray  datas        = daily.getJSONArray("time");
        JSONArray  tempsMax     = daily.getJSONArray("temperature_2m_max");
        JSONArray  tempsMin     = daily.getJSONArray("temperature_2m_min");
        JSONArray  codigosDia   = daily.getJSONArray("weathercode");

        // Primeiro item = hoje (clima atual)
        double tempMax0 = tempsMax.getDouble(0);
        double tempMin0 = tempsMin.getDouble(0);
        String iconeAtual   = weatherCodeParaIcone(weatherCode);
        String descAtual    = weatherCodeParaDescricao(weatherCode);
        String dataFormatada = formatarData(datas.getString(0), true);

        lista.add(new Previsao(
                cidade,
                dataFormatada,
                tempMin0,
                tempMax0,
                tempAtual,
                descAtual,
                iconeAtual,
                umidade,
                vento
        ));

        // Próximos dias (índice 1 em diante)
        for (int i = 1; i < datas.length(); i++) {
            int codigo      = codigosDia.getInt(i);
            double maxTemp  = tempsMax.getDouble(i);
            double minTemp  = tempsMin.getDouble(i);
            String icone    = weatherCodeParaIcone(codigo);
            String desc     = weatherCodeParaDescricao(codigo);
            String data     = formatarData(datas.getString(i), false);

            // Para dias futuros, usamos a média como "temperatura atual"
            double mediaTemp = (maxTemp + minTemp) / 2.0;

            lista.add(new Previsao(
                    cidade,
                    data,
                    minTemp,
                    maxTemp,
                    mediaTemp,
                    desc,
                    icone,
                    0,    // umidade não disponível no endpoint diário
                    0.0   // vento não disponível no endpoint diário
            ));
        }

        return lista;
    }

    /**
     * Formata "2024-06-03" → "Hoje, 03 Jun" ou "Seg, 03 Jun".
     */
    private String formatarData(String dataISO, boolean isHoje) {
        try {
            SimpleDateFormat sdfIn  = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Date date = sdfIn.parse(dataISO);

            if (isHoje) {
                SimpleDateFormat sdfOut = new SimpleDateFormat("dd MMM", new Locale("pt", "BR"));
                return "Hoje, " + sdfOut.format(date);
            } else {
                SimpleDateFormat sdfOut = new SimpleDateFormat("EEE, dd MMM", new Locale("pt", "BR"));
                String formatted = sdfOut.format(date);
                // Capitaliza primeira letra
                return formatted.substring(0, 1).toUpperCase() + formatted.substring(1);
            }
        } catch (Exception e) {
            return dataISO;
        }
    }

    /**
     * Converte WMO Weather Code para o código de ícone no formato OpenWeather
     * (reutilizando o mapeamento emoji já existente no projeto).
     *
     * Referência WMO: https://open-meteo.com/en/docs#weathervariables
     */
    private String weatherCodeParaIcone(int code) {
        if (code == 0)                         return "01d"; // Céu limpo
        if (code == 1 || code == 2)            return "02d"; // Principalmente limpo / parcialmente nublado
        if (code == 3)                         return "03d"; // Nublado
        if (code >= 45 && code <= 48)          return "50d"; // Névoa
        if (code >= 51 && code <= 55)          return "09d"; // Garoa
        if (code >= 61 && code <= 65)          return "10d"; // Chuva
        if (code >= 71 && code <= 77)          return "13d"; // Neve
        if (code >= 80 && code <= 82)          return "10d"; // Pancadas de chuva
        if (code >= 95 && code <= 99)          return "11d"; // Tempestade
        return "01d";
    }

    /**
     * Converte WMO Weather Code para descrição em português.
     */
    private String weatherCodeParaDescricao(int code) {
        if (code == 0)                         return "Céu Limpo";
        if (code == 1)                         return "Principalmente Limpo";
        if (code == 2)                         return "Parcialmente Nublado";
        if (code == 3)                         return "Nublado";
        if (code >= 45 && code <= 48)          return "Névoa";
        if (code >= 51 && code <= 53)          return "Garoa Leve";
        if (code >= 55 && code <= 57)          return "Garoa Intensa";
        if (code >= 61 && code <= 63)          return "Chuva Leve";
        if (code == 65)                        return "Chuva Forte";
        if (code >= 71 && code <= 73)          return "Neve Leve";
        if (code >= 75 && code <= 77)          return "Neve Forte";
        if (code >= 80 && code <= 81)          return "Pancadas Leves";
        if (code == 82)                        return "Pancadas Fortes";
        if (code >= 95 && code <= 99)          return "Tempestade";
        return "Clima Variado";
    }
}
