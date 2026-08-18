package com.example.nightguard.apps

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.nightguard.R
import com.example.nightguard.model.AppInfo

class AppAdapter(
    private val apps: List<AppInfo>
) : RecyclerView.Adapter<AppAdapter.AppViewHolder>() {

    private val selectedPackages =
        mutableSetOf<String>()

    inner class AppViewHolder(
        itemView: View
    ) : RecyclerView.ViewHolder(itemView) {

        val icon: ImageView =
            itemView.findViewById(R.id.appIcon)

        val name: TextView =
            itemView.findViewById(R.id.appName)

        val checkBox: CheckBox =
            itemView.findViewById(R.id.appCheckBox)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): AppViewHolder {

        val view = LayoutInflater
            .from(parent.context)
            .inflate(
                R.layout.app_item,
                parent,
                false
            )

        return AppViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: AppViewHolder,
        position: Int
    ) {

        val app = apps[position]

        holder.name.text = app.appName

        holder.icon.setImageDrawable(
            holder.itemView.context.packageManager
                .getApplicationIcon(app.packageName)
        )

        holder.checkBox.setOnCheckedChangeListener(null)

        holder.checkBox.isChecked =
            selectedPackages.contains(app.packageName)

        holder.checkBox.setOnCheckedChangeListener { _, checked ->

            if (checked) {
                selectedPackages.add(app.packageName)
            } else {
                selectedPackages.remove(app.packageName)
            }
        }
    }

    override fun getItemCount(): Int {
        return apps.size
    }

    fun getSelectedPackages(): Set<String> {
        return selectedPackages.toSet()
    }

    fun setSelectedPackages(
        packages: Set<String>
    ) {
        selectedPackages.clear()
        selectedPackages.addAll(packages)

        notifyDataSetChanged()
    }
}