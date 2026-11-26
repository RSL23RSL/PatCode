package br.patcode.app

import android.content.ClipData
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.InputType
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.CheckBox
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var repo: Repositorio
    private val itens = mutableListOf<ItemPatrimonio>()   // mais recente primeiro
    private lateinit var adapter: ItensAdapter

    private lateinit var editLocal: EditText
    private lateinit var checkContinuo: CheckBox
    private lateinit var checkLanterna: CheckBox
    private lateinit var txtContador: TextView
    private lateinit var txtVazio: TextView

    private val prefs by lazy { getSharedPreferences("patcode", MODE_PRIVATE) }
    private val formatoData = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale("pt", "BR"))

    private val leitor = registerForActivityResult(ScanContract()) { resultado ->
        val conteudo = resultado.contents
        if (conteudo == null) {
            // Usuário voltou da câmera sem ler nada: encerra a leitura contínua.
            return@registerForActivityResult
        }
        processarCodigo(conteudo, resultado.formatName ?: "", vindoDaCamera = true)
    }

    private val exportador =
        registerForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
            if (uri == null) return@registerForActivityResult
            try {
                contentResolver.openOutputStream(uri)?.use {
                    it.write(repo.gerarCsv(itens.asReversed()).toByteArray(Charsets.UTF_8))
                }
                toast("Cópia salva com sucesso")
            } catch (e: Exception) {
                toast("Erro ao salvar: ${e.message}")
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        title = "PatCode – Patrimônio"

        repo = Repositorio(this)
        itens.addAll(repo.carregar().asReversed())

        editLocal = findViewById(R.id.editLocal)
        checkContinuo = findViewById(R.id.checkContinuo)
        checkLanterna = findViewById(R.id.checkLanterna)
        txtContador = findViewById(R.id.txtContador)
        txtVazio = findViewById(R.id.txtVazio)

        editLocal.setText(prefs.getString("local", ""))
        checkContinuo.isChecked = prefs.getBoolean("continuo", false)
        checkLanterna.isChecked = prefs.getBoolean("lanterna", false)

        adapter = ItensAdapter(itens) { pos -> if (pos >= 0) abrirItem(pos) }
        findViewById<RecyclerView>(R.id.lista).apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = this@MainActivity.adapter
        }

        findViewById<View>(R.id.btnEscanear).setOnClickListener { iniciarLeitura() }
        findViewById<View>(R.id.btnDigitar).setOnClickListener { digitarCodigo() }
        findViewById<View>(R.id.btnCompartilhar).setOnClickListener { compartilharArquivo() }

        atualizarTela()
    }

    override fun onPause() {
        super.onPause()
        prefs.edit()
            .putString("local", editLocal.text.toString().trim())
            .putBoolean("continuo", checkContinuo.isChecked)
            .putBoolean("lanterna", checkLanterna.isChecked)
            .apply()
    }

    // ---------- Leitura ----------

    private fun iniciarLeitura() {
        val opcoes = ScanOptions().apply {
            setDesiredBarcodeFormats(ScanOptions.ALL_CODE_TYPES)
            setPrompt(
                if (checkContinuo.isChecked) "Leitura contínua (${itens.size} itens) – volte para encerrar"
                else "Aponte para o código de barras da etiqueta"
            )
            setBeepEnabled(true)
            setOrientationLocked(true)
            setTorchEnabled(checkLanterna.isChecked)
            setCaptureActivity(CaptureActivityPortrait::class.java)
        }
        leitor.launch(opcoes)
    }

    private fun processarCodigo(bruto: String, formato: String, vindoDaCamera: Boolean) {
        val codigo = bruto.trim()
        if (codigo.isEmpty()) return
        val continuo = vindoDaCamera && checkContinuo.isChecked

        if (itens.any { it.codigo == codigo }) {
            if (continuo) {
                toast("⚠ $codigo já registrado – ignorado")
                lerNovamente()
            } else {
                AlertDialog.Builder(this)
                    .setTitle("Código já registrado")
                    .setMessage("O patrimônio $codigo já está na lista. Registrar novamente?")
                    .setPositiveButton("Registrar") { _, _ -> adicionar(codigo, formato) }
                    .setNegativeButton("Cancelar", null)
                    .show()
            }
            return
        }
        adicionar(codigo, formato)
        if (continuo) lerNovamente()
    }

    private fun lerNovamente() {
        Handler(Looper.getMainLooper()).postDelayed({ iniciarLeitura() }, 400)
    }

    private fun adicionar(codigo: String, formato: String) {
        val item = ItemPatrimonio(
            codigo = codigo,
            formato = formato.replace('_', ' '),
            dataHora = formatoData.format(Date()),
            local = editLocal.text.toString().trim(),
            observacao = ""
        )
        itens.add(0, item)
        salvar()
        adapter.notifyItemInserted(0)
        findViewById<RecyclerView>(R.id.lista).scrollToPosition(0)
        atualizarTela()
        if (!checkContinuo.isChecked) toast("✔ $codigo registrado")
    }

    private fun digitarCodigo() {
        val campo = EditText(this).apply {
            hint = "Número do patrimônio"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS
        }
        AlertDialog.Builder(this)
            .setTitle("Digitar código")
            .setMessage("Use quando a etiqueta estiver danificada ou ilegível.")
            .setView(comMargem(campo))
            .setPositiveButton("Adicionar") { _, _ ->
                processarCodigo(campo.text.toString(), "DIGITADO", vindoDaCamera = false)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    // ---------- Edição ----------

    private fun abrirItem(pos: Int) {
        val item = itens[pos]
        val campo = EditText(this).apply {
            hint = "Observação (estado, responsável…)"
            setText(item.observacao)
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
        }
        AlertDialog.Builder(this)
            .setTitle(item.codigo)
            .setMessage("${item.dataHora}\nLocal: ${item.local.ifBlank { "—" }}\nFormato: ${item.formato}")
            .setView(comMargem(campo))
            .setPositiveButton("Salvar") { _, _ ->
                item.observacao = campo.text.toString().trim()
                salvar(); adapter.notifyItemChanged(pos)
            }
            .setNeutralButton("Excluir") { _, _ -> confirmarExclusao(pos) }
            .setNegativeButton("Fechar", null)
            .show()
    }

    private fun confirmarExclusao(pos: Int) {
        AlertDialog.Builder(this)
            .setMessage("Excluir o registro ${itens[pos].codigo}?")
            .setPositiveButton("Excluir") { _, _ ->
                itens.removeAt(pos)
                salvar(); adapter.notifyDataSetChanged(); atualizarTela()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    // ---------- Compartilhar / visualizar ----------

    private fun compartilharArquivo() {
        if (itens.isEmpty()) { toast("Nenhum item para compartilhar"); return }
        salvar()
        val uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", repo.arquivo)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Levantamento de patrimônio – ${itens.size} itens")
            putExtra(Intent.EXTRA_TEXT, "Segue em anexo o levantamento de patrimônio (${itens.size} itens).")
            clipData = ClipData.newRawUri("patrimonio", uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(intent, "Enviar arquivo por…"))
    }

    private fun compartilharTexto() {
        if (itens.isEmpty()) { toast("Nenhum item para compartilhar"); return }
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Levantamento de patrimônio")
            putExtra(Intent.EXTRA_TEXT, repo.gerarTexto(itens.asReversed()))
        }
        startActivity(Intent.createChooser(intent, "Enviar lista por…"))
    }

    private fun verArquivo() {
        val conteudo = if (repo.arquivo.exists())
            repo.arquivo.readText(Charsets.UTF_8).removePrefix("﻿") else "(arquivo vazio)"
        val tv = TextView(this).apply {
            text = conteudo
            setTextIsSelectable(true)
            typeface = android.graphics.Typeface.MONOSPACE
            textSize = 12f
            setPadding(40, 24, 40, 24)
        }
        val rolagem = android.widget.ScrollView(this).apply {
            addView(android.widget.HorizontalScrollView(this@MainActivity).apply { addView(tv) })
        }
        AlertDialog.Builder(this)
            .setTitle(Repositorio.NOME_ARQUIVO)
            .setView(rolagem)
            .setPositiveButton("Fechar", null)
            .setNeutralButton("Compartilhar") { _, _ -> compartilharArquivo() }
            .show()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_ver_arquivo -> verArquivo()
            R.id.menu_compartilhar_texto -> compartilharTexto()
            R.id.menu_exportar -> {
                val data = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())
                exportador.launch("patrimonio_$data.csv")
            }
            R.id.menu_limpar -> AlertDialog.Builder(this)
                .setTitle("Apagar tudo?")
                .setMessage("Os ${itens.size} registros serão apagados. Compartilhe o arquivo antes, se precisar dele.")
                .setPositiveButton("Apagar") { _, _ ->
                    itens.clear(); salvar(); adapter.notifyDataSetChanged(); atualizarTela()
                }
                .setNegativeButton("Cancelar", null)
                .show()
            R.id.menu_sobre -> AlertDialog.Builder(this)
                .setTitle("Arquivo de dados")
                .setMessage("Os registros são salvos automaticamente em:\n\n${repo.arquivo.absolutePath}\n\n" +
                        "Formato CSV (separado por ponto e vírgula), abre no Excel ou Google Planilhas.\n\n" +
                        "Para guardar em outra pasta (ex.: Downloads), use o menu \"Salvar cópia em…\".")
                .setPositiveButton("OK", null)
                .show()
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }

    // ---------- Utilidades ----------

    private fun salvar() = repo.salvar(itens.asReversed())   // arquivo em ordem cronológica

    private fun atualizarTela() {
        txtContador.text = if (itens.size == 1) "1 item registrado" else "${itens.size} itens registrados"
        txtVazio.visibility = if (itens.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun comMargem(v: View): View = FrameLayout(this).apply {
        val m = (20 * resources.displayMetrics.density).toInt()
        setPadding(m, m / 2, m, 0)
        addView(v)
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}
