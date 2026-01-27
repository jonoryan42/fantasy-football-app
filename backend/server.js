const express = require("express");
const cors = require("cors");
const dotenv = require("dotenv");
const { MongoClient } = require("mongodb");
//User Auth
const bcrypt = require("bcrypt");
const jwt = require("jsonwebtoken");
const { ObjectId } = require("mongodb");

// const token = jwt.sign(
//   { userId: user._id },
//   process.env.JWT_SECRET,
//   { expiresIn: "7d" }
// );

dotenv.config();

const app = express();
app.use(cors());
app.use(express.json());

const PORT = process.env.PORT || 8080;
const DB_NAME = process.env.DB_NAME || "fantasy_football";

if (!process.env.MONGODB_URI) {
  console.error("Missing MONGODB_URI in .env");
  process.exit(1);
}

if (!process.env.JWT_SECRET) {
  console.error("Missing JWT_SECRET in .env");
  process.exit(1);
}


//Mongo Collections
let client;
let leaderboardCollection;
let playersCollection;
let gameweekStatsCollection;
let usersCollection;

const TEAM_SIZE = 15;


async function connectToMongo() {
  client = new MongoClient(process.env.MONGODB_URI);
  await client.connect();

  const db = client.db(DB_NAME);
  leaderboardCollection = db.collection("leaderboard");
  playersCollection = db.collection("players");
  gameweekStatsCollection = db.collection("player_gameweek_stats");
  usersCollection = db.collection("users");

  // Unique Indexes for the users and leaderboard for users and their teams.
  await usersCollection.createIndex({ email: 1 }, { unique: true });
  await leaderboardCollection.createIndex({ userId: 1 }, { unique: true });

  console.log(`Connected to MongoDB`);
  console.log(`   DB: ${DB_NAME}`);
  console.log(`   Collections: leaderboard, users, players, player_gameweek_stats`);
}

// Health check (easy to verify server is running)
app.get("/health", (req, res) => {
  res.json({ ok: true, service: "fantasy-football-backend" });
});

//Calculating total points for a player
function calculatePlayerPoints(p) {
  const cleanSheetPts =
    (p.position === "GK" || p.position === "DEF")
      ? (p.cleansheets || 0) * 4
      : 0;

  return (
    (p.goals || 0) * 5 +
    (p.assists || 0) * 3 +
    cleanSheetPts -
    (p.yellows || 0) * 1 -
    (p.reds || 0) * 3 -
    (p.owngoals || 0) * 2
  );
}

//User Auth Functions
function signToken(user) {
  // keep payload small
  return jwt.sign(
    { userId: user._id.toString(), email: user.email },
    process.env.JWT_SECRET,
    { expiresIn: "7d" }
  );
}

function requireAuth(req, res, next) {
  const header = req.headers.authorization || "";
  const [type, token] = header.split(" ");

  if (type !== "Bearer" || !token) {
    return res.status(401).json({ message: "Missing auth token" });
  }

  try {
    const decoded = jwt.verify(token, process.env.JWT_SECRET);
    req.auth = decoded; // { userId, email, iat, exp }
    next();
  } catch (e) {
    return res.status(401).json({ message: "Invalid or expired token" });
  }
}

//Res.statuses
//200 = Ok request succeeded
//201 = New resource created
//400 = Bad Request
//500 = Internal Server Error

//GET leaderboard with calculated points
app.get("/api/leaderboard", async (req, res) => {
  try {
    const teams = await leaderboardCollection.find({}).toArray();

    const computed = await Promise.all(
      teams.map(async (team) => {
        const playerIds = (team.playerIds || []).map(Number);

        const players = await playersCollection
          .find({ id: { $in: playerIds } })
          .toArray();

        const points = players.reduce((sum, p) => {
          const cleanSheetPts =
            (p.position === "GK" || p.position === "DEF")
              ? (p.cleansheets || 0) * 4
              : 0;

          return (
            sum +
            (p.goals || 0) * 5 +
            (p.assists || 0) * 3 +
            cleanSheetPts -
            (p.yellows || 0) * 1 -
            (p.reds || 0) * 3 -
            (p.owngoals || 0) * 2
          );
        }, 0);

        return {
          ...team,
          points,
        };
      })
    );

    computed.sort((a, b) => b.points - a.points);
    res.json(computed);
  } catch (err) {
    console.error("GET /api/leaderboard error:", err);
    res.status(500).json({ message: "Failed to fetch leaderboard" });
  }
});

//GET players (read from MongoDB)
app.get("/api/players", async (req, res) => {
  try {
    const players = await playersCollection.find({}).sort({ id: 1 }).toArray();

    const withPoints = players.map((p) => ({
      ...p,
      points: calculatePlayerPoints(p),
    }));

    res.json(withPoints);
  } catch (err) {
    console.error("GET /api/players error:", err);
    res.status(500).json({ message: "Failed to fetch players" });
  }
});

//GET player gameweek stats
app.get("/api/gameweeks/:gw", async (req, res) => {
  try {
    //convert from string to number, decimal parcing (10)
    const gw = parseInt(req.params.gw, 10);
    //current season
    const season = req.query.season || "2025";

    //convert playerIds from query string to array of integers
    const playerIds = (req.query.playerIds || "")
      .split(",")
      .map((x) => parseInt(x, 10))
      .filter((n) => Number.isInteger(n));

    if (!Number.isInteger(gw) || playerIds.length === 0) {
      return res.status(400).json({ message: "gw and playerIds are required" });
    }

    const stats = await gameweekStatsCollection
      .find({ season, gameweek: gw, playerId: { $in: playerIds } })
      .toArray();

    res.json(stats);
  } catch (err) {
    console.error("GET /api/gameweeks/:gw error:", err);
    res.status(500).json({ message: "Failed to fetch gameweek stats" });
  }
});

