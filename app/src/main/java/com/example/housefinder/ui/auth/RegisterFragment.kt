package com.example.housefinder.ui.auth

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.housefinder.R
import com.example.housefinder.db.entities.AppDatabase
import com.example.housefinder.db.entities.GABORONE_UNIVERSITIES
import com.example.housefinder.db.entities.User
import com.example.housefinder.db.entities.hashPassword
import com.example.housefinder.ui.common.SessionManager
import kotlinx.coroutines.launch

class RegisterFragment : Fragment(R.layout.fragment_register) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val nameInput = view.findViewById<EditText>(R.id.edt_name)
        val emailInput = view.findViewById<EditText>(R.id.edt_email)
        val passwordInput = view.findViewById<EditText>(R.id.edt_password)
        val studentIdInput = view.findViewById<EditText>(R.id.edt_student_id)
        val roleSpinner = view.findViewById<Spinner>(R.id.spinner_role)
        val universitySpinner = view.findViewById<Spinner>(R.id.spinner_university)
        val studentRoleButton = view.findViewById<Button>(R.id.btn_role_student_register)
        val providerRoleButton = view.findViewById<Button>(R.id.btn_role_provider_register)
        val registerButton = view.findViewById<Button>(R.id.btn_register)
        val goLoginButton = view.findViewById<Button>(R.id.btn_go_login)

        val roles = listOf("STUDENT", "PROVIDER")
        roleSpinner.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, roles)

        val universities = GABORONE_UNIVERSITIES.map { it.name }
        universitySpinner.adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            universities
        )

        roleSpinner.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                val isStudent = roles[position] == "STUDENT"
                universitySpinner.isEnabled = isStudent
                studentIdInput.isEnabled = isStudent
                studentRoleButton.setBackgroundResource(
                    if (isStudent) R.drawable.bg_role_pill_selected else R.drawable.bg_role_pill_unselected
                )
                providerRoleButton.setBackgroundResource(
                    if (isStudent) R.drawable.bg_role_pill_unselected else R.drawable.bg_role_pill_selected
                )
                studentRoleButton.setTextColor(
                    ContextCompat.getColor(requireContext(), if (isStudent) R.color.white else R.color.text_primary)
                )
                providerRoleButton.setTextColor(
                    ContextCompat.getColor(requireContext(), if (isStudent) R.color.text_primary else R.color.white)
                )
            }

            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) = Unit
        }

        studentRoleButton.setOnClickListener { roleSpinner.setSelection(0) }
        providerRoleButton.setOnClickListener { roleSpinner.setSelection(1) }

        goLoginButton.setOnClickListener { findNavController().popBackStack() }

        registerButton.setOnClickListener {
            val name = nameInput.text.toString().trim()
            val email = emailInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()
            val studentId = studentIdInput.text.toString().trim()
            val role = roleSpinner.selectedItem.toString()
            val university = if (role == "STUDENT") universitySpinner.selectedItem.toString() else "N/A"

            if (name.isBlank() || email.isBlank() || password.length < 6) {
                Toast.makeText(requireContext(), "Please complete all required fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (role == "STUDENT" && studentId.isBlank()) {
                Toast.makeText(requireContext(), "Student ID is required for students", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            lifecycleScope.launch {
                val db = AppDatabase.getInstance(requireContext())
                val generatedStudentId = if (role == "PROVIDER") {
                    "PRV${System.currentTimeMillis().toString().takeLast(6)}"
                } else {
                    studentId
                }

                val user = User(
                    studentId = generatedStudentId,
                    name = name,
                    email = email,
                    passwordHash = hashPassword(password),
                    role = role,
                    university = university
                )

                val result = db.userDao().insert(user)
                if (result <= 0) {
                    Toast.makeText(requireContext(), "Email or student ID already exists", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                val saved = db.userDao().getByEmail(email)
                if (saved != null) {
                    SessionManager(requireContext()).saveSession(saved.id, saved.role, saved.name)
                    findNavController().navigate(R.id.action_registerFragment_to_listingListFragment)
                }
            }
        }
    }
}

