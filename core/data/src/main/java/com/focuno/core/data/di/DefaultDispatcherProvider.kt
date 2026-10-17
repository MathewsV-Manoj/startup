package com.focuno.core.data.di

import com.focuno.core.common.DispatcherProvider
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultDispatcherProvider @Inject constructor() : DispatcherProvider {
    override val default = Dispatchers.Default
    override val io = Dispatchers.IO
    override val main = Dispatchers.Main
}
