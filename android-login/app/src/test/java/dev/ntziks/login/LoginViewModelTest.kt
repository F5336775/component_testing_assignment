package dev.ntziks.login

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import main.java.dev.ntziks.login.NetworkMonitor
import main.java.dev.ntziks.login.repository.AuthRepository
import main.java.dev.ntziks.login.viewmodel.LoginViewModel
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private class FakeRepo(var result: Result<String> = Result.success("TOKEN")) : AuthRepository {
        var callCount = 0
        override suspend fun login(username: String, password: String): Result<String> {
            callCount++
            return result
        }
    }

    private class FakeNetwork(var online: Boolean) : NetworkMonitor {
        override fun isOnline(): Boolean = online
    }

    private lateinit var repo: FakeRepo
    private lateinit var network: FakeNetwork
    private lateinit var vm: LoginViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repo = FakeRepo()
        network = FakeNetwork(true)
        vm = LoginViewModel(repo, network, testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun LoginViewModel.updateCredentials() {
        onUsernameChanged("user")
        onPasswordChanged("pass")
    }

    //1. Validation enables/disables button.
    @Test
    fun `1 validation enables and disables button`() = runTest {
        assertThat(vm.state.value.isLoginEnabled).isFalse()

        vm.onUsernameChanged("user")
        assertThat(vm.state.value.isLoginEnabled).isFalse()

        vm.onPasswordChanged("pass")
        assertThat(vm.state.value.isLoginEnabled).isTrue()

        // lockout disables
        vm.login(); testDispatcher.scheduler.advanceUntilIdle()
        repo.result = Result.failure(Exception("x"))
        repeat(3) {
            vm.login()
            testDispatcher.scheduler.advanceUntilIdle()
        }
        assertThat(vm.state.value.isLockedOut).isTrue()
        assertThat(vm.state.value.isLoginEnabled).isFalse()
    }

    //2. Success → navigation event.
    @Test
    fun `2 success triggers navigation event`() = runTest {
        vm.updateCredentials()

        vm.login()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = vm.state.value
        assertThat(state.navigateToHome).isTrue()
        assertThat(state.token).isEqualTo("TOKEN")
        assertThat(state.failureCount).isZero()
    }

    //3. Error increments failure count.
    @Test
    fun `3 error increments failure count`() = runTest {
        repo.result = Result.failure(Exception("bad"))
        vm.updateCredentials()

        vm.login()
        testDispatcher.scheduler.advanceUntilIdle()
        assertThat(vm.state.value.failureCount).isEqualTo(1)

        vm.login()
        testDispatcher.scheduler.advanceUntilIdle()
        assertThat(vm.state.value.failureCount).isEqualTo(2)
    }

    //4. Lockout after 3 failures.
    @Test
    fun `4 lockout after 3 failures`() = runTest {
        repo.result = Result.failure(Exception("bad"))
        vm.updateCredentials()

        repeat(3) {
            vm.login()
            testDispatcher.scheduler.advanceUntilIdle()
        }

        val state = vm.state.value
        assertThat(state.failureCount).isEqualTo(3)
        assertThat(state.isLockedOut).isTrue()
        assertThat(state.errorMessage).isEqualTo("Too many attempts")
    }

    //5. Offline → show message, no service call.
    @Test
    fun `5 offline shows message and does not call service`() = runTest {
        network.online = false
        vm.updateCredentials()

        vm.login()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = vm.state.value
        assertThat(state.isOnline).isFalse()
        assertThat(state.errorMessage).isEqualTo("You are offline")
        assertThat(repo.callCount).isEqualTo(0)
    }

    //6. Remember me persists token.
    @Test
    fun `6 remember me persists token`() = runTest {
        vm.onRememberMeChanged(true)
        vm.updateCredentials()

        vm.login()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = vm.state.value
        assertThat(state.token).isEqualTo("TOKEN")
        assertThat(state.isRememberMe).isTrue()
        // in real app, AuthRepository could persist token when isRememberMe is true
    }
}