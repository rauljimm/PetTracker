package pets.vetguard.fragments

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import pets.vetguard.MainActivity
import pets.vetguard.R
import pets.vetguard.adapters.PetAdapter
import pets.vetguard.model.Owner
import pets.vetguard.model.Pet
import pets.vetguard.model.Report
import pets.vetguard.service.RetrofitClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MyPetsFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var petAdapter: PetAdapter
    private lateinit var addPetFab: FloatingActionButton
    private val viewModel: PetsViewModel by viewModels()
    private var owner: Owner? = null
    private var jwtToken: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.my_pets_fragments, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView = view.findViewById(R.id.recyclerViewPets)
        addPetFab = view.findViewById(R.id.addPetFab)

        petAdapter = PetAdapter(mutableListOf()) { pet, position ->
            showPetOptions(pet, position)
        }
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = petAdapter

        addPetFab.setOnClickListener { showFabMenu(it) }

        viewModel.petList.observe(viewLifecycleOwner) { petList ->
            petAdapter.updatePets(petList)
            Log.d("MyPetsFragment", "Lista de mascotas actualizada: ${petList.size}")
        }

        // Cargar datos iniciales desde argumentos
        loadPetsFromArguments()

        // Refrescar las mascotas desde la API
        refreshPetsFromApi()

        parentFragmentManager.setFragmentResultListener("requestKey", viewLifecycleOwner) { _, result ->
            val newPet = result.getParcelable<Pet>("newPet")
            if (newPet != null && newPet.id != null) {
                if (viewModel.addPet(newPet)) {
                    Log.d("MyPetsFragment", "Nueva mascota añadida: ${newPet.name}, ID: ${newPet.id}")
                } else {
                    Toast.makeText(context, "Límite máximo de 6 mascotas alcanzado", Toast.LENGTH_SHORT).show()
                }
            } else {
                Log.w("MyPetsFragment", "newPet es null o no tiene ID")
            }
        }

        parentFragmentManager.setFragmentResultListener("editRequestKey", viewLifecycleOwner) { _, result ->
            val updatedPet = result.getParcelable<Pet>("updatedPet")
            val position = result.getInt("petPosition")
            if (updatedPet != null && viewModel.updatePet(position, updatedPet)) {
                Log.d("MyPetsFragment", "Mascota actualizada en posición $position, ID: ${updatedPet.id}")
            } else {
                Log.w("MyPetsFragment", "Error al actualizar la mascota o updatedPet es null")
            }
        }

        parentFragmentManager.setFragmentResultListener("createReportRequestKey", viewLifecycleOwner) { _, result ->
            val newReport = result.getParcelable<Report>("newReport")
            val position = result.getInt("petPosition")
            if (newReport != null && position >= 0) {
                val pet = viewModel.petList.value?.get(position)
                if (pet != null) {
                    val updatedReports = pet.reportList?.toMutableList() ?: mutableListOf()
                    updatedReports.add(newReport)
                    val updatedPet = pet.copy(reportList = updatedReports)
                    if (viewModel.updatePet(position, updatedPet)) {
                        Log.d("MyPetsFragment", "Reporte añadido a la mascota: ${pet.name}, ID: ${pet.id}")
                    }
                }
            }
        }
    }

    private fun refreshPetsFromApi() {
        val token = getToken()
        if (token.isNullOrEmpty()) {
            Log.e("MyPetsFragment", "Token no disponible, redirigiendo a login")
            parentFragmentManager.beginTransaction()
                .replace(R.id.frameLayout, LogInFragment())
                .commit()
            return
        }
        RetrofitClient.updateToken(token)
        owner?.id?.let { ownerId ->
            CoroutineScope(Dispatchers.Main).launch {
                val updatedPets = (activity as MainActivity).fetchAllPetsForOwner(ownerId)
                viewModel.loadPets(updatedPets)
                Log.d("MyPetsFragment", "Mascotas refrescadas desde API: ${updatedPets.size}")
            }
        } ?: Log.w("MyPetsFragment", "No se pudo refrescar mascotas: ownerId no disponible")
    }

    private fun showPetOptions(pet: Pet, position: Int) {
        val popup = PopupMenu(requireContext(), recyclerView.findViewHolderForAdapterPosition(position)?.itemView)
        popup.menuInflater.inflate(R.menu.pet_options_menu, popup.menu)
        popup.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.menu_edit_pet -> {
                    editPet(pet, position)
                    true
                }

                R.id.menu_view_reports -> {
                    viewReportsForPet(pet, position)
                    true
                }
                else -> false
            }
        }
        popup.show()
    }



    private fun viewReportsForPet(pet: Pet, position: Int) {
        Log.d("MyPetsFragment", "Viendo reportes para la mascota: ${pet.name}, ID: ${pet.id}")
        val token = getToken()
        if (token.isNullOrEmpty()) {
            Log.e("MyPetsFragment", "Token no disponible, redirigiendo a login")
            parentFragmentManager.beginTransaction()
                .replace(R.id.frameLayout, LogInFragment())
                .commit()
            return
        }
        if (pet.id == null) {
            Log.e("MyPetsFragment", "El ID de la mascota es null, no se puede ver reportes")
            Toast.makeText(context, "Error: Mascota inválida", Toast.LENGTH_SHORT).show()
            return
        }
        RetrofitClient.updateToken(token)
        val reportsFragment = ReportsFragment.newInstance(pet.id, position, token)
        parentFragmentManager.beginTransaction()
            .replace(R.id.frameLayout, reportsFragment)
            .addToBackStack(null)
            .commit()
    }

    private fun editPet(pet: Pet, position: Int) {
        Log.d("MyPetsFragment", "Editando mascota: ${pet.name}, ID: ${pet.id}, Position: $position")
        val token = getToken()
        if (token.isNullOrEmpty()) {
            Log.e("MyPetsFragment", "Token no disponible, redirigiendo a login")
            parentFragmentManager.beginTransaction()
                .replace(R.id.frameLayout, LogInFragment())
                .commit()
            return
        }
        if (pet.id == null) {
            Log.e("MyPetsFragment", "El ID de la mascota es null, no se puede editar")
            Toast.makeText(context, "Error: Mascota inválida", Toast.LENGTH_SHORT).show()
            return
        }
        RetrofitClient.updateToken(token)
        val editPetFragment = EditPetFragment.newInstance(pet.id, position, token)
        parentFragmentManager.beginTransaction()
            .replace(R.id.frameLayout, editPetFragment)
            .addToBackStack(null)
            .commit()
    }

    private fun showFabMenu(view: View) {
        val popup = PopupMenu(requireContext(), view)
        popup.menuInflater.inflate(R.menu.fab_menu, popup.menu)
        popup.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.menu_create_pet -> {
                    if (viewModel.petList.value?.size ?: 0 < 6) {
                        val token = getToken()
                        if (token.isNullOrEmpty()) {
                            Log.e("MyPetsFragment", "Token no disponible, redirigiendo a login")
                            parentFragmentManager.beginTransaction()
                                .replace(R.id.frameLayout, LogInFragment())
                                .commit()
                            return@setOnMenuItemClickListener true
                        }
                        RetrofitClient.updateToken(token)
                        val bundle = Bundle().apply {
                            putString("ownerId", owner?.id?.toString() ?: "1")
                            putString("jwtToken", token)
                        }
                        val createPetFragment = CreatePetFragment().apply { arguments = bundle }
                        parentFragmentManager.beginTransaction()
                            .replace(R.id.frameLayout, createPetFragment)
                            .addToBackStack(null)
                            .commit()
                    } else {
                        Toast.makeText(context, "Límite máximo de 6 mascotas alcanzado", Toast.LENGTH_SHORT).show()
                    }
                    true
                }
                else -> false
            }
        }
        popup.show()
    }

    private fun loadPetsFromArguments() {
        arguments?.let {
            owner = it.getParcelable("owner")
            jwtToken = it.getString("jwtToken")
            owner?.let { owner ->
                Log.d("MyPetsFragment", "Owner recibido con ${owner.pets?.size ?: 0} mascotas")
                viewModel.loadPets(owner.pets)
            } ?: Log.w("MyPetsFragment", "No se recibió un owner válido")
            if (jwtToken.isNullOrEmpty()) {
                Log.w("MyPetsFragment", "No se recibió un token válido por argumentos, intentando desde SharedPreferences")
                jwtToken = getTokenFromStorage()
            }
            if (!jwtToken.isNullOrEmpty()) {
                Log.d("MyPetsFragment", "Token recibido: ${jwtToken?.take(10)}...")
            }
        }
    }

    private fun getToken(): String? {
        return jwtToken ?: getTokenFromStorage().also {
            if (it != null) jwtToken = it
        }
    }

    private fun getTokenFromStorage(): String? {
        val sharedPreferences = context?.getSharedPreferences("prefs", Context.MODE_PRIVATE)
        val token = sharedPreferences?.getString("jwt_token", null)
        Log.d("MyPetsFragment", "Token recuperado de SharedPreferences: ${token?.take(10)}...")
        return token
    }
}