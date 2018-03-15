package main.java.fr.univlyon1.actorcritic.policy;

import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;

import java.util.Random;

public class Greedy implements Policy {

    public Greedy(){}

    @Override
    public Integer getAction(INDArray results) {
        return Nd4j.argMax(results).getInt(0);
    }
}
