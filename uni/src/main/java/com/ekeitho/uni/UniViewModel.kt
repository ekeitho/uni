package com.ekeitho.uni

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

abstract class UniViewModel<State, Action>(
    private val emptyState: State,
) : ViewModel(), UnidirectionalViewModel<State, Action> {

    private val mutex = Mutex()
    private val stateLiveDate: MutableLiveData<State> = MutableLiveData()
    override val actionFlow: MutableSharedFlow<Action> = MutableSharedFlow()

    init {
        viewModelScope.launch {
            for (sideEffect in sideEffects) {
                sideEffect.observeActionToAction(actionFlow).collect {
                    dispatch(it)
                }
            }
        }
    }

    override fun dispatch(action: Action) {
        onDispatchLock(action)
    }

    private fun onDispatchLock(action: Action) {
        viewModelScope.launch {
            mutex.withLock {
                actionFlow.emit(action)
                stateLiveDate.value =
                    reduce(action, stateLiveDate.value ?: checkNotNull(emptyState))
            }
        }
    }
}