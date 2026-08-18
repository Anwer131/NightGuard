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
    private val allApps: List<AppInfo>,
    private val mandatoryPackages: Set<String> = emptySet()
) : RecyclerView.Adapter<AppAdapter.AppViewHolder>() {

    /**
     * Apps currently displayed by RecyclerView.
     *
     * Initially this contains all installed apps.
     * When the user searches, this list is filtered.
     */
    private var displayedApps =
        allApps.toList()

    /**
     * Packages currently selected by the user.
     */
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

        val mandatoryText: TextView =
            itemView.findViewById(R.id.mandatoryText)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): AppViewHolder {

        val view =
            LayoutInflater
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

        val app =
            displayedApps[position]

        val isMandatory =
            mandatoryPackages.contains(
                app.packageName
            )

        holder.name.text =
            app.appName

        holder.icon.setImageDrawable(
            holder.itemView.context.packageManager
                .getApplicationIcon(
                    app.packageName
                )
        )

        // Important when RecyclerView reuses a row.
        holder.checkBox.setOnCheckedChangeListener(
            null
        )

        holder.checkBox.isChecked =
            selectedPackages.contains(
                app.packageName
            )

        if (isMandatory) {

            holder.checkBox.isChecked = true
            holder.checkBox.isEnabled = false

            holder.mandatoryText.visibility =
                View.VISIBLE

            holder.mandatoryText.text =
                "Always allowed"

        } else {

            holder.checkBox.isEnabled = true

            holder.mandatoryText.visibility =
                View.GONE

            holder.checkBox.setOnCheckedChangeListener {
                    _,
                    checked ->

                if (checked) {

                    selectedPackages.add(
                        app.packageName
                    )

                } else {

                    selectedPackages.remove(
                        app.packageName
                    )
                }
            }
        }
    }

    override fun getItemCount(): Int =
        displayedApps.size

    /**
     * Filter apps by name.
     *
     * Example:
     *
     * filter("chrome")
     *
     * will display apps whose names contain
     * "chrome".
     */
    fun filter(query: String) {

        val searchQuery =
            query.trim()

        displayedApps =
            if (searchQuery.isEmpty()) {

                allApps.toList()

            } else {

                allApps.filter { app ->

                    app.appName
                        .contains(
                            searchQuery,
                            ignoreCase = true
                        )
                }
            }

        notifyDataSetChanged()
    }

    /**
     * Return all currently selected packages.
     */
    fun getSelectedPackages(): Set<String> {

        // Make absolutely sure mandatory apps
        // are always part of the whitelist.
        selectedPackages.addAll(
            mandatoryPackages
        )

        return selectedPackages.toSet()
    }

    /**
     * Restore saved whitelist.
     */
    fun setSelectedPackages(
        packages: Set<String>
    ) {

        selectedPackages.clear()

        selectedPackages.addAll(
            packages
        )

        // Mandatory apps can never be removed.
        selectedPackages.addAll(
            mandatoryPackages
        )

        notifyDataSetChanged()
    }
}