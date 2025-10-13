package com.christian.distribuidoradealimentosapp.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.christian.distribuidoradealimentosapp.R
import com.christian.distribuidoradealimentosapp.data.Product

class ProductAdapter(
    private val productos: List<Product>,
    private val onAddToCart: (Product) -> Unit
) : RecyclerView.Adapter<ProductAdapter.ProductViewHolder>() {

    inner class ProductViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val nombre: TextView = view.findViewById(R.id.txtNombre)
        val precio: TextView = view.findViewById(R.id.txtPrecio)
        val cantidad: TextView = view.findViewById(R.id.txtCantidad)
        val btnMenos: Button = view.findViewById(R.id.btnMenos)
        val btnMas: Button = view.findViewById(R.id.btnMas)
        val btnAgregar: Button = view.findViewById(R.id.btnAgregar)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_product, parent, false) // 🔹 usa item_product.xml
        return ProductViewHolder(view)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        val producto = productos[position]

        holder.nombre.text = producto.nombre
        holder.precio.text = "$${"%.0f".format(producto.precio)}"

        // Si tu clase Product aún no tiene cantidad, la agregamos por extensión
        if (producto.cantidad == 0) producto.cantidad = 1
        holder.cantidad.text = producto.cantidad.toString()

        holder.btnMas.setOnClickListener {
            producto.cantidad++
            holder.cantidad.text = producto.cantidad.toString()
        }

        holder.btnMenos.setOnClickListener {
            if (producto.cantidad > 1) {
                producto.cantidad--
                holder.cantidad.text = producto.cantidad.toString()
            }
        }

        holder.btnAgregar.setOnClickListener {
            onAddToCart(producto)
        }
    }

    override fun getItemCount(): Int = productos.size
}