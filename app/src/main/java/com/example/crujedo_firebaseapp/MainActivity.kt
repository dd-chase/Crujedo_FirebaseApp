package com.example.crujedo_firebaseapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class MainActivity : AppCompatActivity() {

    private lateinit var editText1: EditText
    private lateinit var editText2: EditText
    private lateinit var btnSubmit: MaterialButton
    private lateinit var btnView: MaterialButton
    private lateinit var listViewEntries: ListView

    private lateinit var databaseReference: DatabaseReference
    private val entryList = mutableListOf<EntryItem>()
    private lateinit var customAdapter: EntryAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        window.statusBarColor = android.graphics.Color.parseColor("#0F1115")

        // Initialize UI views
        editText1 = findViewById(R.id.editText1)
        editText2 = findViewById(R.id.editText2)
        btnSubmit = findViewById(R.id.btnSubmit)
        btnView = findViewById(R.id.btnView)
        listViewEntries = findViewById(R.id.listViewEntries)

        // Initialize Firebase Realtime Database with your exact regional URL
        databaseReference = FirebaseDatabase.getInstance(
            "https://crujedo-firebaseapp-default-rtdb.asia-southeast1.firebasedatabase.app/"
        ).getReference("entries")

        // Attach custom card adapter to ListView
        customAdapter = EntryAdapter(entryList)
        listViewEntries.adapter = customAdapter

        // Button Click Listeners
        btnSubmit.setOnClickListener { saveEntryToFirebase() }
        btnView.setOnClickListener { loadEntriesFromFirebase() }
    }

    private fun saveEntryToFirebase() {
        val text1 = editText1.text.toString().trim()
        val text2 = editText2.text.toString().trim()

        if (text1.isEmpty() || text2.isEmpty()) {
            showToast("Both fields are required!")
            return
        }

        val recordId = databaseReference.push().key ?: return
        val newEntry = EntryItem(id = recordId, text1 = text1, text2 = text2)

        btnSubmit.isEnabled = false
        databaseReference.child(recordId).setValue(newEntry)
            .addOnSuccessListener {
                btnSubmit.isEnabled = true
                editText1.text.clear()
                editText2.text.clear()
                editText1.requestFocus()
                showToast("Saved to Firebase!")
            }
            .addOnFailureListener { error ->
                btnSubmit.isEnabled = true
                showToast("Save failed: ${error.message}")
            }
    }

    private fun loadEntriesFromFirebase() {
        databaseReference.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                entryList.clear()

                if (!snapshot.exists()) {
                    customAdapter.notifyDataSetChanged()
                    showToast("No records found.")
                    return
                }

                for (childSnapshot in snapshot.children) {
                    val item = childSnapshot.getValue(EntryItem::class.java)
                    if (item != null) {
                        entryList.add(item)
                    }
                }

                customAdapter.notifyDataSetChanged()
                showToast("Loaded ${entryList.size} record(s)")
            }

            override fun onCancelled(error: DatabaseError) {
                showToast("Load failed: ${error.message}")
            }
        })
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    inner class EntryAdapter(private val items: List<EntryItem>) :
        ArrayAdapter<EntryItem>(this@MainActivity, R.layout.item_entry, items) {

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view = convertView ?: LayoutInflater.from(context)
                .inflate(R.layout.item_entry, parent, false)

            val item = items[position]
            val tvPrimary = view.findViewById<TextView>(R.id.tvPrimaryText)
            val tvSecondary = view.findViewById<TextView>(R.id.tvSecondaryText)

            tvPrimary.text = item.text1
            tvSecondary.text = item.text2

            return view
        }
    }
}