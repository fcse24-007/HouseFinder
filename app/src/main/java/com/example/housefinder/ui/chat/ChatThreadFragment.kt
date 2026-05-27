package com.example.housefinder.ui.chat

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.housefinder.R
import com.example.housefinder.ui.common.SessionManager
import com.example.housefinder.viewmodel.ChatThreadViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ChatThreadFragment : Fragment(R.layout.fragment_chat_thread) {

    private val viewModel: ChatThreadViewModel by viewModels()
    private val args: ChatThreadFragmentArgs by navArgs()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val currentUserId = SessionManager(requireContext()).getUserId()
        if (currentUserId == null) {
            findNavController().navigate(R.id.loginFragment)
            return
        }

        val conversationId = args.conversationId
        val listingId = args.listingId
        val partnerId = args.partnerId

        if (conversationId.isBlank() || listingId <= 0 || partnerId <= 0) {
            Toast.makeText(requireContext(), R.string.chat_open_error, Toast.LENGTH_SHORT).show()
            findNavController().popBackStack()
            return
        }

        val toolbar = view.findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        val messageInput = view.findViewById<EditText>(R.id.edt_message)
        val sendButton = view.findViewById<View>(R.id.btn_send_message)
        val recycler = view.findViewById<RecyclerView>(R.id.rv_chat_messages)
        val emptyState = view.findViewById<View>(R.id.layout_empty_chat)

        toolbar.setNavigationIcon(R.drawable.ic_back)
        toolbar.setNavigationOnClickListener { findNavController().popBackStack() }

        val adapter = ChatMessageAdapter(currentUserId)
        recycler.layoutManager = LinearLayoutManager(requireContext()).apply { stackFromEnd = true }
        recycler.adapter = adapter

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.partnerName.collectLatest { toolbar.title = it }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.listingTitle.collectLatest { toolbar.subtitle = it }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.messages.collectLatest { messages ->
                adapter.submitList(messages)
                val isEmpty = messages.isEmpty()
                emptyState.visibility = if (isEmpty) View.VISIBLE else View.GONE
                recycler.visibility = if (isEmpty) View.GONE else View.VISIBLE
                if (!isEmpty) {
                    recycler.scrollToPosition(messages.lastIndex)
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.error.collectLatest { message ->
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
                findNavController().popBackStack()
            }
        }

        viewModel.loadThread(conversationId, partnerId, listingId, currentUserId)

        sendButton.setOnClickListener {
            val text = messageInput.text.toString().trim()
            if (text.isBlank()) {
                return@setOnClickListener
            }
            viewModel.sendMessage(currentUserId, partnerId, listingId, conversationId, text)
            messageInput.text?.clear()
        }
    }
}
