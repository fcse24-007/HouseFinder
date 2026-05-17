package com.example.housefinder.ui.auth

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.housefinder.R
import com.example.housefinder.db.entities.AppDatabase
import com.example.housefinder.ui.common.SessionManager
import kotlinx.coroutines.launch
import org.mindrot.jbcrypt.BCrypt

import androidx.fragment.app.viewModels
import com.example.housefinder.viewmodel.LoginViewModel

import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class LoginFragment : Fragment(R.layout.fragment_login) {

    private val viewModel: LoginViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val emailInput = view.findViewById<EditText>(R.id.edt_login_email)
        val passwordInput = view.findViewById<EditText>(R.id.edt_login_password)
        val loginButton = view.findViewById<Button>(R.id.btn_login)
        val createAccountButton = view.findViewById<View>(R.id.btn_go_register)

        loginButton.setOnClickListener {
            val email = emailInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()
            viewModel.login(email, password)
        }

        lifecycleScope.launch {
            viewModel.loginResult.collect { result ->
                when (result) {
                    is LoginViewModel.LoginResult.Success -> {
                        val user = result.user
                        SessionManager(requireContext()).saveSession(user.id)
                        
                        // Navigate based on user role
                        when (user.role) {
                            "PROVIDER" -> findNavController().navigate(R.id.action_loginFragment_to_providerDashboardFragment)
                            else -> findNavController().navigate(R.id.action_loginFragment_to_listingListFragment)
                        }
                    }
                    is LoginViewModel.LoginResult.Error -> {
                        Toast.makeText(requireContext(), result.message, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        createAccountButton.setOnClickListener {
            findNavController().navigate(R.id.action_loginFragment_to_registerFragment)
        }
    }
}
