package pets.vetguard.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import pets.vetguard.R
import pets.vetguard.api.RegisterRequest
import pets.vetguard.api.RegisterResponse
import pets.vetguard.service.RetrofitClient
import retrofit2.HttpException

class RegisterFragment : Fragment() {

    private lateinit var usernameEditText: EditText
    private lateinit var emailEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var registerButton: Button
    private lateinit var backToLoginButton: Button
    private val TAG = "RegisterFragment"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_register, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        backToLoginButton = view.findViewById(R.id.backToLoginButton)
        backToLoginButton.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        usernameEditText = view.findViewById(R.id.usernameEditText)
        emailEditText = view.findViewById(R.id.emailEditText)
        passwordEditText = view.findViewById(R.id.passwordEditText)
        registerButton = view.findViewById(R.id.registerButton)

        usernameEditText.setText("")
        emailEditText.setText("")
        passwordEditText.setText("")

        registerButton.setOnClickListener {
            val username = usernameEditText.text.toString().trim()
            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()

            if (username.isEmpty() || email.isEmpty() || password.isEmpty()) {
                Toast.makeText(context, "Por favor, completa todos los campos", Toast.LENGTH_SHORT).show()
            } else {
                performRegister(username, email, password)
            }
        }
    }

    private fun performRegister(username: String, email: String, password: String) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val request = RegisterRequest(username, email, password)
                Log.d(TAG, "Enviando solicitud de registro: $request")

                val response = RetrofitClient.apiServices.register(request)
                Log.d(TAG, "Respuesta del servidor: ${response.code()} - ${response.message()}")

                if (response.isSuccessful) {
                    response.body()?.let { registerResponse: RegisterResponse ->
                        Log.d(TAG, "Registro exitoso: $registerResponse")
                        Toast.makeText(context, "Registro exitoso", Toast.LENGTH_SHORT).show()
                        val result = Bundle().apply {
                            putSerializable("registeredOwner", registerResponse)
                        }
                        parentFragmentManager.setFragmentResult("registerRequestKey", result)
                        parentFragmentManager.popBackStack() // Vuelve al login
                    } ?: run {
                        Log.w(TAG, "Respuesta exitosa pero body es null")
                        Toast.makeText(context, "Registro exitoso pero no se recibió respuesta", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    val errorBody = response.errorBody()?.string()
                    Log.e(TAG, "Error al registrar: ${response.code()} - ${response.message()} - $errorBody")
                    Toast.makeText(
                        context,
                        "Error al registrar: ${response.code()} - ${response.message()}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            } catch (e: HttpException) {
                Log.e(TAG, "HTTP Exception al registrar: ${e.code()} - ${e.message()}", e)
                Toast.makeText(context, "Error HTTP: ${e.code()} - ${e.message()}", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Log.e(TAG, "Error de conexión al registrar: ${e.message}", e)
                Toast.makeText(context, "Error de conexión: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
}