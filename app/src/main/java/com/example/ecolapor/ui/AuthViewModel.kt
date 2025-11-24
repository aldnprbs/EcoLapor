package com.example.ecolapor.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ecolapor.data.repository.AuthRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class AuthViewModel : ViewModel() {
    private val repository = AuthRepository()
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    var authState by mutableStateOf<AuthState>(AuthState.Idle)
        private set

    fun login(email: String, pass: String) {
        if (email.isBlank() || pass.isBlank()) {
            authState = AuthState.Error("Email dan Password tidak boleh kosong")
            return
        }

        authState = AuthState.Loading
        viewModelScope.launch {
            val result = repository.login(email, pass)
            if (result.isSuccess) {
                val user = result.getOrNull()
                android.util.Log.d("AuthViewModel", "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                android.util.Log.d("AuthViewModel", "✅ LOGIN SUCCESSFUL")
                android.util.Log.d("AuthViewModel", "User ID: ${user?.uid}")
                android.util.Log.d("AuthViewModel", "Email: ${user?.email}")
                android.util.Log.d("AuthViewModel", "Display Name: ${user?.displayName}")
                android.util.Log.d("AuthViewModel", "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                authState = AuthState.Success(user)
            } else {
                android.util.Log.e("AuthViewModel", "❌ Login failed: ${result.exceptionOrNull()?.message}")
                authState = AuthState.Error(result.exceptionOrNull()?.message ?: "Login Gagal")
            }
        }
    }

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

    fun logout() {
        repository.logout()
        authState = AuthState.Idle
    }

    fun deleteAccount(onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val user = auth.currentUser
                if (user == null) {
                    onError("User tidak ditemukan")
                    return@launch
                }

                val userId = user.uid

                try {
                    val reportsSnapshot = firestore.collection("reports")
                        .whereEqualTo("userId", userId)
                        .get()
                        .await()

                    for (document in reportsSnapshot.documents) {
                        document.reference.delete().await()
                    }
                } catch (e: Exception) {
                    android.util.Log.e("AuthViewModel", "Failed to delete reports: ${e.message}")
                }

                user.delete().await()
                authState = AuthState.Idle
                onSuccess()

            } catch (e: Exception) {
                onError(e.message ?: "Gagal menghapus akun")
            }
        }
    }

    fun resetState() {
        authState = AuthState.Idle
    }
}

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    data class Success(val user: FirebaseUser?) : AuthState()
    data class Error(val message: String) : AuthState()
}