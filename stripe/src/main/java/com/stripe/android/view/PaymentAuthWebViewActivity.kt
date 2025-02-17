package com.stripe.android.view

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.ProgressBar
import androidx.annotation.ColorInt
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.stripe.android.PaymentAuthWebViewStarter
import com.stripe.android.R
import com.stripe.android.stripe3ds2.init.ui.StripeToolbarCustomization
import com.stripe.android.stripe3ds2.init.ui.ToolbarCustomization
import com.stripe.android.stripe3ds2.utils.CustomizeUtils
import com.ults.listeners.SdkChallengeInterface.UL_HANDLE_CHALLENGE_ACTION

class PaymentAuthWebViewActivity : AppCompatActivity() {

    private var toolbarCustomization: ToolbarCustomization? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        LocalBroadcastManager.getInstance(this)
            .sendBroadcast(Intent().setAction(UL_HANDLE_CHALLENGE_ACTION))

        setContentView(R.layout.payment_auth_web_view_layout)

        val toolbar = findViewById<Toolbar>(R.id.payment_auth_web_view_toolbar)
        setSupportActionBar(toolbar)
        toolbarCustomization = intent.getParcelableExtra<StripeToolbarCustomization>(
            PaymentAuthWebViewStarter.EXTRA_UI_CUSTOMIZATION
        )
        customizeToolbar(toolbar)

        val clientSecret = intent
            .getStringExtra(PaymentAuthWebViewStarter.EXTRA_CLIENT_SECRET)
        val returnUrl = intent
            .getStringExtra(PaymentAuthWebViewStarter.EXTRA_RETURN_URL)

        setResult(Activity.RESULT_OK, Intent()
            .putExtra(StripeIntentResultExtras.CLIENT_SECRET, clientSecret))

        val webView = findViewById<PaymentAuthWebView>(R.id.auth_web_view)
        val progressBar = findViewById<ProgressBar>(R.id.auth_web_view_progress_bar)
        webView.init(this, progressBar, clientSecret!!, returnUrl)
        webView.loadUrl(intent.getStringExtra(PaymentAuthWebViewStarter.EXTRA_AUTH_URL))
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.payment_auth_web_view_menu, menu)

        toolbarCustomization?.let {
            val buttonText = it.buttonText
            if (!buttonText.isNullOrBlank()) {
                val closeMenuItem = menu.findItem(R.id.action_close)
                closeMenuItem.title = buttonText
            }
        }

        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.action_close) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun customizeToolbar(toolbar: Toolbar) {
        toolbarCustomization?.let { toolbarCustomization ->
            if (!toolbarCustomization.headerText.isNullOrBlank()) {
                toolbar.title = CustomizeUtils.buildStyledText(this,
                    toolbarCustomization.headerText, toolbarCustomization)
            }

            toolbarCustomization.backgroundColor?.let { backgroundColor ->
                @ColorInt val backgroundColorInt = Color.parseColor(backgroundColor)
                toolbar.setBackgroundColor(backgroundColorInt)
                CustomizeUtils.setStatusBarColor(this, backgroundColorInt)
            }
        }
    }
}
