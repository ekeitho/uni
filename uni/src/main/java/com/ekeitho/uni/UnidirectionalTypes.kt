package com.ekeitho.uni

import androidx.lifecycle.LiveData
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*

/**
 * Marker interface for a screen's immutable state. Implement it on the single
 * [data class] that represents everything your screen needs to render.
 */
interface UniState

/**
 * Marker interface for the actions a screen can process. Implement it on a
 * [sealed class] so reducers stay exhaustive and adding a new action is a
 * compile error until it is handled.
 */
interface UniAction

interface UnidirectionalViewModel<State : UniState, Action : UniAction> {
    val sideEffects: List<SideEffect<Action>>
    val actionFlow: SharedFlow<Action>
    val liveDataState: LiveData<State>
    fun dispatch(action: Action)
    fun reduce(action: Action, state: State): State
}

interface SideEffect<Action : UniAction> {
    /**
     * The [CoroutineDispatcher] this effect's flow is collected on. Defaults to
     * [Dispatchers.IO] so async work such as network or database calls runs off the
     * main thread. Override it to pin a specific effect to a specific thread.
     */
    val dispatcher: CoroutineDispatcher get() = Dispatchers.IO
    fun observeActionToAction(actionFlow: Flow<Action>): Flow<Action>
}