package work.lclpnet.gaco.math.solver;

/**
 * A solver for ordinary differential equations.
 */
public interface NumericalSolver {

    /**
     * Advances the state by a discrete time interval.
     * @param state The current state vector. It will be mutated so that it is the new state afterward.
     * @param dt The discrete time interval.
     * @param gradient The gradient.
     */
    void solve(StateVector state, double dt, Gradient gradient);
}
