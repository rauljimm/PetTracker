package pets.vetguard.fragments

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import pets.vetguard.R
import pets.vetguard.adapters.ReportAdapter
import pets.vetguard.model.Pet
import pets.vetguard.model.Report
import pets.vetguard.service.RetrofitClient
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ReportsFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var reportAdapter: ReportAdapter
    private lateinit var addReportFab: FloatingActionButton
    private var petId: Long? = null
    private var pet: Pet? = null
    private var petPosition: Int = -1
    private var jwtToken: String? = null
    private val reports = mutableListOf<Report>()

    companion object {
        private const val ARG_PET_ID = "petId"
        private const val ARG_POSITION = "petPosition"
        private const val ARG_TOKEN = "jwtToken"

        fun newInstance(petId: Long, position: Int, token: String?): ReportsFragment {
            val fragment = ReportsFragment()
            val args = Bundle().apply {
                putLong(ARG_PET_ID, petId)
                putInt(ARG_POSITION, position)
                putString(ARG_TOKEN, token)
            }
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            petId = it.getLong(ARG_PET_ID)
            petPosition = it.getInt(ARG_POSITION)
            jwtToken = it.getString(ARG_TOKEN)
        }
        Log.d("ReportsFragment", "Pet ID recibido: $petId, Posición: $petPosition, Token: ${jwtToken?.take(10)}...")
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_reports, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView = view.findViewById(R.id.recyclerViewReports)
        addReportFab = view.findViewById(R.id.addReportFab)

        reportAdapter = ReportAdapter(reports) { report, position ->
            editReport(report, position)
        }
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = reportAdapter

        loadReportsFromBackend()

        addReportFab.setOnClickListener {
            createReportForPet()
        }

        parentFragmentManager.setFragmentResultListener("createReportRequestKey", viewLifecycleOwner) { _, result ->
            val newReport = result.getParcelable<Report>("newReport")
            val updatedPetPosition = result.getInt("petPosition")
            if (newReport != null && updatedPetPosition == petPosition) {
                Log.d("ReportsFragment", "Nuevo reporte recibido: ${newReport.id}")
                reports.add(newReport)
                reportAdapter.notifyItemInserted(reports.size - 1)
                recyclerView.scrollToPosition(reports.size - 1)
            }
        }

        parentFragmentManager.setFragmentResultListener("editReportRequestKey", viewLifecycleOwner) { _, result ->
            val updatedReport = result.getParcelable<Report>("updatedReport")
            val reportPosition = result.getInt("reportPosition")
            if (updatedReport != null && reportPosition >= 0 && reportPosition < reports.size) {
                Log.d("ReportsFragment", "Reporte actualizado: ${updatedReport.id}")
                reports[reportPosition] = updatedReport
                reportAdapter.notifyItemChanged(reportPosition)
            }
        }
    }

    private fun loadReportsFromBackend() {
        val petId = petId ?: return
        Log.d("ReportsFragment", "Cargando reportes para petId: $petId")

        if (jwtToken.isNullOrEmpty()) {
            Log.e("ReportsFragment", "Token no disponible, redirigiendo a login")
            Toast.makeText(context, "Token no disponible, redirigiendo a login", Toast.LENGTH_SHORT).show()
            parentFragmentManager.beginTransaction()
                .replace(R.id.frameLayout, LogInFragment())
                .commit()
            return
        }
        RetrofitClient.updateToken(jwtToken!!)

        CoroutineScope(Dispatchers.Main).launch {
            try {
                val response = RetrofitClient.apiServices.getPetById(petId)
                if (response.isSuccessful) {
                    val petFromApi = response.body()
                    pet = petFromApi // Guardamos el objeto completo para usarlo en createReportForPet
                    val petReports = petFromApi?.reportList ?: emptyList()
                    Log.d("ReportsFragment", "Reportes obtenidos para petId $petId: ${petReports.size}")
                    reports.clear()
                    reports.addAll(petReports)
                    reportAdapter.notifyDataSetChanged()

                    if (reports.isEmpty()) {
                        Log.w("ReportsFragment", "No se encontraron reportes para petId $petId")
                        Toast.makeText(context, "No hay reportes para esta mascota aún", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Log.e("ReportsFragment", "Error al cargar la mascota: ${response.code()} - ${response.errorBody()?.string()}")
                    Toast.makeText(context, "Error al cargar reportes: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("ReportsFragment", "Fallo de red al cargar reportes: ${e.message}", e)
                Toast.makeText(context, "Fallo de red: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun createReportForPet() {
        pet?.let {
            Log.d("ReportsFragment", "Navegando a CreateReportFragment para ${it.name}")
            parentFragmentManager.beginTransaction()
                .replace(R.id.frameLayout, CreateReportFragment.newInstance(it, petPosition))
                .addToBackStack(null)
                .commit()
        } ?: run {
            Log.e("ReportsFragment", "Pet es null, no se puede crear reporte")
            Toast.makeText(context, "Error: No se puede crear reporte sin datos de la mascota", Toast.LENGTH_SHORT).show()
        }
    }

    private fun editReport(report: Report, position: Int) {
        pet?.let {
            Log.d("ReportsFragment", "Navegando a EditReportFragment para reporte ${report.id}")
            parentFragmentManager.beginTransaction()
                .replace(R.id.frameLayout, EditReportFragment.newInstance(it, petPosition, report, position))
                .addToBackStack(null)
                .commit()
        }
    }
}