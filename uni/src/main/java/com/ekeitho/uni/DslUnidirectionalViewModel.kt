package com.ekeitho.uni

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

fun <State, Action> uniViewModelDSL(
    emptyState: State,
    lambda: DslUnidirectionalViewModel<State, Action>.() -> Unit
): DslUnidirectionalViewModel<State, Action> {
    val vm = DslUnidirectionalViewModel<State, Action>(emptyState)

    lambda(vm)

    return vm.apply {
        for (sideEffect in sideEffects) {
            val flow = sideEffect.observeActionToAction(actionFlow)
            viewModelScope.launch {
                flow.collect { dispatch(it) }
            }
        }
    }
}

class DslUnidirectionalViewModel<State, Action> constructor(
    private val emptyState: State,
) : ViewModel(), UnidirectionalViewModel<State, Action> {

    private val mutex = Mutex()
    private var reducer: ((action: Action, state: State) -> State)? = null
    private var mSideEffects: MutableList<SideEffect<Action>> = mutableListOf()

    override val actionFlow: MutableSharedFlow<Action> = MutableSharedFlow()
    private val mLiveDataState: MutableLiveData<State> = MutableLiveData()
    override val liveDataState: LiveData<State> = mLiveDataState

    fun effect(lambda: (actionFlow: Flow<Action>) -> Flow<Action>) {
        mSideEffects.add(object : SideEffect<Action> {
            override fun observeActionToAction(actionFlow: Flow<Action>): Flow<Action> {
                return lambda(actionFlow)
            }
        })
    }

    fun reducer(lambda: ((action: Action, state: State) -> State)) {
        reducer = lambda
    }

    override var sideEffects: List<SideEffect<Action>> = mSideEffects

    override fun dispatch(action: Action) {
        viewModelScope.launch {
            mutex.withLock {
                actionFlow.emit(action)
                mLiveDataState.value =
                    reduce(action, mLiveDataState.value ?: checkNotNull(emptyState))
            }
        }
    }

    override fun reduce(action: Action, state: State): State {
        return reducer?.let { it(action, state) } ?: state
    }

}