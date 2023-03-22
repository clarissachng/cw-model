package uk.ac.bris.cs.scotlandyard.model;

import com.google.common.collect.ImmutableList;

import javax.annotation.Nonnull;

import com.google.common.collect.ImmutableSet;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard.Factory;

import java.util.ArrayList;
import java.util.List;
import java.util.Observer;

/**
 * cw-model
 */
public final class MyModelFactory implements Factory<Model> {

	@Nonnull @Override public Model build(GameSetup setup, Player mrX, ImmutableList<Player> detectives) {
		return new Model() {

			// initialisation of the attributes
			List<Model.Observer> observerList = new ArrayList<>();
			MyGameStateFactory newFactory = new MyGameStateFactory();
			Board.GameState state = newFactory.build(setup, mrX, detectives);

			//return the current game board
			@Nonnull @Override
			public Board getCurrentBoard() {
				return state;
			}

			// Registers an observer to the model.
			// It is an error to register the same observer more than once
			@Override
			public void registerObserver(@Nonnull Observer observer) {
				if(observer == null) {
					throw new NullPointerException("Observer is null");
				}
				if(observerList.contains(observer)) {
					throw new IllegalArgumentException("Observer already registered");
				}
				observerList.add(observer);
			}

			// Unregisters an observer to the model
			// It is an error to unregister an observer not previously registered with registeredObserver
			@Override
			public void unregisterObserver(@Nonnull Observer observer) {
				if(observer == null) {
					throw new NullPointerException("Observer is null");
				}
				if(!observerList.contains(observer)) {
					throw new IllegalArgumentException("Observer is not registered");
				}

				observerList.remove(observer);
			}

			// return all currently registered observers of the model
			@Nonnull
			@Override
			public ImmutableSet<Observer> getObservers() {
				return ImmutableSet.copyOf(observerList);
			}

			// move delegates the move to the underlying
			@Override
			public void chooseMove(@Nonnull Move move) {
				// Advance the model with move
				state = state.advance(move);

				// initialise an event
				Observer.Event event;

				// get the state on whether the game is over or not
				if(state.getWinner().isEmpty()) event = Observer.Event.MOVE_MADE;
				else event = Observer.Event.GAME_OVER;

				// notify all observers of what just happened.
				for(Observer observer : observerList) {
					observer.onModelChanged(state, event);
				}
			}
		};
	}
}
