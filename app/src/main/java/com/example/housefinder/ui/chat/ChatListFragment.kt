package com.example.housefinder.ui.chat

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.housefinder.R
import com.example.housefinder.db.entities.AppDatabase
import com.example.housefinder.ui.common.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import androidx.fragment.app.viewModels
import com.example.housefinder.viewmodel.ChatListViewModel

import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ChatListFragment : Fragment(R.layout.fragment_chat_list) {

    private val viewModel: ChatListViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val currentUserId = SessionManager(requireContext()).getUserId()
        if (currentUserId == null) {
            findNavController().navigate(R.id.loginFragment)
            return
        }

        val emptyState = view.findViewById<View>(R.id.layout_empty_chats)
        val recycler = view.findViewById<RecyclerView>(R.id.rv_conversations)

        val adapter = ChatConversationAdapter { item ->
            val action = ChatListFragmentDirections.actionChatListFragmentToChatThreadFragment(
                conversationId = item.conversationId,
                listingId = item.listingId,
                partnerId = item.partnerId
            )
            findNavController().navigate(action)
        }

        recycler.layoutManager = LinearLayoutManager(requireContext())
        recycler.adapter = adapter

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.conversations.collectLatest { sortedRows ->
                adapter.submitList(sortedRows)
                val showEmpty = sortedRows.isEmpty()
                emptyState.visibility = if (showEmpty) View.VISIBLE else View.GONE
                recycler.visibility = if (showEmpty) View.GONE else View.VISIBLE
            }
        }

        viewModel.loadConversations(currentUserId)
    }
}
