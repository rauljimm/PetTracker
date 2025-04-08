package pets.vetguard.model

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
data class Prescription(
    @SerializedName("id")
    var id: Long? = null,

    @SerializedName("reportId")
    var reportId: Long? = null,

    @SerializedName("medName")
    val medName: String = "",

    @SerializedName("startDate")
    val startDate: String = "",

    @SerializedName("endDate")
    val endDate: String = "",

    @SerializedName("frequency")
    val frequency: String = ""
) : Parcelable