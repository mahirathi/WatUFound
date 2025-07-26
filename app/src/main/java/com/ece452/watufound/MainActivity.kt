package com.ece452.watufound

import android.content.ContentValues.TAG
import android.content.Intent
import android.os.Bundle
import android.util.Log
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.ece452.watufound.databinding.ActivityMainBinding
import com.ece452.watufound.ui.login.LoginActivity
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuth.AuthStateListener
import com.google.firebase.firestore.firestore

object BottomNavManager {
    fun setup(
        context: AppCompatActivity,
        navView: BottomNavigationView,
        navController: NavController
    ) {
        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.navigation_feed,
                R.id.navigation_post,
                R.id.navigation_messages,
                R.id.navigation_profile,
            )
        )

        navView.setupWithNavController(navController)
    }
}

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val db = Firebase.firestore
    private lateinit var auth: FirebaseAuth
    private lateinit var authStateListener: AuthStateListener
    private var isAppInitialized = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "MainActivity onCreate started")

        auth = FirebaseAuth.getInstance()
        Log.d(TAG, "Firebase Auth initialized, current user: ${auth.currentUser?.email ?: "null"}")
        
        // Check if user is already authenticated
        val currentUser = auth.currentUser
        if (currentUser != null) {
            Log.d(TAG, "User already authenticated: ${currentUser.email}")
            initializeApp()
            isAppInitialized = true
        } else {
            Log.d(TAG, "No authenticated user found, setting up auth listener")
        }
        
        // Set up auth state listener to handle authentication state changes
        authStateListener = AuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            Log.d(TAG, "Auth state changed - User: ${user?.email ?: "null"}, isAppInitialized: $isAppInitialized")
            
            if (user == null) {
                // User is signed out, redirect to login
                Log.d(TAG, "User is signed out, redirecting to login")
                if (!isFinishing) {
                    startActivity(Intent(this, LoginActivity::class.java))
                    finish()
                }
                return@AuthStateListener
            } else {
                // User is signed in, proceed with app initialization
                Log.d(TAG, "User is signed in: ${user.email}")
                if (!isAppInitialized) {
                    Log.d(TAG, "Initializing app for user: ${user.email}")
                    initializeApp()
                    isAppInitialized = true
                } else {
                    Log.d(TAG, "App already initialized, skipping initialization")
                }
            }
        }

        // Add the auth state listener
        auth.addAuthStateListener(authStateListener)
        Log.d(TAG, "Auth state listener added")
    }

    private fun initializeApp() {
        Log.d(TAG, "initializeApp called - setting up UI")
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navView: BottomNavigationView = binding.navView
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment_activity_main) as NavHostFragment
        val navController = navHostFragment.navController

        BottomNavManager.setup(this, navView, navController)

        navView.setOnItemSelectedListener { item ->
                    val handled = androidx.navigation.ui.NavigationUI.onNavDestinationSelected(item, navController)
                    if (handled) {
                        navController.navigate(item.itemId)
                    }
                    handled
                }
        Log.d(TAG, "App initialization completed")
    }

    override fun onSupportNavigateUp(): Boolean {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment_activity_main) as NavHostFragment
        val navController = navHostFragment.navController
        return navController.navigateUp() || super.onSupportNavigateUp()
    }

    override fun onDestroy() {
        super.onDestroy()
        // Remove the auth state listener to prevent memory leaks
        auth.removeAuthStateListener(authStateListener)
    }
}