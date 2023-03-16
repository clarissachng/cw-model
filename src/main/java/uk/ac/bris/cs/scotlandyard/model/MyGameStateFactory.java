package uk.ac.bris.cs.scotlandyard.model;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import java.util.*;

import javax.annotation.Nonnull;
import javax.crypto.spec.PSource;

import uk.ac.bris.cs.scotlandyard.model.Board.GameState;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard.Factory;

/**
 * cw-model
 * Stage 1: Complete this class
 */
public final class MyGameStateFactory implements Factory<GameState> {

	@Nonnull @Override
	public GameState build(GameSetup setup, Player mrX, ImmutableList<Player> detectives) {
		// return new instance of MyGameState
		return new MyGameState(setup, ImmutableSet.of(Piece.MrX.MRX), ImmutableList.of(), mrX, detectives);
	}

	// constructors
	private final class MyGameState implements GameState {
		private GameSetup setup;
		private ImmutableSet<Piece> remaining;
		// hold the travel log and count the moves Mr has taken
		private ImmutableList<LogEntry> log;
		// hold the MrX player
		private Player mrX;
		// hold the detectives
		private List<Player> detectives;
		// calls all players
		private ImmutableList<Player> allPlayers;
		// hold the currently possible/available moves
		private ImmutableSet<Move> moves;
		// hold the current winner(s)
		private ImmutableSet<Piece> winner;



		private MyGameState(
				final GameSetup setup,
				final ImmutableSet<Piece> remaining,
				final ImmutableList<LogEntry> log,
				final Player mrX,
				final List<Player> detectives) {

			// create a list of all new players
			List<Player> allPlayers = new ArrayList<>();
			allPlayers.add(mrX);
			allPlayers.addAll(detectives);

			// initialise the local attributes that are directly supplied
			this.setup = setup;
			this.remaining = remaining;
			this.log = log;
			this.mrX = mrX;
			this.detectives = detectives;
			this.allPlayers = ImmutableList.copyOf(allPlayers);

			// initialise winner and moves
			this.winner = getWinner();
			this.moves = getAvailableMoves();

			// checks whether the parameters passed are not null
			if(setup.moves.isEmpty()) throw new IllegalArgumentException("Moves is empty!");
			if(setup.graph.edges().isEmpty()) throw new IllegalArgumentException("Graph is empty!");
			if(detectives.isEmpty()) throw new IllegalArgumentException("There are no detectives!");
			if(!mrX.isMrX()) throw new IllegalArgumentException("Mr X is empty!");
			if(!mrX.piece().webColour().equals("#000")) throw new IllegalArgumentException("Mr X is not black!");

			// checks properties of players (detectives)
			for(Player detective: detectives) {
				// check if the player is a detective
				if(!detective.isDetective())
					throw new IllegalArgumentException("No detectives are in!");
				// check if the detectives have secret and double ticket
				if(detective.has(ScotlandYard.Ticket.SECRET))
					throw new IllegalArgumentException("No detective should have secret ticket!");
				if(detective.has(ScotlandYard.Ticket.DOUBLE))
					throw new IllegalArgumentException("No detective should have double ticket!");
			}

			// check duplicates of detective
			for(int i = 0; i < detectives.size(); i++) {
				for(int j = i + 1; j < detectives.size(); j++) {
					// check if duplicates exist
					if (detectives.get(i).piece().equals(detectives.get(j).piece()))
						throw new IllegalArgumentException("Duplicate detectives found!");
					// check if detectives are at the same location
					if (detectives.get(i).location() == detectives.get(j).location())
						throw new IllegalArgumentException("Detectives at same location!");
				}
			}
		}

		// GETTERS

		// returns ths current game setup
		@Nonnull @Override
		public GameSetup getSetup(){ return setup; }

		// return all players in the game
		@Nonnull @Override
		public ImmutableSet<Piece> getPlayers() {

			Set<Piece> players = new HashSet<>();
			for(Player detective : detectives) {
				players.add(detective.piece());
			}
			players.add(mrX.piece());

			return ImmutableSet.copyOf(players);
		}

