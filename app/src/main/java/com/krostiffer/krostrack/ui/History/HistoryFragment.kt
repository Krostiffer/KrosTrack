package com.krostiffer.krostrack.ui.History

import android.content.Intent
import android.graphics.Rect
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.PopupMenu
import androidx.fragment.app.Fragment
import com.krostiffer.krostrack.MainActivity
import com.krostiffer.krostrack.R
import com.krostiffer.krostrack.ShowMaps


class HistoryFragment : Fragment() {

    //private lateinit var dashboardViewModel: DashboardViewModel


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {

        val root = inflater.inflate(R.layout.fragment_history, container, false)

        val mainAct: MainActivity = activity as MainActivity

        val layout: LinearLayout = root.findViewById(R.id.history_layout)

        for (loc in mainAct.locationDatabase.locationDao().getAllRoutes()){ //Zeigt alle Routen als Liste an anklickbaren Buttons, beim Klicken öffnet sich einfach eine Karte mit der Route
            if(loc.showTime.isNotEmpty()) {
                val doppelButton = LinearLayout(mainAct).apply {
                    LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,LinearLayout.LayoutParams.MATCH_PARENT)
                }
                val button = Button(mainAct).apply {
                    setBackgroundColor(resources.getColor(R.color.transparent))
                    text = "  ${loc.uid}: ${loc.showTime}" //Jede Route wird nach dem Format "Zahl: Datum Zeit(start) - Zeit(ende)" angezeigt
                    setOnClickListener {
                        val intent = Intent(mainAct, ShowMaps::class.java)
                        intent.putExtra("lat", loc.latitudes)
                        intent.putExtra("lon", loc.longitudes)
                        intent.putExtra("tim", loc.times)
                        intent.putExtra("spe", loc.speeds)
                        startActivity(intent)
                    }
                }

                val deleteButton = Button(mainAct).apply {
                    text = "X"
                    layoutParams = LinearLayout.LayoutParams(100,LinearLayout.LayoutParams.WRAP_CONTENT)
                    setOnClickListener {
                        val popupMenu: PopupMenu = PopupMenu(mainAct,this)
                        popupMenu.menuInflater.inflate(R.menu.delete_popup,popupMenu.menu)
                        popupMenu.setOnMenuItemClickListener { item ->
                            when (item.itemId) {
                                R.id.deleteyes -> {
                                    mainAct.locationDatabase.locationDao().delete(loc)
                                    layout.removeView(doppelButton)
                                }
                            }
                            true
                        }
                        popupMenu.show()
                    }
                }

                //button.textAlignment = View.TEXT_ALIGNMENT_TEXT_START
                doppelButton.addView(deleteButton)
                doppelButton.addView(button)


                layout.addView(doppelButton)
            }
        }
        return root
    }
}