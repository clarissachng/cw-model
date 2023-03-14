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

		// hold the currently possible/available moves
		private ImmutableSet<Move> moves;

		// hold the current winner(s)
		private ImmutableSet<Piece> winner;

		// calls all players
		private ImmutableSet<Player> allPlayers;

		private MyGameState(
				final GameSetup setup,
				final ImmutableSet<Piece> remaining,
				final ImmutableList<LogEntry> log,
				final Player mrX,
				final List<Player> detectives) {

			// create a list of all new players
			List<Player> newPlayers = new ArrayList<>();
			newPlayers.add(mrX);
			newPlayers.addAll(detectives);

			// initialise the local attributes that are directly supplied
			this.setup = setup;
			this.remaining = remaining;
			this.log = log;
			this.mrX = mrX;
			this.detectives = detectives;

			// initialise winner and moves
			this.winner = getWinner();
			this.moves = ImmutableSet.of();;

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
		 * @param piece the player piece
		 * @return the ticket board of the given player; empty if the player is not part of the game
		 */
		@Nonnull @Override
		public Optional<TicketBoard> getPlayerTickets(Piece piece) {
			ImmutableSet<Piece> player = getPlayers();
			if(!player.contains(piece)) return Optional.empty();

			// if the player is Mr X
			else if(piece.isMrX()) return Optional.of(ticket -> mrX.tickets().getOrDefault(ticket, 0));

			// if the player is detective
			else if(piece.isDetective())
				// get tickets of each detective
				for(Player p: detectives) {
					return Optional.of(ticket -> p.tickets().getOrDefault(ticket, 0));
				}

			return Optional.empty();
		}

		@Nonnull @Override
		public ImmutableList<LogEntry> getMrXTravelLog(){
			return log;
		}

		/**
		 * @return the winner of this game; empty if the game has no winners yet
		 * This is mutually exclusive with {@link #getAvailableMoves()}
		 * 2 cases:
		 * detective wins: when detective.location == mrX.location / mrX has no available moves
		 * Mr X wins: when detective.location == mrX.location is not fulfilled after 13 rounds /
		 *            detectives run out of tickets
		 */
		@Nonnull @Override
		public ImmutableSet<Piece> getWinner() {
//			HashSet<Piece> winner = new HashSet<>();
			for (Player detective : detectives){
				// Detective wins if MrX is captured
				if (detective.location() == mrX.location()){
					return ImmutableSet.of(detective.piece());
				}

				// Detective wins if MrX is cornered
				if (makeSingleMoves(setup, detectives, mrX, mrX.location()).isEmpty() &&
				    makeDoubleMoves(setup, detectives, mrX, mrX.location(),log).isEmpty()){
					return ImmutableSet.of(detective.piece());
				}

				// X win: when detectives has no more moves
				if (makeSingleMoves(setup, detectives, detective, detective.location()).isEmpty()){
					return ImmutableSet.of(mrX.piece());
				}
			}

			// X win: when mr x travel log is completely full
			if (getMrXTravelLog().size() == setup.moves.size()){
				return ImmutableSet.of(mrX.piece());
			}


			return ImmutableSet.of();
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

			// case for detectives
			for(Player detective: detectives) {
				if(remaining.contains(detective.piece())) {
					moves.addAll(makeSingleMoves(setup, detectives, detective, detective.location()));
				}
			}

			if(remaining.contains(mrX.piece())) {
				moves.addAll(makeSingleMoves(setup, detectives, mrX, mrX.location()));
				moves.addAll(makeDoubleMoves(setup, detectives, mrX, mrX.location(), log));
			}

//			for(Player p: allPlayers) {
//				// check if got remaining moves
//				if(remaining.contains(p.piece())) {
//					moves.addAll(makeSingleMoves(setup, detectives, p, p.location()));
//					moves.addAll(makeDoubleMoves(setup, detectives, p, p.location(), log));
//				}
//			}
			return ImmutableSet.copyOf(moves);
		}

		// return a new state from the current GameState and a provided Move
		@Nonnull @Override
		public GameState advance(Move move) {
			if(!moves.contains(move)) throw new IllegalArgumentException("Illegal move: "+move);

			List<LogEntry> updateLog = new ArrayList<>(log);
			Player currentPlayer = getCurrentPlayer(move.commencedBy());

			Move.Visitor<Player> visitor = new Move.Visitor<Player>() {
				@Override
				public Player visit(Move.SingleMove move) {
					// when the current player is Mr X
					if(currentPlayer.isMrX()) {
						// check if Mr X's location is revealed
						if(setup.moves.get(log.size())) {
							// revealed location
							updateLog.add(LogEntry.reveal(move.ticket, move.destination));
						}
						// hidden location
						else updateLog.add(LogEntry.hidden(move.ticket));
					}
					else {
						mrX = mrX.give(move.tickets());
					}
					return currentPlayer.use(move.ticket).at(move.destination);
				}

				@Override
				public Player visit(Move.DoubleMove move) {
					Player updatePlayer = currentPlayer.use(move.tickets()).at(move.destination2);

					// moving from source to destination1
					if (setup.moves.get(log.size())) {
						updateLog.add(LogEntry.reveal(move.ticket1, move.destination1));
					}
					else {
						updateLog.add(LogEntry.hidden(move.ticket1));
					}

					// moving from destination1 to destination2
					if (setup.moves.get(updateLog.size())) {
						updateLog.add(LogEntry.reveal(move.ticket2, move.destination2));
					}
					else {
						updateLog.add(LogEntry.hidden(move.ticket2));
					}
					return updatePlayer;
				}
//				Player updatePlayer = move.accept(visitor);
			};
			return new MyGameState(setup, remaining, ImmutableList.copyOf(updateLog), mrX, detectives);
		}

		/* --------------- HELPER FUNCTION   -----------*/
		// gets the player from its piece (detective/ Mr X)
		private Player getCurrentPlayer(Piece piece){
			for (Player p: allPlayers) {
				if(p.piece().equals(piece)) return p;
			}
			return null;
		}

		private static Set<Move.SingleMove> makeSingleMoves(GameSetup setup, List<Player> detectives, Player player, int source){

			// create an empty collection of some sort, say, HashSet, to store all the SingleMove we generate
			HashSet<Move.SingleMove> singleMove = new HashSet<>();
			Set<Integer> playerLocation = new HashSet<>();

			// add player's location into the set
			playerLocation.add(player.location());

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
			HashSet<Move.DoubleMove> doubleMove = new HashSet<>();
			Set<Integer> playerLocation1 = new HashSet<>();

			// add player's location into the set
			playerLocation1.add(player.location());

			// check if player has ticket1 and if the player can go to destination1
			// check if remaining log size is more than 2

			// 	REMEMBER!!!!!!!!!!!!!!!!!!!!!!!!! NEED TO GET THE REMAINING STEPS
			if (setup.moves.size() - log.size() >= 2){
				for(int destination1 : setup.graph.adjacentNodes(source)) {
					if (playerLocation1.contains(destination1)) continue;
					for (ScotlandYard.Transport t1 : setup.graph.edgeValueOrDefault(source, destination1, ImmutableSet.of())) {
						if(player.has(t1.requiredTicket())) {

							// check if player has ticket2 and if player can go to destination2
							for (int destination2 : setup.graph.adjacentNodes(source)) {
								if (playerLocation1.contains(destination2)) continue;
								for (ScotlandYard.Transport t2 : setup.graph.edgeValueOrDefault(destination1, destination2, ImmutableSet.of())) {
									if(player.has(t2.requiredTicket())) {

										// check if both ticket1 and ticket2 are the same mode of transportation
										if (t2.requiredTicket() == t1.requiredTicket()) {
											if (player.hasAtLeast(t2.requiredTicket(), 2)) {
												Move.DoubleMove move = new Move.DoubleMove(player.piece(), source, t1.requiredTicket(), destination1, t2.requiredTicket(), destination2);
												doubleMove.add(move);
											}
										} else {
											if (player.hasAtLeast(t2.requiredTicket(), 2)) {
												Move.DoubleMove move = new Move.DoubleMove(player.piece(), source, t1.requiredTicket(), destination1, t2.requiredTicket(), destination2);
												doubleMove.add(move);
											}
										}
									}
									// when player has secret ticket
									if (player.has(ScotlandYard.Ticket.SECRET)) {
										// combination of ticket1 + secret ticket
										if(player.has(t1.requiredTicket())){
											Move.DoubleMove secret = new Move.DoubleMove(player.piece(), source, t1.requiredTicket(), destination1, ScotlandYard.Ticket.SECRET, destination2);
											doubleMove.add(secret);
										}
										// combination of secret ticket + ticket2
										if(player.has(t2.requiredTicket())){
											Move.DoubleMove secret = new Move.DoubleMove(player.piece(), source, ScotlandYard.Ticket.SECRET, destination1, t2.requiredTicket(), destination2);
											doubleMove.add(secret);
										}
										// combination of 2 secret tickets
										if(player.hasAtLeast(ScotlandYard.Ticket.SECRET, 2)){
											Move.DoubleMove secret = new Move.DoubleMove(player.piece(), source, ScotlandYard.Ticket.SECRET, destination1, ScotlandYard.Ticket.SECRET, destination2);
											doubleMove.add(secret);
										}
									}
								}
							}
						}
					}
				}
			}
			// return the collection of moves
			return doubleMove;
		}
	}
}
