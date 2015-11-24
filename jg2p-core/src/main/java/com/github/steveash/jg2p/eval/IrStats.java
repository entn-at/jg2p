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

package com.github.steveash.jg2p.eval;

import java.util.concurrent.atomic.AtomicLong;

/**
 * @author Steve Ash
 */
public class IrStats {

  final AtomicLong totalQueries = new AtomicLong(0);
  final AtomicLong totalReturned = new AtomicLong(0);
  final AtomicLong totalGoodReturned = new AtomicLong(0);
  final AtomicLong totalGood = new AtomicLong(0);

  void onNewQuery(int goodReturned, int returned, int good) {
    totalQueries.incrementAndGet();
    totalGoodReturned.addAndGet(goodReturned);
    totalReturned.addAndGet(returned);
    totalGood.addAndGet(good);
  }

  public double precision() {
    return ((double) totalGoodReturned.get()) / totalReturned.get();
  }

  public double recall() {
    return ((double) totalGoodReturned.get()) / totalGood.get();
  }

}
