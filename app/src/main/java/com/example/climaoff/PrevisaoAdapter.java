package com.example.climaoff;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class PrevisaoAdapter extends RecyclerView.Adapter<PrevisaoAdapter.ViewHolder> {

    private List<Previsao> previsoes;

    public PrevisaoAdapter(List<Previsao> previsoes) {
        this.previsoes = previsoes;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_previsao, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Previsao previsao = previsoes.get(position);
        holder.tvData.setText(previsao.getData());
        holder.tvIcone.setText(converterIconeParaEmoji(previsao.getIcone()));
        holder.tvMinMax.setText(String.format("%.0f° / %.0f°", previsao.getTemperaturaMin(), previsao.getTemperaturaMax()));
    }

    @Override
    public int getItemCount() {
        return previsoes.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvData, tvIcone, tvMinMax;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvData = itemView.findViewById(R.id.tvItemData);
            tvIcone = itemView.findViewById(R.id.tvItemIcone);
            tvMinMax = itemView.findViewById(R.id.tvItemMinMax);
        }
    }

    private String converterIconeParaEmoji(String icone) {
        // Mapeamento simples de ícones OpenWeather para Emojis
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
