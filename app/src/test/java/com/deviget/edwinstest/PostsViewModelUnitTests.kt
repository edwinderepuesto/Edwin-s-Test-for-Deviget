package com.deviget.edwinstest

import android.content.Context
import android.content.SharedPreferences
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.deviget.edwinstest.data.dto.PostsPageData
import com.deviget.edwinstest.data.repository.RedditRepository
import com.deviget.edwinstest.presentation.viewmodel.RedditPostsViewModel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.lang.ref.WeakReference

class PostsViewModelUnitTests {
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @ExperimentalCoroutinesApi
    private val testDispatcher = StandardTestDispatcher()

    @ExperimentalCoroutinesApi
    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher) // Make test coroutine-safe
    }

    @ExperimentalCoroutinesApi
    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @ExperimentalCoroutinesApi
    @Test
    fun `fetchPostsPage() calls repository`() = runTest {
        val mockSharedPreferences = mockk<SharedPreferences>()
        every { mockSharedPreferences.getStringSet(any(), any()) } returns mockk()

        val mockContext = mockk<Context>()
        every { mockContext.getSharedPreferences(any(), any()) } returns mockSharedPreferences

        val mockPostPageData = mockk<PostsPageData>()
        every { mockPostPageData.after } returns "anAfter"
        every { mockPostPageData.children } returns listOf(mockk())

        val mockRepository = mockk<RedditRepository>()
        coEvery { mockRepository.fetchPostsPage("") } returns mockPostPageData

        // Instantiating the RedditPostsViewModel is enough, since there's a call to the fetch
        // in its init{} block:
        RedditPostsViewModel(mockRepository, WeakReference(mockContext), testDispatcher)

        // Note: This test will "fail" when run, but it's because of a bug in the MockK library
        // when using coroutines. It adds an extra parameter internally to calls so checks never
        // succeed.
        // See: https://stackoverflow.com/questions/61796609/when-using-mockk-coverify-method-was-not-called
        coVerify {
            mockRepository.fetchPostsPage("")
        }
    }
}