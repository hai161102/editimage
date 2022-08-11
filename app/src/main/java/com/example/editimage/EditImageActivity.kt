package com.example.editimage

import android.graphics.Bitmap
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.animation.AnticipateOvershootInterpolator
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintSet
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.transition.ChangeBounds
import androidx.transition.TransitionManager
import com.burhanrashid52.photoediting.ShapeBSFragment
import com.burhanrashid52.photoediting.filters.FilterListener
import com.burhanrashid52.photoediting.filters.FilterViewAdapter
import com.example.editimage.databinding.ActivityEditImageBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import ja.burhanrashid52.photoeditor.*
import ja.burhanrashid52.photoeditor.blur.Blurry
import ja.burhanrashid52.photoeditor.shape.ShapeBuilder
import ja.burhanrashid52.photoeditor.shape.ShapeType

/**
 * Create by tientoan2503 on 28/07/2022
 */
class EditImageActivity : AppCompatActivity(), EditingToolsAdapter.OnItemSelected, FilterListener,
    OnPhotoEditorListener, ShapeBSFragment.Properties {
    private val binding: ActivityEditImageBinding by lazy {
        ActivityEditImageBinding.inflate(layoutInflater)
    }
    private lateinit var photoEditor: PhotoEditor
    private val shapeBuilder: ShapeBuilder by lazy { ShapeBuilder() }
    private val shapeBSFragment: ShapeBSFragment by lazy { ShapeBSFragment() }
    private val constraintSet by lazy { ConstraintSet() }

    companion object {
        var bitmap: Bitmap? = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        setupView()
    }

    private fun setupView() {
        binding.photoEditorView.source.setImageBitmap(bitmap)
        binding.rvConstraintTools.apply {
            adapter = EditingToolsAdapter(this@EditImageActivity)
            layoutManager =
                LinearLayoutManager(this@EditImageActivity, LinearLayoutManager.HORIZONTAL, false)
        }
        binding.rvFilterView.apply {
            layoutManager =
                LinearLayoutManager(this@EditImageActivity, LinearLayoutManager.HORIZONTAL, false)
            adapter = FilterViewAdapter(this@EditImageActivity)
        }
        photoEditor = binding.photoEditorView.run {
            PhotoEditor.Builder(this@EditImageActivity, this)
                .setPinchTextScalable(true) // set flag to make text scalable when pinch
                //.setDefaultTextTypeface(mTextRobotoTf)
                //.setDefaultEmojiTypeface(mEmojiTypeFace)
                .build() // build photo editor sdk
        }
        photoEditor.setOnPhotoEditorListener(this)
        shapeBSFragment.setPropertiesChangeListener(this)

        binding.imgRedo.setOnClickListener { photoEditor.redo() }
        binding.imgUndo.setOnClickListener {
            photoEditor.undo()
            onBackPressed()
        }
        binding.imgClose.setOnClickListener {
            onBackPressed()
        }
    }

    private var isBlurred = false
    override fun onToolSelected(toolType: ToolType?) {
        when (toolType) {
            ToolType.SHAPE -> {
                photoEditor.setBrushDrawingMode(true)
                photoEditor.setShape(shapeBuilder)
                showBottomSheetDialogFragment(shapeBSFragment)
            }
            ToolType.TEXT -> {
                val textEditorDialogFragment = TextEditorDialogFragment.show(this)
                textEditorDialogFragment.setOnTextEditorListener(object :
                    TextEditorDialogFragment.TextEditorListener {
                    override fun onDone(inputText: String?, colorCode: Int) {
                        val styleBuilder = TextStyleBuilder()
                        styleBuilder.withTextColor(colorCode)
                        photoEditor.addText(inputText, styleBuilder)
                    }
                })
            }
            ToolType.ERASER -> {
                photoEditor.brushEraser()
            }
            ToolType.FILTER -> {
                showFilter(true)
            }
            ToolType.BLUR -> {
                isBlurred = true
                Blurry.with(this)
                    .async()
                    .animate(100).onto(binding.photoEditorView)

//                val b = Blurry.with(this)
//                    .radius(10)
//                    .sampling(10)
//                    .capture(binding.photoEditorView).get()
//                binding.photoEditorView.source.setImageBitmap(b)
            }
            else -> {}
        }
    }

    override fun onBackPressed() {
        if (isBlurred) {
            Blurry.delete(binding.photoEditorView)
            isBlurred = false
        } else {
            if (mIsFilterVisible) {
                showFilter(false)
            } else {
                super.onBackPressed()
            }
        }
    }

    private var mIsFilterVisible = false
    private fun showFilter(isVisible: Boolean) {
        mIsFilterVisible = isVisible
        constraintSet.clone(binding.rootView)
        val rvFilterId: Int =
            binding.rvFilterView.id ?: throw IllegalArgumentException("RV Filter ID Expected")
        if (isVisible) {
            constraintSet.clear(rvFilterId, ConstraintSet.START)
            constraintSet.connect(
                rvFilterId, ConstraintSet.START,
                ConstraintSet.PARENT_ID, ConstraintSet.START
            )
            constraintSet.connect(
                rvFilterId, ConstraintSet.END,
                ConstraintSet.PARENT_ID, ConstraintSet.END
            )
        } else {
            constraintSet.connect(
                rvFilterId, ConstraintSet.START,
                ConstraintSet.PARENT_ID, ConstraintSet.END
            )
            constraintSet.clear(rvFilterId, ConstraintSet.END)
        }
        val changeBounds = ChangeBounds()
        changeBounds.duration = 350
        changeBounds.interpolator = AnticipateOvershootInterpolator(1.0f)
        binding.rootView.let { TransitionManager.beginDelayedTransition(it, changeBounds) }
        constraintSet.applyTo(binding.rootView)
    }

    private fun showBottomSheetDialogFragment(fragment: BottomSheetDialogFragment?) {
        if (fragment == null || fragment.isAdded) {
            return
        }
        fragment.show(supportFragmentManager, fragment.tag)
    }

    override fun onFilterSelected(photoFilter: PhotoFilter?) {
        photoEditor.setFilterEffect(photoFilter)
    }

    override fun onEditTextChangeListener(rootView: View?, text: String?, colorCode: Int) {
        val textEditorDialogFragment =
            TextEditorDialogFragment.show(this, text.toString(), colorCode)
        textEditorDialogFragment.setOnTextEditorListener(object :
            TextEditorDialogFragment.TextEditorListener {
            override fun onDone(inputText: String?, colorCode: Int) {
                val styleBuilder = TextStyleBuilder()
                styleBuilder.withTextColor(colorCode)
                if (rootView != null) {
                    photoEditor.editText(rootView, inputText, styleBuilder)
                }
            }
        })
    }

    override fun onAddViewListener(viewType: ViewType?, numberOfAddedViews: Int) {
    }

    override fun onRemoveViewListener(viewType: ViewType?, numberOfAddedViews: Int) {
    }

    override fun onStartViewChangeListener(viewType: ViewType?) {
    }

    override fun onStopViewChangeListener(viewType: ViewType?) {
    }

    override fun onTouchSourceImage(event: MotionEvent?) {
    }

    override fun onColorChanged(colorCode: Int) {
        photoEditor.setShape(shapeBuilder.withShapeColor(colorCode))
    }

    override fun onOpacityChanged(opacity: Int) {
        photoEditor.setShape(shapeBuilder.withShapeOpacity(opacity))
    }

    override fun onShapeSizeChanged(shapeSize: Int) {
        photoEditor.setShape(shapeBuilder.withShapeSize(shapeSize.toFloat()))
    }

    override fun onShapePicked(shapeType: ShapeType?) {
        photoEditor.setShape(shapeBuilder.withShapeType(shapeType))
    }
}