package pets.vetguard.dto

import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize
import android.os.Parcelable
import java.io.Serializable
import java.util.Date

data class ChangePrescriptionDTO(
    @SerializedName("medName") val medName: String?,
    @SerializedName("startDate") val startDate: String?, // String en formato "yyyy-MM-dd"
    @SerializedName("endDate") val endDate: String?,     // String en formato "yyyy-MM-dd"
    @SerializedName("frequency") val frequency: String?
) : Serializable