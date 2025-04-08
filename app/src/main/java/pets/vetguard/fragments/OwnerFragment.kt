import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import pets.vetguard.fragments.LogInFragment
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import pets.vetguard.MainActivity
import pets.vetguard.R
import pets.vetguard.adapters.PetAdapter
import pets.vetguard.api.ImageUpdateRequest
import pets.vetguard.fragments.EditPetFragment
import pets.vetguard.model.Owner
import pets.vetguard.model.Pet
import pets.vetguard.service.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class OwnerFragment : Fragment() {
    private lateinit var ownerProfilePic: ImageView
    private lateinit var editOwnerProfilePicLayout: TextInputLayout
    private lateinit var editOwnerProfilePic: TextInputEditText
    private lateinit var ownerNameText: TextView
    private lateinit var ownerDescriptionText: TextView
    private lateinit var editOwnerNameLayout: TextInputLayout
    private lateinit var editOwnerName: TextInputEditText
    private lateinit var editOwnerDescriptionLayout: TextInputLayout
    private lateinit var editOwnerDescription: TextInputEditText
    private lateinit var buttonEdit: Button
    private lateinit var petsRecyclerView: RecyclerView
    private lateinit var petAdapter: PetAdapter
    private var isEditing = false
    private var jwtToken: String? = null
    private val TAG = "OwnerFragment"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_owner, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Encontrar las vistas
        ownerProfilePic = view.findViewById(R.id.ownerProfilePic)
        editOwnerProfilePicLayout = view.findViewById(R.id.editOwnerProfilePicLayout)
        editOwnerProfilePic = view.findViewById(R.id.editOwnerProfilePic)
        ownerNameText = view.findViewById(R.id.ownerName)
        ownerDescriptionText = view.findViewById(R.id.ownerDescription)
        editOwnerNameLayout = view.findViewById(R.id.editOwnerNameLayout)
        editOwnerName = view.findViewById(R.id.editOwnerName)
        editOwnerDescriptionLayout = view.findViewById(R.id.editOwnerDescriptionLayout)
        editOwnerDescription = view.findViewById(R.id.editOwnerDescription)
        buttonEdit = view.findViewById(R.id.buttonEdit)
        petsRecyclerView = view.findViewById(R.id.petsRecyclerView)

        // Obtener el owner y el token de los argumentos
        val owner = arguments?.getParcelable<Owner>("owner")
        jwtToken = arguments?.getString("jwtToken")

        if (owner != null) {
            // Mostrar datos iniciales
            updateDisplay(owner)

            petAdapter = PetAdapter(mutableListOf()) { pet, position ->

            }
            petsRecyclerView.layoutManager = LinearLayoutManager(context)
            petsRecyclerView.adapter = petAdapter

            // Actualizar las mascotas al entrar al fragmento
            updatePetsFromServer(owner)

            // Configurar el botón de edición
            buttonEdit.setOnClickListener {
                if (isEditing) {
                    saveChanges(owner)
                } else {
                    enterEditMode(owner)
                }
            }
        } else {
            Log.w(TAG, "No se recibió un owner válido")
        }
    }

    // Nueva función para actualizar las mascotas desde el servidor
    private fun updatePetsFromServer(owner: Owner) {
        val mainActivity = activity as? MainActivity
        if (mainActivity != null && owner.id != null) {
            CoroutineScope(Dispatchers.Main).launch {
                try {
                    val updatedPets = mainActivity.fetchAllPetsForOwner(owner.id!!)
                    petAdapter.updatePets(updatedPets)
                    owner.pets = updatedPets // Actualizar el modelo local
                    Log.d(TAG, "Mascotas actualizadas desde el servidor: ${updatedPets.size}")
                } catch (e: Exception) {
                    Log.e(TAG, "Error al actualizar mascotas: ${e.message}", e)
                    Toast.makeText(context, "Error al cargar las mascotas", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            Log.w(TAG, "MainActivity no disponible o ID de owner nulo")
            petAdapter.updatePets(owner.pets ?: emptyList()) // Usar datos locales como fallback
        }
    }

    private fun updateDisplay(owner: Owner) {
        Glide.with(requireContext())
            .load(owner.profilePic)
            .into(ownerProfilePic)
        ownerNameText.text = owner.ownerName
        ownerDescriptionText.text = owner.description

        ownerProfilePic.visibility = View.VISIBLE
        editOwnerProfilePicLayout.visibility = View.GONE
        ownerNameText.visibility = View.VISIBLE
        editOwnerNameLayout.visibility = View.GONE
        ownerDescriptionText.visibility = View.VISIBLE
        editOwnerDescriptionLayout.visibility = View.GONE
        buttonEdit.text = "Editar"
        buttonEdit.setPadding(12, 12, 12, 12)
        buttonEdit.layoutParams = (buttonEdit.layoutParams as androidx.constraintlayout.widget.ConstraintLayout.LayoutParams).apply {
            topToBottom = R.id.ownerDescription
            topMargin = 16
        }
        petsRecyclerView.visibility = View.VISIBLE
        isEditing = false
    }

    private fun enterEditMode(owner: Owner) {
        editOwnerProfilePic.setText(owner.profilePic)
        editOwnerName.setText(owner.ownerName)
        editOwnerDescription.setText(owner.description)

        ownerProfilePic.visibility = View.GONE
        editOwnerProfilePicLayout.visibility = View.VISIBLE
        ownerNameText.visibility = View.GONE
        editOwnerNameLayout.visibility = View.VISIBLE
        ownerDescriptionText.visibility = View.GONE
        editOwnerDescriptionLayout.visibility = View.VISIBLE
        buttonEdit.text = "Guardar"
        buttonEdit.setPadding(12, 12, 12, 12)
        buttonEdit.layoutParams = (buttonEdit.layoutParams as androidx.constraintlayout.widget.ConstraintLayout.LayoutParams).apply {
            topToBottom = R.id.editOwnerDescriptionLayout
            topMargin = 32
        }
        petsRecyclerView.visibility = View.GONE
        isEditing = true

        buttonEdit.requestLayout()
    }

    private fun saveChanges(owner: Owner) {
        val newName = editOwnerName.text.toString().trim()
        val newDescription = editOwnerDescription.text.toString().trim()
        val newProfilePic = editOwnerProfilePic.text.toString().trim()


        if (newName.isEmpty()) {
            editOwnerNameLayout.error = "El nombre no puede estar vacío"
            return
        } else {
            editOwnerNameLayout.error = null
        }
        if (newProfilePic.isEmpty()) {
            editOwnerProfilePicLayout.error = "La URL de la imagen no puede estar vacía"
            return
        } else {
            editOwnerProfilePicLayout.error = null
        }


        val token = jwtToken ?: getTokenFromStorage()
        if (token.isNullOrEmpty()) {
            Log.e(TAG, "Token no disponible, redirigiendo a login")
            Toast.makeText(context, "Sesión expirada, por favor inicia sesión", Toast.LENGTH_SHORT).show()
            parentFragmentManager.beginTransaction()
                .replace(R.id.frameLayout, LogInFragment())
                .commit()
            return
        }
        RetrofitClient.updateToken(token)

        // Crear el request para actualizar la imagen
        val imageRequest = ImageUpdateRequest(newProfilePic)

        // Enviar la solicitud al servidor
        RetrofitClient.apiServices.updateOwnerImage(owner.id ?: return, imageRequest).enqueue(object : Callback<Owner> {
            override fun onResponse(call: Call<Owner>, response: Response<Owner>) {
                if (response.isSuccessful) {
                    val updatedOwner = response.body()
                    if (updatedOwner != null) {
                        // Actualizar el modelo local con los datos del servidor
                        owner.ownerName = newName
                        owner.description = newDescription
                        owner.profilePic = updatedOwner.profilePic // Usar la URL devuelta por el servidor
                        Log.d(TAG, "Imagen actualizada en el servidor: ${owner.profilePic}")

                        // Actualizar la UI
                        updateDisplay(owner)

                        // Notificar a MainActivity para actualizar el owner en otros fragmentos
                        val result = Bundle().apply {
                            putParcelable("updatedOwner", owner)
                        }
                        parentFragmentManager.setFragmentResult("ownerUpdateRequestKey", result)

                        Toast.makeText(context, "Perfil actualizado correctamente", Toast.LENGTH_SHORT).show()
                    } else {
                        Log.e(TAG, "Respuesta vacía del servidor")
                        Toast.makeText(context, "Error al actualizar el perfil", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Log.e(TAG, "Error al actualizar la imagen: ${response.code()}, ${response.errorBody()?.string()}")
                    Toast.makeText(context, "Error al actualizar: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<Owner>, t: Throwable) {
                Log.e(TAG, "Fallo de red al actualizar la imagen: ${t.message}", t)
                Toast.makeText(context, "Fallo de red: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun getTokenFromStorage(): String? {
        val sharedPreferences = context?.getSharedPreferences("prefs", Context.MODE_PRIVATE)
        val token = sharedPreferences?.getString("jwt_token", null)
        Log.d(TAG, "Token recuperado de SharedPreferences: ${token?.take(10)}...")
        return token
    }
}