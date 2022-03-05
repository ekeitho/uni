package com.ekeitho.uni

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

abstract class UniViewModel<State, Action> constructor(
    private val stateLiveDate: MutableLiveData<State> = MutableLiveData(),
    override val actionFlow: MutableSharedFlow<Action> = MutableSharedFlow()
) : ViewModel(), UnidirectionalViewModel<State, Action> {

    private val mutex = Mutex()

    init {
        viewModelScope.launch {
            for (sideEffect in sideEffects) {
                sideEffect.observeActionToAction(actionFlow).collect {
                    onAction(it)
                }
            }
        }
    }

    override fun onAction(action: Action) {
        onActionLock(action)
    }

    private fun onActionLock(action: Action) {
        viewModelScope.launch {
            mutex.withLock {
                actionFlow.emit(action)
                stateLiveDate.value =
                    reduce(action, stateLiveDate.value ?: checkNotNull(emptyState))
            }
        }
    }
}