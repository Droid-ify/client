package com.looker.droidify.utility.extension

import androidx.fragment.app.Fragment
import com.looker.droidify.ScreenActivity

inline val Fragment.screenActivity: ScreenActivity
    get() = requireActivity() as ScreenActivity
