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
    private static final int    VERSAO_BANCO = 1;

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
                COL_VENTO      + " REAL NOT NULL" +
                ")";
        db.execSQL(sql);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABELA);
        onCreate(db);
    }

    // INSERT
    public long inserir(Previsao p) {
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
        long id = db.insert(TABELA, null, values);
        db.close();
        return id;
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

    // DELETE todos (mais eficiente que deletar um por um)
    public void deletarTodos() {
        SQLiteDatabase db = getWritableDatabase();
        db.delete(TABELA, null, null);
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
