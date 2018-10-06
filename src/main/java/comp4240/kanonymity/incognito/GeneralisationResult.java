package comp4240.kanonymity.incognito;

public class GeneralisationResult {

    private DAGNode node;
    private double fitness;

    public GeneralisationResult(DAGNode node, double fitness) {
        this.node = node;
        this.fitness = fitness;
    }

    // -- Getters --

    public DAGNode getNode() {
        return node;
    }

    public double getFitness() {
        return fitness;
    }

     // -- Overrides --

    @Override
    public String toString() {
        return String.format("DAGNode: %s, Fitness: %f", node, fitness);
    }
}
