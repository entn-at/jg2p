import com.github.steveash.jg2p.align.InputReader
import com.github.steveash.jg2p.align.Maximizer
import com.github.steveash.jg2p.align.TrainOptions
import com.github.steveash.jg2p.train.EncoderTrainer

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

/**
 * Driver for the whole end to end training and eval process so that I can play with the alignment code to improve
 * overall performance by measuring the actual error rates for the overall process
 * @author Steve Ash
 */
def trainFile = "cmudict.5kA.txt"
def testFile = "cmudict.5kB.txt"
def train = InputReader.makeDefaultFormatReader().readFromClasspath(trainFile)
def test = InputReader.makeDefaultFormatReader().readFromClasspath(testFile)
def opts = new TrainOptions()
opts.maxXGram = 3
opts.includeXEpsilons = false
opts.maximizer = Maximizer.JOINT

def t = new EncoderTrainer()
t.trainAndEval(train, test, opts)