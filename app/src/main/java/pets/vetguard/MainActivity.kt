package pets.vetguard

import OwnerFragment
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import pets.vetguard.fragments.MyPetsFragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import pets.vetguard.fragments.*
import pets.vetguard.model.Owner
import pets.vetguard.model.Pet
import pets.vetguard.service.RetrofitClient
import retrofit2.HttpException

class MainActivity : AppCompatActivity() {
    private lateinit var bottomNav: BottomNavigationView
    private val TAG = "MainActivity"
    private var jwtToken: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        bottomNav = findViewById(R.id.bottomNav)
        bottomNav.visibility = View.GONE

        // Restaurar token desde SharedPreferences al iniciar
        jwtToken = getTokenFromStorage()
        if (jwtToken != null) {
            RetrofitClient.updateToken(jwtToken!!)
            Log.d(TAG, "Token restaurado desde SharedPreferences: ${jwtToken!!.take(10)}...")
        }

        RetrofitClient.onTokenExpired = {
            Log.w(TAG, "Token expirado o no válido detectado. Redirigiendo a login.")
            runOnUiThread {
                supportFragmentManager.beginTransaction()
                    .replace(R.id.frameLayout, LogInFragment())
                    .commit()
                bottomNav.visibility = View.GONE
                jwtToken = null
                RetrofitClient.updateToken("")
                clearTokenFromStorage() // Limpiar token al expirar
            }
        }

        supportFragmentManager.addOnBackStackChangedListener {
            val currentFragment = supportFragmentManager.findFragmentById(R.id.frameLayout)
            when (currentFragment) {
                is LogInFragment, is RegisterFragment, is EditPetFragment, is CreateReportFragment, is EditReportFragment -> setBottomNavVisibility(false)
                else -> setBottomNavVisibility(true)
            }
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        if (savedInstanceState == null) {
            if (jwtToken != null) {
                fetchOwnerData(jwtToken!!, "unknown") // Ir directamente si hay token
            } else {
                val loginFragment = LogInFragment()
                supportFragmentManager.beginTransaction()
                    .replace(R.id.frameLayout, loginFragment)
                    .commit()
            }
        }

        supportFragmentManager.setFragmentResultListener("loginRequestKey", this) { _, result ->
            val username = result.getString("username")
            val token = result.getString("jwtToken")
            Log.d(TAG, "Resultado recibido - Username: $username, Token: $token (longitud: ${token?.length ?: 0})")
            if (username != null && token != null) {
                Log.d(TAG, "Token completo: $token")
                jwtToken = token
                RetrofitClient.updateToken(token)
                saveTokenToStorage(token) // Guardar token en SharedPreferences
                Log.d(TAG, "Token seteado en RetrofitClient: ${RetrofitClient.jwtToken}")
                fetchOwnerData(token, username)
            } else {
                Log.w(TAG, "Username o token nulos")
            }
        }
    }

    private fun saveTokenToStorage(token: String) {
        val sharedPreferences = getSharedPreferences("prefs", MODE_PRIVATE)
        sharedPreferences.edit().putString("jwt_token", token).apply()
        Log.d(TAG, "Token guardado en SharedPreferences: ${token.take(10)}...")
    }

    private fun getTokenFromStorage(): String? {
        val sharedPreferences = getSharedPreferences("prefs", MODE_PRIVATE)
        val token = sharedPreferences.getString("jwt_token", null)
        Log.d(TAG, "Token recuperado de SharedPreferences: ${token?.take(10)}...")
        return token
    }

    private fun clearTokenFromStorage() {
        val sharedPreferences = getSharedPreferences("prefs", MODE_PRIVATE)
        sharedPreferences.edit().remove("jwt_token").apply()
        Log.d(TAG, "Token eliminado de SharedPreferences")
    }

    private fun getOwnerIdFromToken(token: String): Long? {
        return try {
            val parts = token.split(".")
            if (parts.size != 3) {
                Log.e(TAG, "Token JWT inválido: no tiene 3 partes")
                return null
            }
            val payload = String(Base64.decode(parts[1], Base64.URL_SAFE), Charsets.UTF_8)
            val json = JSONObject(payload)
            json.getString("sub").toLong()
        } catch (e: Exception) {
            Log.e(TAG, "Error al parsear token para obtener ownerId: ${e.message}", e)
            null
        }
    }

    private suspend fun fetchAllPets(): List<Pet> {
        return try {
            val pets = withContext(Dispatchers.IO) {
                if (RetrofitClient.jwtToken.isNullOrEmpty()) {
                    Log.w(TAG, "Token no configurado antes de getAllPets. Reconfigurando...")
                    RetrofitClient.updateToken(jwtToken ?: "")
                }
                RetrofitClient.apiServices.getAllPets()
            }
            Log.d(TAG, "Total de mascotas obtenidas de la API: ${pets.size}")
            pets.forEach { pet ->
                Log.d(TAG, "Pet obtenida: ${pet.name}, ID: ${pet.id}, Owner ID: ${pet.owner?.id}")
            }
            pets
        } catch (e: Exception) {
            Log.e(TAG, "Error al obtener todas las mascotas: ${e.message}", e)
            emptyList()
        }
    }

