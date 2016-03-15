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

package com.github.steveash.jg2p.rerank

import com.github.steveash.jg2p.Word
import com.github.steveash.jg2p.align.InputRecord
import com.github.steveash.jg2p.align.TrainOptions
import com.github.steveash.jg2p.util.CsvFactory
import com.github.steveash.jg2p.util.GroupingIterable
import com.github.steveash.jg2p.util.Percent
import com.google.common.collect.Lists
import com.google.common.collect.Ordering
import com.google.common.util.concurrent.RateLimiter
import groovyx.gpars.GParsPool
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.util.concurrent.atomic.AtomicInteger

import static org.apache.commons.lang3.StringUtils.isNotBlank

/**
 * Class that knows how to collect examples for reranker training
 * @author Steve Ash
 */
//@CompileStatic
class RerankExampleCollector {

  private static final Logger log = LoggerFactory.getLogger(RerankExampleCollector.class);

  private final TrainOptions opts;
  private final RerankableEncoder enc
  private final RateLimiter limiter = RateLimiter.create(1.0 / 5.0)
  private final AtomicInteger total = new AtomicInteger(0)
  private final AtomicInteger skipped = new AtomicInteger(0)

  RerankExampleCollector(RerankableEncoder enc, TrainOptions opts) {
    this.enc = enc
    this.opts = opts;
  }

  Collection<List<RerankExample>> makeExamples(List<InputRecord> inputs) {
    assert Ordering.natural().isOrdered(inputs)

    Iterable<List<InputRecord>> gi = GroupingIterable.groupOver(inputs, InputRecord.EqualByX)
    log.info("Collecting reranking examples from " + inputs.size() + " grouped inputs")
    List<List<RerankExample>> exs = Lists.newArrayListWithExpectedSize((0.90 * inputs.size()) as int)

    GParsPool.withPool(16) {
      gi.eachParallel { List inRecs ->
        List<InputRecord> recs = (List<InputRecord>) inRecs
        Word xWord = (Word) (((InputRecord) recs[0]).left)

        def newTotal = total.incrementAndGet()
        def rrResult = enc.encode(xWord)
        if (rrResult == null) {
          throw new IllegalStateException("enc cant return null " + enc)
        }
        if (!rrResult.isValid) {
          log.warn("Got invalid rr result for $xWord -> $rrResult")
          skipped.incrementAndGet()
          return;
        }
        def goodPhones = recs.collect { InputRecord rec -> rec.yWord.value }.toSet()
        def outs = RerankExample.makeExamples(rrResult, xWord, goodPhones)
        if (outs.every {!it.relevant}) {
          skipped.incrementAndGet()
          return;
        }

        synchronized (exs) {
          exs.add(outs)
        }

        if (limiter.tryAcquire()) {
          log.info "Completed " + total.get() + " of " + inputs.size() + " " + Percent.print(newTotal, inputs.size())
        }
      }
    }

    log.info("Finished all " + total.get() + " entries, skipped " + skipped.get() + " of them")

    synchronized (exs) {
//      if (isNotBlank(opts.writeOutputRerankExampleCsv)) {
//        writeExamples(exs)
//      }
      return exs;
    }

  }

  private void writeExamples(List<RerankExample> exs) {
    def file = makeOutputFile()
    file.withPrintWriter { pw ->
      log.info("Dumping collected examples out to disk...")
      def serial = CsvFactory.make().createSerializer()
      serial.open(pw)
      exs.each { serial.write(it) }
      serial.close(false)
      log.info("Done writing examples to $file")
    }
  }

  private File makeOutputFile() {
    if (isNotBlank(opts.writeOutputRerankExampleCsv)) {
      return new File(opts.writeOutputRerankExampleCsv)
    }
    def outFile = File.createTempFile("reranker-ex", "csv")
    outFile.deleteOnExit()
    log.info("Temporarily writing examples to " + outFile)
    return outFile
  }
}
