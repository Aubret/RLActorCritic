package fr.univlyon1.environment.interactions;

import org.nd4j.linalg.api.ndarray.INDArray;

/**
 * reward not biaised if we used a mean reward in the environement.
 * @param <A>
 */
public class CorrectedGammaInteraction<A> extends Interaction<A> {


    public CorrectedGammaInteraction(A action, INDArray observation, double gamma) {
        super(action, observation, gamma);
    }

    public CorrectedGammaInteraction(A action, INDArray observation) {
        super(action, observation);
    }

    public CorrectedGammaInteraction<A> clone() {
        CorrectedGammaInteraction<A> i = new CorrectedGammaInteraction<A>(this.getAction(), this.getObservation(), this.gamma);
        i.setSecondObservation(this.getSecondObservation());
        i.setReward(this.getReward());
        i.setId(this.getId());
        i.setIdObserver(this.getIdObserver());
        i.setTime(this.time);
        i.setDt(this.dt);
        i.setIdSecondObserver(this.idSecondObserver);
        return i;
    }

    public double getGamma() {
        return gamma;
    }

    public void setGamma(double gamma) {
        this.gamma = gamma;
    }

    public double computeReward() {
        //System.out.println(this.gamma+" "+(this.reward*this.timefactor));
        //ouble reward = sigmo.value(3*this.secondObservation.getDouble(6));
        //double reward = this.reward * Math.max(0,Math.min(1,(this.secondObservation.getDouble(6)+1)/2));
        //System.out.println(this.secondObservation.getDouble(6)+" -> "+dt);
        double reward = this.reward * (1 - Math.pow(this.gamma, this.dt));
        return reward;
    }

    public double computeGamma() {
        //this.timefactor = sigmo.value(3*this.secondObservation.getDouble(6));
        //double gam =this.gamma+(1-this.gamma)*(1- Math.max(0,Math.min(1,(this.secondObservation.getDouble(6)+1)/2)));
        double gam = Math.pow(this.gamma, this.dt);
        return gam;
    }
}
