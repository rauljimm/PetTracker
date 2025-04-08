package pets.vetguard.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.android.material.textfield.TextInputEditText
import pets.vetguard.MainActivity
import pets.vetguard.R
import pets.vetguard.dto.ChangePetDTO
import pets.vetguard.model.Pet
import pets.vetguard.service.RetrofitClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class EditPetFragment : Fragment() {

    private lateinit var editTextName: TextInputEditText
    private lateinit var editTextDescription: TextInputEditText
    private lateinit var editTextAge: TextInputEditText
    private lateinit var editTextSpecies: TextInputEditText
    private lateinit var editTextRace: TextInputEditText
    private lateinit var editTextSex: TextInputEditText
    private lateinit var editTextWeight: TextInputEditText
    private lateinit var editTextProfilePic: TextInputEditText
    private lateinit var buttonSavePet: Button
    private var petId: Long? = null
    private var petPosition: Int = -1
    private var jwtToken: String? = null
    private var petToEdit: Pet? = null

    companion object {
        private const val ARG_PET_ID = "petId"
        private const val ARG_POSITION = "petPosition"
        private const val ARG_TOKEN = "jwtToken"

        fun newInstance(petId: Long, position: Int, token: String?): EditPetFragment {
            val fragment = EditPetFragment()
            val args = Bundle().apply {
                putLong(ARG_PET_ID, petId)
                putInt(ARG_POSITION, position)
                putString(ARG_TOKEN, token)
            }
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            petId = it.getLong(ARG_PET_ID)
            petPosition = it.getInt(ARG_POSITION)
            jwtToken = it.getString(ARG_TOKEN)
        }
        Log.d("EditPetFragment", "onCreate - petId: $petId, petPosition: $petPosition, jwtToken: ${jwtToken?.take(10)}...")
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.edit_pet_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        editTextName = view.findViewById(R.id.editTextName)
        editTextDescription = view.findViewById(R.id.editTextDescription)
        editTextAge = view.findViewById(R.id.editTextAge)
        editTextSpecies = view.findViewById(R.id.editTextSpecies)
        editTextRace = view.findViewById(R.id.editTextRace)
        editTextSex = view.findViewById(R.id.editTextSex)
        editTextWeight = view.findViewById(R.id.editTextWeight)
        editTextProfilePic = view.findViewById(R.id.editTextProfilePic)
        buttonSavePet = view.findViewById(R.id.buttonSavePet)

        (activity as? MainActivity)?.setBottomNavVisibility(false)

        // Cargar datos de la mascota desde la API
        loadPetDataFromApi()

        buttonSavePet.setOnClickListener {
            val updatedPetDTO = createPetDTOFromInputs()
            Log.d("EditPetFragment", "Sending updated Pet DTO: $updatedPetDTO")

            if (petId == null) {
                Log.e("EditPetFragment", "petId is null")
                Toast.makeText(context, "Error: No se ha especificado la mascota a editar", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (updatedPetDTO == null) {
                Log.e("EditPetFragment", "updatedPetDTO is null")
                Toast.makeText(context, "Por favor, completa los campos requeridos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            Log.d("EditPetFragment", "Calling updatePetInBackend with petId: $petId")
            updatePetInBackend(petId!!, updatedPetDTO)
        }
    }

    private fun loadPetDataFromApi() {
        petId?.let { id ->
            if (jwtToken.isNullOrEmpty()) {
                Log.e("EditPetFragment", "Token no disponible, no se puede cargar la mascota")
                Toast.makeText(context, "Error: Token no disponible", Toast.LENGTH_SHORT).show()
                return@let
            }
            RetrofitClient.updateToken(jwtToken!!)
            CoroutineScope(Dispatchers.Main).launch {
                try {
                    val response = RetrofitClient.apiServices.getPetById(id)
                    if (response.isSuccessful) {
                        petToEdit = response.body()
                        petToEdit?.let { pet ->
                            editTextName.setText(pet.name ?: "")
                            editTextDescription.setText(pet.description ?: "")
                            editTextAge.setText(pet.age?.toString() ?: "")
                            editTextSpecies.setText(pet.species ?: "")
                            editTextRace.setText(pet.breed ?: "")
                            editTextSex.setText(pet.sex ?: "")
                            editTextWeight.setText(pet.weight?.toString() ?: "")
                            editTextProfilePic.setText(pet.profilePic ?: "")
                            Log.d("EditPetFragment", "Datos de la mascota cargados desde API: $pet")
                        } ?: Log.w("EditPetFragment", "Respuesta exitosa pero petToEdit es null")
                    } else {
                        Log.e("EditPetFragment", "Error al cargar la mascota: ${response.code()} - ${response.errorBody()?.string()}")
                        Toast.makeText(context, "Error al cargar la mascota: ${response.code()}", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Log.e("EditPetFragment", "Error de red al cargar la mascota: ${e.message}", e)
                    Toast.makeText(context, "Fallo de red: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        } ?: run {
            Log.e("EditPetFragment", "petId es null, no se puede cargar la mascota")
            Toast.makeText(context, "Error: ID de mascota no especificado", Toast.LENGTH_SHORT).show()
        }
    }

    private fun createPetDTOFromInputs(): ChangePetDTO? {
        val name = editTextName.text.toString().trim()
        val description = editTextDescription.text.toString().trim()
        val ageText = editTextAge.text.toString().trim()
        val species = editTextSpecies.text.toString().trim()
        val race = editTextRace.text.toString().trim()
        val sexText = editTextSex.text.toString().trim()
        val weightText = editTextWeight.text.toString().trim()
        val profilePic = editTextProfilePic.text.toString().trim()

        if (name.isEmpty()) {
            editTextName.error = "El nombre es obligatorio"
            return null
        }
        if (species.isEmpty()) {
            editTextSpecies.error = "La especie es obligatoria"
            return null
        }
        if (ageText.isEmpty()) {
            editTextAge.error = "La edad es obligatoria"
            return null
        }
        if (sexText.isEmpty()) {
            editTextSex.error = "El sexo es obligatorio"
            return null
        }
        if (weightText.isEmpty()) {
            editTextWeight.error = "El peso es obligatorio"
            return null
        }

        val age = ageText.toIntOrNull()
        if (age == null || age < 0) {
            editTextAge.error = "La edad debe ser un número válido mayor o igual a 0"
            return null
        }

        val weight = weightText.toDoubleOrNull()
        if (weight == null || weight <= 0) {
            editTextWeight.error = "El peso debe ser un número válido mayor a 0"
            return null
        }

        val sex = if (sexText.isNotEmpty()) sexText else null
        if (sex == null || (sex != "M" && sex != "H")) {
            editTextSex.error = "El sexo debe ser 'M' (macho) o 'H' (hembra)"
            return null
        }

        return ChangePetDTO(
            name = name,
            description = if (description.isEmpty()) null else description,
            age = age,
            species = species,
            breed = if (race.isEmpty()) null else race,
            sex = sex[0],
            weight = weight,
            imageUrl = if (profilePic.isEmpty()) petToEdit?.profilePic else profilePic,
            diseaseList = petToEdit?.diseaseList ?: emptyList()
        )
    }

    private fun updatePetInBackend(petId: Long, petDTO: ChangePetDTO) {
        petDTO.imageUrl = petDTO.imageUrl?.ifEmpty { "https://gatitudapg.sukycms.com/images/animal-default.jpg" }
        petDTO.diseaseList = petDTO.diseaseList ?: emptyList()

        RetrofitClient.apiServices.updatePet(petId, petDTO).enqueue(object : Callback<Pet> {
            override fun onResponse(call: Call<Pet>, response: Response<Pet>) {
                Log.d("EditPetFragment", "Response code: ${response.code()}, body: ${response.body()}")
                if (response.isSuccessful) {
                    val updatedPet = response.body()
                    updatedPet?.let {
                        Toast.makeText(context, "Mascota actualizada: ${it.name}", Toast.LENGTH_SHORT).show()
                        val result = Bundle().apply {
                            putParcelable("updatedPet", it)
                            putInt("petPosition", petPosition)
                        }
                        parentFragmentManager.setFragmentResult("editRequestKey", result)
                        parentFragmentManager.popBackStack()
                    }
                } else {
                    Log.e("EditPetFragment", "Error al actualizar: ${response.code()} - ${response.errorBody()?.string()}")
                    Toast.makeText(context, "Error al actualizar la mascota: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<Pet>, t: Throwable) {
                Log.e("EditPetFragment", "Network failure: ${t.message}", t)
                Toast.makeText(context, "Fallo de red: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        (activity as? MainActivity)?.setBottomNavVisibility(true)
    }
}