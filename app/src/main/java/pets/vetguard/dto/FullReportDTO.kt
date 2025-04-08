package pets.vetguard.dto

import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize
import android.os.Parcelable

@Parcelize
data class FullReportDTO(
    @SerializedName("id") val id: Long?,
    @SerializedName("petId") val petId: Long,
    @SerializedName("reportDate") val reportDate: String,
    @SerializedName("clinic") val clinic: String,
    @SerializedName("reason") val reason: String,
    @SerializedName("prescriptionList") val prescriptionList: List<FullPrescriptionDTO>?
) : Parcelable