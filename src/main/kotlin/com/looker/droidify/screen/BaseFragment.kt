package com.looker.droidify.screen

import androidx.fragment.app.Fragment

open class BaseFragment : Fragment() {
    val screenActivity: ScreenActivity
        get() = requireActivity() as ScreenActivity

    open fun onBackPressed(): Boolean = false
}
