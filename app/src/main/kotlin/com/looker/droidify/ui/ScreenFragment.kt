package com.looker.droidify.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.android.material.appbar.MaterialToolbar
import com.looker.droidify.databinding.FragmentBinding

open class ScreenFragment : Fragment() {
    private var _fragmentBinding: FragmentBinding? = null
    val fragmentBinding get() = _fragmentBinding!!
    val toolbar: MaterialToolbar get() = fragmentBinding.toolbar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _fragmentBinding = FragmentBinding.inflate(layoutInflater)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = fragmentBinding.root

    open fun onBackPressed(): Boolean = false

    override fun onDestroyView() {
        super.onDestroyView()
        _fragmentBinding = null
    }
}
