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

import com.github.steveash.jg2p.PhoneticEncoder
import com.github.steveash.jg2p.align.InputReader
import com.github.steveash.jg2p.align.Maximizer
import com.github.steveash.jg2p.align.TrainOptions
import com.github.steveash.jg2p.train.SimpleEncoderTrainer
import com.github.steveash.jg2p.util.ReadWrite
import org.slf4j.LoggerFactory

/**
 * Driver for the whole end to end training and eval process so that I can play with the alignment code to improve
 * overall performance by measuring the actual error rates for the overall process
 * @author Steve Ash
 */
def trainFile = "g014b2b.train"
def testFile = "g014b2b.test"
def train = InputReader.makePSaurusReader().readFromClasspath(trainFile)
def test = InputReader.makePSaurusReader().readFromClasspath(testFile)
def opts = new TrainOptions()
opts.maxXGram = 2
opts.maxYGram = 2
opts.onlyOneGrams = true
opts.maxPronouncerTrainingIterations = 130
opts.useWindowWalker = true
opts.includeXEpsilons = true 
opts.trainingAlignerMaximizer = Maximizer.JOINT
opts.topKAlignCandidates = 1
opts.minAlignScore = Integer.MIN_VALUE
opts.initCrfFromModelFile = "../resources/psaur_22_xEps_ww_F7_pe1.dat"
//opts.alignAllowedFile = new File("../resources/possible-aligns.txt")
def log = LoggerFactory.getLogger("psaurus")
log.info("Starting training with $trainFile and $testFile with opts $opts")

PhoneticEncoder from = ReadWrite.readFromFile(PhoneticEncoder, new File(opts.initCrfFromModelFile))

def t = new SimpleEncoderTrainer()
t.setAlignModel(from.alignModel)
t.skipAlignTrain = true

//def t = new RetaggingEncoderTrainer()
//def t = new JointEncoderTrainer()
def model = t.train(train, opts)
ReadWrite.writeTo(model, new File("../resources/psaur_22_xEps_ww_F7_pe1.dat"))

log.info("***********************************Finished*************************************")
