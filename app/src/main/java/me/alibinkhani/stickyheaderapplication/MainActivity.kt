package me.alibinkhani.stickyheaderapplication

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.xq.stickylayoutmanager.StickyHeadersLinearLayoutManager
import me.alibinkhani.stickyheaderapplication.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val adapter = StickyHeaderAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityMainBinding.inflate(layoutInflater)
        setupViews()
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun setupViews() = binding.apply {
        val layoutManager = StickyHeadersLinearLayoutManager(this@MainActivity)
        layoutManager.setStickyHeaderProvider { adapter, position ->
            adapter is StickyHeaderAdapter && adapter.isStickyHeader(position)
        }

        recyclerView.layoutManager = layoutManager
        recyclerView.adapter = adapter
    }
}