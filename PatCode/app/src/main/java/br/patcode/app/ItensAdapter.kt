package br.patcode.app

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ItensAdapter(
    private val itens: List<ItemPatrimonio>,
    private val aoClicar: (Int) -> Unit
) : RecyclerView.Adapter<ItensAdapter.VH>() {

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val codigo: TextView = v.findViewById(R.id.txtCodigo)
        val detalhes: TextView = v.findViewById(R.id.txtDetalhes)
        val obs: TextView = v.findViewById(R.id.txtObs)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
        VH(LayoutInflater.from(parent.context).inflate(R.layout.item_patrimonio, parent, false))

    override fun getItemCount() = itens.size

    override fun onBindViewHolder(h: VH, pos: Int) {
        val item = itens[pos]
        h.codigo.text = "${itens.size - pos}.  ${item.codigo}"
        h.detalhes.text = listOf(item.dataHora, item.local, item.formato)
            .filter { it.isNotBlank() }.joinToString("  ·  ")
        h.obs.text = item.observacao
        h.obs.visibility = if (item.observacao.isBlank()) View.GONE else View.VISIBLE
        h.itemView.setOnClickListener { aoClicar(h.bindingAdapterPosition) }
    }
}
