package com.example.fantasyfootballapp.ui.pickTeam

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.fantasyfootballapp.R
import com.example.fantasyfootballapp.model.Player
import com.example.fantasyfootballapp.ui.common.jerseyDrawableForClub

class SlotAdapter(
    private val slots: List<PlayerSlot>,
    private val getPlayerById: (Int) -> Player?,
    private val onSlotClicked: (PlayerSlot) -> Unit,
    private val onSlotCleared: (PlayerSlot) -> Unit
) : RecyclerView.Adapter<SlotAdapter.SlotVH>() {

    inner class SlotVH(itemView: View) : RecyclerView.ViewHolder(itemView) {

        val imgAdd: ImageView = itemView.findViewById(R.id.imgAdd)
        val imgJersey: ImageView = itemView.findViewById(R.id.imgJersey)
        val txtPlayerName: TextView = itemView.findViewById(R.id.txtPlayerName)
        val txtPlayerMeta: TextView = itemView.findViewById(R.id.txtPlayerMeta)
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SlotVH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.view_player_slot, parent, false)
        return SlotVH(v)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: SlotVH, position: Int) {
        val slot = slots[position]
        val player = slot.playerId?.let(getPlayerById)

        //Reset (recycling-safe)
        holder.imgAdd.visibility = View.VISIBLE
        holder.imgJersey.visibility = View.VISIBLE
        holder.txtPlayerName.visibility = View.GONE
        holder.txtPlayerMeta.visibility = View.GONE

        holder.txtPlayerName.text = ""
        holder.txtPlayerMeta.text = ""

        if (player == null) {
            // EMPTY
            holder.imgAdd.visibility = View.VISIBLE
            holder.imgJersey.visibility = View.GONE
        } else {
            // FILLED
            holder.imgAdd.visibility = View.GONE
            holder.imgJersey.visibility = View.VISIBLE
            holder.txtPlayerName.visibility = View.VISIBLE
            holder.txtPlayerMeta.visibility = View.VISIBLE

            holder.txtPlayerName.text = player.name
            holder.txtPlayerMeta.text = "${player.club} (A)" //replace with real opponent

            //uses func for showing jersey from PlayerSlotBinder
            holder.imgJersey.setImageResource(R.drawable.bohemiansjersey)
            holder.imgJersey.visibility = View.VISIBLE
            holder.imgJersey.bringToFront()
        }

        holder.itemView.setOnClickListener { onSlotClicked(slot) }
        holder.itemView.setOnLongClickListener {
            if (player != null) { onSlotCleared(slot); true } else false
        }
    }


    override fun getItemCount(): Int = slots.size
}
