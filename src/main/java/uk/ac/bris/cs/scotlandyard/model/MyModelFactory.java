package uk.ac.bris.cs.scotlandyard.model;

import com.google.common.collect.ImmutableList;

import javax.annotation.Nonnull;

import com.google.common.collect.ImmutableSet;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard.Factory;

/**
 * cw-model
 * Stage 2: Complete this class
 */
public final class MyModelFactory implements Factory<Model> {

	@Nonnull @Override public Model build(GameSetup setup,
	                                      Player mrX,
	                                      ImmutableList<Player> detectives) {
		throw new IllegalArgumentException("Implement me");

		//return the current game board
//		@Nonnull Board getCurrentBoard();

		// Registers an observer to the model.
		// It is an error to register the same observer more than once
//		void registerObserver(@Nonnull Observer observer);

		// Unregisters an observer to the model
		// It is an error to unregister an observer not previously registered with registeredObserver
//		void unregisterObserver(@Nonnull Observer observer);

		// return all currently registered observers of the model
//		@Nonnull ImmutableSet<Model.Observer> getObservers();

		// move delegates the move to the underlying
//		void chooseMove(@Nonnull Move move);
	}
}