//GET current user
app.get("/api/auth/me", requireAuth, async (req, res) => {
  try {
    const userId = new ObjectId(req.auth.userId);
    const user = await usersCollection.findOne(
      { _id: userId },
      { projection: { passwordHash: 0 } }
    );
    if (!user) return res.status(404).json({ message: "User not found" });

    res.json(user);
  } catch (err) {
    console.error("GET /api/auth/me error:", err);
    res.status(500).json({ message: "Failed to fetch user" });
  }
});


//POST new team (save from Confirm button)
app.post("/api/leaderboard", requireAuth, async (req, res) => {
  console.log("Content-Type:", req.headers["content-type"]);
  console.log("Body:", req.body);
  console.log("playerIds type:", typeof req.body.playerIds);
  console.log("playerIds isArray:", Array.isArray(req.body.playerIds));

  try {
    let { teamName, playerIds } = req.body;
    const userId = new ObjectId(req.auth.userId);

    // --- Validate team name ---
    if (!teamName || typeof teamName !== "string" || !teamName.trim()) {
      return res.status(400).json({ message: "Team Name is required" });
    }

    // --- Validate playerIds ---
    if (!Array.isArray(playerIds) || playerIds.length !== TEAM_SIZE) {
      return res
        .status(400)
        .json({ message: `playerIds must be an array of ${TEAM_SIZE} items` });
    }

    // Force numeric IDs (handles ["1","2"] etc.)
    playerIds = playerIds.map((x) => Number(x));

    // Validate numeric + integers
    if (playerIds.some((x) => !Number.isInteger(x))) {
      return res.status(400).json({ message: "playerIds must be integers" });
    }

    // Validate no duplicates
    if (new Set(playerIds).size !== playerIds.length) {
      return res.status(400).json({ message: "playerIds must be unique" });
    }

    // Optional: friendly check (unique index still protects you)
    const existing = await leaderboardCollection.findOne({ userId });
    if (existing) {
      return res.status(409).json({ message: "User already has a team" });
    }

    const doc = {
      userId, // link to user
      teamName: teamName.trim(),
      playerIds,
      createdAt: new Date(),
      points: 0, // GET calculates anyway
    };

    const result = await leaderboardCollection.insertOne(doc);

    res.status(201).json({ ...doc, _id: result.insertedId });
  } catch (err) {
    // Duplicate key (e.g. unique index on userId)
    if (err?.code === 11000) {
      return res.status(409).json({ message: "User already has a team" });
    }

    console.error("POST /api/leaderboard error:", err);
    res.status(500).json({ message: "Failed to save team" });
  }
});

//Signup
app.post("/api/auth/register", async (req, res) => {
  try {
    let { fname, lname, email, password } = req.body;

    if (!fname || !lname || !email || !password) {
      return res.status(400).json({ message: "fname, lname, email, password are required" });
    }

    email = email.trim().toLowerCase();

    if (!email.includes("@") || password.length < 6) {
      return res.status(400).json({ message: "Invalid email or password too short" });
    }

    const passwordHash = await bcrypt.hash(password, 10);

    const doc = {
      fname: fname.trim(),
      lname: lname.trim(),
      email,
      passwordHash,
      createdAt: new Date(),
    };

    const result = await usersCollection.insertOne(doc);

    const user = { _id: result.insertedId, fname: doc.fname, lname: doc.lname, email: doc.email };
    const token = signToken(user);

    res.status(201).json({ token, user });
  } catch (err) {
    // duplicate email error
    if (err?.code === 11000) {
      return res.status(409).json({ message: "Email already registered" });
    }
    console.error("POST /api/auth/register error:", err);
    res.status(500).json({ message: "Failed to register" });
  }
});

//Login
app.post("/api/auth/login", async (req, res) => {
  try {
    let { email, password } = req.body;

    if (!email || !password) {
      return res.status(400).json({ message: "email and password are required" });
    }

    email = email.trim().toLowerCase();

    const user = await usersCollection.findOne({ email });
    if (!user) return res.status(401).json({ message: "Invalid credentials" });

    const ok = await bcrypt.compare(password, user.passwordHash);
    if (!ok) return res.status(401).json({ message: "Invalid credentials" });

    const token = signToken(user);

    res.json({
      token,
      user: { _id: user._id, fname: user.fname, lname: user.lname, email: user.email },
    });
  } catch (err) {
    console.error("POST /api/auth/login error:", err);
    res.status(500).json({ message: "Failed to login" });
  }
});

//Update current user's teamName
app.patch("/api/users/me/teamname", requireAuth, async (req, res) => {
  try {
    const { teamName } = req.body;
    if (!teamName || !teamName.trim()) {
      return res.status(400).json({ message: "teamName is required" });
    }

    const userId = new ObjectId(req.auth.userId);

    await usersCollection.updateOne(
      { _id: userId },
      { $set: { teamName: teamName.trim() } }
    );

    res.json({ ok: true, teamName: teamName.trim() });
  } catch (err) {
    console.error("PATCH /api/users/me/teamname error:", err);
    res.status(500).json({ message: "Failed to update team name" });
  }
});


connectToMongo()
  .then(() => {
    app.listen(PORT, () => {
      console.log(`API running at http://localhost:${PORT}`);
    });
  })
  .catch((err) => {
    console.error("Mongo connection failed:", err);
    process.exit(1);
  });

// Graceful shutdown
process.on("SIGINT", async () => {
  try {
    if (client) await client.close();
  } finally {
    process.exit(0);
  }
});

