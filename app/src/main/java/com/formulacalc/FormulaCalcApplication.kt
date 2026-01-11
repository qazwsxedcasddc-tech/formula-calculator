package com.formulacalc

import android.app.Application
import com.formulacalc.util.AppLogger

class FormulaCalcApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AppLogger.init(this)
    }
}
