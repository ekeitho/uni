package com.ekeitho.unidirectional.wikipedia

import com.ekeitho.uni.uniViewModelDSL
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map


data class Wiki(
    private val wikiService: WikiService,
) {

    data class State(val wikiResponse: WikiResponse? = null)
    sealed class Action {
        object FetchRandomWikiAction : Action()
        data class WikiResponseAction(val wikiResponse: WikiResponse) : Action()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun getViewModel() =
        uniViewModelDSL<State, Action>(State()) {
            effect { flow ->
                flow.flatMapLatest { action ->
                    when (action) {
                        Action.FetchRandomWikiAction ->
                            wikiService.getRandomWiki().map { Action.WikiResponseAction(it) }
                        else -> emptyFlow()
                    }
                }
            }

            reducer { action, state ->
                when (action) {
                    is Action.WikiResponseAction -> state.copy(wikiResponse = action.wikiResponse)
                    is Action.FetchRandomWikiAction -> state.copy(wikiResponse = null)
                }
            }
        }

}