		// get location of detective
		@Nonnull @Override
		public Optional<Integer> getDetectiveLocation(Piece.Detective detective){
			// For all detectives, if Detective#piece == detective, then return the location in an Optional.of();
			for (Player player: detectives) {
				if(player.piece().equals(detective)) return Optional.of(player.location());
			}
			return Optional.empty();
		}

		/**
		 * piece the player piece
		 * the ticket board of the given player; empty if the player is not part of the game
		 */
		@Nonnull @Override
		public Optional<TicketBoard> getPlayerTickets(Piece piece) {
			ImmutableSet<Piece> player = getPlayers();

			// if the piece is not in the player set
			if(!player.contains(piece)) return Optional.empty();

			// if the player is Mr X
			if(piece.isMrX()) return Optional.of(ticket -> mrX.tickets().getOrDefault(ticket, 0));
			// if the player is detective
			else if(piece.isDetective())
				// get tickets of each detective
				for(Player p: detectives) {
					// check if the player is the current piece
					if (p.piece() == piece) {
						return Optional.of(ticket -> p.tickets().getOrDefault(ticket, 0));
					}
				}

			return Optional.empty();
		}

		@Nonnull @Override
		public ImmutableList<LogEntry> getMrXTravelLog(){
			return log;
		}

		/**
		 * return the winner of this game; empty if the game has no winners yet
		 * This is mutually exclusive with {@link #getAvailableMoves()}
		 * 2 cases:
		 * detective wins: when detective.location == mrX.location / mrX has no available moves
		 * Mr X wins: when detective.location == mrX.location is not fulfilled after 13 rounds /
		 *            detectives run out of tickets
		 */
		@Nonnull @Override
		public ImmutableSet<Piece> getWinner() {
			// initialise empty list to store all detectives
			// Why: when we return, we will return the whole list
			// when one detective wins, ALL detectives win
			List<Piece> detectiveWinner = new ArrayList<>();
			List<Integer> detectivesLocation = new ArrayList<>();

			for (Player detective : detectives) {
				// add all the detective pieces and location into the list
				detectiveWinner.add(detective.piece());
				detectivesLocation.add(detective.location());
			}

			for (Player detective : detectives) {
				// Detective wins if MrX is captured
				if (detective.location() == mrX.location()){
					return ImmutableSet.copyOf(detectiveWinner);
				}
			}

			// mr x wins when detectives has no more moves
			if (noDetectiveHasMove(detectives)){
				return ImmutableSet.of(mrX.piece());
			}

			// Detective wins if MrX is cornered
			if (makeSingleMoves(setup, detectives, mrX, mrX.location()).isEmpty() &&
					makeDoubleMoves(setup, detectives, mrX, mrX.location(),log).isEmpty() &&
					remaining.contains(mrX.piece())){
				return ImmutableSet.copyOf(detectiveWinner);
			}

			// mr x wins when mr x travel log is completely full
			if (getMrXTravelLog().size() == setup.moves.size() &&
					remaining.contains(mrX.piece())){
				return ImmutableSet.of(mrX.piece());
			}
			return ImmutableSet.of();
		}

		public boolean noDetectiveHasMove(List<Player> detectives){
			for (Player detective : detectives){
				// check if there are available moves for any of the detectives
				if (!makeSingleMoves(setup, detectives, detective, detective.location()).isEmpty()){
					return false;
				}
			}
			return true;
		}

		/**
		 * return the current available moves of the game.
		 * This is mutually exclusive with {@link #getWinner()}
		 */
		@Nonnull @Override
		public ImmutableSet<Move> getAvailableMoves() {
			// initialise an empty set to store the moves
			HashSet<Move> moves = new HashSet<>();

			// if there's a winner, there will be no available moves
			if (!getWinner().isEmpty()) {
				return ImmutableSet.of();
			}
			// case for mr x
			if(remaining.contains(mrX.piece())) {
				moves.addAll(makeSingleMoves(setup, detectives, mrX, mrX.location()));
				moves.addAll(makeDoubleMoves(setup, detectives, mrX, mrX.location(), log));
			}

			// case for detectives
			for(Player detective: detectives) {
				if(remaining.contains(detective.piece())) {
					moves.addAll(makeSingleMoves(setup, detectives, detective, detective.location()));
				}
			}
			return ImmutableSet.copyOf(moves);
		}

