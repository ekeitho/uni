package com.ekeitho.uni

import androidx.compose.runtime.Composable
import kotlinx.coroutines.flow.*

interface UnidirectionalViewModel<State, Action> {
    val emptyState: State
    val sideEffects: List<SideEffect<Action>>
    val actionFlow: SharedFlow<Action>
    @Composable
    fun observeAsState(): androidx.compose.runtime.State<State>
    fun onAction(action: Action)
    fun reduce(action: Action, state: State): State
}

interface SideEffect<Action> {
    fun observeActionToAction(actionFlow: Flow<Action>): Flow<Action>
}