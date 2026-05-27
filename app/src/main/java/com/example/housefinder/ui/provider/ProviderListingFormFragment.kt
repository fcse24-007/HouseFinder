package com.example.housefinder.ui.provider

import android.app.DatePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.housefinder.R
import com.example.housefinder.db.entities.AppDatabase
import com.example.housefinder.ui.common.HouseDateFormatter
import com.example.housefinder.ui.common.ListingImageLoader
import com.example.housefinder.ui.common.ListingInputOptions
import com.example.housefinder.ui.common.SessionManager
import com.example.housefinder.viewmodel.ProviderListingFormViewModel
import com.google.android.material.textfield.TextInputLayout
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.Calendar

@AndroidEntryPoint
class ProviderListingFormFragment : Fragment(R.layout.fragment_provider_listing_form) {

    private val viewModel: ProviderListingFormViewModel by viewModels()
    private val args: ProviderListingFormFragmentArgs by navArgs()

    private val imagePicker = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri == null) {
            return@registerForActivityResult
        }

        try {
            requireContext().contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        } catch (_: SecurityException) {
            // Some providers do not grant persistable permission; preview/save can still proceed.
        }

        viewModel.setCoverImage(uri.toString())
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val session = SessionManager(requireContext())
        val providerId = session.getUserId()
        if (providerId == null) {
            findNavController().navigate(R.id.loginFragment)
            return
        }

        viewLifecycleOwner.lifecycleScope.launch {
            val sessionUser = AppDatabase.getInstance(requireContext()).userDao().getById(providerId)
            if (sessionUser?.role != "PROVIDER") {
                Toast.makeText(requireContext(), "Provider access required", Toast.LENGTH_SHORT).show()
                findNavController().navigate(R.id.listingListFragment)
            }
        }

        val listingId = args.listingId

        val titleLayout = view.findViewById<TextInputLayout>(R.id.input_listing_title)
        val descriptionLayout = view.findViewById<TextInputLayout>(R.id.input_listing_description)
        val locationLayout = view.findViewById<TextInputLayout>(R.id.input_listing_location)
        val typeLayout = view.findViewById<TextInputLayout>(R.id.input_listing_type)
        val amenitiesLayout = view.findViewById<TextInputLayout>(R.id.input_listing_amenities)
        val priceLayout = view.findViewById<TextInputLayout>(R.id.input_listing_price)
        val depositLayout = view.findViewById<TextInputLayout>(R.id.input_listing_deposit)
        val availabilityLayout = view.findViewById<TextInputLayout>(R.id.input_listing_availability)
        val titleInput = view.findViewById<EditText>(R.id.edt_listing_title)
        val descriptionInput = view.findViewById<EditText>(R.id.edt_description)
        val priceInput = view.findViewById<EditText>(R.id.edt_listing_price)
        val locationInput = view.findViewById<AutoCompleteTextView>(R.id.edt_listing_location)
        val typeInput = view.findViewById<AutoCompleteTextView>(R.id.edt_listing_type)
        val amenitiesInput = view.findViewById<EditText>(R.id.edt_listing_amenities)
        val depositInput = view.findViewById<EditText>(R.id.edt_listing_deposit)
        val availabilityInput = view.findViewById<AutoCompleteTextView>(R.id.edt_listing_availability)
        val imageStatusText = view.findViewById<TextView>(R.id.txt_property_image_status)
        val uploadButton = view.findViewById<Button>(R.id.btnUpload)
        val saveButton = view.findViewById<Button>(R.id.btn_save_listing)
        val toolbar = view.findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.toolbar)

        toolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }

        locationInput.setAdapter(
            ArrayAdapter(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                ListingInputOptions.gaboroneAreas
            )
        )
        locationInput.threshold = 0
        locationInput.setRawInputType(InputType.TYPE_NULL)
        locationInput.setOnClickListener { locationInput.showDropDown() }
        locationInput.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                locationInput.showDropDown()
            }
        }
        locationInput.setOnTouchListener { _, _ ->
            locationInput.showDropDown()
            false
        }
        typeInput.setAdapter(
            ArrayAdapter(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                ListingInputOptions.roomTypeLabels
            )
        )
        typeInput.threshold = 0
        typeInput.setRawInputType(InputType.TYPE_NULL)
        typeInput.setOnClickListener { typeInput.showDropDown() }
        typeInput.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                typeInput.showDropDown()
            }
        }
        typeInput.setOnTouchListener { _, _ ->
            typeInput.showDropDown()
            false
        }
        availabilityInput.setRawInputType(InputType.TYPE_NULL)
        availabilityInput.setOnClickListener {
            showDatePicker { selectedDate ->
                availabilityInput.setText(selectedDate, false)
            }
        }

        uploadButton.setOnClickListener {
            imagePicker.launch(arrayOf("image/*"))
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.listing.collectLatest { listing ->
                if (listing != null) {
                    titleInput.setText(listing.title)
                    descriptionInput.setText(listing.description)
                    priceInput.setText(listing.price.toInt().toString())
                    locationInput.setText(listing.location, false)
                    typeInput.setText(ListingInputOptions.toDisplayType(listing.type), false)
                    amenitiesInput.setText(listing.amenities)
                    depositInput.setText(listing.depositAmount.toString())
                    availabilityInput.setText(HouseDateFormatter.toDisplayDate(listing.availabilityDate), false)
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.coverImage.collectLatest { path ->
                updateSelectedImageUi(path)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.saveResult.collectLatest { result ->
                when (result) {
                    is ProviderListingFormViewModel.SaveResult.Success -> {
                        Toast.makeText(requireContext(), result.message, Toast.LENGTH_SHORT).show()
                        findNavController().popBackStack()
                    }
                    is ProviderListingFormViewModel.SaveResult.Error -> {
                        Toast.makeText(requireContext(), result.message, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        viewModel.loadListing(listingId)

        saveButton.setOnClickListener {
            clearInputErrors(
                titleLayout,
                descriptionLayout,
                locationLayout,
                typeLayout,
                amenitiesLayout,
                priceLayout,
                depositLayout,
                availabilityLayout
            )
            imageStatusText.setTextColor(getColorCompat(R.color.register_text_light))

            val title = titleInput.text.toString().trim()
            val description = descriptionInput.text.toString().trim()
            val priceStr = priceInput.text.toString().trim()
            val location = locationInput.text.toString().trim()
            val typeLabel = typeInput.text.toString().trim()
            val type = ListingInputOptions.toStorageType(typeLabel)
            val amenities = amenitiesInput.text.toString().trim()
            val depositStr = depositInput.text.toString().trim()
            val availability = availabilityInput.text.toString().trim()

            val price = priceStr.toFloatOrNull()
            val deposit = depositStr.toIntOrNull()

            var hasError = false
            if (title.isBlank()) {
                titleLayout.error = getString(R.string.listing_title_required)
                hasError = true
            }
            if (description.isBlank()) {
                descriptionLayout.error = getString(R.string.listing_description_required)
                hasError = true
            }
            if (location.isBlank()) {
                locationLayout.error = getString(R.string.listing_location_required)
                hasError = true
            }
            if (type == null) {
                typeLayout.error = getString(R.string.listing_type_required)
                hasError = true
            }
            if (amenities.isBlank()) {
                amenitiesLayout.error = getString(R.string.listing_amenities_required)
                hasError = true
            }
            if (availability.isBlank()) {
                availabilityLayout.error = getString(R.string.listing_availability_required)
                hasError = true
            }
            if (price == null || price <= 0) {
                priceLayout.error = getString(R.string.listing_price_required)
                hasError = true
            }
            if (deposit == null || deposit <= 0) {
                depositLayout.error = getString(R.string.listing_deposit_required)
                hasError = true
            }
            if (viewModel.coverImage.value.isNullOrBlank()) {
                imageStatusText.text = getString(R.string.listing_image_required)
                imageStatusText.setTextColor(getColorCompat(R.color.error))
                hasError = true
            }
            if (hasError) {
                return@setOnClickListener
            }

            val resolvedType = type ?: run {
                typeLayout.error = getString(R.string.listing_type_required)
                return@setOnClickListener
            }
            val resolvedPrice = price ?: run {
                priceLayout.error = getString(R.string.listing_price_required)
                return@setOnClickListener
            }
            val resolvedDeposit = deposit ?: run {
                depositLayout.error = getString(R.string.listing_deposit_required)
                return@setOnClickListener
            }

            viewModel.saveListing(
                listingId = listingId,
                providerId = providerId,
                title = title,
                description = description,
                price = resolvedPrice,
                location = location,
                type = resolvedType,
                amenities = amenities,
                deposit = resolvedDeposit,
                availability = availability
            )
        }
    }

    private fun clearInputErrors(vararg layouts: TextInputLayout) {
        layouts.forEach { it.error = null }
    }

    private fun getColorCompat(colorRes: Int): Int {
        return androidx.core.content.ContextCompat.getColor(requireContext(), colorRes)
    }

    private fun updateSelectedImageUi(path: String?) {
        view?.let { root ->
            val imagePreview = root.findViewById<ImageView>(R.id.img_property_preview)
            val imagePlaceholder = root.findViewById<LinearLayout>(R.id.layout_property_placeholder)
            val imageStatusText = root.findViewById<TextView>(R.id.txt_property_image_status)

            val loaded = ListingImageLoader.bind(imagePreview, path)
            val hasImage = !path.isNullOrBlank() && loaded
            imagePlaceholder.visibility = if (hasImage) View.GONE else View.VISIBLE
            imageStatusText.text = if (hasImage) {
                getString(R.string.property_image_selected)
            } else {
                getString(R.string.property_image_none)
            }
            imageStatusText.setTextColor(getColorCompat(R.color.register_text_light))
        }
    }

    private fun showDatePicker(onDateSelected: (String) -> Unit) {
        val calendar = Calendar.getInstance()
        DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                val selected = String.format("%02d-%02d-%04d", dayOfMonth, month + 1, year)
                onDateSelected(selected)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }
}
