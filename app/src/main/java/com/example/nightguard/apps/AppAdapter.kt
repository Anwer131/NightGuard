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

    /*
     * Apps currently displayed in the RecyclerView.
     *
     * Initially this contains all installed apps.
     * When the user searches, this list contains only
     * the matching apps.
     */
    private var filteredApps =
        apps.toList()

    /*
     * Packages selected by the user.
     *
     * This contains the complete whitelist, not just
     * the apps currently visible after filtering.
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
            filteredApps[position]

        holder.name.text =
            app.appName

        holder.icon.setImageDrawable(
            holder.itemView.context
                .packageManager
                .getApplicationIcon(
                    app.packageName
                )
        )

        /*
         * Remove the previous listener before changing
         * isChecked.
         *
         * Otherwise RecyclerView recycling can trigger
         * the listener unexpectedly.
         */
        holder.checkBox.setOnCheckedChangeListener(null)

        holder.checkBox.isChecked =
            selectedPackages.contains(
                app.packageName
            )

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

    override fun getItemCount(): Int {
        return filteredApps.size
    }

    /*
     * Filter apps based on the application name.
     *
     * Search is case-insensitive.
     *
     * Empty search shows all installed apps.
     */
    fun filter(query: String) {

        val searchQuery =
            query.trim()

        filteredApps =
            if (searchQuery.isEmpty()) {

                apps.toList()

            } else {

                apps.filter { app ->

                    app.appName
                        .contains(
                            searchQuery,
                            ignoreCase = true
                        )
                }
            }

        notifyDataSetChanged()
    }

    /*
     * Return the complete whitelist.
     *
     * This is NOT affected by search filtering.
     */
    fun getSelectedPackages(): Set<String> {

        return selectedPackages.toSet()
    }

    /*
     * Restore the saved whitelist.
     */
    fun setSelectedPackages(
        packages: Set<String>
    ) {

        selectedPackages.clear()

        selectedPackages.addAll(
            packages
        )

        notifyDataSetChanged()
    }
}