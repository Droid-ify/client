package com.looker.droidify.screen

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.Toolbar
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.CollapsingToolbarLayout
import com.looker.droidify.R

open class ScreenFragment : BaseFragment() {
    lateinit var toolbar: Toolbar
    lateinit var collapsingToolbar: CollapsingToolbarLayout
    lateinit var appBar: AppBarLayout

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment, container, false)
        this.toolbar = view.findViewById(R.id.toolbar)
        this.collapsingToolbar = view.findViewById(R.id.collapsing_toolbar)
        this.appBar = view.findViewById(R.id.appbar_layout)
        return view
    }
}
