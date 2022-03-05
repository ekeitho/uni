package com.ekeitho.unidirectional

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.annotation.ExperimentalCoilApi
import coil.compose.rememberImagePainter
import com.ekeitho.uni.DslUnidirectionalViewModel
import com.ekeitho.unidirectional.ui.MainTheme
import com.ekeitho.unidirectional.wikipedia.Wiki
import com.ekeitho.unidirectional.wikipedia.WikiResponse
import kotlinx.coroutines.delay
import org.koin.androidx.compose.getViewModel

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val vm = getViewModel<DslUnidirectionalViewModel<Wiki.State, Wiki.Action>>()
            val state by vm.liveDataState.observeAsState(initial = Wiki.State())

            LaunchedEffect(Unit) {
                delay(1000)
                vm.dispatch(Wiki.Action.FetchRandomWikiAction)
            }

            MainTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val wikiResponse = state.wikiResponse
                    if (wikiResponse != null) {
                        WikiScreen(
                            wikiResponse = wikiResponse,
                            onButtonTap = {
                                vm.dispatch(Wiki.Action.FetchRandomWikiAction)
                            }
                        )
                    } else {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            CircularProgressIndicator(
                                color = Color.Black,
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalCoilApi::class)
@Composable
fun WikiScreen(
    wikiResponse: WikiResponse,
    onButtonTap: () -> Unit,
) {
    Column {
        Image(
            modifier = Modifier.fillMaxWidth().fillMaxHeight(.5F),
            contentScale = ContentScale.Fit,
            painter = rememberImagePainter(data = wikiResponse.originalimage.source),
            contentDescription = null,
        )

        LazyColumn(
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            item {
                Text(
                    modifier = Modifier.padding(16.dp),
                    text = wikiResponse.extract
                )
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }

            item {
                Button(onClick = { onButtonTap() }) {
                    Text(text = "Randomize Wiki!")
                }
            }
        }

    }
}