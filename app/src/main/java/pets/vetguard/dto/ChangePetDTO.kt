package pets.vetguard.dto

import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize
import android.os.Parcelable

@Parcelize
data class ChangePetDTO(
    @SerializedName("name") val name: String,
    @SerializedName("description") val description: String?,
    @SerializedName("age") val age: Int,
    @SerializedName("species") val species: String,
    @SerializedName("breed") val breed: String?,
    @SerializedName("sex") val sex: Char?,
    @SerializedName("weight") val weight: Double,
    @SerializedName("imageUrl") var imageUrl: String?,
    @SerializedName("diseaseList") var diseaseList: List<String>
) : Parcelable