package com.ekeitho.uni

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * [DslMarker] for the uni ViewModel DSL. It keeps DSL blocks from accidentally reaching
 * an outer DSL's receiver if they are ever nested.
 */
@DslMarker
annotation class UniDsl

internal const val MISSING_REDUCER_MESSAGE =
  "No reducer set. Provide one with reducer { action, state -> ... } in the uniViewModelDSL block."

/**
 * Builds a [DslUnidirectionalViewModel] from an [emptyState] and a DSL block where you
 * declare your [DslUnidirectionalViewModel.reducer] and any [DslUnidirectionalViewModel.effect]s.
 * Effects are collected on [coroutineDispatcher], which defaults to [Dispatchers.IO].
 *
 * @throws IllegalStateException if the block does not set a reducer.
 */
fun <State : UniState, Action : UniAction> uniViewModelDSL(
  emptyState: State,
  coroutineDispatcher: CoroutineDispatcher = Dispatchers.IO,
  lambda: DslUnidirectionalViewModel<State, Action>.() -> Unit,
): DslUnidirectionalViewModel<State, Action> {
  val vm = DslUnidirectionalViewModel<State, Action>(emptyState)

  lambda(vm)
  check(vm.hasReducer()) { MISSING_REDUCER_MESSAGE }

  return vm.apply {
    for (sideEffect in sideEffects) {
      val flow = sideEffect.observeActionToAction(actionFlow)
      viewModelScope.launch {
        withContext(coroutineDispatcher) {
          flow.collect { dispatch(it) }
        }
      }
    }
  }
}

/**
 * This version exists in case you want to return your own type that extends from
 * [DslUnidirectionalViewModel]. Returning your own type may be useful when injecting unique
 * dependencies in your tree.
 *
 * @throws IllegalStateException if the block does not set a reducer.
 */
fun <State : UniState, Action : UniAction, DSL : DslUnidirectionalViewModel<State, Action>> uniViewModelDSL(
  customVm: DSL,
  coroutineDispatcher: CoroutineDispatcher = Dispatchers.IO,
  lambda: DSL.() -> Unit,
): DSL {

  lambda(customVm)
  check(customVm.hasReducer()) { MISSING_REDUCER_MESSAGE }

  return customVm.apply {
    for (sideEffect in sideEffects) {
      val flow = sideEffect.observeActionToAction(actionFlow)
      viewModelScope.launch {
        withContext(coroutineDispatcher) {
          flow.collect { dispatch(it) }
        }
      }
    }
  }
}

/**
 * Class is open in case you need unique types that extend from [DslUnidirectionalViewModel] which
 * may be useful when wanting unique types in your DI tree.
 */
@UniDsl
open class DslUnidirectionalViewModel<State : UniState, Action : UniAction> constructor(
  private val emptyState: State,
) : ViewModel(), UnidirectionalViewModel<State, Action> {

  private val mutex = Mutex()
  private var reducer: ((action: Action, state: State) -> State)? = null
  private var mSideEffects: MutableList<SideEffect<Action>> = mutableListOf()

  override val actionFlow: MutableSharedFlow<Action> = MutableSharedFlow()
  private val mLiveDataState: MutableLiveData<State> = MutableLiveData()
  override val liveDataState: LiveData<State> = mLiveDataState

  /**
   * Registers a side effect: a function that observes the action stream and maps it into new
   * actions, which is where async work such as network or database calls lives. Can be called
   * multiple times to register more than one effect.
   */
  fun effect(lambda: (actionFlow: Flow<Action>) -> Flow<Action>) {
    mSideEffects.add(object : SideEffect<Action> {
      override fun observeActionToAction(actionFlow: Flow<Action>): Flow<Action> {
        return lambda(actionFlow)
      }
    })
  }

  /**
   * Sets the pure reducer that turns an action and the current state into the next state. A
   * reducer is required; calling this more than once replaces the previous one.
   */
  fun reducer(lambda: ((action: Action, state: State) -> State)) {
    reducer = lambda
  }

  internal fun hasReducer(): Boolean = reducer != null

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
    val reduce = checkNotNull(reducer) { MISSING_REDUCER_MESSAGE }
    return reduce(action, state)
  }

}