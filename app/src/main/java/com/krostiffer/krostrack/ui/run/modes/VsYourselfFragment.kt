package com.krostiffer.krostrack.ui.run.modes

import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
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
        changePref(mainAct.BUTTON_UID_STORE, -1) //wenn die View erstellt wird, ist kein Button mehr ausgewählt
        for (loc in mainAct.locationDatabase.locationDao().getAllRoutes()){ //erstellt die Buttons, die man anwählen kann um eine Route auszuwählen
            if(loc.showTime.isNotEmpty()) {
                val typedValue = TypedValue()
                val button = Button(mainAct)

                button.setBackgroundColor(resources.getColor(R.color.transparent))
                button.id = View.generateViewId()
                //button.textAlignment = View.TEXT_ALIGNMENT_TEXT_START
                button.text = "${loc.uid}: ${loc.showTime}"

                button.setOnClickListener {

                    val bId = mainAct.prefs!!.getInt(mainAct.BUTTON_UID_STORE, -1) //Einfaches Umschalten der Buttons (wenn einer gedrückt wird, wird der andere wieder entmarkiert und der neue markiert)
                    if(bId>=0){
                        Log.println(Log.ASSERT,"Buttons are stored", button.id.toString())
                        val buttonSetOff: Button = root.findViewById(bId)
                        buttonSetOff.setBackgroundColor(resources.getColor(R.color.transparent))
                    }
                    //Der geklickte Button wird in der PrimaryColor gefärbt
                    mainAct.theme.resolveAttribute(R.attr.colorPrimary, typedValue, true)
                    val color = typedValue.data
                    button.setBackgroundColor(color)
                    //Der geklickte Button und die entsprechende ID der Route die dazugehört werden in der Mainactivity gespeichert
                    changePref(mainAct.BUTTON_UID_STORE, button.id)
                    changePref(mainAct.SELECTED_UID_FROM_DATABASE, loc.uid)

                }
                layout.addView(button)
            }
        }
        return root

    }
    //Hilfsfunktion um einfach sharedPreferences in der MainActivity zu ändern
    private fun changePref(side:String, value: Int) {
        val mainAct: MainActivity = activity as MainActivity
        val editor = mainAct.prefs!!.edit()
        editor.putInt(side, value)
        editor.apply()
    }
}