package com.example.housefinder.ui.provider

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
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
import com.example.housefinder.ui.common.HouseDateFormatter
import com.example.housefinder.ui.common.ListingImageLoader
import com.example.housefinder.ui.common.SessionManager
import com.example.housefinder.viewmodel.ProviderListingFormViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

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
        val providerId = session.getUserId() ?: return

        val listingId = args.listingId

        val titleInput = view.findViewById<EditText>(R.id.edt_listing_title)
        val descriptionInput = view.findViewById<EditText>(R.id.edt_description)
        val priceInput = view.findViewById<EditText>(R.id.edt_listing_price)
        val locationInput = view.findViewById<EditText>(R.id.edt_listing_location)
        val depositInput = view.findViewById<EditText>(R.id.edt_listing_deposit)
        val availabilityInput = view.findViewById<EditText>(R.id.edt_listing_availability)
        val uploadButton = view.findViewById<Button>(R.id.btnUpload)
        val saveButton = view.findViewById<Button>(R.id.btn_save_listing)
        val backButton = view.findViewById<View>(R.id.btn_form_back)

        uploadButton.setOnClickListener {
            imagePicker.launch(arrayOf("image/*"))
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.listing.collectLatest { listing ->
                if (listing != null) {
                    titleInput.setText(listing.title)
                    descriptionInput.setText(listing.description)
                    priceInput.setText(listing.price.toInt().toString())
                    locationInput.setText(listing.location)
                    depositInput.setText(listing.depositAmount.toString())
                    availabilityInput.setText(HouseDateFormatter.toDisplayDate(listing.availabilityDate))
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
            val title = titleInput.text.toString().trim()
            val description = descriptionInput.text.toString().trim()
            val priceStr = priceInput.text.toString().trim()
            val location = locationInput.text.toString().trim()
            val depositStr = depositInput.text.toString().trim()
            val availability = availabilityInput.text.toString().trim()

            // Validate inputs
            if (title.isBlank() || priceStr.isBlank() || location.isBlank() ||
                depositStr.isBlank() || availability.isBlank()) {
                Toast.makeText(requireContext(), "Basic fields are required", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val price = priceStr.toFloatOrNull()
            val deposit = depositStr.toIntOrNull()

            if (price == null || price <= 0) {
                Toast.makeText(requireContext(), "Price must be a valid number > 0", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (deposit == null || deposit <= 0) {
                Toast.makeText(requireContext(), "Deposit must be a valid number > 0", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            viewModel.saveListing(
                listingId = listingId,
                providerId = providerId,
                title = title,
                description = description,
                price = price,
                location = location,
                deposit = deposit,
                availability = availability
            )
        }

        backButton.setOnClickListener {
            findNavController().popBackStack()
        }
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
        }
    }
}
