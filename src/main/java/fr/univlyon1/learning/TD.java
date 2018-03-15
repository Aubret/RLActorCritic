package main.java.fr.univlyon1.learning;

import main.java.fr.univlyon1.App;
import main.java.fr.univlyon1.actorcritic.policy.Greedy;
import main.java.fr.univlyon1.actorcritic.Learning;
import main.java.fr.univlyon1.environment.Interaction;
import main.java.fr.univlyon1.environment.Observation;
import main.java.fr.univlyon1.memory.ExperienceReplay;
import main.java.fr.univlyon1.networks.Approximator;
import org.apache.commons.math3.analysis.function.Sigmoid;
import org.apache.commons.math3.analysis.function.Tan;
import org.apache.commons.math3.analysis.function.Tanh;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.lossfunctions.LossFunctions;

public class TD<A> implements Algorithm<A> {
    protected Interaction<A> lastInteraction;
    protected double gamma ;
    protected Greedy policy;
    protected Learning<A> learning ;
    protected Approximator approximator ;

    public TD(double gamma,Learning<A> learning){
        this.gamma = gamma ;
        this.policy = new Greedy();
        this.learning = learning;
        this.approximator = this.learning.getApproximator() ;
    }

    @Override
    public void step(INDArray observation, A action,INDArray results) {
        this.lastInteraction = new Interaction<A>(action,observation, results);
    }

    @Override
    public void evaluate(INDArray input, Double reward) {
        if(this.lastInteraction != null) { // Avoir des interactions complètes
            this.lastInteraction.setSecondObservation(input);
            this.lastInteraction.setReward(reward);
            this.learn();
        }
    }

    protected void learn(){
        Approximator approximator = this.learning.getApproximator();
        INDArray res = this.labelize(this.lastInteraction,approximator);
        approximator.learn(this.lastInteraction.getObservation(), res,1);
    }

    /**
     * Renvoie les labels avec la formue r + lamba*maxQ
     * @param approximator
     */
    protected INDArray labelize(Interaction<A> interaction,Approximator approximator){
        INDArray results = approximator.getOneResult(interaction.getSecondObservation());// Trouve l'estimation de QValue
        Integer indice = this.learning.getActionSpace().mapActionToNumber(interaction.getAction());

        INDArray res = approximator.getOneResult(interaction.getObservation())/*.dup()*/; // résultat précédent dans lequel on change une seule qvalue
        //System.out.println(interaction.getReward() + this.gamma * results.getDouble(this.policy.getAction(results)));
        Double newValue = interaction.getReward() + this.gamma * results.getDouble(this.policy.getAction(results));
        //newValue = res.getDouble(indice)+ (new Tanh()).value(newValue - res.getDouble(indice));
        res.putScalar(indice,newValue );
        return res ;
    }

    public Approximator getApproximator(){
        return this.approximator ;
    }
}
