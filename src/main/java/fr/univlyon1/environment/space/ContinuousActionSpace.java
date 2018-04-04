package fr.univlyon1.environment.space;

import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;

public class ContinuousActionSpace<A extends ContinuousAction> extends ActionSpace<A> {
    public ContinuousActionSpace() {
        super();
    }

    public void addAction(A action) {
        this.actions.add(action);
    }

    public A mapNumberToAction(Object number) {
        this.actions.get(0).constructAction((INDArray)number);
        return this.actions.get(0);
    }

    @Override
    public Object mapActionToNumber(A action) {
        return action.DeconstructAction();
    }

    @Override
    public Object randomAction() {
        return ( Nd4j.rand(1,this.getSize()).mul(2)).add(-1);
        //return null ;
    }

}