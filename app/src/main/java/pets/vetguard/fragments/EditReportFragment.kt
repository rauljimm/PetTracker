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
import com.google.android.material.textfield.TextInputEditText
import android.widget.Button
import android.widget.Toast
import androidx.core.view.isVisible
import pets.vetguard.R
import pets.vetguard.adapters.PrescriptionAdapter
import pets.vetguard.dto.ChangePrescriptionDTO
import pets.vetguard.dto.ChangeReportDTO
import pets.vetguard.dto.FullPrescriptionDTO
import pets.vetguard.model.Pet
import pets.vetguard.model.Prescription
import pets.vetguard.model.Report
import pets.vetguard.service.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class EditReportFragment : Fragment() {

    private lateinit var editTextDate: TextInputEditText
    private lateinit var editTextClinic: TextInputEditText
    private lateinit var editTextReason: TextInputEditText
    private lateinit var recyclerViewPrescriptions: RecyclerView
    private lateinit var buttonAddPrescription: Button
    private lateinit var buttonUpdateReport: Button
    private var pet: Pet? = null
    private var petPosition: Int = -1
    private var report: Report? = null
    private var reportPosition: Int = -1
    private val prescriptionList = mutableListOf<Prescription>()
    private lateinit var prescriptionAdapter: PrescriptionAdapter
    private val TAG = "EditReportFragment"
    private var completed = 0
    private var success = true

    companion object {
        private const val ARG_PET = "pet"
        private const val ARG_PET_POSITION = "petPosition"
        private const val ARG_REPORT = "report"
        private const val ARG_REPORT_POSITION = "reportPosition"

        fun newInstance(pet: Pet, petPosition: Int, report: Report, reportPosition: Int): EditReportFragment {
            val fragment = EditReportFragment()
            val args = Bundle().apply {
                putParcelable(ARG_PET, pet)
                putInt(ARG_PET_POSITION, petPosition)
                putParcelable(ARG_REPORT, report)
                putInt(ARG_REPORT_POSITION, reportPosition)
            }
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            pet = it.getParcelable(ARG_PET)
            petPosition = it.getInt(ARG_PET_POSITION)
            report = it.getParcelable(ARG_REPORT)
            reportPosition = it.getInt(ARG_REPORT_POSITION)
        }
        Log.d(TAG, "Reporte recibido: ${report?.id}, Pet: ${pet?.name}, Posición: $petPosition")
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
        buttonUpdateReport = view.findViewById(R.id.buttonCreateReport)

        buttonUpdateReport.text = "Actualizar Reporte"

        val isoDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        editTextDate.setText(report?.date ?: isoDateFormat.format(Date()))
        editTextClinic.setText(report?.clinic ?: "")
        editTextReason.setText(report?.reason ?: "")
        prescriptionList.addAll(report?.recipes ?: emptyList())

        recyclerViewPrescriptions.layoutManager = LinearLayoutManager(context)
        prescriptionAdapter = PrescriptionAdapter(prescriptionList) { }
        recyclerViewPrescriptions.adapter = prescriptionAdapter

        // Deshabilitar el botón si ya hay una prescripción al cargar
        if (prescriptionList.isNotEmpty()) {
            buttonAddPrescription.isEnabled = false
            Log.d(TAG, "Botón deshabilitado al cargar: ya hay ${prescriptionList.size} prescripciones")
        }

        buttonAddPrescription.setOnClickListener {
            if (prescriptionList.isEmpty()) { // Permitir añadir solo si no hay prescripciones
                prescriptionList.add(Prescription(null, null, "", isoDateFormat.format(Date()), isoDateFormat.format(Date()), ""))
                prescriptionAdapter.notifyItemInserted(prescriptionList.size - 1)
                buttonAddPrescription.isVisible = false // Deshabilitar el botón después de añadir
                Log.d(TAG, "Prescripción añadida, botón deshabilitado")
            } else {
                Toast.makeText(context, "Solo se permite una prescripción", Toast.LENGTH_SHORT).show()
            }
        }

        buttonUpdateReport.setOnClickListener {
            val reportDTO = createReportDTOFromInputs()
            if (reportDTO != null) {
                updateReportInBackend(reportDTO)
            } else {
                Toast.makeText(context, "Por favor, completa los campos requeridos", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun createReportDTOFromInputs(): ChangeReportDTO? {
        val dateText = editTextDate.text?.toString()?.trim() ?: ""
        val clinic = editTextClinic.text?.toString()?.trim() ?: ""
        val reason = editTextReason.text?.toString()?.trim() ?: ""

        Log.d(TAG, "Creando DTO - Clinic: '$clinic', Reason: '$reason', Pet ID: ${pet?.id}")

        if (clinic.isEmpty() || reason.isEmpty()) {
            editTextClinic.error = if (clinic.isEmpty()) "Requerido" else null
            editTextReason.error = if (reason.isEmpty()) "Requerido" else null
            return null
        }

        val isoDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val inputDateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val reportDate: String = if (dateText.isEmpty()) {
            isoDateFormat.format(Date())
        } else {
            try {
                if (dateText.matches(Regex("\\d{4}-\\d{2}-\\d{2}"))) {
                    dateText
                } else {
                    isoDateFormat.format(inputDateFormat.parse(dateText) ?: Date())
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error al parsear fecha: $dateText", e)
                editTextDate.error = "Formato inválido"
                return null
            }
        }

        val reportDTO = ChangeReportDTO(
            id = report?.id ?: return null,
            reportDate = reportDate,
            clinic = clinic,
            reason = reason,
            prescriptionList = prescriptionList.map { prescription ->
                FullPrescriptionDTO(
                    id = prescription.id,
                    reportId = report?.id,
                    medName = prescription.medName,
                    startDate = prescription.startDate,
                    endDate = prescription.endDate,
                    frequency = prescription.frequency.toString()
                )
            }
        )

        Log.d(TAG, "DTO creado: $reportDTO")
        return reportDTO
    }

    private fun updateReportInBackend(reportDTO: ChangeReportDTO) {
        Log.d(TAG, "Actualizando reporte en backend: $reportDTO")

        val token = getTokenFromStorage()
        if (token.isNullOrEmpty()) {
            Log.e(TAG, "Token no disponible")
            Toast.makeText(context, "Error: No se ha iniciado sesión", Toast.LENGTH_SHORT).show()
            parentFragmentManager.beginTransaction()
                .replace(R.id.frameLayout, LogInFragment())
                .commit()
            return
        }

        RetrofitClient.updateToken(token)
        RetrofitClient.apiServices.updateReport(reportDTO.id, reportDTO).enqueue(object : Callback<Report> {
            override fun onResponse(call: Call<Report>, response: Response<Report>) {
                if (response.isSuccessful) {
                    val updatedReport = response.body()
                    Log.d(TAG, "Reporte actualizado: $updatedReport")
                    if (updatedReport != null) {
                        updatePrescriptionsInBackend(updatedReport)
                    } else {
                        Toast.makeText(context, "Error: Respuesta vacía del servidor", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    val errorBody = response.errorBody()?.string() ?: "Sin cuerpo de error"
                    Log.e(TAG, "Error al actualizar reporte: ${response.code()}, Error: $errorBody")
                    Toast.makeText(context, "Error al actualizar el reporte: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<Report>, t: Throwable) {
                Log.e(TAG, "Fallo de red: ${t.message}", t)
                Toast.makeText(context, "Fallo de red: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun updatePrescriptionsInBackend(updatedReport: Report) {
        Log.d(TAG, "Actualizando ${prescriptionList.size} prescripciones para reporte ID: ${updatedReport.id}")

        val token = getTokenFromStorage()
        if (token.isNullOrEmpty()) {
            Log.e(TAG, "Token no disponible para actualizar prescripciones")
            Toast.makeText(context, "Error: No se ha iniciado sesión", Toast.LENGTH_SHORT).show()
            return
        }
        RetrofitClient.updateToken(token)

        completed = 0
        success = true

        if (prescriptionList.isEmpty()) {
            finishReportUpdate(updatedReport)
            return
        }

        prescriptionList.forEachIndexed { index, prescription ->
            if (prescription.id == null) {
                val prescriptionDTO = FullPrescriptionDTO(
                    id = null,
                    reportId = updatedReport.id,
                    medName = prescription.medName,
                    startDate = prescription.startDate,
                    endDate = prescription.endDate,
                    frequency = prescription.frequency.toString()
                )
                RetrofitClient.apiServices.createPrescription(prescriptionDTO).enqueue(object : Callback<Prescription> {
                    override fun onResponse(call: Call<Prescription>, response: Response<Prescription>) {
                        handlePrescriptionResponse(response, index, updatedReport)
                    }
                    override fun onFailure(call: Call<Prescription>, t: Throwable) {
                        handlePrescriptionFailure(t, updatedReport)
                    }
                })
            } else {
                val prescriptionDTO = ChangePrescriptionDTO(
                    medName = prescription.medName,
                    startDate = prescription.startDate, // Ya en "yyyy-MM-dd"
                    endDate = prescription.endDate,     // Ya en "yyyy-MM-dd"
                    frequency = prescription.frequency.toString()
                )
                RetrofitClient.apiServices.updatePrescription(prescription.id!!, prescriptionDTO).enqueue(object : Callback<Prescription> {
                    override fun onResponse(call: Call<Prescription>, response: Response<Prescription>) {
                        handlePrescriptionResponse(response, index, updatedReport)
                    }
                    override fun onFailure(call: Call<Prescription>, t: Throwable) {
                        handlePrescriptionFailure(t, updatedReport)
                    }
                })
            }
        }
    }

    private fun handlePrescriptionResponse(response: Response<Prescription>, index: Int, updatedReport: Report) {
        completed++
        if (response.isSuccessful) {
            val createdOrUpdatedPrescription = response.body()
            Log.d(TAG, "Prescripción procesada: $createdOrUpdatedPrescription")
            if (createdOrUpdatedPrescription != null && createdOrUpdatedPrescription.id != null) {
                prescriptionList[index] = createdOrUpdatedPrescription
            } else {
                success = false
            }
        } else {
            success = false
            Log.e(TAG, "Error al procesar prescripción: ${response.code()}")
        }
        if (completed == prescriptionList.size) {
            if (success) {
                finishReportUpdate(updatedReport)
            } else {
                Toast.makeText(context, "Error al procesar algunas prescripciones", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun handlePrescriptionFailure(t: Throwable, updatedReport: Report) {
        completed++
        success = false
        Log.e(TAG, "Fallo de red al procesar prescripción: ${t.message}", t)
        if (completed == prescriptionList.size) {
            Toast.makeText(context, "Error de red al procesar prescripciones", Toast.LENGTH_SHORT).show()
        }
    }

    private fun finishReportUpdate(updatedReport: Report) {
        updatedReport.recipes = prescriptionList
        Log.d(TAG, "Reporte actualizado exitosamente con ${prescriptionList.size} prescripciones")
        Toast.makeText(context, "Reporte actualizado para ${pet?.name}", Toast.LENGTH_SHORT).show()

        val result = Bundle().apply {
            putParcelable("updatedReport", updatedReport)
            putInt("reportPosition", reportPosition)
        }
        parentFragmentManager.setFragmentResult("editReportRequestKey", result)
        parentFragmentManager.popBackStack()
    }

    private fun getTokenFromStorage(): String? {
        val sharedPreferences = context?.getSharedPreferences("prefs", Context.MODE_PRIVATE)
        return sharedPreferences?.getString("jwt_token", null)
    }
}