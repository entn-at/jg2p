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

package com.github.steveash.jg2p.train;

import com.github.steveash.jg2p.PhoneticEncoder;
import com.github.steveash.jg2p.PhoneticEncoderFactory;
import com.github.steveash.jg2p.align.AlignModel;
import com.github.steveash.jg2p.align.AlignerTrainer;
import com.github.steveash.jg2p.align.Alignment;
import com.github.steveash.jg2p.align.InputRecord;
import com.github.steveash.jg2p.align.TrainOptions;
import com.github.steveash.jg2p.aligntag.AlignTagModel;
import com.github.steveash.jg2p.aligntag.AlignTagTrainer;
import com.github.steveash.jg2p.seq.PhonemeCrfModel;
import com.github.steveash.jg2p.seq.PhonemeCrfTrainer;
import com.github.steveash.jg2p.seq.PhonemeHmmTrainer;

import java.util.List;

/**
 * Encoder trainer that only does a forward pass from alignment stage to phoneme classification stage and doesnt feed
 * anything back to the alignment model
 *
 * @author Steve Ash
 */
public class SimpleEncoderTrainer extends AbstractEncoderTrainer {

  private boolean useCrf = true;

  public SimpleEncoderTrainer() {
  }

  public SimpleEncoderTrainer(boolean useCrf) {
    this.useCrf = useCrf;
  }

  @Override
  public PhoneticEncoder train(List<InputRecord> inputs, TrainOptions opts) {
    AlignerTrainer alignTrainer = new AlignerTrainer(opts);
    AlignTagTrainer alignTagTrainer = new AlignTagTrainer();

    AlignModel model = alignTrainer.train(inputs);
    setAlignModel(model);

    List<Alignment> crfExamples = makeCrfExamples(inputs, model, opts);
    AlignTagModel alignTagModel = alignTagTrainer.train(crfExamples);

    PhonemeCrfModel crfModel;
    if (useCrf) {
      PhonemeCrfTrainer crfTrainer = PhonemeCrfTrainer.open(opts);
      crfTrainer.trainFor(crfExamples);
      crfModel = crfTrainer.buildModel();
    } else {
      PhonemeHmmTrainer hmmTrainer = PhonemeHmmTrainer.open(opts);
      hmmTrainer.trainFor(crfExamples);
      crfModel = hmmTrainer.buildModel();
    }
    PhoneticEncoder encoder = PhoneticEncoderFactory.make(alignTagModel, crfModel);
    encoder.setAlignModel(model);
    return encoder;
  }

}
