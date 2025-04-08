package pets.vetguard.service

import pets.vetguard.api.ImageUpdateRequest
import pets.vetguard.api.LoginRequest
import pets.vetguard.api.LoginResponse
import pets.vetguard.api.RegisterRequest
import pets.vetguard.api.RegisterResponse
import pets.vetguard.dto.ChangePetDTO
import pets.vetguard.dto.ChangePrescriptionDTO
import pets.vetguard.dto.ChangeReportDTO
import pets.vetguard.dto.CreatePetDTO
import pets.vetguard.dto.FullPrescriptionDTO
import pets.vetguard.dto.FullReportDTO
import pets.vetguard.model.Owner
import pets.vetguard.model.Pet
import pets.vetguard.model.Prescription
import pets.vetguard.model.Report
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface ApiServices {
    @Headers("Content-Type: application/json")
    @POST("/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @POST("/auth/register") // Ajusta la ruta según tu backend
    suspend fun register(@Body request: RegisterRequest): Response<RegisterResponse>

    @GET("/api/owner/{id}")
    suspend fun getOwnerById(@Path("id") id: Long): Owner

    @GET("/api/pets")
    suspend fun getAllPets(): List<Pet>

    @POST("/api/pet")
    fun createPet(@Body petDTO: CreatePetDTO): Call<Pet>

    @PUT("/api/pet/{id}")
    fun updatePet(@Path("id") id: Long, @Body petDTO: ChangePetDTO): Call<Pet>

    @GET("/api/reports")
    fun getAllReports(): Call<List<Report>>

    @GET("api/pet/{id}")
    suspend fun getPetById(@Path("id") id: Long): Response<Pet>

    @POST("/api/report")
    fun createReport(@Body reportDTO: FullReportDTO): Call<Report>

    @PUT("/api/report/{id}")
    fun updateReport(@Path("id") id: Long, @Body reportDTO: ChangeReportDTO): Call<Report>

    @POST("/api/prescription")
    fun createPrescription(@Body prescriptionDTO: FullPrescriptionDTO): Call<Prescription>

    @PUT("/api/prescription/{id}")
    fun updatePrescription(@Path("id") id: Long, @Body prescriptionDTO: ChangePrescriptionDTO): Call<Prescription>

    @DELETE("/api/prescription/{id}")
    fun deletePrescription(@Path("id") id: Long): Call<Void>

    @GET("/api/reports/pet/{petId}")
    fun getReportsByPetId(@Path("petId") petId: Long): Call<List<Report>>

    // Nuevo método para actualizar la imagen del Owner
    @PUT("/api/owner/image/{id}")
    fun updateOwnerImage(@Path("id") id: Long, @Body imageRequest: ImageUpdateRequest): Call<Owner>
}