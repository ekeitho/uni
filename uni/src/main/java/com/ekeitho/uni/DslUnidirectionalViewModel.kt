package com.ekeitho.uni

import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
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
        viewModelScope.launch {
            for (sideEffect in sideEffects) {
                sideEffect.observeActionToAction(actionFlow).collect {
                    onAction(it)
                }
            }
        }
    }
}

class DslUnidirectionalViewModel<State, Action> constructor(
    override val emptyState: State,
    private val stateLiveDate: MutableLiveData<State> = MutableLiveData(),
    override val actionFlow: MutableSharedFlow<Action> = MutableSharedFlow()
) : ViewModel(), UnidirectionalViewModel<State, Action> {

    private val mutex = Mutex()
    private var reducer: ((action: Action, state: State) -> State)? = null
    private var mSideEffects: MutableList<SideEffect<Action>> = mutableListOf()

    fun effect(lambda: (actionFlow: Flow<Action>) -> Flow<Action>) {
        mSideEffects.add(object : SideEffect<Action> {
            override fun observeActionToAction(actionFlow: Flow<Action>): Flow<Action> {
                return lambda(actionFlow)
            }
        })
    }

    // i have to create the object first for DSL style & then allow to override
    fun reducer(lambda: ((action: Action, state: State) -> State)) {
        reducer = lambda
    }

    override var sideEffects: List<SideEffect<Action>> = mSideEffects

    override fun onAction(action: Action) {
        viewModelScope.launch {
            mutex.withLock {
                actionFlow.emit(action)
                stateLiveDate.value =
                    reduce(action, stateLiveDate.value ?: checkNotNull(emptyState))
            }
        }
    }

    override fun reduce(action: Action, state: State): State {
        return reducer?.let { it(action, state) } ?: state
    }

    @Composable
    override fun observeAsState(): androidx.compose.runtime.State<State> {
        return stateLiveDate.observeAsState(initial = emptyState)
    }
}