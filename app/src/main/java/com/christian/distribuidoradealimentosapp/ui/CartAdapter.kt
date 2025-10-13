package com.christian.distribuidoradealimentosapp.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.christian.distribuidoradealimentosapp.R
import com.christian.distribuidoradealimentosapp.data.CartItem

class CartAdapter(private val items: List<CartItem>) :
    RecyclerView.Adapter<CartAdapter.CartViewHolder>() {

    inner class CartViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val txtNombre: TextView = view.findViewById(R.id.txtNombreProd)
        val txtCantidad: TextView = view.findViewById(R.id.txtCantidadProd)
        val txtSubtotal: TextView = view.findViewById(R.id.txtSubtotalProd)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CartViewHolder {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_carrito, parent, false)
        return CartViewHolder(v)
    }

    override fun onBindViewHolder(holder: CartViewHolder, position: Int) {
        val item = items[position]
        holder.txtNombre.text = item.nombre
        holder.txtCantidad.text = "x${item.cantidad}"
        holder.txtSubtotal.text = "$${"%.0f".format(item.subtotal())}"
    }

    override fun getItemCount() = items.size
}