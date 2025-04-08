package pets.vetguard.api


import com.google.gson.annotations.SerializedName

data class ImageUpdateRequest(
    @SerializedName("imageUrl") val imageUrl: String
)