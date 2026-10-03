package com.example.marvel.ui

import android.text.method.PasswordTransformationMethod
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.*
import androidx.core.widget.doAfterTextChanged
import com.example.marvel.R

fun ScreenRenderer.emailAuth(host: FrameLayout) {
    val view = activity.layoutInflater.inflate(R.layout.screen_email_auth, host, false)
    host.addView(view)
    view.findViewById<LinearLayout>(R.id.auth_form_panel).background = CollageSurface(activity, "paper", 1)
    val signup = vm.route.screen == "sign-up"
    val reset = vm.route.screen == "password-reset"
    val fields = listOf("name", "email", "password", "confirm")
    val ids = listOf(R.id.auth_name, R.id.auth_email, R.id.auth_password, R.id.auth_confirm)
    val labels = listOf(R.id.auth_name_label, R.id.auth_email_label, R.id.auth_password_label, R.id.auth_confirm_label)
    val values = listOf(vm.authName, vm.authEmail, vm.authPassword, vm.authConfirmPassword)
    view.findViewById<TextView>(R.id.auth_title).text = when { signup -> "Create your account"; reset -> "Reset password"; else -> "Welcome back" }
    view.findViewById<TextView>(R.id.auth_copy).text = when {
        signup -> "Your own archive. Favorites, teams and missions, saved together. Use a password with at least 6 characters."
        reset -> "Enter your account email. We’ll send a link to choose a new password."
        else -> "Sign in to pick up your next mission."
    }
    for (index in fields.indices) {
        val field = view.findViewById<EditText>(ids[index])
        val visible = when (fields[index]) { "name", "confirm" -> signup; "password" -> !reset; else -> true }
        field.visibility = if (visible) View.VISIBLE else View.GONE
        view.findViewById<TextView>(labels[index]).visibility = field.visibility
        field.setText(values[index]); field.isEnabled = !vm.authBusy
        if (fields[index] == "name") field.filters = arrayOf(android.text.InputFilter.LengthFilter(80))
        if (signup && fields[index] == "password") field.setAutofillHints("newPassword")
        field.doAfterTextChanged { value ->
            when (fields[index]) { "name" -> vm.authName = value.toString(); "email" -> vm.authEmail = value.toString(); "password" -> vm.authPassword = value.toString(); "confirm" -> vm.authConfirmPassword = value.toString() }
        }
        val last = if (reset) index == 1 else if (signup) index == 3 else index == 2
        if (last) {
            field.imeOptions = EditorInfo.IME_ACTION_DONE
            field.setOnEditorActionListener { _, action, _ -> if (action == EditorInfo.IME_ACTION_DONE) { activity.hideKeyboard(); vm.submitEmailAuth(); true } else false }
        }
    }
    view.findViewById<CheckBox>(R.id.auth_show_password).apply {
        visibility = if (reset) View.GONE else View.VISIBLE; isEnabled = !vm.authBusy
        isChecked = vm.authShowPasswords
        for (id in listOf(R.id.auth_password, R.id.auth_confirm)) view.findViewById<EditText>(id).let {
            it.transformationMethod = if (vm.authShowPasswords) null else PasswordTransformationMethod.getInstance()
        }
        setOnCheckedChangeListener { _, checked ->
            vm.authShowPasswords = checked
            for (id in listOf(R.id.auth_password, R.id.auth_confirm)) view.findViewById<EditText>(id).let {
                val cursor = it.selectionStart
                it.transformationMethod = if (checked) null else PasswordTransformationMethod.getInstance()
                it.setSelection(cursor.coerceIn(0, it.length()))
            }
        }
    }
    view.findViewById<TextView>(R.id.auth_feedback).apply {
        text = vm.authError ?: vm.authNotice.orEmpty()
        visibility = if (text.isBlank()) View.GONE else View.VISIBLE
        setTextColor(color(if (vm.authError != null) R.color.deep_red else R.color.ink_black))
    }
    view.findViewById<ProgressBar>(R.id.auth_progress).visibility = if (vm.authBusy) View.VISIBLE else View.GONE
    view.findViewById<Button>(R.id.auth_submit).apply {
        text = if (vm.authBusy) "Please wait…" else when { signup -> "Create account"; reset -> "Send reset link"; else -> "Sign in" }
        isEnabled = !vm.authBusy
        setOnClickListener { activity.hideKeyboard(); vm.submitEmailAuth() }
    }
    view.findViewById<Button>(R.id.auth_back).apply { setOnClickListener { activity.hideKeyboard(); vm.back() } }
    view.findViewById<Button>(R.id.auth_reset).apply {
        visibility = if (!signup && !reset) View.VISIBLE else View.GONE
        isEnabled = !vm.authBusy; setOnClickListener { activity.hideKeyboard(); vm.openAuth("password-reset") }
    }
    view.findViewById<Button>(R.id.auth_alternative).apply {
        text = if (signup) "Already have an account? Sign in" else if (reset) "Back to sign in" else "New here? Create an account"
        isEnabled = !vm.authBusy
        setOnClickListener {
            activity.hideKeyboard()
            val target = if (signup || reset) "email-sign-in" else "sign-up"
            if (vm.stack.size > 1 && vm.stack[vm.stack.lastIndex - 1].screen == target) vm.back() else vm.openAuth(target)
        }
    }
    val availableWidth = activity.resources.configuration.screenWidthDp - 40
    view.findViewById<LinearLayout>(R.id.auth_content).layoutParams = LinearLayout.LayoutParams(
        dp(minOf(520, availableWidth)), ViewGroup.LayoutParams.WRAP_CONTENT
    ).apply { gravity = android.view.Gravity.CENTER_HORIZONTAL }
}
