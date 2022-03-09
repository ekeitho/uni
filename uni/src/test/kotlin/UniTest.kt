import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.ekeitho.uni.uniViewModelDSL
import com.jraska.livedata.test
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
        data class TestSend(val p : Int): Action()
        data class TestNum(val num: Int): Action()
    }

    @get:Rule
    val instantTaskRule = InstantTaskExecutorRule()

    @get:Rule
    var coroutinesTestRule = CoroutinesTestRule()


    @Test
    fun testReduce_viewModel() {
        val vm = uniViewModelDSL<State, Action>(State()) {
            effect { flow ->
                flow
                    .filterIsInstance<Action.UpdatePageNum>()
                    .map { action ->
                        Action.SideEffectNum(action.num + 3)
                    }
            }

            effect { flow ->
                flow
                    .filterIsInstance<Action.TestSend>()
                    .map {
                        Action.TestNum(1)
                    }
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

        //assert(vm.sideEffects.size == 2)

        vm.dispatch(Action.UpdatePageNum(10))
        test.assertValue(State(10, 13))

        vm.dispatch(Action.UpdatePageNum(5))
        test.assertValue(State(5, 8))

        vm.dispatch(Action.TestSend(0))
        test.assertValue(State(5, 8, 1))
    }
}


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