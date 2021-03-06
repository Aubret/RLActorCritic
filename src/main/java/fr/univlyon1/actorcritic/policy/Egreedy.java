package fr.univlyon1.actorcritic.policy;

import fr.univlyon1.environment.space.ActionSpace;
import fr.univlyon1.learning.Informations;
import org.nd4j.linalg.api.ndarray.INDArray;

import java.util.Random;

public class Egreedy<A> implements Policy<A>{
    protected double epsilon;
    private Random random ;
    private ActionSpace<A> actionSpace ;
    private Policy greedyPolicy ;

    public Egreedy(double epsilon,long seed,ActionSpace<A> actionSpace, Policy greedyPolicy){
        this.epsilon = epsilon ;
        this.random = new Random(seed);
        this.greedyPolicy = greedyPolicy ;
        this.actionSpace = actionSpace;
    }

    @Override
    public Object getAction(INDArray results,Informations information) {
        if (this.random.nextDouble() < this.epsilon) {
            //indice = random.nextInt(results.size(1));
            return actionSpace.randomAction();
        } else{
            return this.greedyPolicy.getAction(results,information);
            //return Nd4j.argMax(results).getInt(0);
        }
    }
}
