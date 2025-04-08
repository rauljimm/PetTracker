package pets.vetguard.model

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize


@Parcelize
data class Pet(
    @SerializedName("id")
    val id: Long? = null,

    @SerializedName("name")
    val name: String? = null,

    @SerializedName("description")
    val description: String? = null,

    @SerializedName("age")
    val age: Int? = null,

    @SerializedName("species")
    val species: String? = null,

    @SerializedName("breed")
    val breed: String? = null,

    @SerializedName("sex")
    val sex: String? = null,

    @SerializedName("weight")
    val weight: Double? = null,

    @SerializedName("urlImage")
    val profilePic: String? = null,

    @SerializedName("diseaseList")
    val diseaseList: List<String>? = null,

    @SerializedName("vaccineList")
    val vaccineList: List<String>? = null,

    @SerializedName("reportList")
    val reportList: List<Report>? = null,

    @SerializedName("physicalExerciseList")
    val physicalExerciseList: List<String>? = null,

    @SerializedName("owner")
    val owner: Owner? = null
) : Parcelable