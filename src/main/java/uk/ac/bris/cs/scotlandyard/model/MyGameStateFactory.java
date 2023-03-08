package uk.ac.bris.cs.scotlandyard.model;

import com.google.common.collect.ImmutableList;

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
		return new MyGameState(setup, ImmutableSet.of(MrX.MRX), ImmutableList.of(), mrX, detectives);
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

			// initialise the local attributes that are directly supplied
			this.setup = setup;
			this.remaining = remaining;
			this.log = log;
			this.mrX = mrX;
			this.detectives = detectives;

			// checks whether the parameters passed are not null
			if(setup.moves.isEmpty()) throw new IllegalArgumentException("Moves is empty!");
			if(setup.graph.edges().isEmpty()) throw new IllegalArgumentException("Graph is empty!");
			if(!detectives.isEmpty()) throw new IllegalArgumentException("There are no detectives!");
			if(!mrX.isMrX()) throw new IllegalArgumentException("Mr X is empty!");
			if(!mrX.piece().webColour().equals("#000")) throw new IllegalArgumentException("Mr X is not black!");

			// checks properties of players (detectives)

			// check duplicates of detective

		}

		// GETTERS

		// returns ths current game setup
		@Nonnull @Override
		public GameSetup getSetup(){ return setup; }

		//
		@Nonnull @Override
		public ImmutableSet<Piece> getPlayers() {
			return null;
		}
		@Nonnull @Override
		public ImmutableList<LogEntry> getMrXTravelLog(){
			return log;
		}
		@Override public Optional<Integer> getDetectiveLocation(Detective detective){
			// For all detectives, if Detective#piece == detective, then return the location in an Optional.of();
			return Optional.empty();
		}

		private static Set<SingleMove> makeSingleMoves(GameSetup setup, List<Player> detectives, Player player, int source){

			// TODO create an empty collection of some sort, say, HashSet, to store all the SingleMove we generate

			for(int destination : setup.graph.adjacentNodes(source)) {
				// TODO find out if destination is occupied by a detective
				//  if the location is occupied, don't add to the collection of moves to return

				for(Transport t : setup.graph.edgeValueOrDefault(source, destination, ImmutableSet.of()) ) {
					// TODO find out if the player has the required tickets
					//  if it does, construct a SingleMove and add it the collection of moves to return
				}

				// TODO consider the rules of secret moves here
				//  add moves to the destination via a secret ticket if there are any left with the player
			}

			// TODO return the collection of moves

			return null;
		}

		@Override
		public GameState advance(Move move) {
			if(!moves.contains(move)) throw new IllegalArgumentException("Illegal move: "+move);
			else return null;
		}

	}
}
