package com.example.ecolapor.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ecolapor.data.repository.AuthRepository
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.launch

class AuthViewModel : ViewModel() {
    private val repository = AuthRepository()

    var authState by mutableStateOf<AuthState>(AuthState.Idle)
        private set

    // Fungsi Login
    fun login(email: String, pass: String) {
        if (email.isBlank() || pass.isBlank()) {
            authState = AuthState.Error("Email dan Password tidak boleh kosong")
            return
        }

        authState = AuthState.Loading
        viewModelScope.launch {
            val result = repository.login(email, pass)
            authState = if (result.isSuccess) {
                AuthState.Success(result.getOrNull())
            } else {
                AuthState.Error(result.exceptionOrNull()?.message ?: "Login Gagal")
            }
        }
    }

    // Fungsi Register
    fun register(name: String, email: String, pass: String) {
        if (name.isBlank() || email.isBlank() || pass.isBlank()) {
            authState = AuthState.Error("Semua data harus diisi")
            return
        }

        authState = AuthState.Loading
        viewModelScope.launch {
            val result = repository.register(name, email, pass)
            authState = if (result.isSuccess) {
                AuthState.Success(result.getOrNull())
            } else {
                AuthState.Error(result.exceptionOrNull()?.message ?: "Gagal Mendaftar")
            }
        }
    }

    // Reset status agar tidak stuck
    fun resetState() {
        authState = AuthState.Idle
    }
}

// Status-status yang mungkin terjadi
sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    data class Success(val user: FirebaseUser?) : AuthState()
    data class Error(val message: String) : AuthState()
}