package com.example.fantasyfootballapp.ui.pickTeam

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.fantasyfootballapp.R
import com.example.fantasyfootballapp.model.Player

class SlotAdapter(
    private val slots: List<PlayerSlot>,
    private val getPlayerById: (Int) -> Player?,
    private val onSlotClicked: (PlayerSlot) -> Unit,
    private val onSlotCleared: (PlayerSlot) -> Unit
) : RecyclerView.Adapter<SlotAdapter.SlotVH>() {

    inner class SlotVH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val txtSlotTitle: TextView = itemView.findViewById(R.id.txtSlotTitle)
        val txtSlotPlayer: TextView = itemView.findViewById(R.id.txtSlotPlayer)
        val btnClear: ImageButton = itemView.findViewById(R.id.btnClearSlot)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SlotVH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_slot, parent, false)
        return SlotVH(v)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: SlotVH, position: Int) {
        val slot = slots[position]

        val p = slot.playerId?.let { getPlayerById(it) }

        // Show Slot 1..N for THIS list (starters: 1..11, bench: 1..4)
        holder.txtSlotTitle.text = "Slot ${position + 1}"

        holder.txtSlotPlayer.text =
            if (p == null)
                holder.itemView.context.getString(
                    R.string.empty_tap_to_pick,
                    slot.position
                )
            else
//                "${p.name} • ${p.club} • €%.1fm • ${p.gwPoints} pts".format(p.price)
                "${p.name} • ${p.club} • €%.1fm • ${p.points} pts".format(p.price)


        holder.btnClear.visibility = if (p == null) View.GONE else View.VISIBLE

        holder.itemView.setOnClickListener { onSlotClicked(slot) }
        holder.btnClear.setOnClickListener { onSlotCleared(slot) }
    }

    override fun getItemCount(): Int = slots.size
}
