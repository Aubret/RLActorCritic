package fr.univlyon1.environment;

import lombok.Getter;
import org.nd4j.linalg.api.ndarray.INDArray;

import java.util.ArrayList;
import java.util.Map;

@Getter
public class HiddenState {
    protected ArrayList<Map<String, INDArray>> state;

    public HiddenState(ArrayList<Map<String, INDArray>> state){
        this.state = state ;
    }

    public int size(){
        return this.state.size() ;
    }
}