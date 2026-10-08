package com.example.mybudgettree

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.lifecycleScope
import com.example.mybudgettree.database.entries.User
import com.example.mybudgettree.database.managers.shared.DeleteReturnStatus
import com.example.mybudgettree.database.managers.shared.UpdateReturnInfo
import com.example.mybudgettree.database.managers.shared.UpdateReturnStatus
import com.google.android.material.button.MaterialButton
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.materialswitch.MaterialSwitch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import android.text.InputType

class EditProfileActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "EditProfileActivity"
    }

    private var cameraFile: File? = null

    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) savePhotoFromUri(uri)
    }
    private val takePicture = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        val file = cameraFile
        if (success && file != null) savePhotoFromFile(file)
    }
    private val requestCamera = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) launchCamera() else Toast.makeText(this, R.string.camera_permission_needed, Toast.LENGTH_SHORT).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (UserSession.currentUser == null) {
            startActivity(Intent(this, WelcomeActivity::class.java))
            finish()
            return
        }

        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = true
        setContentView(R.layout.activity_edit_profile)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.editProfileRoot)) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(top = bars.top, bottom = bars.bottom)
            insets
        }

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }
        findViewById<ImageButton>(R.id.btnHeaderBell).setOnClickListener {
            startActivity(Intent(this, NotificationsActivity::class.java))
        }
        findViewById<View>(R.id.btnChangePhoto).setOnClickListener { showPhotoChooser() }
        findViewById<MaterialButton>(R.id.btnUpdateProfile).setOnClickListener { saveProfile() }
        findViewById<MaterialButton>(R.id.btnDeleteAccount).setOnClickListener { confirmDeleteAccount() }
        MainNavigation.bind(this, MainNavigation.Tab.PROFILE)
        bindFields()
    }

    override fun onResume() {
        super.onResume()
        bindPhotoAndName()
    }

    private fun bindFields() {
        val user = UserSession.currentUser ?: return
        findViewById<EditText>(R.id.etDisplayName).setText(user.displayName)
        findViewById<EditText>(R.id.etPhone).setText(user.phoneNumber)
        findViewById<EditText>(R.id.etEmail).setText(user.email)
        findViewById<TextView>(R.id.tvCurrency).text = user.currency
        findViewById<MaterialSwitch>(R.id.switchPush).isChecked =
            ProfilePreferences.isPushEnabled(this, user.uid)
        bindPhotoAndName()
    }

    private fun bindPhotoAndName() {
        val user = UserSession.currentUser ?: return
        findViewById<TextView>(R.id.tvEditName).text = user.displayName
        findViewById<TextView>(R.id.tvEditId).text = getString(R.string.profile_id, user.email)
        val photo = findViewById<ShapeableImageView>(R.id.ivEditPhoto)
        val app = application as BudgetTreeApplication
        lifecycleScope.launch { ProfilePhoto.bind(photo, ProfilePhoto.load(app, user)) }
    }

    private fun saveProfile() {
        val user = UserSession.currentUser ?: return
        val displayName = findViewById<EditText>(R.id.etDisplayName).text?.toString()?.trim().orEmpty()
        val phone = findViewById<EditText>(R.id.etPhone).text?.toString()?.trim().orEmpty()
        val email = findViewById<EditText>(R.id.etEmail).text?.toString()?.trim().orEmpty()
        val pushEnabled = findViewById<MaterialSwitch>(R.id.switchPush).isChecked
        if (displayName.isBlank() || phone.isBlank() || email.isBlank()) {
            Toast.makeText(this, R.string.signup_fields_required, Toast.LENGTH_SHORT).show()
            return
        }
        val app = application as BudgetTreeApplication
        lifecycleScope.launch {
            var current = user
            if (displayName != current.displayName) {
                val result = app.userDatabaseSystem.updateDisplayName(current, displayName)
                if (!applyUpdate(result)) return@launch
                current = UserSession.currentUser ?: current
            }
            if (phone != current.phoneNumber) {
                val result = app.userDatabaseSystem.updatePhoneNumber(current, phone)
                if (!applyUpdate(result)) return@launch
                current = UserSession.currentUser ?: current
            }
            if (email != current.email) {
                val result = app.userDatabaseSystem.updateEmail(current, email)
                if (!applyUpdate(result)) return@launch
            }
            ProfilePreferences.setPushEnabled(this@EditProfileActivity, current.uid, pushEnabled)
            Toast.makeText(this@EditProfileActivity, R.string.profile_updated, Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun applyUpdate(result: UpdateReturnInfo<User>): Boolean {
        return when (result.status) {
            UpdateReturnStatus.Succeeded -> {
                Log.i(TAG, "Profile field updated for user '${result.value?.uid}'")
                result.value?.let { UserSession.login(it) }
                true
            }
            UpdateReturnStatus.NoChange -> {
                result.value?.let { UserSession.login(it) }
                true
            }
            UpdateReturnStatus.PendingVerification -> {
                Toast.makeText(this, R.string.email_verification_sent, Toast.LENGTH_LONG).show()
                true
            }
            UpdateReturnStatus.Failed -> {
                Log.w(TAG, "Failed to update profile field: ${result.errMsg}")
                Toast.makeText(this, result.errMsg ?: getString(R.string.signup_fields_required), Toast.LENGTH_SHORT).show()
                false
            }
        }
    }

    private fun confirmDeleteAccount() {
        AlertDialog.Builder(this)
            .setTitle(R.string.delete_account)
            .setMessage(R.string.delete_account_confirm)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.delete) { _, _ -> askForPasswordThenDelete() }
            .show()
    }

    // Deleting an account is sensitive, so the password is asked for again
    private fun askForPasswordThenDelete() {
        val passwordField = EditText(this).apply {
            setHint(R.string.delete_account_password_hint)
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        AlertDialog.Builder(this)
            .setTitle(R.string.delete_account)
            .setView(passwordField)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.delete) { _, _ -> deleteAccount(passwordField.text?.toString().orEmpty()) }
            .show()
    }

    private fun deleteAccount(password: String) {
        val user = UserSession.currentUser ?: return
        val app = application as BudgetTreeApplication
        lifecycleScope.launch {
            val status = try {
                app.userDatabaseSystem.deleteUser(user, password)
            } catch (e: kotlin.coroutines.cancellation.CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Failed to delete account: ${e.message}")
                Toast.makeText(this@EditProfileActivity, R.string.delete_account_failed, Toast.LENGTH_SHORT).show()
                return@launch
            }
            if (status != DeleteReturnStatus.Deleted) {
                val message = if (status == DeleteReturnStatus.ReauthenticationFailed) R.string.delete_account_wrong_password
                else R.string.delete_account_failed
                Toast.makeText(this@EditProfileActivity, message, Toast.LENGTH_SHORT).show()
                return@launch
            }
            UserSession.logout()
            Toast.makeText(this@EditProfileActivity, R.string.account_deleted, Toast.LENGTH_SHORT).show()
            startActivity(
                Intent(this@EditProfileActivity, WelcomeActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
            )
            finish()
        }
    }

    private fun showPhotoChooser() {
        AlertDialog.Builder(this)
            .setItems(arrayOf(getString(R.string.upload_image), getString(R.string.take_photo))) { _, which ->
                if (which == 0) pickImage.launch("image/*") else ensureCameraPermission()
            }
            .show()
    }

    private fun ensureCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            launchCamera()
        } else {
            requestCamera.launch(Manifest.permission.CAMERA)
        }
    }

    private fun launchCamera() {
        val file = File(cacheDir, "profile_${System.currentTimeMillis()}.jpg")
        cameraFile = file
        val uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
        takePicture.launch(uri)
    }

    private fun savePhotoFromUri(uri: Uri) {
        val app = application as BudgetTreeApplication
        lifecycleScope.launch {
            val bytes = withContext(Dispatchers.IO) {
                contentResolver.openInputStream(uri)?.use { it.readBytes() }
            }
            persistPhoto(bytes)
        }
    }

    private fun savePhotoFromFile(file: File) {
        lifecycleScope.launch {
            val bytes = withContext(Dispatchers.IO) { file.readBytes() }
            persistPhoto(bytes)
        }
    }

    private suspend fun persistPhoto(bytes: ByteArray?) {
        val user = UserSession.currentUser ?: return
        val app = application as BudgetTreeApplication
        val photo = bytes?.let { withContext(Dispatchers.Default) { ProfilePhoto.compress(it) } } ?: return
        val result = app.userDatabaseSystem.updateProfilePhoto(user, photo)
        if (applyUpdate(result)) {
            Toast.makeText(this, R.string.profile_photo_updated, Toast.LENGTH_SHORT).show()
            bindPhotoAndName()
        }
    }
}
