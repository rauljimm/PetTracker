package pets.vetguard.dto


import com.google.gson.annotations.SerializedName
import java.io.Serializable


data class FullPrescriptionDTO(
    @SerializedName("id") val id: Long?,
    @SerializedName("reportId") val reportId: Long?,
    @SerializedName("medName") val medName: String?,
    @SerializedName("startDate") val startDate: String?,
    @SerializedName("endDate") val endDate: String?,
    @SerializedName("frequency") val frequency: String?
) : Serializable