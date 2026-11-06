package com.darchums.dalmiren

import com.darchums.dalmiren.data.repository.ParentingAssistantRepositoryImpl
import com.darchums.dalmiren.domain.model.MessageSender
import com.darchums.dalmiren.ui.screens.companion.CompanionViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CompanionViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: ParentingAssistantRepositoryImpl
    private lateinit var viewModel: CompanionViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = ParentingAssistantRepositoryImpl()
        viewModel = CompanionViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialMessages_containsAssistantWelcome() {
        val msgs = viewModel.messages.value
        assertEquals(1, msgs.size)
        assertEquals(MessageSender.ASSISTANT, msgs.first().sender)
        assertTrue(msgs.first().suggestedPrompts.isNotEmpty())
    }

    @Test
    fun sendMessage_addsUserMessageAndFetchesResponse() = runTest {
        viewModel.sendMessage("Help me with bedtime boundaries")
        testDispatcher.scheduler.advanceUntilIdle()

        val msgs = viewModel.messages.value
        assertEquals(3, msgs.size)
        assertEquals(MessageSender.USER, msgs[1].sender)
        assertEquals("Help me with bedtime boundaries", msgs[1].text)
        assertEquals(MessageSender.ASSISTANT, msgs[2].sender)
        assertFalse(viewModel.isLoading.value)
    }

    @Test
    fun clearChat_resetsMessageHistory() {
        viewModel.sendMessage("Test query")
        viewModel.clearChat()

        val msgs = viewModel.messages.value
        assertEquals(1, msgs.size)
        assertEquals(MessageSender.ASSISTANT, msgs.first().sender)
    }
}
