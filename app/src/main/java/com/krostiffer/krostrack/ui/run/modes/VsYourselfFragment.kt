package com.krostiffer.krostrack.ui.run.modes

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import com.krostiffer.krostrack.MainActivity
import com.krostiffer.krostrack.R


class VsYourselfFragment : Fragment() {




    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val root = inflater.inflate(R.layout.fragment_vs_yourself, container, false)

        val mainAct: MainActivity = activity as MainActivity

        val layout: LinearLayout = root.findViewById(R.id.vsyouListLayout)
        changePref(mainAct.BUTTON_UID_STORE, -1)
        for (loc in mainAct.locationDatabase.locationDao().getAllRoutes()){
            if(loc.showTime.isNotEmpty()) {
                var button = Button(mainAct)
                button.setBackgroundColor(resources.getColor(R.color.transparent_20))
                button.id = View.generateViewId()
                button.textAlignment = View.TEXT_ALIGNMENT_TEXT_START
                button.text = " " + loc.uid.toString() + ": " + loc.showTime

                button.setOnClickListener {

                    var bId = mainAct.prefs!!.getInt(mainAct.BUTTON_UID_STORE, -1)
                    if(bId>=0){
                        Log.println(Log.ASSERT,"Buttons are stored", button.id.toString())
                        var buttonSetOff: Button = root.findViewById(bId)
                        if(buttonSetOff != null){
                            buttonSetOff.setBackgroundColor(resources.getColor(R.color.transparent_20))
                        }

                    }
                    button.setBackgroundColor(resources.getColor(R.color.orange_700))

                    changePref(mainAct.BUTTON_UID_STORE, button.id)
                    changePref(mainAct.SELECTED_UID_FROM_DATABASE, loc.uid)

                }
                layout.addView(button)
            }
        }
        return root

    }
    fun changePref(side:String, value: Int) {
        val mainAct: MainActivity = activity as MainActivity
        val editor = mainAct.prefs!!.edit()
        editor.putInt(side, value)
        editor.apply()
    }
}