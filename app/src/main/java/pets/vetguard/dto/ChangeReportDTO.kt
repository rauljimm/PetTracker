package pets.vetguard.dto

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class ChangeReportDTO(
    @SerializedName("id") val id: Long,
    @SerializedName("reportDate") val reportDate: String, // String en formato "yyyy-MM-dd"
    @SerializedName("clinic") val clinic: String,
    @SerializedName("reason") val reason: String,
    @SerializedName("prescriptionList") val prescriptionList: List<FullPrescriptionDTO>
) : Serializable