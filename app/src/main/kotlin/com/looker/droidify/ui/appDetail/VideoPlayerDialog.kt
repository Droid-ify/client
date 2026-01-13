package com.looker.droidify.ui.appDetail

import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import androidx.media3.common.MediaItem
import androidx.media3.datasource.DataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import com.looker.droidify.R
import com.looker.droidify.databinding.VideoPlayerDialogBinding
import com.looker.droidify.di.CacheDataSourceFactory
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class VideoPlayerDialog : DialogFragment() {

    private var _binding: VideoPlayerDialogBinding? = null
    private val binding get() = _binding!!

    @Inject
    @CacheDataSourceFactory
    lateinit var dataSourceFactory: DataSource.Factory

    private val player: ExoPlayer by lazy {
        ExoPlayer.Builder(requireContext())
            .setMediaSourceFactory(
                DefaultMediaSourceFactory(requireContext()).setDataSourceFactory(dataSourceFactory),
            )
            .build()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.Theme_Dialog_Fullscreen)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = VideoPlayerDialogBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        player.release()
        _binding = null
        super.onDestroyView()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.closePlayer.setOnClickListener { dismiss() }

        // Get the video uri from the arguments and if none exists, dismiss the dialog without any playback.
        val videoUri: Uri? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arguments?.getParcelable(ARG_VIDEO_URI, Uri::class.java)
        } else {
            arguments?.getParcelable(ARG_VIDEO_URI)
        }
        if (videoUri == null) {
            dismiss()
            return
        }

        binding.playerView.player = player

        val mediaItem = MediaItem.fromUri(videoUri)
        player.setMediaItem(mediaItem)
        player.prepare()

        // Restore playback state if we got recreated.
        val currentPosition = savedInstanceState?.getLong(ARG_VIDEO_PLAYBACK_MS)
        if (currentPosition != null) {
            player.seekTo(currentPosition)
        }

        // Start the video as soon as the player is ready.
        player.playWhenReady = true
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        try {
            outState.putLong(ARG_VIDEO_PLAYBACK_MS, player.currentPosition)
        } catch (exc: Exception) {
            Log.e(TAG, "Could not save current position", exc)
        }
    }

    companion object {
        private const val TAG = "VideoPlayerDialog"

        private const val ARG_VIDEO_URI = "video_uri"
        private const val ARG_VIDEO_PLAYBACK_MS = "video_playback_ms"

        fun showVideoFromUri(fragmentManager: FragmentManager, videoUri: Uri) {
            val videoPlayerDialog = VideoPlayerDialog().apply {
                arguments = Bundle().apply {
                    putParcelable(ARG_VIDEO_URI, videoUri)
                }
            }
            videoPlayerDialog.show(fragmentManager, TAG)
        }

    }
}