    suspend fun fetchAllPetsForOwner(ownerId: Long): List<Pet> {
        return try {
            val pets = fetchAllPets()
            val ownerPets = pets.filter { it.owner?.id == ownerId }
            Log.d(TAG, "Mascotas filtradas para ownerId $ownerId: ${ownerPets.size}")
            ownerPets
        } catch (e: Exception) {
            Log.e(TAG, "Error al obtener mascotas para ownerId $ownerId: ${e.message}", e)
            emptyList()
        }
    }

    private fun fetchOwnerData(token: String, username: String) {
        if (token.isBlank()) {
            Log.e(TAG, "Token vacío - No se puede proceder")
            return
        }

        jwtToken = token
        RetrofitClient.updateToken(token)
        Log.d(TAG, "Token configurado en RetrofitClient antes de obtener owner: ${token.take(10)}...")

        CoroutineScope(Dispatchers.Main).launch {
            try {
                val ownerId = getOwnerIdFromToken(token) ?: run {
                    Log.w(TAG, "No se pudo obtener ownerId del token. Usando ID por defecto: 1")
                    1L
                }
                Log.d(TAG, "Intentando obtener owner con ID: $ownerId para username: $username")

                val ownerFromApi = withContext(Dispatchers.IO) {
                    RetrofitClient.apiServices.getOwnerById(ownerId)
                }
                Log.d(TAG, "Respuesta del backend para Owner: $ownerFromApi")

                val allPets = fetchAllPets()
                val ownerPets = allPets.filter { it.owner?.id == ownerId }

                val owner = Owner(
                    id = ownerFromApi.id,
                    ownerName = ownerFromApi.ownerName,
                    email = ownerFromApi.email,
                    profilePic = ownerFromApi.profilePic ?: "",
                    description = ownerFromApi.description ?: "",
                    pets = ownerPets.map { pet ->
                        Pet(
                            id = pet.id ?: 0,
                            name = pet.name ?: "",
                            description = pet.description,
                            age = pet.age,
                            species = pet.species ?: "",
                            breed = pet.breed,
                            sex = pet.sex,
                            weight = pet.weight,
                            profilePic = pet.profilePic,
                            diseaseList = pet.diseaseList,
                            vaccineList = pet.vaccineList,
                            reportList = pet.reportList,
                            physicalExerciseList = pet.physicalExerciseList,
                            owner = pet.owner
                        )
                    }
                )

                Log.d(TAG, "Owner cargado con ${owner.pets?.size ?: 0} mascotas filtradas desde la API")
                proceedWithOwner(owner)

            } catch (e: HttpException) {
                Log.e(TAG, "Error HTTP al obtener Owner: ${e.code()} - ${e.message()}", e)
                when (e.code()) {
                    403 -> {
                        Log.w(TAG, "Acceso denegado (403). Posible problema de permisos")
                        runOnUiThread {
                            supportFragmentManager.beginTransaction()
                                .replace(R.id.frameLayout, LogInFragment())
                                .commit()
                            bottomNav.visibility = View.GONE
                        }
                    }
                    401 -> Log.w(TAG, "Token no válido (401). Debería manejarse por RetrofitClient")
                    else -> Log.e(TAG, "Otro error HTTP: ${e.code()}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Excepción inesperada al obtener Owner: ${e.message}", e)
            }
        }
    }

    private fun proceedWithOwner(owner: Owner) {
        Log.i(TAG, "Owner procesado: ${owner.ownerName}, ID: ${owner.id}, Pets: ${owner.pets?.size ?: 0}")
        owner.pets?.forEach { pet ->
            Log.d(TAG, "Pet en owner: ${pet.name}, ID: ${pet.id}")
        }
        bottomNav.visibility = View.VISIBLE

        val bundle = Bundle().apply {
            putParcelable("owner", owner)
            putString("jwtToken", jwtToken) // Pasar el token a HomeFragment
        }
        val homeFragment = HomeFragment().apply {
            arguments = bundle
        }
        supportFragmentManager.beginTransaction()
            .replace(R.id.frameLayout, homeFragment)
            .commit()

        setupBottomNavigation(owner)
    }

    private fun setupBottomNavigation(owner: Owner) {
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.home_menu -> {
                    val bundle = Bundle().apply {
                        putParcelable("owner", owner)
                        putString("jwtToken", jwtToken) // Pasar el token
                    }
                    val homeFragment = HomeFragment().apply { arguments = bundle }
                    replaceFragment(homeFragment)
                }
                R.id.pets_menu -> {
                    val bundle = Bundle().apply {
                        putParcelable("owner", owner)
                        putString("jwtToken", jwtToken) // Pasar el token
                    }
                    val myPetsFragment = MyPetsFragment().apply { arguments = bundle }
                    replaceFragment(myPetsFragment)
                }
                R.id.owner_menu -> {
                    val bundle = Bundle().apply {
                        putParcelable("owner", owner)
                        putString("jwtToken", jwtToken) // Pasar el token
                    }
                    val ownerFragment = OwnerFragment().apply { arguments = bundle }
                    replaceFragment(ownerFragment)
                }
            }
            true
        }
    }

    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.frameLayout, fragment)
            .commit()
    }

    fun setBottomNavVisibility(isVisible: Boolean) {
        bottomNav.visibility = if (isVisible) View.VISIBLE else View.GONE
    }
}