import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.viewModelScope
import com.ekeitho.uni.DslUnidirectionalViewModel
import com.ekeitho.uni.uniViewModelDSL
import com.jraska.livedata.test
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description

class UniTest {
    data class State(
        val pageNum: Int = 0,
        val sideEffectNum: Int = 0,
        val testNum: Int = 0,
    )

    sealed class Action {
        data class UpdatePageNum(val num: Int) : Action()
        data class SideEffectNum(val num: Int) : Action()
        object TestSend : Action()
        data class TestNum(val num: Int) : Action()
    }

    @get:Rule
    val instantTaskRule = InstantTaskExecutorRule()

    @OptIn(ExperimentalCoroutinesApi::class)
    @get:Rule
    var coroutinesTestRule = CoroutinesTestRule()

    @Test
    fun testWithMultipleSideEffects() {
        val vm = testUniViewModelDSL<State, Action>(State()) {
            effect { flow ->
                flow
                    .filterIsInstance<Action.UpdatePageNum>()
                    .map { Action.SideEffectNum(it.num + 3) }
            }

            effect { flow ->
                flow
                    .filterIsInstance<Action.TestSend>()
                    .map { Action.TestNum(1) }
            }

            reducer { action, state ->
                when (action) {
                    is Action.UpdatePageNum -> state.copy(pageNum = action.num)
                    is Action.SideEffectNum -> state.copy(sideEffectNum = action.num)
                    is Action.TestNum -> state.copy(testNum = action.num)
                    else -> state
                }
            }
        }

        val test = vm.liveDataState.test()

        vm.dispatch(Action.UpdatePageNum(10))
        vm.dispatch(Action.UpdatePageNum(5))
        vm.dispatch(Action.TestSend)

        // demonstrates that when we use side-effects state can multiple 2 or more times
        // ie) dispatching UpdatePageNum(10) with a side-effect listening to this action produces two emissions
        // 1st emission reducing this action & then 2nd coming from a side-effect that maps the SideEffectNum action

        test.assertValueHistory(
            State(10, 0),
            State(10, 13),

            State(5, 13),
            State(5, 8),
            State(5, 8),
            State(5, 8, 1)
        )
    }


    @Test
    fun testSideEffectTriggeringAnotherSideEffect() {
        val vm = testUniViewModelDSL<State, Action>(State()) {
            effect { flow ->
                flow
                    .filterIsInstance<Action.UpdatePageNum>()
                    .map { Action.SideEffectNum(it.num + 3) }
            }

            effect { flow ->
                flow
                    .filterIsInstance<Action.SideEffectNum>()
                    .map { Action.TestNum(it.num + 3) }
            }

            reducer { action, state ->
                when (action) {
                    is Action.UpdatePageNum -> state.copy(pageNum = action.num)
                    is Action.SideEffectNum -> state.copy(sideEffectNum = action.num)
                    is Action.TestNum -> state.copy(testNum = action.num)
                    else -> state
                }
            }
        }

        val test = vm.liveDataState.test()

        vm.dispatch(Action.UpdatePageNum(10))

        test.assertValueHistory(
            State(10, 0),
            State(10, 13),
            State(10, 13, 16),
        )
    }

    @Test
    fun testThousandsOfDispatches() {
        val vm = testUniViewModelDSL<State, Action>(State()) {

            reducer { action, state ->
                when (action) {
                    is Action.UpdatePageNum -> state.copy(pageNum = action.num)
                    else -> state
                }
            }
        }

        val test = vm.liveDataState.test()

        for (i in 0..10000) {
            vm.dispatch(Action.UpdatePageNum(i))
        }

        assert(test.valueHistory() == (0..10000).map { State(pageNum = it, 0) })
    }
}

private fun <State, Action> testUniViewModelDSL(
    emptyState: State,
    dispatcher: CoroutineDispatcher = Dispatchers.Main,
    lambda: DslUnidirectionalViewModel<State, Action>.() -> Unit,
) =
    uniViewModelDSL<State, Action>(emptyState, dispatcher, lambda)


@ExperimentalCoroutinesApi
class CoroutinesTestRule(
    val testDispatcher: TestCoroutineDispatcher = TestCoroutineDispatcher()
) : TestWatcher() {

    override fun starting(description: Description?) {
        super.starting(description)
        Dispatchers.setMain(testDispatcher)
    }

    override fun finished(description: Description?) {
        super.finished(description)
        Dispatchers.resetMain()
        testDispatcher.cleanupTestCoroutines()
    }
}