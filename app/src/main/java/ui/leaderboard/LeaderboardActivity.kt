package ui.leaderboard

import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.fantasyfootballapp.R
import data.FantasyRepository

class LeaderboardActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_leaderboard)

        val currentUser = FantasyRepository.getCurrentUser()
        val leaderboard = FantasyRepository.getLeaderboard()

        // Just for now, show how many entries & current user team in logs
        Log.d("LeaderboardActivity", "Current user: ${currentUser.teamName}")
        Log.d("LeaderboardActivity", "Leaderboard size: ${leaderboard.size}")
    }
}
