package com.christian.distribuidoradealimentosapp.ui

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.christian.distribuidoradealimentosapp.R
import com.christian.distribuidoradealimentosapp.data.FirebaseRepo
import com.christian.distribuidoradealimentosapp.data.Product
import com.google.firebase.database.*

class CatalogActivity : AppCompatActivity() {

    private lateinit var rv: RecyclerView
    private val productos = mutableListOf<Product>()
    private lateinit var adapter: ProductAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_catalog)

        rv = findViewById(R.id.rvCatalogo)
        rv.layoutManager = LinearLayoutManager(this)

        adapter = ProductAdapter(productos) { producto: Product ->
            FirebaseRepo.agregarAlCarrito(producto)
            Toast.makeText(this, "${producto.nombre} agregado al carrito", Toast.LENGTH_SHORT).show()
        }

        rv.adapter = adapter
        cargarCatalogo()
    }

    private fun cargarCatalogo() {
        FirebaseRepo.catalogoRef().addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                productos.clear()

                // Si el catálogo está vacío, crear productos por defecto
                if (!snapshot.hasChildren()) {
                    val seed = listOf(
                        Product(nombre = "Pechuga de pollo 1Kg", precio = 5490.0, requiereFrio = true),
                        Product(nombre = "Trutro entero 1Kg", precio = 3990.0, requiereFrio = true),
                        Product(nombre = "Carne molida 500g", precio = 4490.0, requiereFrio = true),
                        Product(nombre = "Leche entera 1L", precio = 1200.0, requiereFrio = true),
                        Product(nombre = "Arroz 1Kg", precio = 1450.0, requiereFrio = false),
                        Product(nombre = "Fideos 400g", precio = 890.0, requiereFrio = false),
                        Product(nombre = "Aceite vegetal 1L", precio = 2590.0, requiereFrio = false)
                    )

                    seed.forEach { p ->
                        val ref = FirebaseRepo.catalogoRef().push()
                        p.id = ref.key
                        ref.setValue(p)
                    }

                    // Recargar el catálogo una vez agregados
                    cargarCatalogo()
                    return
                }

                // Si ya hay productos en Firebase, los cargamos
                snapshot.children.forEach {
                    it.getValue(Product::class.java)?.let { producto -> productos.add(producto) }
                }
                adapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@CatalogActivity, "Error al cargar catálogo", Toast.LENGTH_SHORT).show()
            }
        })
    }
}