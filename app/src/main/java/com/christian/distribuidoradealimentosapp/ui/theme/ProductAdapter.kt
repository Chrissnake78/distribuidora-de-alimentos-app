package com.christian.distribuidoradealimentosapp.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.christian.distribuidoradealimentosapp.R
import com.christian.distribuidoradealimentosapp.data.CartItem
import com.christian.distribuidoradealimentosapp.data.FirebaseRepo
import com.christian.distribuidoradealimentosapp.data.Product

class ProductAdapter(
    private val data: List<Product>
): RecyclerView.Adapter<ProductAdapter.VH>() {

    class VH(v: View): RecyclerView.ViewHolder(v) {
        val nombre: TextView = v.findViewById(R.id.txtNombre)
        val precio: TextView = v.findViewById(R.id.txtPrecio)
        val btnAgregar: Button = v.findViewById(R.id.btnAgregar)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_product, parent, false)
        return VH(v)
    }

    override fun getItemCount(): Int = data.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val p = data[position]
        holder.nombre.text = p.nombre
        holder.precio.text = "$${p.precio}"

        holder.btnAgregar.setOnClickListener {
            val ref = FirebaseRepo.carritoRef().push()
            val item = CartItem(
                id = ref.key ?: "",
                nombre = p.nombre,
                cantidad = 1,
                precio = p.precio,
                subtotal = p.precio
            )
            ref.setValue(item)
        }
    }
}