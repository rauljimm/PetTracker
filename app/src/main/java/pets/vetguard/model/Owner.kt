package pets.vetguard.model

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize
import java.io.Serializable

@Parcelize
data class Owner(
    @SerializedName("id")
    val id: Long? = null,

    @SerializedName("username")
    var ownerName: String,

    @SerializedName("email")
    var email: String,

    @SerializedName("urlImage")
    var profilePic: String,

    @SerializedName("description")
    var description: String = "",

    @SerializedName("petList")
    var pets: List<Pet>?
) : Parcelable {

}