		// return a new state from the current GameState and a provided Move
		@Nonnull @Override
		public GameState advance(Move move) {
			if(!moves.contains(move)) throw new IllegalArgumentException("Illegal move: "+move);
			List<LogEntry> updatedLog = new ArrayList<>(log);
			List<Player> updatedDetectives = new ArrayList<>();
			Player updatedMrX;
			List<Piece> oldRemaining = new ArrayList<>(remaining);
			List<Piece> updatedRemaining = new ArrayList<>();

			Move.Visitor<Player> visitor = new Move.Visitor<>() {

				Player currentPlayer = getCurrentPlayer(move.commencedBy());
				@Override
				public Player visit(Move.SingleMove move) {
					currentPlayer.use(move.ticket).at(move.destination);
					// when the current player is Mr X
					if(currentPlayer.isMrX()) {
						// check if mr x's moves are revealed
						if(setup.moves.get(log.size())) {
							// revealed moves
							updatedLog.add(LogEntry.reveal(move.ticket, move.destination));
						}
						// hidden moves
						else updatedLog.add(LogEntry.hidden(move.ticket));
					}
					else {
						// give mr x with new ticket from detectives
						mrX = mrX.give(move.tickets());
					}
					return currentPlayer;
				}

				@Override
				public Player visit(Move.DoubleMove move) {
					currentPlayer.use(move.tickets()).at(move.destination2);

					// moving from source to destination1
					if (setup.moves.get(log.size())) {
						updatedLog.add(LogEntry.reveal(move.ticket1, move.destination1));
					}
					else {
						updatedLog.add(LogEntry.hidden(move.ticket1));
					}

					// moving from destination1 to destination2
					if (setup.moves.get(updatedLog.size())) {
						updatedLog.add(LogEntry.reveal(move.ticket2, move.destination2));
					}
					else {
						updatedLog.add(LogEntry.hidden(move.ticket2));
					}
					return currentPlayer;
				}
			};

			Player newPlayer = move.accept(visitor);

			// only update newDetectives if newPlayer is a detective
			for (Player p : detectives) {
				if (p.piece() == newPlayer.piece()) {
					updatedDetectives.add(newPlayer);
				} else {
					updatedDetectives.add(p);
				}
			}

			// exclude the piece moved for remaining
			for (Piece p : remaining) {
				if (p != newPlayer.piece()) {
					updatedRemaining.add(p);
				}
			}

			// dont understand
			if (newPlayer.isMrX()) {
				updatedMrX = newPlayer;
			} else {
				updatedMrX = mrX;
			}

			//  remaining pieces in play for the current round
			if(!move.commencedBy().isMrX()) {
				for(Piece player : oldRemaining) {
					if(player != move.commencedBy()) {
						updatedRemaining.add(player);
					}
				}
			}
			//  (and if none remain an initialisation of players for the next round).
			else {
				for(Player everyone : allPlayers) {
					updatedRemaining.add(everyone.piece());
				}
			}

			// dont understand
			for(Piece p : remaining) {
				Player player = getCurrentPlayer(p);
				if(p.isMrX()) {
                    /* The game is not over if MrX is cornered, but he can still escape
                       can scape using a double move, or secret */
					if(makeSingleMoves(setup, updatedDetectives, player, player.location()).isEmpty()
							&& !makeDoubleMoves(setup, updatedDetectives, updatedMrX, updatedMrX.location(), ImmutableList.copyOf(updatedLog)).isEmpty() ) {
						updatedRemaining.add(updatedMrX.piece());
					}
				} else {
					// check if a detective still has moves
					if(makeSingleMoves(setup, updatedDetectives, player, player.location()).isEmpty()) {
						updatedRemaining.add(updatedMrX.piece());
					}
				}
			}

			if (updatedRemaining.isEmpty()) {
				updatedRemaining.add(updatedMrX.piece());
			}

			return new MyGameState(setup, ImmutableSet.copyOf(updatedRemaining), ImmutableList.copyOf(updatedLog), mrX, detectives);
		}

