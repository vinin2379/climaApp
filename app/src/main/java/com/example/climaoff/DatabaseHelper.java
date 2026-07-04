// app/src/main/java/com/exemplo/appclima/database/DatabaseHelper.java
package com.example.climaoff;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.example.climaoff.Previsao;

import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String NOME_BANCO   = "clima.db";
    private static final int    VERSAO_BANCO = 2; // v2: adiciona timestamp + atual (cache multi-cidade)

    // Tabela e colunas
    private static final String TABELA           = "previsoes";
    private static final String COL_ID           = "id";
    private static final String COL_CIDADE       = "cidade";
    private static final String COL_DATA         = "data";
    private static final String COL_TEMP_MIN     = "temperatura_min";
    private static final String COL_TEMP_MAX     = "temperatura_max";
    private static final String COL_TEMP_ATUAL   = "temperatura_atual";
    private static final String COL_DESCRICAO    = "descricao";
    private static final String COL_ICONE        = "icone";
    private static final String COL_UMIDADE      = "umidade";
    private static final String COL_VENTO        = "vento";
    private static final String COL_TIMESTAMP    = "timestamp";
    private static final String COL_ATUAL        = "atual"; // 1 = registro "de hoje" daquela cidade

    public DatabaseHelper(Context context) {
        super(context, NOME_BANCO, null, VERSAO_BANCO);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String sql = "CREATE TABLE " + TABELA + " (" +
                COL_ID         + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_CIDADE     + " TEXT NOT NULL, " +
                COL_DATA       + " TEXT NOT NULL, " +
                COL_TEMP_MIN   + " REAL NOT NULL, " +
                COL_TEMP_MAX   + " REAL NOT NULL, " +
                COL_TEMP_ATUAL + " REAL NOT NULL, " +
                COL_DESCRICAO  + " TEXT NOT NULL, " +
                COL_ICONE      + " TEXT NOT NULL, " +
                COL_UMIDADE    + " INTEGER NOT NULL, " +
                COL_VENTO      + " REAL NOT NULL, " +
                COL_TIMESTAMP  + " INTEGER NOT NULL, " +
                COL_ATUAL      + " INTEGER NOT NULL DEFAULT 0" +
                ")";
        db.execSQL(sql);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABELA);
        onCreate(db);
    }

    // INSERT (agora recebe se esse item é o "atual" daquela cidade, isto é, o primeiro/hoje)
    public long inserir(Previsao p, boolean atual) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_CIDADE,     p.getCidade());
        values.put(COL_DATA,       p.getData());
        values.put(COL_TEMP_MIN,   p.getTemperaturaMin());
        values.put(COL_TEMP_MAX,   p.getTemperaturaMax());
        values.put(COL_TEMP_ATUAL, p.getTemperaturaAtual());
        values.put(COL_DESCRICAO,  p.getDescricao());
        values.put(COL_ICONE,      p.getIcone());
        values.put(COL_UMIDADE,    p.getUmidade());
        values.put(COL_VENTO,      p.getVento());
        values.put(COL_TIMESTAMP,  System.currentTimeMillis());
        values.put(COL_ATUAL,      atual ? 1 : 0);
        long id = db.insert(TABELA, null, values);
        db.close();
        return id;
    }

    // Mantido para compatibilidade: insere sem marcar como "atual"
    public long inserir(Previsao p) {
        return inserir(p, false);
    }

    // SELECT ALL
    public List<Previsao> listarTodos() {
        List<Previsao> lista = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(TABELA, null, null, null, null, null, COL_DATA + " ASC");

        if (cursor.moveToFirst()) {
            do {
                lista.add(cursorParaPrevisao(cursor));
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return lista;
    }

    // SELECT por cidade
    public List<Previsao> listarPorCidade(String cidade) {
        List<Previsao> lista = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(TABELA, null,
                COL_CIDADE + " = ?", new String[]{cidade},
                null, null, COL_DATA + " ASC");

        if (cursor.moveToFirst()) {
            do {
                lista.add(cursorParaPrevisao(cursor));
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return lista;
    }

    /**
     * Retorna 1 registro (o marcado como "atual") por cidade, das mais
     * recentes primeiro. Usado para montar os "chips" de últimas cidades.
     */
    public List<Previsao> listarResumoCidadesRecentes(int limite) {
        List<Previsao> lista = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT * FROM " + TABELA +
                        " WHERE " + COL_ATUAL + " = 1" +
                        " GROUP BY " + COL_CIDADE +
                        " ORDER BY MAX(" + COL_TIMESTAMP + ") DESC LIMIT ?",
                new String[]{String.valueOf(limite)});

        if (cursor.moveToFirst()) {
            do {
                lista.add(cursorParaPrevisao(cursor));
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return lista;
    }

    // UPDATE
    public int atualizar(Previsao p) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_CIDADE,     p.getCidade());
        values.put(COL_DATA,       p.getData());
        values.put(COL_TEMP_MIN,   p.getTemperaturaMin());
        values.put(COL_TEMP_MAX,   p.getTemperaturaMax());
        values.put(COL_TEMP_ATUAL, p.getTemperaturaAtual());
        values.put(COL_DESCRICAO,  p.getDescricao());
        values.put(COL_ICONE,      p.getIcone());
        values.put(COL_UMIDADE,    p.getUmidade());
        values.put(COL_VENTO,      p.getVento());
        int rows = db.update(TABELA, values, COL_ID + " = ?",
                new String[]{String.valueOf(p.getId())});
        db.close();
        return rows;
    }

    // DELETE por id
    public int deletar(int id) {
        SQLiteDatabase db = getWritableDatabase();
        int rows = db.delete(TABELA, COL_ID + " = ?",
                new String[]{String.valueOf(id)});
        db.close();
        return rows;
    }

    // DELETE por cidade (apaga só os dados daquela cidade, mantendo as outras)
    public void deletarPorCidade(String cidade) {
        SQLiteDatabase db = getWritableDatabase();
        db.delete(TABELA, COL_CIDADE + " = ?", new String[]{cidade});
        db.close();
    }

    // DELETE todos (mais eficiente que deletar um por um)
    public void deletarTodos() {
        SQLiteDatabase db = getWritableDatabase();
        db.delete(TABELA, null, null);
        db.close();
    }

    /**
     * Mantém no banco apenas as N cidades pesquisadas mais recentemente,
     * apagando os dados das demais. Chamar sempre depois de salvar uma
     * nova cidade.
     */
    public void limparCidadesAntigas(int maxCidades) {
        SQLiteDatabase db = getWritableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT " + COL_CIDADE + " FROM " + TABELA +
                        " GROUP BY " + COL_CIDADE +
                        " ORDER BY MAX(" + COL_TIMESTAMP + ") DESC", null);

        List<String> cidades = new ArrayList<>();
        if (cursor.moveToFirst()) {
            do {
                cidades.add(cursor.getString(0));
            } while (cursor.moveToNext());
        }
        cursor.close();

        for (int i = maxCidades; i < cidades.size(); i++) {
            db.delete(TABELA, COL_CIDADE + " = ?", new String[]{cidades.get(i)});
        }
        db.close();
    }

    // Converte Cursor → Previsao
    private Previsao cursorParaPrevisao(Cursor cursor) {
        return new Previsao(
                cursor.getInt(cursor.getColumnIndexOrThrow(COL_ID)),
                cursor.getString(cursor.getColumnIndexOrThrow(COL_CIDADE)),
                cursor.getString(cursor.getColumnIndexOrThrow(COL_DATA)),
                cursor.getDouble(cursor.getColumnIndexOrThrow(COL_TEMP_MIN)),
                cursor.getDouble(cursor.getColumnIndexOrThrow(COL_TEMP_MAX)),
                cursor.getDouble(cursor.getColumnIndexOrThrow(COL_TEMP_ATUAL)),
                cursor.getString(cursor.getColumnIndexOrThrow(COL_DESCRICAO)),
                cursor.getString(cursor.getColumnIndexOrThrow(COL_ICONE)),
                cursor.getInt(cursor.getColumnIndexOrThrow(COL_UMIDADE)),
                cursor.getDouble(cursor.getColumnIndexOrThrow(COL_VENTO))
        );
    }
}
