package com.digitalsignage.dsnetcoreplayer.listeners

import android.view.GestureDetector
import android.view.MotionEvent
import com.digitalsignage.dsnetcoreplayer.constants.InteractionType
import com.digitalsignage.dsnetcoreplayer.data.ViewInteraction
import org.greenrobot.eventbus.EventBus

class CustomGestureListener(private val interactions: List<String>) : GestureDetector.SimpleOnGestureListener() {

    override fun onSingleTapConfirmed(e: MotionEvent):Boolean {
        if (interactions.contains(InteractionType.TAP) || interactions.contains(InteractionType.TOUCH)) {
            handleAction()
        }
        return super.onSingleTapConfirmed(e)
    }

    override fun onFling(e1: MotionEvent?, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
        val diffX = e2!!.x - e1!!.x
        val diffY = e2.y - e1.y
        if(interactions.contains(InteractionType.TOUCH)){
            handleAction()
        }
        if (Math.abs(diffX) > Math.abs(diffY)) {
            if (diffX > 0 && interactions.contains(InteractionType.SWIPE_LEFT_TO_RIGHT)) {
               handleAction()
            } else if (diffX < 0 && interactions.contains(InteractionType.SWIPE_RIGHT_TO_LEFT)) {
                handleAction()
            }
        } else {
            if (diffY > 0 && interactions.contains(InteractionType.SWIPE_UP_TO_DOWN)) {
                handleAction()
            } else if (diffY < 0 && interactions.contains(InteractionType.SWIPE_DOWN_TO_UP)) {
                handleAction()
            }
        }
        return super.onFling(e1, e2, velocityX, velocityY)
    }

    private fun handleAction() {
       if(interactions[2]!=null && !interactions[2].equals("none")){
           EventBus.getDefault().post(ViewInteraction(interactions[1]))
       }
    }


}
