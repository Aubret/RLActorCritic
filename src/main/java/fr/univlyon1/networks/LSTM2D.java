package fr.univlyon1.networks;

import com.sun.scenario.effect.impl.sw.sse.SSEBlend_SRC_OUTPeer;
import org.deeplearning4j.nn.conf.BackpropType;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.WorkspaceMode;
import org.deeplearning4j.nn.conf.layers.DenseLayer;
import org.deeplearning4j.nn.conf.layers.GravesLSTM;
import org.deeplearning4j.nn.conf.layers.LossLayer;
import org.deeplearning4j.nn.conf.layers.RnnOutputLayer;
import org.deeplearning4j.nn.conf.preprocessor.RnnToFeedForwardPreProcessor;
import org.deeplearning4j.nn.weights.WeightInit;
import org.deeplearning4j.optimize.api.IterationListener;
import org.deeplearning4j.optimize.api.TrainingListener;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.api.ops.impl.accum.MatchCondition;
import org.nd4j.linalg.api.ops.impl.controlflow.WhereNumpy;
import org.nd4j.linalg.api.ops.impl.indexaccum.IAMax;
import org.nd4j.linalg.api.ops.impl.indexaccum.IMax;
import org.nd4j.linalg.cpu.nativecpu.NDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.indexing.BooleanIndexing;
import org.nd4j.linalg.indexing.INDArrayIndex;
import org.nd4j.linalg.indexing.NDArrayIndex;
import org.nd4j.linalg.indexing.conditions.Condition;
import org.nd4j.linalg.indexing.conditions.Conditions;
import org.nd4j.linalg.indexing.conditions.GreaterThan;
import org.nd4j.linalg.learning.config.Sgd;
import org.nd4j.linalg.lossfunctions.LossUtil;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class LSTM2D extends LSTM {


    protected INDArray indices ;

    public LSTM2D(LSTM2D lstm, boolean listener) {
        super(lstm, listener);
    }

    public LSTM2D(int input, int output, long seed){
        super(input,output,seed);
        this.hiddenActivation = Activation.TANH ;
    }

    public void init(){
        int cursor = 0 ;
        if(this.updater ==null){
            this.updater = new Sgd(this.learning_rate);
        }
        NeuralNetConfiguration.Builder b = new NeuralNetConfiguration.Builder()
                .seed(this.seed+1)
                .trainingWorkspaceMode(WorkspaceMode.NONE)
                .inferenceWorkspaceMode(WorkspaceMode.NONE)
                //.l2(0.001)
                .weightInit(WeightInit.XAVIER)
                .updater(this.updater);
        if(l2 != null) {
            b.l2(this.l2);
        }
        NeuralNetConfiguration.ListBuilder builder = b.list() ;
        //-------------------------------------- Initialisation des couches------------------
        int node = this.numNodesPerLayer.size() >0 ? this.numNodesPerLayer.get(0) : numNodes ;
        builder.layer(cursor, new GravesLSTM.Builder()
                .activation(this.hiddenActivation)
                .units(node)
                .gateActivationFunction(Activation.SIGMOID)
                .nIn(input).nOut(output)
                .build()
        );
        cursor++;
        for (int i = 1; i < numLayers; i++){
            int previousNode = this.numNodesPerLayer.size() > i-1 ? this.numNodesPerLayer.get(i-1) : numNodes ;
            node = this.numNodesPerLayer.size() > i ? this.numNodesPerLayer.get(i) : numNodes ;
            //if(i == numLayers -1)
            builder.layer(cursor, new GravesLSTM.Builder()
                    .activation(this.hiddenActivation)
                    .units(node)
                    .nIn(output).nOut(output)
                    .build()
            );
            cursor++ ;
        }
        /*node = this.numNodesPerLayer.size() > numLayers-1 ? this.numNodesPerLayer.get(numLayers-1) : numNodes ;
        builder.layer(cursor,
                new RnnOutputLayer.Builder()
                        .lossFunction(this.lossFunction)
                        .nIn(node)
                        .nOut(output)
                        .activation(this.lastActivation)
                        .build());*/
        builder.layer(cursor, new LossLayer.Builder().lossFunction(this.lossFunction).build());
        builder.inputPreProcessor(cursor,new RnnToFeedForwardPreProcessor());

        this.multiLayerConfiguration = builder
                .backpropType(BackpropType.Standard)
                .backprop(true).pretrain(false)
                .build();

        this.model = new EpsilonMultiLayerNetwork(this.multiLayerConfiguration);
        if(this.listener)
            this.attachListener(this.model);
        this.model.init();
        this.tmp = this.model.params().dup();
    }


    public INDArray getOneResult(INDArray data){
        //this.model.setInputMiniBatchSize(data.shape()[0]);
        INDArray res = this.model.rnnTimeStep(data);
        if(data.rank() ==2)
            return res ;
        return crop3dData(res,this.maskLabel);
    }


    public INDArray getOneTrainingResult(INDArray data){
        //this.model.rnnClearPreviousState();
        for(int i = 0 ; i < this.model.getnLayers()-1 ; i++) {
            ((org.deeplearning4j.nn.layers.recurrent.GravesLSTM) this.model.getLayer(i)).rnnSetTBPTTState(new HashMap<>());
        }
        List<INDArray> workspace = this.model.rnnActivateUsingStoredState(data, true, true);
        INDArray last = workspace.get(workspace.size()-1); // Dernière couche
        return crop3dData(last,this.maskLabel);
        //return last.getRow(last.size(0)-1);
    }



    @Override
    public INDArray forwardLearn(INDArray input, INDArray labels, int number, INDArray mask, INDArray maskLabel){
        this.model.rnnClearPreviousState();
        /*for(int i = 0 ; i < this.model.getnLayers()-1 ; i++) { // On nettoie d'abord l'état de sa mémoire
            ((org.deeplearning4j.nn.layers.recurrent.GravesLSTM) this.model.getLayer(i)).rnnSetTBPTTState(new HashMap<>());
        }*/
        this.mask=mask;
        this.maskLabel = maskLabel ;
        //System.out.println(((org.deeplearning4j.nn.layers.recurrent.GravesLSTM) this.model.getLayer(0)).rnnGetTBPTTState());
        this.model.setInputMiniBatchSize(number);
        this.model.setInput(input);
        List<INDArray> workspace = this.model.rnnActivateUsingStoredState(input, true, true);

        for(IterationListener it : this.model.getListeners()){
            if(it instanceof TrainingListener){
                TrainingListener tl = (TrainingListener)it;
                tl.onForwardPass(this.model, workspace);
            }
        }
        INDArray last = workspace.get(workspace.size()-1); // Dernière couche
        return crop3dData(last,maskLabel);
    }


    public INDArray crop3dData(INDArray data,INDArray maskLabel){

        INDArray linspace = Nd4j.linspace(1,data.shape()[0],data.shape()[0]);
        INDArray indicesAndZeros = maskLabel.mul(linspace);//.reshape(data.shape()[0]);
        this.indices = BooleanIndexing.chooseFrom(new INDArray[]{indicesAndZeros},Arrays.asList(0.0), Collections.emptyList(),new GreaterThan()).addi(-1);
        INDArray newData = data.get(this.indices);
        newData = newData.reshape(newData.shape()[0],newData.shape()[2]);
        return newData;
    }

    @Override
    public Object learn(INDArray input,INDArray labels,int number) {
        return super.learn(input,this.uncrop2dData(labels,number),number);
    }

    public INDArray uncrop2dData(INDArray labels,int number){
        INDArray newLabels = Nd4j.zeros(number,this.output);
        for(int i = 0 ; i < this.indices.size(0); i++){
            INDArrayIndex[] ndi = new INDArrayIndex[]{NDArrayIndex.point(i)};
            INDArray lab = labels.get(NDArrayIndex.point(i),NDArrayIndex.all()) ;
            Double ind = this.indices.getDouble(i);
            newLabels.put(new INDArrayIndex[]{NDArrayIndex.point(ind.intValue()),NDArrayIndex.all()},lab);
        }
        //System.out.println(newLabels);
        return newLabels ;

    }


    @Override
    public StateApproximator clone(boolean listener) {
        LSTM2D m = new LSTM2D(this,listener);
        return m ;
    }
}
