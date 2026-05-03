package com.example.housefinder.ui.help

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.housefinder.R
import com.google.android.material.appbar.MaterialToolbar

import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class HelpFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_help, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        view.findViewById<MaterialToolbar>(R.id.toolbar).setNavigationOnClickListener {
            (activity as? com.example.housefinder.MainActivity)?.findViewById<androidx.drawerlayout.widget.DrawerLayout>(R.id.drawer_layout)?.openDrawer(androidx.core.view.GravityCompat.START) }
    }
}
