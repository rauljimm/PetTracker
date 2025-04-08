package pets.vetguard.fragments

import android.os.Bundle
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
import pets.vetguard.api.LoginRequest
import pets.vetguard.service.RetrofitClient
import android.util.Log

class LogInFragment : Fragment() {

    private lateinit var usernameEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var loginButton: Button
    private lateinit var registerButton: Button


    private val TAG = "LogInFragment"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_login, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        registerButton = view.findViewById(R.id.registerButton)
        registerButton.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.frameLayout, RegisterFragment())
                .addToBackStack(null)
                .commit()
        }


        usernameEditText = view.findViewById(R.id.emailEditText)
        passwordEditText = view.findViewById(R.id.passwordEditText)
        loginButton = view.findViewById(R.id.loginButton)

        usernameEditText.setText("")
        passwordEditText.setText("")

        loginButton.setOnClickListener {
            val username = usernameEditText.text.toString()
            val password = passwordEditText.text.toString()

            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(context, "Por favor, completa todos los campos", Toast.LENGTH_SHORT).show()
                Log.w(TAG, "Campos vacíos detectados")
            } else {
                performLogin(username, password)
            }
        }
    }

    private fun performLogin(username: String, password: String) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                Log.i(TAG, "Enviando login para username: $username")
                val request = LoginRequest(username, password)
                val response = RetrofitClient.apiServices.login(request)

                if (response.isSuccessful) {
                    response.body()?.let { loginResponse ->
                        Log.i(TAG, "Login exitoso: ${loginResponse.username}, Token: ${loginResponse.token}")
                        val result = Bundle().apply {
                            putString("username", loginResponse.username)
                            putString("jwtToken", loginResponse.token)
                        }
                        parentFragmentManager.setFragmentResult("loginRequestKey", result)
                        Toast.makeText(context, "Login exitoso", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    val errorBody = response.errorBody()?.string() ?: "Sin detalles del error"
                    Log.e(TAG, "Error ${response.code()}: $errorBody")
                    Toast.makeText(
                        context,
                        "Error ${response.code()}: $errorBody",
                        Toast.LENGTH_LONG
                    ).show()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Excepción en login: ${e.message}", e)
                Toast.makeText(
                    context,
                    "Excepción: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}