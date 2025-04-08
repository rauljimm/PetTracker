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
import pets.vetguard.R
import pets.vetguard.dto.CreatePetDTO
import pets.vetguard.model.Pet
import pets.vetguard.service.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class CreatePetFragment : Fragment() {

    private lateinit var editTextName: TextInputEditText
    private lateinit var editTextDescription: TextInputEditText
    private lateinit var editTextAge: TextInputEditText
    private lateinit var editTextSpecies: TextInputEditText
    private lateinit var editTextRace: TextInputEditText
    private lateinit var editTextSex: TextInputEditText
    private lateinit var editTextWeight: TextInputEditText
    private lateinit var editTextProfilePic: TextInputEditText
    private lateinit var buttonCreatePet: Button
    private var ownerId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ownerId = arguments?.getString("ownerId")
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.create_pet_fragment, container, false)
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
        buttonCreatePet = view.findViewById(R.id.buttonCreatePet)

        buttonCreatePet.setOnClickListener {
            val petDTO = createPetDTOFromInputs()
            if (petDTO != null) {
                createPetInBackend(petDTO)
            } else {
                Toast.makeText(context, "Por favor, completa los campos requeridos", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun createPetDTOFromInputs(): CreatePetDTO? {
        val name = editTextName.text.toString().trim()
        val description = editTextDescription.text.toString().trim()
        val ageText = editTextAge.text.toString().trim()
        val species = editTextSpecies.text.toString().trim()
        val race = editTextRace.text.toString().trim()
        val sexText = editTextSex.text.toString().trim()
        val weightText = editTextWeight.text.toString().trim()

        if (name.isEmpty() || species.isEmpty() || ageText.isEmpty() || ownerId == null) {
            editTextName.error = if (name.isEmpty()) "Requerido" else null
            editTextSpecies.error = if (species.isEmpty()) "Requerido" else null
            editTextAge.error = if (ageText.isEmpty()) "Requerido" else null
            return null
        }

        val age = ageText.toIntOrNull() ?: 0
        val sex = if (sexText.isNotEmpty()) sexText[0] else null
        val weight = weightText.toDoubleOrNull() ?: 0.0

        return CreatePetDTO(
            id = null,
            ownerId = ownerId!!,
            name = name,
            description = if (description.isEmpty()) null else description,
            age = age,
            species = species,
            breed = if (race.isEmpty()) null else race,
            sex = sex,
            weight = weight,
            diseaseList = emptyList()
        )
    }

    private fun createPetInBackend(petDTO: CreatePetDTO) {
        RetrofitClient.apiServices.createPet(petDTO).enqueue(object : Callback<Pet> {
            override fun onResponse(call: Call<Pet>, response: Response<Pet>) {
                if (response.isSuccessful) {
                    val petCreated = response.body()
                    Log.d("CreatePetFragment", "Respuesta completa: ${petCreated}")

                    petCreated?.let {
                        // Verifica que el ID no sea nulo antes de continuar
                        if (it.id != null) {
                            Toast.makeText(context, "Mascota creada: ${it.name}, ID: ${it.id}", Toast.LENGTH_SHORT).show()

                            val result = Bundle().apply {
                                putParcelable("newPet", it)
                            }
                            parentFragmentManager.setFragmentResult("requestKey", result)
                            parentFragmentManager.popBackStack()
                        } else {
                            Log.e("CreatePetFragment", "La mascota creada tiene ID nulo")
                            Toast.makeText(context, "Error: La mascota creada no tiene ID", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {

                    Log.e("CreatePetFragment", "Error al crear mascota: ${response.code()}")
                    Log.e("CreatePetFragment", "Cuerpo del error: ${response.errorBody()?.string()}")
                    Toast.makeText(context, "Error al crear la mascota: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<Pet>, t: Throwable) {
                Log.e("CreatePetFragment", "Fallo de red: ${t.message}", t)
                Toast.makeText(context, "Fallo de red: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

}