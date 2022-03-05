package com.ekeitho.uni

import androidx.lifecycle.LiveData
import kotlinx.coroutines.flow.*

interface UnidirectionalViewModel<State, Action> {
    val sideEffects: List<SideEffect<Action>>
    val actionFlow: SharedFlow<Action>
    val liveDataState: LiveData<State>
    fun dispatch(action: Action)
    fun reduce(action: Action, state: State): State
}

interface SideEffect<Action> {
    fun observeActionToAction(actionFlow: Flow<Action>): Flow<Action>
}