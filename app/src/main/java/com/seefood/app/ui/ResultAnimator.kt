package com.seefood.app.ui

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.view.View
import android.view.animation.BounceInterpolator
import android.view.animation.OvershootInterpolator

object ResultAnimator {

    fun popIn(view: View) {
        view.scaleX = 0f
        view.scaleY = 0f
        view.alpha = 0f
        view.visibility = View.VISIBLE

        AnimatorSet().apply {
            playTogether(
                ObjectAnimator.ofFloat(view, View.SCALE_X, 0f, 1f).apply {
                    duration = 400
                    interpolator = OvershootInterpolator(1.5f)
                },
                ObjectAnimator.ofFloat(view, View.SCALE_Y, 0f, 1f).apply {
                    duration = 400
                    interpolator = OvershootInterpolator(1.5f)
                },
                ObjectAnimator.ofFloat(view, View.ALPHA, 0f, 1f).apply {
                    duration = 200
                }
            )
            start()
        }
    }

    fun shake(view: View) {
        ObjectAnimator.ofFloat(
            view, View.TRANSLATION_X,
            0f, 20f, -20f, 16f, -16f, 8f, -8f, 0f
        ).apply {
            duration = 500
            interpolator = BounceInterpolator()
            start()
        }
    }

    fun pulse(view: View) {
        val scaleUp = AnimatorSet().apply {
            playTogether(
                ObjectAnimator.ofFloat(view, View.SCALE_X, 1f, 1.05f).setDuration(150),
                ObjectAnimator.ofFloat(view, View.SCALE_Y, 1f, 1.05f).setDuration(150)
            )
        }
        val scaleDown = AnimatorSet().apply {
            playTogether(
                ObjectAnimator.ofFloat(view, View.SCALE_X, 1.05f, 1f).setDuration(150),
                ObjectAnimator.ofFloat(view, View.SCALE_Y, 1.05f, 1f).setDuration(150)
            )
        }
        AnimatorSet().apply {
            play(scaleDown).after(scaleUp)
            start()
        }
    }
}
