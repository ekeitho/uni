package com.ekeitho.unidirectional

import android.app.Application
import com.ekeitho.unidirectional.network.FlowCallAdapterFactory
import com.ekeitho.unidirectional.wikipedia.Wiki
import com.ekeitho.unidirectional.wikipedia.WikiService
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.startKoin
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

class UnidirectionalApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidContext(this@UnidirectionalApplication)
            modules(appModule)
        }
    }

    private val appModule = module {

        single<WikiService> {
            Retrofit.Builder()
                .baseUrl("https://en.wikipedia.org/api/rest_v1/")
                .addCallAdapterFactory(FlowCallAdapterFactory.create())
                .addConverterFactory(MoshiConverterFactory.create())
                .build()
                .create(WikiService::class.java)
        }

        viewModel { Wiki(get()).getViewModel() }
    }
}