package uk.ac.bris.cs.scotlandyard.model;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import java.util.*;

import javax.annotation.Nonnull;

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

			Set<Piece> allPlayers = new HashSet<>();
			for(Player detective : detectives) {
				allPlayers.add(detective.piece());
			}
			allPlayers.add(mrX.piece());

			return (ImmutableSet<Piece>) allPlayers;
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
		 */
		@Nonnull @Override
		public ImmutableSet<Piece> getWinner() {

			return null;
		}

		/**
		 * @return the current available moves of the game.
		 * This is mutually exclusive with {@link #getWinner()}
		 */
		@Nonnull @Override
		public ImmutableSet<Move> getAvailableMoves() {
			return null;
		}



//		private static Set<SingleMove> makeSingleMoves(GameSetup setup, List<Player> detectives, Player player, int source){
//
//			// TODO create an empty collection of some sort, say, HashSet, to store all the SingleMove we generate
//
//			for(int destination : setup.graph.adjacentNodes(source)) {
//				// TODO find out if destination is occupied by a detective
//				//  if the location is occupied, don't add to the collection of moves to return
//
//				for(Transport t : setup.graph.edgeValueOrDefault(source, destination, ImmutableSet.of()) ) {
//					// TODO find out if the player has the required tickets
//					//  if it does, construct a SingleMove and add it the collection of moves to return
//				}
//
//				// TODO consider the rules of secret moves here
//				//  add moves to the destination via a secret ticket if there are any left with the player
//			}
//
//			// TODO return the collection of moves
//
//			return null;
//		}

		@Override
		public GameState advance(Move move) {
			if(!moves.contains(move)) throw new IllegalArgumentException("Illegal move: "+move);
			else return null;
		}

	}
}
