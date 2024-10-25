package com.example.booksearchapp

import android.annotation.SuppressLint
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.booksearchapp.ui.theme.BookSearchAppTheme
import androidx.compose.material3.*
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import androidx.compose.ui.text.style.TextAlign
import kotlinx.coroutines.launch


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Create the repository (assuming RetrofitInstance is already set up)
        val repository = BookRepository()

        // Create the ViewModelFactory with the repository
        val factory = BookViewModelFactory(repository)

        setContent {
            BookSearchAppTheme {
                // Pass the factory to the ViewModel
                val viewModel: BookViewModel = viewModel(factory = factory)
                BookSearchApp(viewModel)
            }
        }
    }
}

@SuppressLint("StateFlowValueCalledInComposition")
@Composable
fun BookSearchApp(viewModel: BookViewModel) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "search") {
        composable("search") {
            SearchScreen(navController, viewModel)
        }
        composable("detail/{bookId}") { backStackEntry ->
            val bookId = backStackEntry.arguments?.getString("bookId")
            DetailScreen(bookId = bookId, viewModel.books.value, navController)
        }
    }
}


@Composable
fun SearchScreen(navController: NavController, viewModel: BookViewModel) {
    val books by viewModel.books.collectAsState()
    val query by viewModel.query.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

    if (isLandscape) {
        Row(modifier = Modifier.fillMaxSize()) {
            // Left Pane: Search bar
            Column(modifier = Modifier.weight(0.4f).padding(16.dp)) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { newQuery -> viewModel.updateQuery(newQuery) },
                    label = { Text("Search for books by title or author") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    trailingIcon = {
                        IconButton(onClick = {
                            viewModel.searchBooks() // Perform search on click
                        }) {
                            Icon(Icons.Default.Search, contentDescription = "Search")
                        }
                    }
                )
            }

            // Right Pane: Book list or welcome text
            Box(modifier = Modifier.weight(0.6f).fillMaxSize().padding(16.dp)) {
                if (isLoading) {
                    CircularProgressIndicator()
                } else if (error != null) {
                    Text("Error: $error", color = Color.Red)
                } else if (books.isEmpty()) {
                    Text("Welcome to Yuanman's Google Books Search :)", style = MaterialTheme.typography.titleLarge)
                } else {
                    BookList(books = books, navController = navController)
                }
            }
        }
    } else {
        // Regular layout for portrait mode
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            OutlinedTextField(
                value = query,
                onValueChange = { newQuery -> viewModel.updateQuery(newQuery) },
                label = { Text("Search for books by title or author") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                trailingIcon = {
                    IconButton(onClick = {
                        viewModel.searchBooks() // Perform search on click
                    }) {
                        Icon(Icons.Default.Search, contentDescription = "Search")
                    }
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (isLoading) {
                CircularProgressIndicator()
            } else if (error != null) {
                Text("Error: $error", color = Color.Red)
            } else if (books.isNotEmpty()) {
                BookList(books = books, navController = navController)
            }
        }
    }
}


@Composable
fun BookList(books: List<Volume>, navController: NavController) {
    LazyColumn {
        items(books) { book ->
            BookItem(book = book) {
                navController.navigate("detail/${book.id}")
            }
            HorizontalDivider()
        }
    }
}

@Composable
fun BookItem(book: Volume, onItemClick: () -> Unit) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val imageUrl = book.volumeInfo.imageLinks?.thumbnail ?: book.volumeInfo.imageLinks?.smallThumbnail

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onItemClick() }
            .padding(8.dp)
    ) {
        if (imageUrl != null) {
            Image(
                painter = rememberAsyncImagePainter(
                    model = imageUrl,
                    contentScale = ContentScale.Crop
                ),
                contentDescription = "${book.volumeInfo.title} cover",
                modifier = Modifier
                    .size(80.dp)
                    .padding(end = 8.dp)
            )
        } else {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .padding(end = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("No Image")
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(text = book.volumeInfo.title, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = book.volumeInfo.authors?.joinToString(", ") ?: "Unknown Author",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}


@Composable
fun DetailScreen(bookId: String?, books: List<Volume>, navController: NavController) {
    var book by remember { mutableStateOf<Volume?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()
    val configuration = LocalConfiguration.current

    LaunchedEffect(bookId) {
        if (bookId != null) {
            coroutineScope.launch {
                try {
                    book = RetrofitInstance.api.getBookById(bookId)
                } catch (e: Exception) {
                    error = e.message
                } finally {
                    isLoading = false
                }
            }
        }
    }

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else if (error != null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Error: $error")
        }
    } else {
        // Detect orientation
        if (configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            // Two-pane layout for landscape mode
            Row(modifier = Modifier.fillMaxSize()) {
                // Left Pane: Book List
                Box(
                    modifier = Modifier
                        .weight(0.3f)
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    BookList(books = books, navController = navController) // Show list of books
                }

                // Right Pane: Book Details
                LazyColumn(
                    modifier = Modifier
                        .weight(0.7f)
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    item {
                        val imageUrl = book?.volumeInfo?.imageLinks?.thumbnail
                        if (imageUrl != null) {
                            Image(
                                painter = rememberAsyncImagePainter(imageUrl),
                                contentDescription = "${book?.volumeInfo?.title} cover",
                                modifier = Modifier
                                    .height(200.dp)
                                    .fillMaxWidth()
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(text = book?.volumeInfo?.title ?: "", style = MaterialTheme.typography.titleLarge)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Author(s): ${book?.volumeInfo?.authors?.joinToString(", ") ?: "Unknown"}",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        // Use the stripHtml extension function
                        Text(
                            text = book?.volumeInfo?.description?.stripHtml() ?: "No description available.",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Justify
                        )
                    }
                }
            }
        } else {
            // Single-pane layout for portrait mode
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                item {
                    val imageUrl = book?.volumeInfo?.imageLinks?.thumbnail
                    if (imageUrl != null) {
                        Image(
                            painter = rememberAsyncImagePainter(imageUrl),
                            contentDescription = "${book?.volumeInfo?.title} cover",
                            modifier = Modifier
                                .height(200.dp)
                                .fillMaxWidth()
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(text = book?.volumeInfo?.title ?: "", style = MaterialTheme.typography.titleLarge)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Author(s): ${book?.volumeInfo?.authors?.joinToString(", ") ?: "Unknown"}",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // Use the stripHtml extension function
                    Text(
                        text = book?.volumeInfo?.description?.stripHtml() ?: "No description available.",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Justify
                    )
                }
            }
        }
    }
}

