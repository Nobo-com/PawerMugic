package com.example.viewmodel

import android.app.Application
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URL

class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val context = application.applicationContext

    private val _currentUser = MutableStateFlow<FirebaseUser?>(auth.currentUser)
    val currentUser: StateFlow<FirebaseUser?> = _currentUser.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _deviceInfo = MutableStateFlow<String>("")
    val deviceInfo: StateFlow<String> = _deviceInfo.asStateFlow()

    private val _ipAddress = MutableStateFlow<String>("Fetching...")
    val ipAddress: StateFlow<String> = _ipAddress.asStateFlow()

    init {
        auth.addAuthStateListener { firebaseAuth ->
            _currentUser.value = firebaseAuth.currentUser
            if (firebaseAuth.currentUser != null) {
                fetchDeviceAndIpInfo()
            }
        }
    }

    fun getGoogleSignInClient(): GoogleSignInClient {
        // You MUST replace "YOUR_WEB_CLIENT_ID" with the actual Web Client ID from Firebase Console -> Project Settings -> General -> Web App
        // Since google-services.json did not contain oauth_client, you must manually add the Web Client ID here or re-download google-services.json after enabling Google Sign In in Firebase Authentication.
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken("374513992367-n5kercp4pbdc9h6ctjcehcctfc7f3r7u.apps.googleusercontent.com") // Usually ends with apps.googleusercontent.com
            .requestEmail()
            .build()
        return GoogleSignIn.getClient(context, gso)
    }

    fun handleSignInResult(completedTask: Task<GoogleSignInAccount>) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                val account = completedTask.getResult(ApiException::class.java)
                val idToken = account.idToken
                if (idToken != null) {
                    firebaseAuthWithGoogle(idToken)
                } else {
                    _errorMessage.value = "Failed to retrieve ID token."
                    _isLoading.value = false
                }
            } catch (e: ApiException) {
                _errorMessage.value = "Google sign in failed: ${e.message} (Status code: ${e.statusCode})"
                _isLoading.value = false
            }
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                _isLoading.value = false
                if (task.isSuccessful) {
                    _currentUser.value = auth.currentUser
                } else {
                    _errorMessage.value = "Firebase auth failed: ${task.exception?.message}"
                }
            }
    }

    fun signOut() {
        auth.signOut()
        getGoogleSignInClient().signOut()
        _currentUser.value = null
    }

    fun setErrorMessage(message: String?) {
        _errorMessage.value = message
    }

    fun setLoading(loading: Boolean) {
        _isLoading.value = loading
    }

    private fun fetchDeviceAndIpInfo() {
        val manufacturer = Build.MANUFACTURER
        val model = Build.MODEL
        val osVersion = Build.VERSION.RELEASE
        _deviceInfo.value = "Device: ${manufacturer.capitalize()} $model\nOS: Android $osVersion"

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val ip = URL("https://api.ipify.org").readText()
                withContext(Dispatchers.Main) {
                    _ipAddress.value = ip
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _ipAddress.value = "Unknown"
                }
            }
        }
    }
}
