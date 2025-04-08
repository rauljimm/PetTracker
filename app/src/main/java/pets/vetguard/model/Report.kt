package pets.vetguard.model

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize
import java.util.Date

@Parcelize
data class Report(
    @SerializedName("id") val id: Long? = null,
    @SerializedName("pet") val pet: Pet? = null,
    @SerializedName("reportDate") val date: String? = null,
    @SerializedName("clinic") val clinic: String? = null,
    @SerializedName("reason") val reason: String? = null,
    @SerializedName("prescriptionList") var recipes: MutableList<Prescription>? = mutableListOf()
) : Parcelable