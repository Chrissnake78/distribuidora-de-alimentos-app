package com.christian.distribuidoradealimentosapp.ui

import android.os.Bundle
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
        adapter = ProductAdapter(productos)
        rv.adapter = adapter

        cargarCatalogo()
    }

    private fun cargarCatalogo() {
        FirebaseRepo.catalogoRef().addListenerForSingleValueEvent(object: ValueEventListener{
            override fun onDataChange(s: DataSnapshot) {
                productos.clear()

                // Si NO hay datos en "catalogo", sembramos 14 productos (incluye carnes)
                if (!s.hasChildren()) {
                    val seed = listOf(
                        // ---- CARNES (requiere frío) ----
                        Product(nombre = "Pechuga de pollo 1Kg",   precio = 5490, requiereFrio = true),
                        Product(nombre = "Trutro entero 1Kg",      precio = 3990, requiereFrio = true),
                        Product(nombre = "Carne molida 500g",      precio = 4490, requiereFrio = true),
                        Product(nombre = "Lomo vetado 1Kg",        precio = 11990, requiereFrio = true),
                        Product(nombre = "Posta negra 1Kg",        precio = 9290, requiereFrio = true),
                        Product(nombre = "Costillar de cerdo 1Kg", precio = 7590, requiereFrio = true),

                        // ---- LÁCTEOS / REFRIGERADOS ----
                        Product(nombre = "Leche entera 1L",         precio = 1200, requiereFrio = true),
                        Product(nombre = "Mantequilla 250g",        precio = 2190, requiereFrio = true),
                        Product(nombre = "Yoghurt batido 1Kg",      precio = 2490, requiereFrio = true),

                        // ---- ABARROTES ----
                        Product(nombre = "Arroz 1Kg",               precio = 1450, requiereFrio = false),
                        Product(nombre = "Fideos spaghetti 400g",   precio = 890,  requiereFrio = false),
                        Product(nombre = "Aceite vegetal 1L",       precio = 2590, requiereFrio = false),
                        Product(nombre = "Pan molde 500g",          precio = 1650, requiereFrio = false),
                        Product(nombre = "Azúcar 1Kg",              precio = 1350, requiereFrio = false)
                    )

                    seed.forEach { p ->
                        val r = FirebaseRepo.catalogoRef().push()
                        p.id = r.key
                        r.setValue(p)
                    }

                    // Cuando termine de sembrar, vuelve a leer para mostrar
                    cargarCatalogo()
                    return
                }

                // Si ya hay datos, los cargamos a la lista
                s.children.forEach { c ->
                    c.getValue(Product::class.java)?.let { productos.add(it) }
                }
                adapter.notifyDataSetChanged()
            }
            override fun onCancelled(error: DatabaseError) { /* no-op */ }
        })
    }
}