package com.ekeitho.uni

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

abstract class UniViewModel<State, Action>(
    private val emptyState: State,
    private val coroutineDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ViewModel(), UnidirectionalViewModel<State, Action> {

    private val mutex = Mutex()
    private val stateLiveDate: MutableLiveData<State> = MutableLiveData()
    final override val actionFlow: MutableSharedFlow<Action> = MutableSharedFlow()

    init {
        for (sideEffect in sideEffects) {
            val flow = sideEffect.observeActionToAction(actionFlow)
            viewModelScope.launch {
                withContext(coroutineDispatcher) {
                    flow.collect { dispatch(it) }
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