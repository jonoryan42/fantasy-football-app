package com.example.fantasyfootballapp.ui.viewTeam

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.fantasyfootballapp.R
import com.example.fantasyfootballapp.model.Player

class ViewTeamPlayerAdapter(
    private val players: List<Player>
) : RecyclerView.Adapter<ViewTeamPlayerAdapter.VH>() {

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        val txtLine: TextView = view.findViewById(R.id.txtLine)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_view_team_player, parent, false)
        return VH(view)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: VH, position: Int) {
        val p = players[position]
        holder.txtLine.text = "${p.position}  •  ${p.name}  •  ${p.club}  •  ${p.points} pts"
    }

    override fun getItemCount() = players.size
}
