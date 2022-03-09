![uni github actions](https://github.com/ekeitho/uni/actions/workflows/android.yml/badge.svg)

# uni
Unidirectional "Flow" architecture. Main intention of usage is for Android.

An example usage
```kotlin

data class Wiki(private val wikiService: WikiService) {

    data class State(val wikiResponse: WikiResponse? = null)
    sealed class Action {
        object FetchRandomWikiAction : Action()
        data class WikiResponseAction(val wikiResponse: WikiResponse) : Action()
    }
    
    fun getViewModel() =
        uniViewModelDSL<State, Action>(State()) {
            effect { flow ->
                flow
                    .filterIsInstance<Action.FetchRandomWikiAction>()
                    .flatMapLatest {
                        wikiService.getRandomWiki().map { Action.WikiResponseAction(it) }
                    }
            }

            reducer { action, state ->
                when (action) {
                    is Action.WikiResponseAction -> state.copy(wikiResponse = action.wikiResponse)
                    is Action.FetchRandomWikiAction -> state.copy(wikiResponse = null)
                }
            }
        }
}
```

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.ekeitho.uni/uni/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.ekeitho.uni/uni)

To use in your project:

```
implementation "com.ekeitho.uni:uni:$latest-version"
```
