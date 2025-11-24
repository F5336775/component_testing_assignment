package main.java.dev.ntziks.login.repository

interface AuthRepository {
    suspend fun login(username: String, password: String): Result<String>
}