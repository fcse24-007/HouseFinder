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

class LoginFragment : Fragment(R.layout.fragment_login) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val emailInput = view.findViewById<EditText>(R.id.edt_email)
        val passwordInput = view.findViewById<EditText>(R.id.edt_password)
        val studentRoleButton = view.findViewById<Button>(R.id.btn_role_student_login)
        val providerRoleButton = view.findViewById<Button>(R.id.btn_role_provider_login)
        val loginButton = view.findViewById<Button>(R.id.btn_login)
        val createAccountButton = view.findViewById<Button>(R.id.btn_create_account)

        fun updateRoleVisuals(studentSelected: Boolean) {
            studentRoleButton.setBackgroundResource(
                if (studentSelected) R.drawable.bg_role_pill_selected else R.drawable.bg_role_pill_unselected
            )
            providerRoleButton.setBackgroundResource(
                if (studentSelected) R.drawable.bg_role_pill_unselected else R.drawable.bg_role_pill_selected
            )
            studentRoleButton.setTextColor(
                ContextCompat.getColor(requireContext(), if (studentSelected) R.color.white else R.color.text_primary)
            )
            providerRoleButton.setTextColor(
                ContextCompat.getColor(requireContext(), if (studentSelected) R.color.text_primary else R.color.white)
            )
        }

        updateRoleVisuals(studentSelected = true)
        studentRoleButton.setOnClickListener { updateRoleVisuals(studentSelected = true) }
        providerRoleButton.setOnClickListener { updateRoleVisuals(studentSelected = false) }

        loginButton.setOnClickListener {
            val email = emailInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()

            if (email.isBlank() || password.isBlank()) {
                Toast.makeText(requireContext(), "Enter email and password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            lifecycleScope.launch {
                val db = AppDatabase.getInstance(requireContext())
                val user = db.userDao().getByEmail(email)

                if (user == null || !BCrypt.checkpw(password, user.passwordHash)) {
                    Toast.makeText(requireContext(), "Invalid credentials", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                SessionManager(requireContext()).saveSession(user.id, user.role, user.name)
                findNavController().navigate(R.id.action_loginFragment_to_listingListFragment)
            }
        }

        createAccountButton.setOnClickListener {
            findNavController().navigate(R.id.action_loginFragment_to_registerFragment)
        }
    }
}

