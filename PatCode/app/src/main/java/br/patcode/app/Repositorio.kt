package br.patcode.app

import android.content.Context
import java.io.File

data class ItemPatrimonio(
    val codigo: String,
    val formato: String,
    val dataHora: String,
    val local: String,
    var observacao: String
)

/**
 * Guarda os registros em um arquivo CSV no próprio celular
 * (pasta privada do app: Android/data/br.patcode.app/files/patrimonio.csv).
 * Separador ";" e codificação UTF-8 com BOM, para abrir certinho no Excel.
 */
class Repositorio(context: Context) {

    val arquivo: File = File(context.getExternalFilesDir(null) ?: context.filesDir, NOME_ARQUIVO)

    fun carregar(): MutableList<ItemPatrimonio> {
        if (!arquivo.exists()) return mutableListOf()
        val linhas = arquivo.readText(Charsets.UTF_8).removePrefix(BOM).lines()
        return linhas.drop(1)                       // pula cabeçalho
            .filter { it.isNotBlank() }
            .map { parseLinha(it) }
            .filter { it.isNotEmpty() && it[0].isNotBlank() }
            .map { c ->
                ItemPatrimonio(
                    codigo = c.getOrElse(0) { "" },
                    formato = c.getOrElse(1) { "" },
                    dataHora = c.getOrElse(2) { "" },
                    local = c.getOrElse(3) { "" },
                    observacao = c.getOrElse(4) { "" }
                )
            }.toMutableList()
    }

    fun salvar(itens: List<ItemPatrimonio>) {
        arquivo.parentFile?.mkdirs()
        arquivo.writeText(gerarCsv(itens), Charsets.UTF_8)
    }

    fun gerarCsv(itens: List<ItemPatrimonio>): String {
        val sb = StringBuilder(BOM)
        sb.append("Codigo;Formato;DataHora;Local;Observacao\r\n")
        for (i in itens) {
            sb.append(listOf(i.codigo, i.formato, i.dataHora, i.local, i.observacao)
                .joinToString(";") { esc(it) })
            sb.append("\r\n")
        }
        return sb.toString()
    }

    fun gerarTexto(itens: List<ItemPatrimonio>): String {
        val sb = StringBuilder("Levantamento de patrimônio – ${itens.size} itens\n\n")
        itens.forEachIndexed { n, i ->
            sb.append("${n + 1}. ${i.codigo}")
            if (i.local.isNotBlank()) sb.append(" | ${i.local}")
            if (i.observacao.isNotBlank()) sb.append(" | ${i.observacao}")
            sb.append(" | ${i.dataHora}\n")
        }
        return sb.toString()
    }

    private fun esc(v: String): String {
        val limpo = v.replace("\r", " ").replace("\n", " ")
        return if (limpo.contains(';') || limpo.contains('"'))
            "\"" + limpo.replace("\"", "\"\"") + "\"" else limpo
    }

    private fun parseLinha(linha: String): List<String> {
        val campos = mutableListOf<String>()
        val atual = StringBuilder()
        var aspas = false
        var i = 0
        while (i < linha.length) {
            val ch = linha[i]
            when {
                aspas && ch == '"' && i + 1 < linha.length && linha[i + 1] == '"' -> { atual.append('"'); i++ }
                ch == '"' -> aspas = !aspas
                ch == ';' && !aspas -> { campos.add(atual.toString()); atual.clear() }
                else -> atual.append(ch)
            }
            i++
        }
        campos.add(atual.toString().trimEnd('\r'))
        return campos
    }

    companion object {
        const val NOME_ARQUIVO = "patrimonio.csv"
        private const val BOM = "﻿"
    }
}
