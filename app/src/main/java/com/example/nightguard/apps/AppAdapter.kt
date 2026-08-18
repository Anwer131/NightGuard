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
     * Initially all apps are displayed.
     * Search updates this list.
     */
    private var displayedApps =
        allApps.toList()

    /**
     * Packages currently selected/whitelisted.
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

        /*
         * Important when RecyclerView
         * reuses a row.
         */
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

                /*
                 * Re-sort immediately so that
                 * newly enabled apps move to
                 * the top.
                 */
                refreshDisplayOrder()
            }
        }
    }

    override fun getItemCount(): Int =
        displayedApps.size

    /**
     * Sort apps so that:
     *
     * 1. Selected/whitelisted apps appear first.
     * 2. Mandatory apps are also treated as selected.
     * 3. Unselected apps appear afterwards.
     *
     * Within each group apps are alphabetical.
     */
    private fun sortApps(
        apps: List<com.example.nightguard.model.AppInfo>
    ): List<com.example.nightguard.model.AppInfo> {

        return apps.sortedWith(
            compareByDescending<com.example.nightguard.model.AppInfo> {

                selectedPackages.contains(
                    it.packageName
                ) ||
                        mandatoryPackages.contains(
                            it.packageName
                        )

            }.thenBy {

                it.appName.lowercase()
            }
        )
    }

    /**
     * Refresh the current list while
     * preserving the current search.
     */
    private fun refreshDisplayOrder() {

        displayedApps =
            sortApps(displayedApps)

        notifyDataSetChanged()
    }

    /**
     * Filter apps by name.
     *
     * Selected apps remain at the top
     * even when searching.
     */
    fun filter(query: String) {

        val searchQuery =
            query.trim()

        val filteredApps =
            if (searchQuery.isEmpty()) {

                allApps

            } else {

                allApps.filter { app ->

                    app.appName.contains(
                        searchQuery,
                        ignoreCase = true
                    )
                }
            }

        displayedApps =
            sortApps(filteredApps)

        notifyDataSetChanged()
    }

    /**
     * Return all currently selected packages.
     *
     * Mandatory apps are always included.
     */
    fun getSelectedPackages(): Set<String> {

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

        /*
         * Mandatory apps can never be removed.
         */
        selectedPackages.addAll(
            mandatoryPackages
        )

        /*
         * Apply the enabled-first ordering
         * after restoring the whitelist.
         */
        displayedApps =
            sortApps(displayedApps)

        notifyDataSetChanged()
    }
}