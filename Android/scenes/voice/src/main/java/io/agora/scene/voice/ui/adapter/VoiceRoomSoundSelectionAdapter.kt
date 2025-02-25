package io.agora.scene.voice.ui.adapter

import android.view.View
import io.agora.scene.base.component.BaseRecyclerViewAdapter
import io.agora.scene.base.component.OnItemClickListener
import io.agora.voice.common.utils.ResourcesTools
import io.agora.scene.voice.model.SoundSelectionBean
import io.agora.scene.voice.R
import io.agora.scene.voice.databinding.VoiceItemSoundSelectionBinding

class VoiceRoomSoundSelectionAdapter constructor(
    dataList: List<SoundSelectionBean>,
    listener: OnItemClickListener<SoundSelectionBean>?,
    viewHolderClass: Class<SoundSelectViewHolder>
) : BaseRecyclerViewAdapter<VoiceItemSoundSelectionBinding, SoundSelectionBean, VoiceRoomSoundSelectionAdapter.SoundSelectViewHolder>(
    dataList, listener, viewHolderClass
) {

    init {
        selectedIndex = 0
    }

    fun setSelectedPosition(position: Int) {
        selectedIndex = position
        notifyDataSetChanged()
    }

    class SoundSelectViewHolder constructor(private val binding: VoiceItemSoundSelectionBinding) :
        BaseViewHolder<VoiceItemSoundSelectionBinding, SoundSelectionBean>(binding) {
        override fun binding(soundSelectionBean: SoundSelectionBean?, selectedIndex: Int) {
            soundSelectionBean?.let {
                setData( it, selectedIndex)
            }
        }

        private fun setData(bean: SoundSelectionBean, selectedPosition: Int) {
            binding.soundName.text = bean.soundName
            binding.soundDesc.text = bean.soundIntroduce
            val context = binding.item.context
            if (selectedPosition == bindingAdapterPosition) {
                binding.ivSoundSelected.visibility = View.VISIBLE
                binding.mcvSoundSelectionContent.strokeColor =
                    ResourcesTools.getColor(context.resources, R.color.voice_color_009fff, null)
            } else {
                binding.ivSoundSelected.visibility = View.GONE
                binding.mcvSoundSelectionContent.strokeColor =
                    ResourcesTools.getColor(context.resources, R.color.voice_color_d8d8d8, null)
            }
        }
    }
}