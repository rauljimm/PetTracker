package pets.vetguard.adapters

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import pets.vetguard.R
import pets.vetguard.model.Report
import java.text.SimpleDateFormat
import java.util.Locale

class ReportAdapter(
    private val reportList: MutableList<Report>,
    private val onDoubleClick: (Report, Int) -> Unit
) : RecyclerView.Adapter<ReportAdapter.ReportViewHolder>() {

    class ReportViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val dateTextView: TextView = itemView.findViewById(R.id.reportDateTextView)
        val clinicTextView: TextView = itemView.findViewById(R.id.reportClinicTextView)
        val reasonTextView: TextView = itemView.findViewById(R.id.reportReasonTextView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReportViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_report, parent, false)
        return ReportViewHolder(view)
    }

    override fun onBindViewHolder(holder: ReportViewHolder, @SuppressLint("RecyclerView") position: Int) {
        val report = reportList[position]
        val inputDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) // Formato del servidor
        val outputDateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) // Formato para la UI

        // Manejo de reportDate como String
        holder.dateTextView.text = report.date?.let { dateString ->
            try {
                val date = inputDateFormat.parse(dateString)
                date?.let { outputDateFormat.format(it) } ?: dateString // Si no se puede parsear, mostrar el string original
            } catch (e: Exception) {
                dateString // Si hay error, mostrar el string sin formatear
            }
        } ?: "Sin fecha"

        holder.clinicTextView.text = report.clinic ?: "Sin clínica"
        holder.reasonTextView.text = report.reason ?: "Sin motivo"

        // Doble clic con tiempo por ítem
        holder.itemView.setOnClickListener(object : View.OnClickListener {
            private val DOUBLE_CLICK_TIME_DELTA: Long = 300
            private var lastClickTime: Long = 0

            override fun onClick(v: View?) {
                val clickTime = System.currentTimeMillis()
                if (clickTime - lastClickTime < DOUBLE_CLICK_TIME_DELTA) {
                    onDoubleClick(report, position)
                }
                lastClickTime = clickTime
            }
        })
    }

    override fun getItemCount(): Int = reportList.size

    // Método para actualización dinámica
    fun updateReports(newReports: List<Report>) {
        reportList.clear()
        reportList.addAll(newReports)
        notifyDataSetChanged() // Podrías usar DiffUtil para mayor eficiencia
    }
}