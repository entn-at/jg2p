/*
 * Copyright 2014 Steve Ash
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.steveash.jg2p.aligntag;

import com.google.common.base.Function;
import com.google.common.base.Stopwatch;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;

import com.github.steveash.jg2p.align.Alignment;
import com.github.steveash.jg2p.seq.NeighborTokenFeature;
import com.github.steveash.jg2p.seq.PhonemeCrfModel;
import com.github.steveash.jg2p.seq.SeqInputReader;
import com.github.steveash.jg2p.seq.StringListToTokenSequence;
import com.github.steveash.jg2p.seq.TokenSequenceToFeature;
import com.github.steveash.jg2p.util.ReadWrite;

import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Random;

import javax.annotation.Nullable;

import cc.mallet.fst.CRF;
import cc.mallet.fst.CRFTrainerByThreadedLabelLikelihood;
import cc.mallet.fst.TokenAccuracyEvaluator;
import cc.mallet.fst.TransducerTrainer;
import cc.mallet.pipe.Pipe;
import cc.mallet.pipe.SerialPipes;
import cc.mallet.pipe.Target2LabelSequence;
import cc.mallet.pipe.TokenSequence2FeatureVectorSequence;
import cc.mallet.pipe.TokenSequenceLowercase;
import cc.mallet.types.Alphabet;
import cc.mallet.types.CrossValidationIterator;
import cc.mallet.types.Instance;
import cc.mallet.types.InstanceList;
import cc.mallet.types.LabelAlphabet;

import static org.apache.commons.lang3.StringUtils.isBlank;

/**
 * Trains a CRF to use the alignment model
 *
 * @author Steve Ash
 */
public class AlignTagTrainer {

  private static final Logger log = LoggerFactory.getLogger(AlignTagTrainer.class);

  public void trainAndSave(List<SeqInputReader.AlignGroup> inputs) throws IOException {
    InstanceList examples = makeExamples(inputs);
    trainExamples(examples);
  }

  public PhonemeCrfModel train(List<Alignment> inputs) {
    InstanceList examples = makeExamplesFromAligns(inputs);
    Pipe pipe = examples.getPipe();

    log.info("Training on whole data...");
    TransducerTrainer trainer = trainOnce(pipe, examples);
    return new PhonemeCrfModel((CRF) trainer.getTransducer());
  }

  private void trainExamples(InstanceList examples) throws IOException {
    Pipe pipe = examples.getPipe();

    log.info("Training on whole data...");
    TransducerTrainer trainer = trainOnce(pipe, examples);
    writeModel(trainer);

    int round = 0;
    CrossValidationIterator trials = new CrossValidationIterator(examples, 4, new Random(123321123));
    SummaryStatistics overall = new SummaryStatistics();

    while (trials.hasNext()) {
      log.info("Starting training round {}...", round);
      InstanceList[] split = trials.next();
      InstanceList trainData = split[0];
      InstanceList testData = split[1];

      trainer = trainOnce(pipe, trainData);
      double thisAccuracy = evaluateOnce(round, trainData, testData, trainer);
      overall.addValue(thisAccuracy);

      round += 1;
    }

    log.info("Done! overall " + overall.getMean() + " stddev " + overall.getStandardDeviation());
  }

  private void writeModel(TransducerTrainer trainer) throws IOException {
    File file = new File("g2p_crf.dat");
    CRF crf = (CRF) trainer.getTransducer();
    ReadWrite.writeTo(new PhonemeCrfModel(crf), file);
    log.info("Wrote for whole data");
  }

  private double evaluateOnce(int round, InstanceList trainData, InstanceList testData, TransducerTrainer trainer) {
    log.info("Starting evaluation for round {}...", round);
    TokenAccuracyEvaluator teval = new TokenAccuracyEvaluator(trainData, "trainAndSave", testData, "test");
    teval.evaluate(trainer);
    double testAccuracy = teval.getAccuracy("test");
    log.info("For round {} trainAndSave {} and test {}", round, teval.getAccuracy("trainAndSave"), testAccuracy);
    return testAccuracy;
  }

  private TransducerTrainer trainOnce(Pipe pipe, InstanceList trainData) {
    Stopwatch watch = Stopwatch.createStarted();

    CRF crf = new CRF(pipe, null);
    crf.addOrderNStates(trainData, new int[]{1}, null, null, null, null, false);
    crf.addStartState();
//      crf.addFullyConnectedStatesForLabels();
    crf.setWeightsDimensionAsIn(trainData, false);

    log.info("Starting training...");
    CRFTrainerByThreadedLabelLikelihood trainer = new CRFTrainerByThreadedLabelLikelihood(crf, 8);
    trainer.setGaussianPriorVariance(2);
    trainer.train(trainData);
    trainer.shutdown();
    watch.stop();

    log.info("CRF Training took " + watch.toString());
    return trainer;
  }

  private InstanceList makeExamples(List<SeqInputReader.AlignGroup> inputs) {
    Iterable<Alignment> alignsToTrain = getAlignsFromGroup(inputs);
    return makeExamplesFromAligns(alignsToTrain);
  }

  private InstanceList makeExamplesFromAligns(Iterable<Alignment> alignsToTrain) {
    Pipe pipe = makePipe();
    int count = 0;
    InstanceList instances = new InstanceList(pipe);
    for (Alignment align : alignsToTrain) {

      List<String> phones = align.getAllYTokensAsList();
      updateEpsilons(phones);
      Instance ii = new Instance(align.getAllXTokensAsList(), phones, null, null);
      instances.addThruPipe(ii);
      count += 1;

//      if (count > 1000) {
//        break;
//      }
    }
    log.info("Read {} instances of training data", count);
    return instances;
  }

  private Iterable<Alignment> getAlignsFromGroup(List<SeqInputReader.AlignGroup> inputs) {
    return Iterables.transform(inputs, new Function<SeqInputReader.AlignGroup, Alignment>() {
      @Override
      public Alignment apply(SeqInputReader.AlignGroup input) {
        return input.alignments.get(0);
      }
    });
//    return FluentIterable.from(inputs).transformAndConcat(
//        new Function<SeqInputReader.AlignGroup, Iterable<Alignment>>() {
//          @Override
//          public Iterable<Alignment> apply(SeqInputReader.AlignGroup input) {
//            return input.alignments;
//          }
//        });
  }

  private void updateEpsilons(List<String> phones) {
    String last = "<EPS>";
    int blankCount = 0;
    for (int i = 0; i < phones.size(); i++) {
      String p = phones.get(i);
      if (isBlank(p)) {
//        phones.set(i, last + "_" + blankCount);
        phones.set(i, "<EPS>");
        blankCount += 1;
      } else {
        last = p;
        blankCount = 0;
      }
    }
  }

  private Pipe makePipe() {
    Alphabet alpha = new Alphabet();
    Target2LabelSequence labelPipe = new Target2LabelSequence();
    LabelAlphabet labelAlpha = (LabelAlphabet) labelPipe.getTargetAlphabet();

    return new SerialPipes(ImmutableList.of(
        new StringListToTokenSequence(alpha, labelAlpha),   // convert to token sequence
        new TokenSequenceLowercase(),                       // make all lowercase
        new NeighborTokenFeature(true, -2, -1, +1),         // grab neighboring graphemes
        new TokenSequenceToFeature(),                       // convert the strings in the text to features
        new TokenSequence2FeatureVectorSequence(alpha, true, true),
        labelPipe
    ));
  }
}
