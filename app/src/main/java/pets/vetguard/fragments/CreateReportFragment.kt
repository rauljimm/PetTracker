package pets.vetguard.fragments

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textfield.TextInputEditText
import android.widget.Button
import android.widget.Toast
import androidx.fragment.app.Fragment
import pets.vetguard.R
import pets.vetguard.adapters.PrescriptionAdapter
import pets.vetguard.model.Pet
import pets.vetguard.model.Prescription
import pets.vetguard.model.Report
import pets.vetguard.dto.FullReportDTO
import pets.vetguard.dto.FullPrescriptionDTO
import pets.vetguard.service.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CreateReportFragment : Fragment() {

    private lateinit var editTextDate: TextInputEditText
    private lateinit var editTextClinic: TextInputEditText
    private lateinit var editTextReason: TextInputEditText
    private lateinit var recyclerViewPrescriptions: RecyclerView
    private lateinit var buttonAddPrescription: Button
    private lateinit var buttonCreateReport: Button
    private var pet: Pet? = null
    private var petPosition: Int = -1
    private val prescriptionList = mutableListOf<Prescription>()
    private lateinit var prescriptionAdapter: PrescriptionAdapter
    private val TAG = "CreateReportFragment"

    companion object {
        private const val ARG_PET = "pet"
        private const val ARG_POSITION = "petPosition"

        fun newInstance(pet: Pet, position: Int): CreateReportFragment {
            val fragment = CreateReportFragment()
            val args = Bundle().apply {
                putParcelable(ARG_PET, pet)
                putInt(ARG_POSITION, position)
            }
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            pet = it.getParcelable(ARG_PET)
            petPosition = it.getInt(ARG_POSITION)
        }

        if (pet == null) {
            Log.e(TAG, "Pet es null en onCreate")
            Toast.makeText(context, "Pet es null en onCreate", Toast.LENGTH_LONG).show()
        } else {
            Log.d(TAG, "Pet ID: ${pet?.id}, Name: ${pet?.name}")
            Toast.makeText(context, "Pet ID: ${pet?.id}", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.create_report_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        editTextDate = view.findViewById(R.id.editTextDate)
        editTextClinic = view.findViewById(R.id.editTextClinic)
        editTextReason = view.findViewById(R.id.editTextReason)
        recyclerViewPrescriptions = view.findViewById(R.id.recyclerViewPrescriptions)
        buttonAddPrescription = view.findViewById(R.id.buttonAddPrescription)
        buttonCreateReport = view.findViewById(R.id.buttonCreateReport)

        // Establecer la fecha actual por defecto
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        editTextDate.setText(dateFormat.format(Date()))

        recyclerViewPrescriptions.layoutManager = LinearLayoutManager(context)
        prescriptionAdapter = PrescriptionAdapter(prescriptionList) { }
        recyclerViewPrescriptions.adapter = prescriptionAdapter

        buttonAddPrescription.setOnClickListener {
            if (prescriptionList.isEmpty()) { // Permitir añadir solo si no hay prescripciones
                prescriptionList.add(Prescription(null, null, "", "", "", ""))
                prescriptionAdapter.notifyItemInserted(prescriptionList.size - 1)
                buttonAddPrescription.isEnabled = false // Deshabilitar el botón después de añadir
                Log.d(TAG, "Prescripción añadida, botón deshabilitado")
            } else {
                Toast.makeText(context, "Solo se permite una prescripción", Toast.LENGTH_SHORT).show()
            }
        }

        buttonCreateReport.setOnClickListener {
            val reportDTO = createReportDTOFromInputs()
            if (reportDTO != null) {
                createReportInBackend(reportDTO)
            } else {
                Toast.makeText(context, "Por favor, completa los campos requeridos", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun createReportDTOFromInputs(): FullReportDTO? {
        val dateText = editTextDate.text?.toString()?.trim() ?: ""
        val clinic = editTextClinic.text?.toString()?.trim() ?: ""
        val reason = editTextReason.text?.toString()?.trim() ?: ""

        Log.d(TAG, "Creando DTO - Clinic: '$clinic', Reason: '$reason', Pet ID: ${pet?.id}")

        if (clinic.isEmpty() || reason.isEmpty()) {
            editTextClinic.error = if (clinic.isEmpty()) "Requerido" else null
            editTextReason.error = if (reason.isEmpty()) "Requerido" else null
            Log.w(TAG, "Campos requeridos vacíos - Clinic: $clinic, Reason: $reason")
            return null
        }

        val backendDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val reportDate = if (dateText.isEmpty()) {
            backendDateFormat.format(Date())
        } else {
            try {
                val parsedDate = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).parse(dateText)
                backendDateFormat.format(parsedDate)
            } catch (e: Exception) {
                Log.e(TAG, "Error al parsear fecha: $dateText", e)
                editTextDate.error = "Formato inválido (dd/MM/yyyy)"
                return null
            }
        }

        val petId = pet?.id
        if (petId == null || petId == 0L) {
            Log.e(TAG, "Error: Pet ID es nulo o 0")
            Toast.makeText(context, "Error: ID de mascota inválido", Toast.LENGTH_SHORT).show()
            return null
        }

        var isValid = true
        prescriptionList.forEachIndexed { index, prescription ->
            if (prescription.medName.trim().isEmpty()) {
                prescriptionAdapter.setError(index, "Requerido")
                isValid = false
                Log.w(TAG, "Prescripción $index tiene nombre vacío")
            } else {
                prescriptionAdapter.setError(index, null)
            }
        }

        if (!isValid) {
            Toast.makeText(context, "Todas las recetas necesitan un nombre", Toast.LENGTH_SHORT).show()
            return null
        }

        val reportDTO = FullReportDTO(
            id = null,
            petId = petId,
            reportDate = reportDate,
            clinic = clinic,
            reason = reason,
            prescriptionList = null
        )

        Log.d(TAG, "DTO creado: $reportDTO")
        return reportDTO
    }

    private fun createReportInBackend(reportDTO: FullReportDTO) {
        Log.d(TAG, "Enviando DTO al backend: $reportDTO")

        val token = getTokenFromStorage()
        if (token.isNullOrEmpty()) {
            Log.e(TAG, "Token no disponible")
            Toast.makeText(context, "Error: No se ha iniciado sesión", Toast.LENGTH_SHORT).show()
            parentFragmentManager.beginTransaction()
                .replace(R.id.frameLayout, LogInFragment())
                .commit()
            return
        }

        Log.d(TAG, "Token usado: ${token.take(10)}...")
        RetrofitClient.updateToken(token)

        RetrofitClient.apiServices.createReport(reportDTO).enqueue(object : Callback<Report> {
            override fun onResponse(call: Call<Report>, response: Response<Report>) {
                if (response.isSuccessful) {
                    val reportCreated = response.body()
                    Log.d(TAG, "Respuesta exitosa: $reportCreated")

                    if (reportCreated == null) {
                        Log.e(TAG, "Error: El cuerpo de la respuesta es nulo")
                        Toast.makeText(context, "Error: Respuesta vacía del servidor", Toast.LENGTH_SHORT).show()
                        return
                    }

                    if (reportCreated.id == null) {
                        Log.e(TAG, "Error: El reporte creado tiene ID nulo")
                        Toast.makeText(context, "Error: El reporte creado no tiene ID", Toast.LENGTH_SHORT).show()
                        return
                    }

                    prescriptionList.forEach { it.reportId = reportCreated.id }

                    if (prescriptionList.isEmpty()) {
                        finishReportCreation(reportCreated)
                    } else {
                        createPrescriptionsInBackend(reportCreated)
                    }
                } else {
                    val errorBody = response.errorBody()?.string() ?: "Sin cuerpo de error"
                    Log.e(TAG, "Error al crear reporte: ${response.code()}, Error: $errorBody")
                    Toast.makeText(context, "Error al crear el reporte: ${response.code()} - $errorBody", Toast.LENGTH_LONG).show()
                }
            }

            override fun onFailure(call: Call<Report>, t: Throwable) {
                Log.e(TAG, "Fallo de red: ${t.message}", t)
                Toast.makeText(context, "Fallo de red: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun getTokenFromStorage(): String? {
        val sharedPreferences = context?.getSharedPreferences("prefs", Context.MODE_PRIVATE)
        return sharedPreferences?.getString("jwt_token", null)
    }

    private fun createPrescriptionsInBackend(reportCreated: Report) {
        Log.d(TAG, "Creando ${prescriptionList.size} prescripciones para reporte ID: ${reportCreated.id}")

        val token = getTokenFromStorage()
        if (token.isNullOrEmpty()) {
            Log.e(TAG, "Token no disponible para crear prescripciones")
            Toast.makeText(context, "Error: No se ha iniciado sesión", Toast.LENGTH_SHORT).show()
            return
        }
        RetrofitClient.updateToken(token)

        var completed = 0
        val total = prescriptionList.size
        var success = true

        prescriptionList.forEach { prescription ->
            val prescriptionDTO = FullPrescriptionDTO(
                id = null,
                reportId = reportCreated.id,
                medName = prescription.medName,
                startDate = prescription.startDate,
                endDate = prescription.endDate,
                frequency = prescription.frequency.toString()
            )

            RetrofitClient.apiServices.createPrescription(prescriptionDTO).enqueue(object : Callback<Prescription> {
                override fun onResponse(call: Call<Prescription>, response: Response<Prescription>) {
                    completed++
                    if (response.isSuccessful) {
                        val createdPrescription = response.body()
                        Log.d(TAG, "Prescripción creada: $createdPrescription")
                        if (createdPrescription != null && createdPrescription.id != null) {
                            prescription.id = createdPrescription.id
                        } else {
                            success = false
                        }
                    } else {
                        success = false
                        Log.e(TAG, "Error al crear prescripción: ${response.code()}")
                    }

                    if (completed == total) {
                        if (success) {
                            finishReportCreation(reportCreated)
                        } else {
                            Toast.makeText(context, "Error al crear algunas prescripciones", Toast.LENGTH_SHORT).show()
                        }
                    }
                }

                override fun onFailure(call: Call<Prescription>, t: Throwable) {
                    completed++
                    success = false
                    if (completed == total) {
                        Toast.makeText(context, "Error de red al crear prescripciones", Toast.LENGTH_SHORT).show()
                    }
                }
            })
        }
    }

    private fun finishReportCreation(reportCreated: Report) {
        reportCreated.recipes = prescriptionList
        Log.d(TAG, "Reporte creado exitosamente con ${prescriptionList.size} prescripciones")
        Toast.makeText(context, "Reporte creado para ${pet?.name}", Toast.LENGTH_SHORT).show()

        val result = Bundle().apply {
            putParcelable("newReport", reportCreated)
            putInt("petPosition", petPosition)
        }
        parentFragmentManager.setFragmentResult("createReportRequestKey", result)
        parentFragmentManager.popBackStack()
    }
}