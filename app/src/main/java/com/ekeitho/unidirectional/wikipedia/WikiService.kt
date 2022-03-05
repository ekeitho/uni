package com.ekeitho.unidirectional.wikipedia

import com.squareup.moshi.JsonClass
import kotlinx.coroutines.flow.Flow
import retrofit2.http.GET

interface WikiService {
    @GET("page/random/summary")
    fun getRandomWiki(): Flow<WikiResponse>
}

@JsonClass(generateAdapter = true)
data class WikiResponse(
    val extract: String,
    val originalimage: WikiImage
)

@JsonClass(generateAdapter = true)
data class WikiImage(
    val source: String
)