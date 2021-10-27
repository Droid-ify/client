package com.looker.droidify.screen

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.Toolbar
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.CollapsingToolbarLayout
import com.looker.droidify.databinding.FragmentBinding

open class ScreenFragment : BaseFragment() {
    lateinit var fragmentBinding: FragmentBinding

    lateinit var toolbar: Toolbar
    lateinit var collapsingToolbar: CollapsingToolbarLayout
    lateinit var appBar: AppBarLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fragmentBinding = FragmentBinding.inflate(layoutInflater)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        this.toolbar = fragmentBinding.toolbar
        this.collapsingToolbar = fragmentBinding.collapsingToolbar
        this.appBar = fragmentBinding.appbarLayout
        return fragmentBinding.root
    }
}
