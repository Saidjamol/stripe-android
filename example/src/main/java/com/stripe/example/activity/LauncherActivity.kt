package com.stripe.example.activity

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.stripe.android.PaymentConfiguration
import com.stripe.example.R
import com.stripe.example.Settings

class LauncherActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_launcher)

        PaymentConfiguration.init(this, Settings.PUBLISHABLE_KEY)

        val examples = findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.examples)
        val linearLayoutManager = androidx.recyclerview.widget.LinearLayoutManager(this)
        linearLayoutManager.orientation = androidx.recyclerview.widget.LinearLayoutManager.VERTICAL
        examples.setHasFixedSize(true)
        examples.layoutManager = linearLayoutManager
        examples.adapter = ExamplesAdapter(this)
    }

    private class ExamplesAdapter constructor(
        private val activity: Activity
    ) : androidx.recyclerview.widget.RecyclerView.Adapter<ExamplesAdapter.ExamplesViewHolder>() {
        private val items = listOf(
            Item(activity.getString(R.string.payment_auth_example),
                PaymentAuthActivity::class.java),
            Item(activity.getString(R.string.create_card_tokens),
                CardTokenActivity::class.java),
            Item(activity.getString(R.string.create_card_payment_methods),
                PaymentMultilineActivity::class.java),
            Item(activity.getString(R.string.create_three_d_secure),
                RedirectActivity::class.java),
            Item(activity.getString(R.string.launch_customer_session),
                CustomerSessionActivity::class.java),
            Item(activity.getString(R.string.launch_payment_session),
                PaymentSessionActivity::class.java),
            Item(activity.getString(R.string.launch_payment_session_from_fragment),
                FragmentExamplesActivity::class.java),
            Item(activity.getString(R.string.launch_pay_with_google),
                PayWithGoogleActivity::class.java)
        )

        override fun onCreateViewHolder(viewGroup: ViewGroup, i: Int): ExamplesViewHolder {
            val root = LayoutInflater.from(viewGroup.context)
                .inflate(R.layout.launcher_item, viewGroup, false)
            return ExamplesViewHolder(root)
        }

        override fun onBindViewHolder(examplesViewHolder: ExamplesViewHolder, i: Int) {
            (examplesViewHolder.itemView as TextView).text = items[i].text
            examplesViewHolder.itemView.setOnClickListener {
                activity.startActivity(Intent(activity, items[i].activityClass))
            }
        }

        override fun getItemCount(): Int {
            return items.size
        }

        private data class Item constructor(val text: String, val activityClass: Class<*>)

        private class ExamplesViewHolder constructor(
            itemView: View
        ) : androidx.recyclerview.widget.RecyclerView.ViewHolder(itemView)
    }
}
