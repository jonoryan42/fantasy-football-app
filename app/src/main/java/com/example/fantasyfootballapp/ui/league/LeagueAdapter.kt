package com.example.fantasyfootballapp.ui.league

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.fantasyfootballapp.R
import com.example.fantasyfootballapp.model.LeagueRow

class LeagueAdapter(
    private val rows: List<LeagueRow>
) : RecyclerView.Adapter<LeagueAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val pos: TextView = view.findViewById(R.id.pos)
        val team: TextView = view.findViewById(R.id.team)
        val played: TextView = view.findViewById(R.id.played)
        val wins: TextView = view.findViewById(R.id.wins)
        val draws: TextView = view.findViewById(R.id.draws)
        val losses: TextView = view.findViewById(R.id.losses)
        val points: TextView = view.findViewById(R.id.points)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_league_row, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount() = rows.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        val row = rows[position]

        holder.pos.text = row.position.toString()
        holder.team.text = row.team
        holder.played.text = row.played.toString()
        holder.wins.text = row.wins.toString()
        holder.draws.text = row.draws.toString()
        holder.losses.text = row.losses.toString()
        holder.points.text = row.points.toString()
    }
}