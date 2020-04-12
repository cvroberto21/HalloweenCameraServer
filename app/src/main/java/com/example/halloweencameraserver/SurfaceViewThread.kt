package com.example.halloweencameraserver

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.os.Handler
import android.text.TextUtils
import android.util.AttributeSet
import android.util.Log
import android.view.SurfaceHolder
import android.view.SurfaceView

class SurfaceViewThread(context: Context?, attr: AttributeSet?, bluetoothHandler: Handler ) : SurfaceView( context, attr ),
    SurfaceHolder.Callback, Runnable {
    private var threadRunning = false
    private var textX = 0f
    private var textY = 0f
    var text = "Hello"
    lateinit var bitmap : Bitmap

    override fun surfaceCreated(surfaceHolder: SurfaceHolder) { // Create the child thread when SurfaceView is created.
        val thread = Thread(this)
        // Start to run the child thread.
        thread.start()
        // Set thread running flag to true.
        threadRunning = true
        // Get screen width and height.
    }

    override fun surfaceChanged(
        surfaceHolder: SurfaceHolder,
        i: Int,
        i1: Int,
        i2: Int
    ) {
    }

    override fun surfaceDestroyed(surfaceHolder: SurfaceHolder) { // Set thread running flag to false when Surface is destroyed.
// Then the thread will jump out the while loop and complete.
        threadRunning = false
    }

    override fun run() {
        while (threadRunning) {
            if (TextUtils.isEmpty(text)) {
                text = "Input text in above text box."
            }
            val startTime = System.currentTimeMillis()
            textX += 100f
            textY += 100f
            if (textX >  width) {
                textX = 0f
            }
            if (textY > height ) {
                textY = 0f
            }
            drawText()
            val endTime = System.currentTimeMillis()
            val deltaTime = endTime - startTime
            if (deltaTime < 200) {
                try {
                    Thread.sleep(200 - deltaTime)
                } catch (ex: InterruptedException) {
                    Log.e(LOG_TAG, ex.message!!)
                }
            }
        }
    }

    private fun drawText() {
        val margin = 100
        val right = width - margin
        val bottom = height - margin
        val rect = Rect(margin, margin, right, bottom)
        // Only draw text on the specified rectangle area.
        val canvas = this.holder.lockCanvas(rect)
        // Draw the specify canvas background color.
        val backgroundPaint = Paint()
        backgroundPaint.setColor(Color.BLUE)
        canvas.drawRect(rect, backgroundPaint)
        // Draw text in the canvas.
        canvas.drawText(text, textX, textY, paint)
        // Send message to main UI thread to update the drawing to the main view special area.
        this.holder.unlockCanvasAndPost(canvas)
    }

    fun drawBitmap( bitmap : Bitmap) {
        val bwidth = bitmap.width
        val bheight = bitmap.height
        val r = Rect( 0, 0, bwidth, bheight )
        val canvas = this.holder.lockCanvas()
        // Draw the specify canvas background color.
        val paint = Paint()

//        for(i in 0..240-1) {
//            for( j in 0..320-1) {
//                bitmap.set(j,i, argb(255, 255, 100, 0) )
//            }
//        }
        canvas.drawBitmap( bitmap, 0.0f, 0.0f, null )
        // Send message to main UI thread to update the drawing to the main view special area.
        this.holder.unlockCanvasAndPost(canvas)
    }

    companion object {
        private const val LOG_TAG = "SURFACE_VIEW_THREAD"
    }

    private val paint: Paint

    init {
        isFocusable = true

        bitmap = Bitmap.createBitmap( width, height, Bitmap.Config.ARGB_8888 )

        // Get SurfaceHolder object.
        // Add current object as the callback listener.
        this.holder.addCallback(this)
        // Create the paint object which will draw the text.
        paint = Paint()
        paint.setTextSize(100f)
        paint.setColor(Color.GREEN)
        // Set the SurfaceView object at the top of View object.
        setZOrderOnTop(true)
        //setBackgroundColor(Color.RED);
    }
}