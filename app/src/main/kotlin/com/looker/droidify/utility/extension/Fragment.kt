package com.looker.droidify.utility.extension

import androidx.fragment.app.Fragment
import com.looker.droidify.MainActivity

inline val Fragment.screenActivity: MainActivity
    get() = requireActivity() as MainActivity
