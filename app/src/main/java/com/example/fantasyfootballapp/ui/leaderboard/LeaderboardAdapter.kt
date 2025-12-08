package com.example.fantasyfootballapp.ui.leaderboard

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.fantasyfootballapp.R
import com.example.fantasyfootballapp.model.LeaderboardEntry

class LeaderboardAdapter(
    private val entries: List<LeaderboardEntry>
) : RecyclerView.Adapter<LeaderboardAdapter.LeaderboardViewHolder>() {

    inner class LeaderboardViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val txtRank: TextView = itemView.findViewById(R.id.txtRank)
        val txtTeam: TextView = itemView.findViewById(R.id.txtUserTeam)
        val txtPoints: TextView = itemView.findViewById(R.id.txtUserPoints)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LeaderboardViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_leaderboard, parent, false)
        return LeaderboardViewHolder(view)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(
        holder: LeaderboardViewHolder,
        position: Int
    ) {
        val entry = entries[position]

        holder.txtRank.text = (position + 1).toString()
        holder.txtTeam.text = entry.user.teamName
        holder.txtPoints.text = "${entry.totalPoints} pts"
    }

    override fun getItemCount(): Int = entries.size
}