		/* --------------- HELPER FUNCTIONS   ----------- */

		// gets the player from its piece (detective/ Mr X)
		private Player getCurrentPlayer(Piece piece){
			for (Player p: allPlayers) {
				if(p.piece().equals(piece)) return p;
			}
			return null;
		}

		public static Integer getDestination(Move m) {
			return m.accept(new Move.Visitor<>() {

				@Override
				public Integer visit(Move.SingleMove move) {
					return move.destination;
				}

				@Override
				public Integer visit(Move.DoubleMove move) {
					return move.destination2;
				}
			});
		}

		private static Set<Move.SingleMove> makeSingleMoves(GameSetup setup, List<Player> detectives, Player player, int source){

			// create an empty collection of some sort, say, HashSet, to store all the SingleMove we generate
			Set<Integer> playerLocation = new HashSet<>();
			Set<Move.SingleMove> singleMove = new HashSet<>();

			// add player's location into the set
			for(Player detective: detectives) playerLocation.add(detective.location());

			for(int destination : setup.graph.adjacentNodes(source)) {
				// find out if destination is occupied by a detective
				//  if the location is occupied, don't add to the collection of moves to return
				if (playerLocation.contains(destination)) continue;

				for(ScotlandYard.Transport t : setup.graph.edgeValueOrDefault(source, destination, ImmutableSet.of()) ) {
					// find out if the player has the required tickets
					//  if it does, construct a SingleMove and add it the collection of moves to return
					if(player.has(t.requiredTicket())) {
						Move.SingleMove move = new Move.SingleMove(player.piece(), source, t.requiredTicket(), destination);
						singleMove.add(move);
					}
				}

				// consider the rules of secret moves here
				// add moves to the destination via a secret ticket if there are any left with the player
				if(player.has(ScotlandYard.Ticket.SECRET)) {
					Move.SingleMove secret = new Move.SingleMove(player.piece(), source, ScotlandYard.Ticket.SECRET, destination);
					singleMove.add(secret);
				}
			}
			// return the collection of moves
			return singleMove;
		}

		private static Set<Move.DoubleMove> makeDoubleMoves(GameSetup setup, List<Player> detectives, Player player, int source, ImmutableList<LogEntry> log){

			// create an empty collection of some sort, say, HashSet, to store all the DoubleMove we generate
			Set<Move.DoubleMove> doubleMove = new HashSet<>();
			Set<Move.SingleMove> singleMove = makeSingleMoves(setup, detectives, player, source);

			// integer to store available moves
			int availableMoves = setup.moves.size() - log.size();

			// check if available moves >= 2 and if player has double tix
			if (player.has(ScotlandYard.Ticket.DOUBLE) && (availableMoves >= 2)){
				for(Move.SingleMove move1: singleMove) {
					// player used a tix -> ticket value - 1
					int destination1 = getDestination(move1);
					Player tempPlayer =  player.use(move1.ticket).at(destination1);
					Set<Move.SingleMove> secondMove = makeSingleMoves(setup, detectives, tempPlayer, destination1);

					// player making the second move
					for(Move.SingleMove move2: secondMove) {
						int destination2 = getDestination(move2);
						Move.DoubleMove double1 = new Move.DoubleMove(player.piece(), source, move1.ticket, destination1, move2.ticket, destination2);
						doubleMove.add(double1);
					}
				}
			}
//			// if player does not have enough ticket
//			if()
			// return the collection of moves
			return doubleMove;
		}
	}
}
