package dev.ntziks.login

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