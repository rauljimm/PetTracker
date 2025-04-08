package pets.vetguard.adapters

import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textfield.TextInputEditText
import android.widget.Button
import pets.vetguard.R
import pets.vetguard.model.Prescription
import java.text.SimpleDateFormat
import java.util.Locale

class PrescriptionAdapter(
    private val prescriptionList: MutableList<Prescription>,
    private val onRemove: (Int) -> Unit
) : RecyclerView.Adapter<PrescriptionAdapter.PrescriptionViewHolder>() {

    private val errorMap = mutableMapOf<Int, String?>()

    class PrescriptionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val editTextMedName: TextInputEditText = itemView.findViewById(R.id.editTextMedName)
        val editTextStartDate: TextInputEditText = itemView.findViewById(R.id.editTextStartDate)
        val editTextEndDate: TextInputEditText = itemView.findViewById(R.id.editTextEndDate)
        val editTextFrequency: TextInputEditText = itemView.findViewById(R.id.editTextFrequency)
        val buttonRemove: Button = itemView.findViewById(R.id.buttonRemovePrescription)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PrescriptionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_prescription_form, parent, false)
        return PrescriptionViewHolder(view)
    }

    override fun onBindViewHolder(holder: PrescriptionViewHolder, position: Int) {
        val prescription = prescriptionList[position]
        val inputDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) // Formato del servidor
        val outputDateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) // Formato para la UI

        // Manejo de nulos y conversión de fechas
        holder.editTextMedName.setText(prescription.medName ?: "")
        holder.editTextStartDate.setText(prescription.startDate?.let { dateString ->
            try {
                val date = inputDateFormat.parse(dateString)
                date?.let { outputDateFormat.format(it) } ?: dateString
            } catch (e: Exception) {
                dateString // Mostrar el string original si no se puede parsear
            }
        } ?: "")
        holder.editTextEndDate.setText(prescription.endDate?.let { dateString ->
            try {
                val date = inputDateFormat.parse(dateString)
                date?.let { outputDateFormat.format(it) } ?: dateString
            } catch (e: Exception) {
                dateString // Mostrar el string original si no se puede parsear
            }
        } ?: "")
        holder.editTextFrequency.setText(prescription.frequency ?: "")

        holder.editTextMedName.error = errorMap[position]

        holder.buttonRemove.setOnClickListener {
            val pos = holder.adapterPosition
            if (pos != RecyclerView.NO_POSITION && pos < prescriptionList.size) {
                prescriptionList.removeAt(pos)
                errorMap.remove(pos)
                notifyItemRemoved(pos)
                notifyItemRangeChanged(pos, prescriptionList.size)
                onRemove(pos)
            }
        }

        holder.editTextMedName.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val pos = holder.adapterPosition
                if (pos != RecyclerView.NO_POSITION && pos < prescriptionList.size) {
                    prescriptionList[pos] = prescriptionList[pos].copy(medName = s.toString().trim())
                    if (s.toString().trim().isNotEmpty()) {
                        holder.editTextMedName.error = null
                        errorMap[pos] = null
                    }
                }
            }
        })

        holder.editTextStartDate.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val pos = holder.adapterPosition
                if (pos != RecyclerView.NO_POSITION && pos < prescriptionList.size) {
                    val dateString = s.toString().trim()
                    val formattedDate = if (dateString.isNotEmpty()) {
                        try {
                            val date = outputDateFormat.parse(dateString) // Parsear desde "dd/MM/yyyy"
                            inputDateFormat.format(date) // Convertir a "yyyy-MM-dd" para el modelo
                        } catch (e: Exception) {
                            dateString // Si falla, mantener el input como está (el usuario deberá corregirlo)
                        }
                    } else {
                        "" // Vacío si no hay input
                    }
                    prescriptionList[pos] = prescriptionList[pos].copy(startDate = formattedDate)
                }
            }
        })

        holder.editTextEndDate.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val pos = holder.adapterPosition
                if (pos != RecyclerView.NO_POSITION && pos < prescriptionList.size) {
                    val dateString = s.toString().trim()
                    val formattedDate = if (dateString.isNotEmpty()) {
                        try {
                            val date = outputDateFormat.parse(dateString) // Parsear desde "dd/MM/yyyy"
                            inputDateFormat.format(date) // Convertir a "yyyy-MM-dd" para el modelo
                        } catch (e: Exception) {
                            dateString // Si falla, mantener el input como está (el usuario deberá corregirlo)
                        }
                    } else {
                        "" // Vacío si no hay input
                    }
                    prescriptionList[pos] = prescriptionList[pos].copy(endDate = formattedDate)
                }
            }
        })

        holder.editTextFrequency.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val pos = holder.adapterPosition
                if (pos != RecyclerView.NO_POSITION && pos < prescriptionList.size) {
                    val frequency = s.toString().trim()
                    prescriptionList[pos] = prescriptionList[pos].copy(frequency = frequency)
                }
            }
        })
    }

    override fun getItemCount(): Int = prescriptionList.size

    fun setError(position: Int, error: String?) {
        if (position in 0 until prescriptionList.size) {
            errorMap[position] = error
            notifyItemChanged(position)
        }
    }
}