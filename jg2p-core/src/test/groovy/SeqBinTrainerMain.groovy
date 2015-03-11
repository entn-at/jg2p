import com.github.steveash.jg2p.align.InputReader
import com.github.steveash.jg2p.seq.SeqInputReader
import com.github.steveash.jg2p.seqbin.SeqBinTrainer
import com.github.steveash.jg2p.util.ReadWrite
import com.google.common.base.Charsets

import static com.google.common.io.Resources.asCharSource
import static com.google.common.io.Resources.getResource

/*
 * Copyright 2015 Steve Ash
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
 * Trains a seq bin trainer from the given input data
 * @author Steve Ash
 */
def inputFile = "g014b2b-results.train"
def training = InputReader.makeDefaultFormatReader().readFromClasspath(inputFile)
def sbt = new SeqBinTrainer()
def model = sbt.trainFor(training)
ReadWrite.writeTo(model, new File("../resources/cmu_gb_seqbin_A.dat"))
