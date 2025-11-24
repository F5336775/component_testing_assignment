package main.java.dev.ntziks.login

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material.Button
import androidx.compose.material.Checkbox
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.PasswordVisualTransformation
import main.java.dev.ntziks.login.viewmodel.LoginViewModel

@Composable
fun LoginScreen(
    viewModel: LoginViewModel,
    onNavigateHome: () -> Unit
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(state.navigateToHome) {
        if (state.navigateToHome) onNavigateHome()
    }

    Column {
        TextField(
            value = state.username,
            onValueChange = viewModel::onUsernameChanged,
            label = { Text("Username") }
        )
        TextField(
            value = state.password,
            onValueChange = viewModel::onPasswordChanged,
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation()
        )
        Row {
            Checkbox(
                checked = state.isRememberMe,
                onCheckedChange = viewModel::onRememberMeChanged
            )
            Text("Remember me")
        }
        if (state.errorMessage != null) {
            Text(state.errorMessage!!)
        }
        Button(
            enabled = state.isLoginEnabled,
            onClick = { viewModel.login() }
        ) {
            Text("Log in")
        }
    }
}
