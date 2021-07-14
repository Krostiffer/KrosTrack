package com.krostiffer.krostrack.ui.History

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import com.krostiffer.krostrack.MainActivity
import com.krostiffer.krostrack.R
import com.krostiffer.krostrack.ShowMaps

class HistoryFragment : Fragment() {

    //private lateinit var dashboardViewModel: DashboardViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val root = inflater.inflate(R.layout.fragment_history, container, false)

        val mainAct: MainActivity = activity as MainActivity

        val layout: LinearLayout = root.findViewById(R.id.history_layout)
        for (loc in mainAct.locationDatabase.locationDao().getAllRoutes()){
            if(loc.showTime.isNotEmpty()) {
                val button = Button(mainAct)
                button.setBackgroundColor(resources.getColor(R.color.transparent_20))
                button.textAlignment = View.TEXT_ALIGNMENT_TEXT_START
                button.text = "  ${loc.uid}: ${loc.showTime}"
                button.setOnClickListener {
                    val intent = Intent(mainAct, ShowMaps::class.java)
                    intent.putExtra("lat", loc.latitudes)
                    intent.putExtra("lon", loc.longitudes)
                    intent.putExtra("tim", loc.times)
                    intent.putExtra("spe", loc.speeds)
                    startActivity(intent)
                }
                layout.addView(button)
            }
        }
        return root
    }
}