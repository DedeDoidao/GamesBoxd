package com.example.gamesboxd.ui.conta

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.gamesboxd.R
import com.example.gamesboxd.databinding.FragmentContaBinding

class Conta : Fragment() {
    private lateinit var binding: FragmentContaBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentContaBinding.inflate(inflater,container, false)
        return binding.root
    }
}