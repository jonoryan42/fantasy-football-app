package com.example.fantasyfootballapp.ui.pickTeam

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.CompoundButton
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.example.fantasyfootballapp.R
import com.example.fantasyfootballapp.model.Player

class PlayerAdapter(
    private val players: List<Player>,
    private val selectedPlayerIds: MutableSet<String>,
    private val maxPlayers: Int,
    private val onSelectionChanged: (Int) -> Unit
) : RecyclerView.Adapter<PlayerAdapter.PlayerViewHolder>() {

    inner class PlayerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val txtName: TextView = itemView.findViewById(R.id.txtPlayerName)
        val txtDetails: TextView = itemView.findViewById(R.id.txtPlayerDetails)
        val cbSelected: CheckBox = itemView.findViewById(R.id.cbSelected)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlayerViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_player, parent, false)
        return PlayerViewHolder(view)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: PlayerViewHolder, position: Int) {
        val player = players[position]

        holder.txtName.text = player.name
        holder.txtDetails.text =
            "${player.position} \u2022 ${player.teamName} \u2022 ${player.points} pts"

        // avoid old listeners firing when we change checked state
        holder.cbSelected.setOnCheckedChangeListener(null)
        holder.cbSelected.isChecked = selectedPlayerIds.contains(player.id)

        // Handle toggling selection via checkbox
        holder.cbSelected.setOnCheckedChangeListener { _, isChecked ->
            val currentlySelected = selectedPlayerIds.contains(player.id)

            if (isChecked && !currentlySelected) {
                // trying to select
                if (selectedPlayerIds.size < maxPlayers) {
                    selectedPlayerIds.add(player.id)
                    onSelectionChanged(selectedPlayerIds.size)
                } else {
                    // hit max – revert checkbox and show message
                    holder.cbSelected.setOnCheckedChangeListener(null)
                    holder.cbSelected.isChecked = false
                    holder.cbSelected.setOnCheckedChangeListener(this::onCheckedChangedStub)
                    Toast.makeText(
                        holder.itemView.context,
                        "Max $maxPlayers players selected",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } else if (!isChecked && currentlySelected) {
                // deselect
                selectedPlayerIds.remove(player.id)
                onSelectionChanged(selectedPlayerIds.size)
            }
        }

        // Let tapping the row also toggle the checkbox
        holder.itemView.setOnClickListener {
            holder.cbSelected.performClick()
        }
    }

    // Small helper to re-attach a no-op listener after reverting state
    private fun onCheckedChangedStub(button: CompoundButton?, isChecked: Boolean) {
        // no-op – needed just to restore type of listener after reverting
    }

    override fun getItemCount(): Int = players.size

    fun getSelectedPlayerIds(): Set<String> = selectedPlayerIds
}