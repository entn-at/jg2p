/*
 * Copyright 2016 Steve Ash
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

package com.github.steveash.jg2p.syll;

import com.google.common.base.Preconditions;
import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableList;

import com.github.steveash.jg2p.Word;
import com.github.steveash.jg2p.align.Alignment;
import com.github.steveash.jg2p.aligntag.AlignTagModel;
import com.github.steveash.jg2p.seq.LeadingTrailingFeature;
import com.github.steveash.jg2p.seq.NeighborTokenFeature;
import com.github.steveash.jg2p.seq.StringListToTokenSequence;
import com.github.steveash.jg2p.seq.TokenSequenceToFeature;
import com.github.steveash.jg2p.seq.TokenWindow;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;

import javax.annotation.Nullable;

import cc.mallet.fst.CRF;
import cc.mallet.fst.CRFTrainerByThreadedLabelLikelihood;
import cc.mallet.fst.TransducerTrainer;
import cc.mallet.pipe.Pipe;
import cc.mallet.pipe.SerialPipes;
import cc.mallet.pipe.Target2LabelSequence;
import cc.mallet.pipe.TokenSequence2FeatureVectorSequence;
import cc.mallet.pipe.TokenSequenceLowercase;
import cc.mallet.types.Alphabet;
import cc.mallet.types.Instance;
import cc.mallet.types.InstanceList;
import cc.mallet.types.LabelAlphabet;

/**
 * takes a sequence of phones and learns where syllable breaks should be
 *
 * @author Steve Ash
 */
public class PhoneSyllTagTrainer {

  private static final Logger log = LoggerFactory.getLogger(PhoneSyllTagTrainer.class);

  private CRF pullFrom = null;

  public void setPullFrom(CRF pullFrom) {
    this.pullFrom = pullFrom;
  }

  public PhoneSyllTagModel train(Collection<SWord> inputs) {
    InstanceList examples = makeExamplesFromAligns(inputs);
    Pipe pipe = examples.getPipe();

    log.info("Training test-time syll phone tagger on whole data...");
    TransducerTrainer trainer = trainOnce(pipe, examples);

    return new PhoneSyllTagModel((CRF) trainer.getTransducer());
  }

  private InstanceList makeExamplesFromAligns(Collection<SWord> inputs) {
    Pipe pipe = makePipe();
    int count = 0;
    InstanceList instances = new InstanceList(pipe);
    for (SWord word : inputs) {
     Instance ii = new Instance(word, null, null, null);
      instances.addThruPipe(ii);
      count += 1;
    }
    log.info("Read {} instances of training data for syll phone tag", count);
    return instances;
  }

  private TransducerTrainer trainOnce(Pipe pipe, InstanceList trainData) {
    Stopwatch watch = Stopwatch.createStarted();

    CRF crf = new CRF(pipe, null);
//    if (pullFrom != null) {
//      crf.initializeApplicableParametersFrom(pullFrom);
//    }
    crf.addOrderNStates(trainData, new int[]{1}, null, null, null, null, false);
    crf.addStartState();

//    crf.addFullyConnectedStatesForTriLabels();

    log.info("Starting syll phone training...");
    CRFTrainerByThreadedLabelLikelihood trainer = new CRFTrainerByThreadedLabelLikelihood(crf, 8);
    trainer.setGaussianPriorVariance(2);
//    trainer.setAddNoFactors(true);
    trainer.setUseSomeUnsupportedTrick(false);
    trainer.train(trainData);
    trainer.shutdown();
    watch.stop();

    log.info("Align Tag CRF Training took " + watch.toString());
    return trainer;
  }

  private Pipe makePipe() {
    Alphabet alpha = new Alphabet();
    Target2LabelSequence labelPipe = new Target2LabelSequence();
    LabelAlphabet labelAlpha = (LabelAlphabet) labelPipe.getTargetAlphabet();

    return new SerialPipes(ImmutableList.of(
        new SWordConverterPipe(),
        new StringListToTokenSequence(alpha, labelAlpha),   // convert to token sequence
        new TokenSequenceLowercase(),                       // make all lowercase
        new NeighborTokenFeature(true, makeNeighbors()),         // grab neighboring graphemes
        new VowelNeighborPipe(),
//          new SurroundingTokenFeature(false),
//          new SurroundingTokenFeature(true),
//          new NeighborShapeFeature(true, makeShapeNeighs()),
        new IsFirstPipe(),
        new TokenSequenceToFeature(),                       // convert the strings in the text to features
        new TokenSequence2FeatureVectorSequence(alpha, true, true),
        labelPipe
    ));
  }

  private List<TokenWindow> makeNeighbors() {
      return ImmutableList.of(
          new TokenWindow(1, 1),
          new TokenWindow(2, 1),
          new TokenWindow(3, 1),
          new TokenWindow(-1, 1),
          new TokenWindow(-2, 1),
          new TokenWindow(-3, 1)
      );
    }
}