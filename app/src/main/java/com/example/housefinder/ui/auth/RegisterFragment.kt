package com.example.housefinder.ui.auth

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

import androidx.fragment.app.viewModels
import com.example.housefinder.viewmodel.RegisterViewModel

import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class RegisterFragment : Fragment(R.layout.fragment_register) {

    private val viewModel: RegisterViewModel by viewModels()

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
        val selectedRoleText = view.findViewById<TextView>(R.id.txt_role_selected)
        val registerTitle = view.findViewById<TextView>(R.id.txt_register_title)
        val registerSubtitle = view.findViewById<TextView>(R.id.txt_register_subtitle)
        val registerButton = view.findViewById<Button>(R.id.btn_register)
        val goLoginButton = view.findViewById<View>(R.id.btn_go_login)
        val studentFieldsContainer = view.findViewById<View>(R.id.container_student_fields)
        val landlordFieldsContainer = view.findViewById<View>(R.id.container_landlord_fields)

        val roles = listOf("STUDENT", "PROVIDER")
        roleSpinner.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, roles)

        val universities = GABORONE_UNIVERSITIES.map { it.name }
        universitySpinner.adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            universities
        )

        fun updateRoleUi(isStudent: Boolean) {
            studentFieldsContainer.visibility = if (isStudent) View.VISIBLE else View.GONE
            landlordFieldsContainer.visibility = if (isStudent) View.GONE else View.VISIBLE

            if (isStudent) {
                // Do not auto-clear provider path data here.
            } else {
                studentIdInput.text?.clear()
            }

            selectedRoleText.text = if (isStudent) {
                getString(R.string.selected_role_student)
            } else {
                getString(R.string.selected_role_landlord)
            }

            registerTitle.text = if (isStudent) {
                getString(R.string.register_title_student)
            } else {
                getString(R.string.register_title_landlord)
            }

            registerSubtitle.text = if (isStudent) {
                getString(R.string.register_subtitle_student)
            } else {
                getString(R.string.register_subtitle_landlord)
            }

            registerButton.text = if (isStudent) {
                getString(R.string.create_account_student_cta)
            } else {
                getString(R.string.create_account_landlord_cta)
            }

            studentRoleButton.setBackgroundResource(
                if (isStudent) R.drawable.bg_role_pill_active else R.drawable.bg_role_pill_inactive
            )
            providerRoleButton.setBackgroundResource(
                if (isStudent) R.drawable.bg_role_pill_inactive else R.drawable.bg_role_pill_active
            )
            studentRoleButton.setTextColor(
                ContextCompat.getColor(requireContext(), if (isStudent) R.color.white else R.color.register_text_dark)
            )
            providerRoleButton.setTextColor(
                ContextCompat.getColor(requireContext(), if (isStudent) R.color.register_text_dark else R.color.white)
            )
        }

        roleSpinner.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                updateRoleUi(roles[position] == "STUDENT")
            }

            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) = Unit
        }

        studentRoleButton.setOnClickListener {
            roleSpinner.setSelection(0)
            updateRoleUi(true)
        }
        providerRoleButton.setOnClickListener {
            roleSpinner.setSelection(1)
            updateRoleUi(false)
        }

        goLoginButton.setOnClickListener { findNavController().popBackStack() }

        registerButton.setOnClickListener {
            val name = nameInput.text.toString().trim()
            val email = emailInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()
            val role = roleSpinner.selectedItem.toString()
            val studentIdValue = studentIdInput.text.toString().trim()
            val university = universitySpinner.selectedItem?.toString().orEmpty()

            if (name.isBlank() || email.isBlank() || password.length < 6) {
                Toast.makeText(requireContext(), "Please complete all required fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (role == "STUDENT" && studentIdValue.isBlank()) {
                Toast.makeText(requireContext(), "Student ID is required", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            viewModel.register(name, email, password, role, studentIdValue, university)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.registrationResult.collect { result ->
                when (result) {
                    is RegisterViewModel.RegistrationResult.Success -> {
                        val saved = result.user
                        SessionManager(requireContext()).saveSession(saved.id)
                        
                        // Navigate based on user role
                        when (saved.role) {
                            "PROVIDER" -> findNavController().navigate(R.id.action_registerFragment_to_providerDashboardFragment)
                            else -> findNavController().navigate(R.id.action_registerFragment_to_listingListFragment)
                        }
                    }
                    is RegisterViewModel.RegistrationResult.Error -> {
                        Toast.makeText(requireContext(), result.message, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }
}
